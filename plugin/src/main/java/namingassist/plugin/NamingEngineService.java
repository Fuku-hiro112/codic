package namingassist.plugin;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import namingassist.engine.NamingEngine;
import namingassist.engine.NamingEngines;
import namingassist.engine.dictionary.Dictionary;

/**
 * {@link NamingEngine} を 1 つだけ持つアプリケーションサービス。組み立て(辞書の読み込み)は
 * 初回だけで、バックグラウンドで行う。利用者の層を差し替えると組み立て直す。
 */
@Service(Service.Level.APP)
public final class NamingEngineService {
    private static final Dictionary EMPTY_USER_LAYER = word -> Optional.empty();

    private CompletableFuture<NamingEngine> loading;

    /** まだなら組み立てを始める(待たない)。 */
    public synchronized CompletableFuture<NamingEngine> preload() {
        if (loading == null) {
            loading = build(EMPTY_USER_LAYER);
        }
        return loading;
    }

    /** 利用者の層を差し替えて組み立て直す。 */
    public synchronized void replaceUserLayer(Dictionary userLayer) {
        loading = build(userLayer);
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

    private static CompletableFuture<NamingEngine> build(Dictionary userLayer) {
        return CompletableFuture.supplyAsync(
            () -> NamingEngines.createStandard(userLayer), AppExecutorUtil.getAppExecutorService());
    }
}
