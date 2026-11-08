package namingassist.plugin.edit;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementAction;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.util.Collection;
import java.util.List;
import namingassist.engine.Candidate;
import namingassist.engine.NamingCase;

/**
 * 候補一覧(Lookup)の本プラグインの候補に「単語を並べ替え・結合」の操作が付くこと(LookupActionProvider)と、
 * 確定したときに日本語の入力が編集後の英語名に置き換わること。
 */
public class EditActionProviderTest extends BasePlatformTestCase {

    private LookupImpl completeJapanese(String source, String phrase) {
        myFixture.configureByText("A.java", source);
        myFixture.type(phrase);
        myFixture.completeBasic();
        LookupImpl lookup = (LookupImpl) myFixture.getLookup();
        assertNotNull("Lookup が出ること", lookup);
        return lookup;
    }

    private static LookupElement pluginItem(LookupImpl lookup, String rendered) {
        return lookup.getItems().stream()
            .filter(e -> e.getObject() instanceof Candidate c && c.rendered().equals(rendered))
            .findFirst()
            .orElseThrow(() -> new AssertionError("候補 " + rendered + " が無い: " + lookup.getItems()));
    }

    private static List<String> actionTexts(Collection<LookupElementAction> actions) {
        return actions.stream().map(LookupElementAction::getText).toList();
    }

    public void testLookupOffersEditActionForPluginCandidate() {
        LookupImpl lookup = completeJapanese("class A { void f() { int <caret> } }", "ページタイトルを取得する");
        LookupElement item = pluginItem(lookup, "getPageTitle");

        List<String> texts = actionTexts(lookup.getActionsFor(item));
        System.out.println("[LOOKUP-ACTIONS] getPageTitle=" + texts);
        assertTrue("本プラグインの候補に操作が付くこと: " + texts, texts.contains(EditActionProvider.ACTION_TEXT));
    }

    public void testNoEditActionForOtherItems() {
        myFixture.configureByText("A.java", "class A { void f() { String strA = \"\", strB = \"\"; str<caret> } }");
        myFixture.completeBasic();
        LookupImpl lookup = (LookupImpl) myFixture.getLookup();
        assertNotNull("Lookup が出ること", lookup);
        for (LookupElement item : lookup.getItems()) {
            List<String> texts = actionTexts(lookup.getActionsFor(item));
            assertFalse(item.getLookupString() + " に操作が付かないこと: " + texts,
                texts.contains(EditActionProvider.ACTION_TEXT));
        }
    }

    public void testTargetCoversJapanesePhraseAndKeepsCase() {
        LookupImpl lookup = completeJapanese("class A { void f() { int <caret> } }", "ページタイトルを取得する");
        EditTarget target = EditActionProvider.targetFor(pluginItem(lookup, "getPageTitle"), lookup);
        assertNotNull(target);
        assertEquals(NamingCase.CAMEL, target.namingCase());
        assertEquals("ページタイトルを取得する", target.originalText());
        assertEquals(List.of("get", "page", "title"),
            target.candidate().name().parts().stream().map(p -> p.text()).toList());
    }

    public void testCommitReplacesJapanesePhraseWithEditedName() {
        LookupImpl lookup = completeJapanese("class A { void f() { int <caret> } }", "ページタイトルを取得する");
        EditTarget target = EditActionProvider.targetFor(pluginItem(lookup, "getPageTitle"), lookup);
        lookup.hideLookup(true);

        NameEditorModel model = new NameEditorModel(target.candidate().name());
        model.move(2, 1);
        target.replaceWith(model.name());

        myFixture.checkResult("class A { void f() { int getTitlePage<caret> } }");
    }

    public void testCommitUsesConstantCaseOfChosenCandidate() {
        LookupImpl lookup = completeJapanese(
            "class A { static final int <caret> = 1; }", "稼働時刻");
        Candidate first = (Candidate) lookup.getItems().stream()
            .filter(e -> e.getObject() instanceof Candidate)
            .findFirst().orElseThrow().getObject();
        System.out.println("[CONST] first=" + first.rendered());
        EditTarget target = EditActionProvider.targetFor(pluginItem(lookup, first.rendered()), lookup);
        assertEquals(NamingCase.UPPER_SNAKE, target.namingCase());
        lookup.hideLookup(true);

        NameEditorModel model = new NameEditorModel(target.candidate().name());
        model.move(model.name().parts().size() - 1, 0);
        target.replaceWith(model.name());

        String expected = model.name().render(NamingCase.UPPER_SNAKE);
        System.out.println("[CONST] edited=" + expected);
        myFixture.checkResult("class A { static final int " + expected + "<caret> = 1; }");
    }

    private EditTarget targetForPageTitle() {
        LookupImpl lookup = completeJapanese("class A { void f() { int <caret> } }", "ページタイトルを取得する");
        EditTarget target = EditActionProvider.targetFor(pluginItem(lookup, "getPageTitle"), lookup);
        lookup.hideLookup(true);
        return target;
    }

    private void edit(Runnable change) {
        WriteCommandAction.runWriteCommandAction(getProject(), change);
    }

    /** 日本語の入力が消えた後(範囲が無効)に確定しても、何も書き込まない。 */
    public void testCommitDoesNothingWhenRangeIsGone() {
        EditTarget target = targetForPageTitle();
        Document document = myFixture.getEditor().getDocument();
        edit(() -> document.deleteString(target.range().getStartOffset(), target.range().getEndOffset()));
        String before = document.getText();

        assertFalse("書き込まないこと", target.replaceWith(target.candidate().name()));
        assertEquals(before, document.getText());
    }

    /** 日本語の入力が書き換わった後に確定しても、何も書き込まない(別の文字列を潰さない)。 */
    public void testCommitDoesNothingWhenTextChanged() {
        EditTarget target = targetForPageTitle();
        Document document = myFixture.getEditor().getDocument();
        edit(() -> document.insertString(target.range().getStartOffset() + 1, "X"));
        String before = document.getText();

        assertFalse("書き込まないこと", target.replaceWith(target.candidate().name()));
        assertEquals(before, document.getText());
    }

    /** 読み取り専用の文書では、例外を出さずに書き込まない。 */
    public void testCommitDoesNothingOnReadOnlyDocument() {
        EditTarget target = targetForPageTitle();
        Document document = myFixture.getEditor().getDocument();
        document.setReadOnly(true);
        try {
            String before = document.getText();
            assertFalse("書き込まないこと", target.replaceWith(target.candidate().name()));
            assertEquals(before, document.getText());
        } finally {
            document.setReadOnly(false);
        }
    }

    /** 実際の操作(performLookupAction)を通す。Lookup は閉じ、確定するまで日本語は残る。 */
    public void testPerformingActionHidesLookupAndKeepsTextUntilCommit() {
        LookupImpl lookup = completeJapanese("class A { void f() { int <caret> } }", "ページタイトルを取得する");
        LookupElementAction action = lookup.getActionsFor(pluginItem(lookup, "getPageTitle")).stream()
            .filter(a -> EditActionProvider.ACTION_TEXT.equals(a.getText()))
            .findFirst().orElseThrow();

        LookupElementAction.Result result = action.performLookupAction();
        assertSame(LookupElementAction.Result.HIDE_LOOKUP, result);
        lookup.hideLookup(true);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        myFixture.checkResult("class A { void f() { int ページタイトルを取得する<caret> } }");
    }
}
