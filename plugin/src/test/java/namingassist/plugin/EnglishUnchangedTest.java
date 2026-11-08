package namingassist.plugin;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionContributorEP;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.ExtensionTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.util.List;

/** 英字の入力では、本プラグインの contributor が有効でも無効でも補完結果が変わらないこと。 */
public class EnglishUnchangedTest extends BasePlatformTestCase {
    private static final String SOURCE = "class A { void f() { String strA = \"\", strB = \"\"; int operand = 0; <caret> } }";
    private static final String[] ENGLISH_INPUTS = {"str", "oper", "Str", "a"};

    /** 補完の結果。単一候補で自動挿入されたときは lookup が null になるので、文書の中身も比べる。 */
    private record Outcome(List<String> lookup, List<String> fromPlugin, String text) {
    }

    private Outcome complete(String typed) {
        myFixture.configureByText("A.java", SOURCE);
        myFixture.type(typed);
        LookupElement[] elements = myFixture.completeBasic();
        return new Outcome(PluginLookups.all(elements), PluginLookups.fromPlugin(elements),
            myFixture.getEditor().getDocument().getText());
    }

    /** fixture の中だけで、本プラグインの contributor を拡張点から外す(テストの終わりに戻る)。 */
    private void disableContributor() {
        List<CompletionContributorEP> others = CompletionContributor.EP.getExtensionList().stream()
            .filter(ep -> !JapaneseNamingContributor.class.getName().equals(ep.implementationClass))
            .toList();
        assertTrue("外す前は登録されていること",
            others.size() < CompletionContributor.EP.getExtensionList().size());
        ExtensionTestUtil.maskExtensions(CompletionContributor.EP, others, getTestRootDisposable());
    }

    public void testEnglishCompletionIsSameWithAndWithoutContributor() {
        Outcome[] enabled = new Outcome[ENGLISH_INPUTS.length];
        for (int i = 0; i < ENGLISH_INPUTS.length; i++) {
            enabled[i] = complete(ENGLISH_INPUTS[i]);
        }
        disableContributor();
        for (int i = 0; i < ENGLISH_INPUTS.length; i++) {
            Outcome disabled = complete(ENGLISH_INPUTS[i]);
            System.out.println("[EN] typed=" + ENGLISH_INPUTS[i] + " enabled=" + enabled[i].lookup()
                + " disabled=" + disabled.lookup());
            assertEquals("lookup: " + ENGLISH_INPUTS[i], disabled.lookup(), enabled[i].lookup());
            assertEquals("text: " + ENGLISH_INPUTS[i], disabled.text(), enabled[i].text());
        }
    }

    public void testNoPluginCandidatesWithoutJapanese() {
        for (String typed : ENGLISH_INPUTS) {
            Outcome outcome = complete(typed);
            System.out.println("[NOJP] typed=" + typed + " fromPlugin=" + outcome.fromPlugin());
            assertEquals("日本語を含まない入力 " + typed, List.of(), outcome.fromPlugin());
        }
    }

    /** 対照: 外す手段が効いていること(外すと日本語でも本プラグインの候補が出ない)。 */
    public void testDisablingRemovesJapaneseCandidates() {
        Outcome before = complete("稼働時刻");
        assertTrue("外す前は出る: " + before.fromPlugin(), before.fromPlugin().contains("operationTime"));
        disableContributor();
        Outcome after = complete("稼働時刻");
        System.out.println("[CONTROL] before=" + before.fromPlugin() + " after=" + after.fromPlugin());
        assertEquals(List.of(), after.fromPlugin());
    }
}
