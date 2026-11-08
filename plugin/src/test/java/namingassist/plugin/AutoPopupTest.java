package namingassist.plugin;

import com.intellij.codeInsight.completion.JavaCompletionAutoPopupTestCase;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.testFramework.EdtTestUtil;
import java.util.List;

/** 日本語を打った直後に、補完を呼ばなくても Lookup が出ること。 */
public class AutoPopupTest extends JavaCompletionAutoPopupTestCase {

    public void testLookupAppearsRightAfterJapaneseTyped() {
        myFixture.configureByText("A.java", "class A { void f() { int <caret> } }");
        type("稼働時刻"); // 1 文字ずつ打つ。「稼」だけでは候補が無いので、打ち足しで作り直されること
        LookupImpl lookup = getLookup();
        List<String> fromPlugin = lookup == null ? List.of()
            : PluginLookups.fromPlugin(lookup.getItems().toArray(LookupElement[]::new));
        System.out.println("[AUTOPOPUP] lookup=" + (lookup == null ? "null" : fromPlugin));
        assertNotNull("補完を呼ばなくても Lookup が出ること", lookup);
        assertTrue("自動ポップアップに operationTime: " + fromPlugin, fromPlugin.contains("operationTime"));
    }

    /** 設計書 §1 の観測の再現。assert はせず、4 か所の挙動を表にして標準出力へ出す(事実の記録)。 */
    public void testContextObservation() {
        String[][] places = {
            {"コメント内", "class A { void f() { // <caret>\n } }"},
            {"文字列内", "class A { void f() { String s = \"<caret>\"; } }"},
            {"@Scheduled(cron = <ここ>)", "class A { @Scheduled(cron = <caret>) void f() { } }"},
            {"@Scheduled(cron = \"<ここ>\")(参考: 文字列内)", "class A { @Scheduled(cron = \"<caret>\") void f() { } }"},
            {"フィールド名", "class A { private int <caret>; }"},
        };
        StringBuilder table = new StringBuilder();
        table.append("[CTX-TABLE] 入力「稼働時刻」\n");
        table.append("| 位置 | 自動ポップアップ(type 後) | 手動補完(completeBasic)の本プラグイン候補 | 手動補完の全候補 |\n");
        table.append("|---|---|---|---|\n");
        for (String[] place : places) {
            myFixture.configureByText("A.java", place[1]);
            type("稼働時刻");
            LookupImpl popup = getLookup();
            String auto = popup == null ? "出ない" : "出る " + PluginLookups.fromPlugin(
                popup.getItems().toArray(LookupElement[]::new));

            myFixture.configureByText("A.java", place[1].replace("<caret>", "稼働時刻<caret>"));
            LookupElement[][] manual = new LookupElement[1][];
            EdtTestUtil.runInEdtAndWait(() -> manual[0] = myFixture.completeBasic());
            table.append("| ").append(place[0])
                .append(" | ").append(auto)
                .append(" | ").append(PluginLookups.fromPlugin(manual[0]))
                .append(" | ").append(PluginLookups.all(manual[0]))
                .append(" |\n");
        }
        System.out.println(table);
    }
}
