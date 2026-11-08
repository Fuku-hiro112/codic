package namingassist.plugin;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.util.List;
import namingassist.engine.NamingKind;

/** 日本語の句から、本物の engine と同梱辞書を通した英語の候補が補完に出ること。 */
public class JapaneseCompletionTest extends BasePlatformTestCase {

    public void testJapanesePhraseAtVariableNameGivesOperationTime() {
        myFixture.configureByText("A.java", "class A { void f() { int <caret> } }");
        myFixture.type("稼働時刻");
        LookupElement[] elements = myFixture.completeBasic();

        List<String> fromPlugin = PluginLookups.fromPlugin(elements);
        System.out.println("[JP-VARNAME] all=" + PluginLookups.all(elements) + " fromPlugin=" + fromPlugin);
        assertTrue("候補に operationTime が出ること: " + fromPlugin, fromPlugin.contains("operationTime"));
    }

    /** 30 文字の句で候補生成が 1 秒以内(閾値は仮置き)。engine は初期化済みの状態で測る。 */
    public void testLongPhraseDoesNotBlock() {
        String phrase = "顧客の注文の稼働時刻と終了時刻を取得して画面に表示する処理群";
        assertEquals(30, phrase.codePointCount(0, phrase.length()));
        NamingEngineService service = ApplicationManager.getApplication().getService(NamingEngineService.class);
        service.engine().suggest("稼働時刻", NamingKind.VARIABLE); // 初期化(辞書の読み込み)を済ませる

        long engineStart = System.nanoTime();
        int engineCount = service.engine().suggest(phrase, NamingKind.VARIABLE).size();
        long engineMillis = (System.nanoTime() - engineStart) / 1_000_000;

        myFixture.configureByText("A.java", "class A { void f() { int " + phrase + "<caret> } }");
        long completionStart = System.nanoTime();
        LookupElement[] elements = myFixture.completeBasic();
        long completionMillis = (System.nanoTime() - completionStart) / 1_000_000;

        System.out.println("[LONG] phrase=" + phrase + " engine=" + engineMillis + "ms (" + engineCount
            + " candidates) completeBasic=" + completionMillis + "ms fromPlugin=" + PluginLookups.fromPlugin(elements));
        assertTrue("engine の候補生成が 1 秒以内: " + engineMillis + "ms", engineMillis < 1000);
        assertTrue("補完全体が 1 秒以内: " + completionMillis + "ms", completionMillis < 1000);
    }
}
