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
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPromoter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.actionSystem.impl.UtilsKt;
import com.intellij.openapi.extensions.DefaultPluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.TitlePanel;
import com.intellij.ui.UiInterceptors;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.ui.awt.RelativePoint;
import java.util.ArrayList;
import java.util.Objects;
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
    /**
     * Alt+Enter を押したとき IDE が実行するアクション。IdeKeyEventDispatcher と同じく、既定のキーマップの割り当てを
     * ActionPromoter で並べ替え(拡張点 + アクション自身が promoter のもの)、update で有効な最初のものを選ぶ。
     */
    private String actionChosenForAltEnter() {
        Keymap keymap = KeymapManager.getInstance().getKeymap("$default");
        List<AnAction> bound = keymap.getActionIdList(new KeyboardShortcut(KeyStroke.getKeyStroke("alt ENTER"), null))
            .stream().map(id -> ActionManager.getInstance().getAction(id)).filter(Objects::nonNull).toList();
        DataContext context = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, getProject())
            .add(CommonDataKeys.EDITOR, myFixture.getEditor())
            .add(CommonDataKeys.PSI_FILE, myFixture.getFile())
            .build();
        List<ActionPromoter> promoters = new ArrayList<>(ActionPromoter.EP_NAME.getExtensionList());
        bound.stream().filter(a -> a instanceof ActionPromoter).map(a -> (ActionPromoter) a).forEach(promoters::add);
        List<AnAction> ordered = UtilsKt.rearrangeByPromotersImpl(bound, context, promoters);
        ActionManager manager = ActionManager.getInstance();
        System.out.println("[ALT-ENTER] ordered=" + ordered.stream().map(manager::getId).toList());
        for (AnAction action : ordered) {
            var event = TestActionEvent.createTestEvent(action, context);
            ActionUtil.performDumbAwareUpdate(action, event, false);
            if (event.getPresentation().isEnabled()) {
                return manager.getId(action);
            }
        }
        return null;
    }

    /** 実機の Alt+Enter の一覧は IDE の描画の不具合で文字が出ない。本プラグインの候補が並ぶ間は小窓を直接開く。 */
    public void testAltEnterIsBoundToEditAction() {
        Keymap keymap = KeymapManager.getInstance().getKeymap("$default");
        KeyboardShortcut altEnter = new KeyboardShortcut(KeyStroke.getKeyStroke("alt ENTER"), null);
        List<String> bound = keymap.getActionIdList(altEnter);
        System.out.println("[SHORTCUT] alt ENTER -> " + bound);
        assertTrue("既定のキーマップで Alt+Enter が割り当てられていること: " + bound, bound.contains(EditCandidateAction.ID));
        assertTrue("IDE の Alt+Enter も残ること: " + bound, bound.contains(IdeActions.ACTION_SHOW_INTENTION_ACTIONS));
    }

    public void testAltEnterOpensEditorWhilePluginCandidatesShown() {
        completePageTitle();
        assertEquals(EditCandidateAction.ID, actionChosenForAltEnter());
    }

    public void testAltEnterOpensEditorWhenOtherItemSelected() {
        completeAndSelectOtherItem();
        assertEquals(EditCandidateAction.ID, actionChosenForAltEnter());
    }

    /** 英字だけの一覧(本プラグインの候補が無い)や一覧が無いときは、IDE の Alt+Enter のまま。 */
    public void testAltEnterLeftToIdeWithoutPluginCandidates() {
        myFixture.configureByText("A.java", "class A { void f() { String strA = \"\", strB = \"\"; str<caret> } }");
        myFixture.completeBasic();
        assertNotNull("Lookup が出ること", myFixture.getLookup());
        assertEquals(IdeActions.ACTION_SHOW_INTENTION_ACTIONS, actionChosenForAltEnter());
    }

    public void testAltEnterLeftToIdeWithoutLookup() {
        myFixture.configureByText("A.java", "class A { void f() { int ページタイトルを取得する<caret> } }");
        assertEquals(IdeActions.ACTION_SHOW_INTENTION_ACTIONS, actionChosenForAltEnter());
    }

    /** 候補の操作(電球の一覧)に渡す名前とアイコン。空の四角形の原因がこちらでないことの確認。 */
    public void testLookupActionHasTextAndIcon() {
        LookupImpl lookup = completePageTitle();
        LookupElementAction action = lookup.getActionsFor(pluginItem(lookup, "getPageTitle")).stream()
            .filter(a -> EditActionProvider.ACTION_TEXT.equals(a.getText()))
            .findFirst().orElseThrow();
        assertFalse(action.getText().isBlank());
        assertSame(AllIcons.Actions.Edit, action.getIcon());
    }
}
