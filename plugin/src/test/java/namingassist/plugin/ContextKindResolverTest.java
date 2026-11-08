package namingassist.plugin;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionContributorEP;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.testFramework.ExtensionTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.util.ArrayList;
import java.util.List;
import namingassist.engine.NamingKind;
import org.jetbrains.annotations.NotNull;

/**
 * 打った位置の文脈で命名の種別が決まり、先頭候補の書き方が変わること。
 * 種別は、補完が本当に渡す位置(補完用コピーのファイル、ダミー識別子入り)で resolve した結果を記録して確かめる。
 */
public class ContextKindResolverTest extends BasePlatformTestCase {
    private static final String PHRASE = "稼働時刻";

    /** テスト専用: 補完が渡す位置で resolve した種別を記録する(本プラグインの contributor と同じ入力)。 */
    public static final class KindRecorder extends CompletionContributor {
        static final List<NamingKind> recorded = new ArrayList<>();

        @Override
        public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
            recorded.add(new ContextKindResolver().resolve(parameters.getPosition()));
        }
    }

    private record Outcome(NamingKind kind, String first) {
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CompletionContributorEP ours = CompletionContributor.EP.getExtensionList().stream()
            .filter(ep -> JapaneseNamingContributor.class.getName().equals(ep.implementationClass))
            .findFirst().orElseThrow();
        // 本プラグインの直後に置く(後ろの方だと、式の位置で他の contributor が打ち切り、呼ばれないことを実測した)。
        List<CompletionContributorEP> withRecorder = new ArrayList<>(CompletionContributor.EP.getExtensionList());
        withRecorder.add(withRecorder.indexOf(ours) + 1,
            new CompletionContributorEP("JAVA", KindRecorder.class.getName(), ours.getPluginDescriptor()));
        ExtensionTestUtil.maskExtensions(CompletionContributor.EP, withRecorder, getTestRootDisposable());
        KindRecorder.recorded.clear();
    }

    /** source の <caret> に「稼働時刻」を置いて補完を呼び、記録した種別と Lookup の先頭(本プラグイン由来)を返す。 */
    private Outcome complete(String source) {
        myFixture.configureByText("A.java", source.replace("<caret>", PHRASE + "<caret>"));
        myFixture.completeBasic();
        LookupImpl lookup = (LookupImpl) myFixture.getLookup();
        assertNotNull("Lookup が出ること: " + source, lookup);
        List<String> items = PluginLookups.fromPlugin(lookup.getItems().toArray(LookupElement[]::new));
        assertEquals("resolve が 1 回だけ呼ばれること: " + source, 1, KindRecorder.recorded.size());
        Outcome outcome = new Outcome(KindRecorder.recorded.get(0), items.isEmpty() ? null : items.get(0));
        KindRecorder.recorded.clear();
        System.out.println("[KIND] " + source + " -> " + outcome.kind() + " first=" + outcome.first() + " items=" + items);
        return outcome;
    }

    private void assertOutcome(String source, NamingKind kind, String first) {
        Outcome outcome = complete(source);
        assertEquals("種別: " + source, kind, outcome.kind());
        assertEquals("先頭候補: " + source, first, outcome.first());
    }

    public void testLocalVariable() {
        assertOutcome("class A { void f() { int <caret> } }", NamingKind.VARIABLE, "operationTime");
        assertOutcome("class A { void f() { int <caret> = 0; } }", NamingKind.VARIABLE, "operationTime");
    }

    public void testField() {
        assertOutcome("class A { private int <caret>; }", NamingKind.VARIABLE, "operationTime");
        assertOutcome("class A { static int <caret>; }", NamingKind.VARIABLE, "operationTime");
        assertOutcome("class A { final int <caret> = 0; }", NamingKind.VARIABLE, "operationTime");
    }

    public void testStaticFinalField() {
        assertOutcome("class A { static final int <caret> = 0; }", NamingKind.CONSTANT, "OPERATION_TIME");
        assertOutcome("class A { private static final int <caret>; }", NamingKind.CONSTANT, "OPERATION_TIME");
        assertOutcome("class A { static final int <caret> }", NamingKind.CONSTANT, "OPERATION_TIME");
    }

    /** インタフェースのフィールドは暗黙に static final。 */
    public void testInterfaceFieldIsConstant() {
        assertOutcome("interface I { int <caret> = 0; }", NamingKind.CONSTANT, "OPERATION_TIME");
    }

    /**
     * インタフェース・アノテーション型の中で初期化子の無い宣言は、名前の後に「()」を打つ前のメソッドであることが多い。
     * 暗黙の static final だけを根拠に定数にしない(変数に落とす)。
     */
    public void testInterfaceMemberWithoutInitializerIsNotConstant() {
        assertOutcome("interface I { String <caret> }", NamingKind.VARIABLE, "operationTime");
        assertOutcome("interface I { int <caret> }", NamingKind.VARIABLE, "operationTime");
        assertOutcome("@interface A { String <caret> }", NamingKind.VARIABLE, "operationTime");
    }

    /** 明示の static final は初期化子が無くても定数(クラス内・インタフェース内とも)。 */
    public void testExplicitStaticFinalWithoutInitializerIsConstant() {
        assertOutcome("class A { static final int <caret> }", NamingKind.CONSTANT, "OPERATION_TIME");
        assertOutcome("interface I { static final int <caret> }", NamingKind.CONSTANT, "OPERATION_TIME");
    }

    /** 名前の直後に「(」がある。補完用コピーでは「名前;」と解析されフィールドに見えるので、次の字で見分ける。 */
    public void testMethodName() {
        assertOutcome("class A { void <caret>() { } }", NamingKind.METHOD, "operationTime");
        assertOutcome("class A { public String <caret>() { return null; } }", NamingKind.METHOD, "operationTime");
    }

    /** 「(」をまだ打っていなくても、戻り値が void ならフィールドではありえないのでメソッド。 */
    public void testVoidMemberWithoutParenthesesIsMethod() {
        assertOutcome("class A { void <caret> }", NamingKind.METHOD, "operationTime");
    }

    public void testClassName() {
        assertOutcome("class <caret> { }", NamingKind.CLASS, "OperationTime");
        assertOutcome("class A { } class <caret> { }", NamingKind.CLASS, "OperationTime");
        assertOutcome("class <caret>", NamingKind.CLASS, "OperationTime");
    }

    /** 判定できない位置(式の途中・文の始め)では変数(camel)。 */
    public void testUndecidableFallsBackToVariable() {
        assertOutcome("class A { void f() { int x = 1 + <caret>; } }", NamingKind.VARIABLE, "operationTime");
        assertOutcome("class A { void f() { <caret> } }", NamingKind.VARIABLE, "operationTime");
    }

    /** 曖昧: 戻り値が void 以外で「(」が無いと、フィールドかメソッドか決まらない。変数に落とす。 */
    public void testNonVoidMemberWithoutParenthesesIsVariable() {
        assertOutcome("class A { public String <caret> }", NamingKind.VARIABLE, "operationTime");
    }

    public void testNullDoesNotThrow() {
        assertEquals(NamingKind.VARIABLE, new ContextKindResolver().resolve(null));
    }
}
