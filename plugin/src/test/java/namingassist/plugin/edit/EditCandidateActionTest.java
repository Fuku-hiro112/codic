package namingassist.plugin.edit;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionContributorEP;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementAction;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.extensions.DefaultPluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.TitlePanel;
import com.intellij.ui.UiInterceptors;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.ui.awt.RelativePoint;
import java.util.ArrayList;
import java.util.List;
import javax.swing.KeyStroke;
import namingassist.engine.Candidate;
import org.jetbrains.annotations.NotNull;

/**
 * 候補の編集の小窓を開く入口(エディタのアクションとそのショートカット)。
 * 実機で Lookup 表示中の Alt+Enter が何も出さなかった件(選ばれていた候補が本プラグインの候補でないと、
 * IDE は操作の一覧を黙って出さない)への対処。
 */
public class EditCandidateActionTest extends BasePlatformTestCase {

    private final List<String> shownPopups = new ArrayList<>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // テストでは画面に出せないので、表示しようとした小窓を横取りして記録する
        UiInterceptors.registerPersistent(getTestRootDisposable(),
            new UiInterceptors.PersistentUiInterceptor<>(JBPopup.class) {
                @Override
                public boolean shouldIntercept(JBPopup popup) {
                    return true;
                }

                @Override
                protected void doIntercept(JBPopup popup, RelativePoint point) {
                    shownPopups.add(popup instanceof AbstractPopup p && p.getTitle() instanceof TitlePanel t
                        ? t.getLabel().getText() : popup.getClass().getSimpleName());
                    popup.cancel();
                }
            });
    }

    /** 本プラグイン以外の候補。実機の一覧に並ぶ ML の予測などの代わりに、日本語のプレフィックスにも 1 件足す。 */
    public static final class OtherContributor extends CompletionContributor {
        @Override
        public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
            result.addElement(LookupElementBuilder.create("otherSuggestion")
                .withLookupString(result.getPrefixMatcher().getPrefix()));
        }
    }

    private void registerOtherContributor() {
        CompletionContributor.EP.getPoint().registerExtension(
            new CompletionContributorEP("JAVA", OtherContributor.class.getName(),
                new DefaultPluginDescriptor(PluginId.getId("test.other"), getClass().getClassLoader())),
            getTestRootDisposable());
    }

    private LookupImpl completePageTitle() {
        myFixture.configureByText("A.java", "class A { void f() { int <caret> } }");
        myFixture.type("ページタイトルを取得する");
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

    /** 本プラグイン以外の候補も並ぶ一覧を出し、その候補を選ぶ。 */
    private LookupImpl completeAndSelectOtherItem() {
        registerOtherContributor();
        LookupImpl lookup = completePageTitle();
        LookupElement other = lookup.getItems().stream()
            .filter(e -> "otherSuggestion".equals(e.getLookupString()))
            .findFirst().orElseThrow(() -> new AssertionError("標準の候補が無い: " + lookup.getItems()));
        lookup.setCurrentItem(other);
        assertSame("標準の候補が選ばれていること", other, lookup.getCurrentItem());
        return lookup;
    }

    public void testShortcutIsBoundAndUnique() {
        Keymap keymap = KeymapManager.getInstance().getKeymap("$default");
        assertNotNull(keymap);
        KeyStroke stroke = KeyStroke.getKeyStroke(EditCandidateAction.SHORTCUT);
        assertNotNull("ショートカットの書式が正しいこと: " + EditCandidateAction.SHORTCUT, stroke);
        assertTrue("既定のキーマップに割り当てられていること",
            List.of(keymap.getShortcuts(EditCandidateAction.ID)).contains(new KeyboardShortcut(stroke, null)));
        List<String> bound = keymap.getActionIdList(new KeyboardShortcut(stroke, null));
        System.out.println("[SHORTCUT] " + EditCandidateAction.SHORTCUT + " -> " + bound);
        assertEquals("他のアクションと衝突しないこと", List.of(EditCandidateAction.ID), bound);
    }

    public void testActionOpensEditorForSelectedPluginCandidate() {
        LookupImpl lookup = completePageTitle();
        lookup.setCurrentItem(pluginItem(lookup, "fetchPageTitle"));
        EditTarget target = EditCandidateAction.targetIn(lookup);
        assertNotNull(target);
        assertEquals("選んでいる候補が対象", "fetchPageTitle", target.candidate().rendered());

        myFixture.performEditorAction(EditCandidateAction.ID);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        System.out.println("[ACTION] popups=" + shownPopups);
        assertNull("Lookup は閉じること", LookupManager.getActiveLookup(myFixture.getEditor()));
        assertEquals("小窓が 1 つ開くこと: " + shownPopups, 1, shownPopups.size());
        assertTrue(shownPopups.get(0), shownPopups.get(0).contains("ページタイトルを取得する"));
        myFixture.checkResult("class A { void f() { int ページタイトルを取得する<caret> } }");
    }

    /** 選ばれているのが本プラグインの候補でなくても、一覧の先頭の本プラグインの候補を対象にする。 */
    public void testActionUsesFirstPluginCandidateWhenOtherItemSelected() {
        LookupImpl lookup = completeAndSelectOtherItem();

        EditTarget target = EditCandidateAction.targetIn(lookup);
        assertNotNull("標準の候補を選んでいても対象が決まること", target);
        assertEquals("getPageTitle", target.candidate().rendered());

        myFixture.performEditorAction(EditCandidateAction.ID);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        assertEquals("小窓が 1 つ開くこと: " + shownPopups, 1, shownPopups.size());
    }

    /** Alt+Enter の経路: 標準の候補を選んでいても、一覧に本プラグインの候補があれば操作を出す。 */
    public void testAltEnterActionsOfferedWhenOtherItemSelected() {
        LookupImpl lookup = completeAndSelectOtherItem();
        LookupElement other = lookup.getCurrentItem();

        List<String> texts = lookup.getActionsFor(other).stream().map(LookupElementAction::getText).toList();
        System.out.println("[LOOKUP-ACTIONS] other=" + texts);
        assertTrue("標準の候補を選んでいても操作が付くこと: " + texts, texts.contains(EditActionProvider.ACTION_TEXT));
    }

    /** Lookup が無いときは何も書き換えず、小窓も開かない(例外も出さない)。 */
    public void testWithoutLookupDoesNothing() {
        myFixture.configureByText("A.java", "class A { void f() { int ページタイトルを取得する<caret> } }");
        myFixture.performEditorAction(EditCandidateAction.ID);
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

        assertEquals(List.of(), shownPopups);
        myFixture.checkResult("class A { void f() { int ページタイトルを取得する<caret> } }");
    }
}
