package namingassist.plugin;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.util.List;
import java.util.function.Supplier;
import namingassist.engine.NamingEngine;
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

    /** Lookup に並ぶ、本プラグイン由来の候補(Lookup の表示順のまま)。 */
    private List<String> pluginItemsInLookupOrder(String phrase) {
        myFixture.configureByText("A.java", "class A { void f() { int <caret> } }");
        myFixture.type(phrase);
        myFixture.completeBasic();
        LookupImpl lookup = (LookupImpl) myFixture.getLookup();
        assertNotNull("Lookup が出ること", lookup);
        return PluginLookups.fromPlugin(lookup.getItems().toArray(LookupElement[]::new));
    }

    /** engine の優先順が Lookup の並びに保たれること(辞書順に落ちない)。 */
    public void testLookupKeepsEnginePriorityOrder() {
        List<String> operation = pluginItemsInLookupOrder("稼働時刻");
        System.out.println("[ORDER] 稼働時刻=" + operation);
        assertEquals("先頭が operationTime: " + operation, "operationTime", operation.get(0));
        assertTrue("10 件以下: " + operation.size(), operation.size() <= 10);

        List<String> pageTitle = pluginItemsInLookupOrder("ページタイトルを取得する");
        System.out.println("[ORDER] ページタイトルを取得する=" + pageTitle);
        assertEquals("先頭が getPageTitle: " + pageTitle, "getPageTitle", pageTitle.get(0));
        int fetch = pageTitle.indexOf("fetchPageTitle");
        assertTrue("fetchPageTitle があり、getPageTitle より後: " + pageTitle,
            fetch >= 0 && fetch > pageTitle.indexOf("getPageTitle"));
        assertTrue("10 件以下: " + pageTitle.size(), pageTitle.size() <= 10);
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

    /**
     * 組み合わせが爆発しうる句(長い句・先頭が数字の句・ハイフンの訳しかない語)でも、補完が 2 秒以内に終わり、
     * 候補が出ること(最終レビュー Critical-1 の反例)。
     */
    public void testHardPhrasesCompleteInTimeWithCandidates() {
        NamingEngineService.getInstance().engine().suggest("稼働時刻", NamingKind.VARIABLE); // 初期化を済ませる
        for (String phrase : List.of(
                "2次元配列の要素数の最大値の平均値の合計値の表示位置",
                "最終更新のタイムスタンプの表示位置の初期値の平均値",
                "2段階認証の有効期限",
                "内蔵カメラ")) {
            myFixture.configureByText("A.java", "class A { void f() { int " + phrase + "<caret> } }");
            long start = System.nanoTime();
            LookupElement[] elements = myFixture.completeBasic();
            long millis = (System.nanoTime() - start) / 1_000_000;
            String text = myFixture.getEditor().getDocument().getText();
            List<String> fromPlugin = PluginLookups.fromPlugin(elements);
            System.out.println("[HARD] " + phrase + " " + millis + "ms fromPlugin=" + fromPlugin + " text=" + text);
            assertTrue(phrase + " の補完が 2 秒以内: " + millis + "ms", millis < 2000);
            // 候補が 1 件だけなら completeBasic は Lookup を出さずに挿入する(elements は null)
            assertTrue(phrase + " の候補が出ること", !fromPlugin.isEmpty() || (elements == null && !text.contains(phrase)));
        }
    }

    /** 補完が取り消されたら、候補生成は ProcessCanceledException で止まる(取り消しを engine へ渡している)。 */
    public void testSuggestionStopsWhenCompletionIsCanceled() {
        NamingEngine engine = NamingEngineService.getInstance().engine();
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        try {
            ProgressManager.getInstance().runProcess(() -> {
                indicator.cancel();
                JapaneseNamingContributor.suggest(() -> engine, "2次元配列の要素数の最大値の平均値", NamingKind.VARIABLE);
                fail("取り消し後に候補生成が最後まで進んだ");
            }, indicator);
            fail("ProcessCanceledException が投げられること");
        } catch (ProcessCanceledException expected) {
            // 期待どおり
        }
    }

    /** engine を組み立てられないとき、補完へ例外を漏らさず、候補なしで返す(何度呼んでも)。 */
    public void testEngineFailureGivesNoCandidates() {
        Supplier<NamingEngine> broken = () -> {
            throw new IllegalStateException("命名エンジンを組み立てられない(テスト)");
        };
        assertEquals(List.of(), JapaneseNamingContributor.suggest(broken, "稼働時刻", NamingKind.VARIABLE));
        assertEquals(List.of(), JapaneseNamingContributor.suggest(broken, "稼働時刻", NamingKind.VARIABLE));
    }
}
