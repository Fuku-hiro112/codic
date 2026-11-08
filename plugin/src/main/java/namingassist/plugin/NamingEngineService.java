package namingassist.plugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import namingassist.engine.NamingEngine;
import namingassist.engine.NamingEngines;
import namingassist.engine.dictionary.Dictionary;
import namingassist.engine.dictionary.UserDictionary;
import namingassist.plugin.settings.UserDictionaryStore;

/**
 * {@link NamingEngine} を 1 つだけ持つアプリケーションサービス。組み立て(辞書の読み込み)は
 * 初回だけで、バックグラウンドで行う。利用者の層を差し替えると組み立て直す。
 */
@Service(Service.Level.APP)
public final class NamingEngineService {
    private CompletableFuture<NamingEngine> loading;

    public static NamingEngineService getInstance() {
        return ApplicationManager.getApplication().getService(NamingEngineService.class);
    }

    /**
     * まだなら組み立てを始める(待たない)。利用者の層は設定フォルダに保存済みの利用者辞書。
     * 無い・壊れているときは空の層になる({@link UserDictionary} は例外を投げない)ので、同梱辞書だけで動く。
     */
    public synchronized CompletableFuture<NamingEngine> preload() {
        if (loading == null) {
            loading = build(() -> new UserDictionary(UserDictionaryStore.defaultFile()));
        }
        return loading;
    }

    /** 利用者の層を差し替えて組み立て直す。 */
    public synchronized void replaceUserLayer(Dictionary userLayer) {
        loading = build(() -> userLayer);
    }

    /** 組み立て済みの engine。初回は組み立てを待つ(補完が取り消されたら待つのをやめる)。 */
    public NamingEngine engine() {
        CompletableFuture<NamingEngine> future = preload();
        while (true) {
            ProgressManager.checkCanceled();
            try {
                return future.get(20, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // 取り消しを確かめてから待ち直す
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("命名エンジンの組み立てを待つ間に割り込まれた", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("命名エンジンを組み立てられない", e.getCause());
            }
        }
    }

    private static CompletableFuture<NamingEngine> build(Supplier<Dictionary> userLayer) {
        return CompletableFuture.supplyAsync(
            () -> NamingEngines.createStandard(userLayer.get()), AppExecutorUtil.getAppExecutorService());
    }
}
