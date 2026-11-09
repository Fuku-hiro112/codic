package namingassist.plugin;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.util.List;

/**
 * 数字で始まる句(2次元配列)。先頭の数字は識別子の先頭に使えず、補完のプレフィックスに入らない(プレフィックスは「次元配列」)。
 * キャレット直前の連続する数字も句の先頭に含めて解析し、確定でその数字ごと置き換えること。
 */
public class LeadingDigitCompletionTest extends BasePlatformTestCase {

    /** 「2」の直後から日本語を打った状態で補完し、本プラグインの先頭候補を確定する。確定後の文書を返す。 */
    private String completeAndChooseFirst(String before, String typed, String expectedFirst) {
        myFixture.configureByText("A.java", "class A { void f() { int " + before + "<caret> } }");
        myFixture.type(typed);
        LookupElement[] elements = myFixture.completeBasic();
        List<String> fromPlugin = PluginLookups.fromPlugin(elements);
        System.out.println("[DIGIT] before=" + before + " typed=" + typed + " fromPlugin=" + fromPlugin);
        assertFalse("候補が出ること", fromPlugin.isEmpty());
        assertEquals("先頭候補: " + fromPlugin, expectedFirst, fromPlugin.get(0));

        LookupImpl lookup = (LookupImpl) myFixture.getLookup();
        LookupElement first = lookup.getItems().stream()
            .filter(e -> e.getLookupString().equals(expectedFirst)).findFirst().orElseThrow();
        lookup.setCurrentItem(first);
        myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
        String text = myFixture.getEditor().getDocument().getText();
        System.out.println("[DIGIT] after=" + text);
        return text;
    }

    public void testHalfWidthLeadingDigitIsReadAsNumeralAndReplaced() {
        String text = completeAndChooseFirst("2", "次元配列", "twoDimensionArray");
        assertEquals("class A { void f() { int twoDimensionArray } }", text);
    }

    public void testFullWidthLeadingDigitIsReadAsNumeralAndReplaced() {
        String text = completeAndChooseFirst("２", "次元配列", "twoDimensionArray");
        assertEquals("class A { void f() { int twoDimensionArray } }", text);
    }

    /** 数字が直前に無い通常の句は、これまでどおり句だけを置き換える。 */
    public void testPhraseWithoutDigitIsUnchanged() {
        String text = completeAndChooseFirst("", "稼働時刻", "operationTime");
        assertEquals("class A { void f() { int operationTime } }", text);
    }
}
