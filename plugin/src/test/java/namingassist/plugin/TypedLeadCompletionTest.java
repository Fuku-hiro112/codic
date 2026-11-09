package namingassist.plugin;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.util.List;
import namingassist.engine.NamingKind;

/**
 * 英字の直後に日本語を打ったとき(pageDecisionああ)、英字(打ち済みの識別子)はそのまま残し、
 * 日本語の部分だけを英語にして後ろにつなぐこと。英字が略語に見える(ID など大文字・数字・_ だけ)ときは、
 * これまでどおり句の一部として訳す。
 */
public class TypedLeadCompletionTest extends BasePlatformTestCase {

    /** engine が日本語の部分だけから作る先頭候補(期待値の英語の部分をこれに従わせる)。 */
    private static String engineFirst(String phrase, NamingKind kind) {
        return NamingEngineService.getInstance().engine().suggest(phrase, kind).get(0).rendered();
    }

    private static String capitalizeFirst(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** source の caret に typed を打って補完し、本プラグインの先頭候補が expectedFirst であることを確かめて確定する。 */
    private String completeAndChooseFirst(String source, String typed, String expectedFirst) {
        myFixture.configureByText("A.java", source);
        myFixture.type(typed);
        LookupElement[] elements = myFixture.completeBasic();
        List<String> fromPlugin = PluginLookups.fromPlugin(elements);
        System.out.println("[LEAD] source=" + source + " typed=" + typed + " fromPlugin=" + fromPlugin);
        assertFalse("候補が出ること", fromPlugin.isEmpty());
        assertEquals("先頭候補: " + fromPlugin, expectedFirst, fromPlugin.get(0));

        LookupImpl lookup = (LookupImpl) myFixture.getLookup();
        LookupElement first = lookup.getItems().stream()
            .filter(e -> e.getLookupString().equals(expectedFirst)).findFirst().orElseThrow();
        lookup.setCurrentItem(first);
        myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR);
        String text = myFixture.getEditor().getDocument().getText();
        System.out.println("[LEAD] after=" + text);
        return text;
    }

    /** 実機の不具合の再現(スクリーンショット: pageDecisionああ → pagedecisionLikeThat)。 */
    public void testCamelLeadIsKeptAndJapanesePartIsAppended() {
        String expected = "pageDecision" + capitalizeFirst(engineFirst("ああ", NamingKind.VARIABLE));
        String text = completeAndChooseFirst("class A { void f() { int pageDecision<caret> } }", "ああ", expected);
        assertEquals("class A { void f() { int " + expected + " } }", text);
    }

    public void testShortLowercaseLeadIsKept() {
        String expected = "str" + capitalizeFirst(engineFirst("稼働時刻", NamingKind.VARIABLE));
        assertEquals("strOperationTime", expected);
        String text = completeAndChooseFirst("class A { void f() { int str<caret> } }", "稼働時刻", expected);
        assertEquals("class A { void f() { int strOperationTime } }", text);
    }

    /** メソッド名(CAMEL)。 */
    public void testMethodLeadIsKept() {
        String expected = "load" + capitalizeFirst(engineFirst("ページタイトルを取得する", NamingKind.METHOD));
        String text = completeAndChooseFirst("class A { void load<caret>() {} }", "ページタイトルを取得する", expected);
        assertEquals("class A { void " + expected + "() {} }", text);
    }

    /** クラス名(PASCAL)。英字はそのまま、英語の部分は PASCAL の綴りのまま後ろにつなぐ。 */
    public void testPascalLeadIsKept() {
        String english = engineFirst("観測地点", NamingKind.CLASS);
        assertEquals("ObservationPoint", english);
        String text = completeAndChooseFirst("class Pollen<caret> {}", "観測地点", "PollenObservationPoint");
        assertEquals("class PollenObservationPoint {}", text);
    }

    /** 定数(UPPER_SNAKE)。英字を大文字にして _ でつなぐ。 */
    public void testUpperSnakeLeadIsUppercasedAndJoinedWithUnderscore() {
        String english = engineFirst("稼働時刻", NamingKind.CONSTANT);
        String expected = "MAX_" + english;
        String text = completeAndChooseFirst("class A { static final int max<caret> = 1; }", "稼働時刻", expected);
        assertEquals("class A { static final int " + expected + " = 1; }", text);
    }

    /** 英字の末尾の数字は英字の一部(value1次元 の 1 は数詞として読まない)。 */
    public void testLeadEndingWithDigitKeepsDigit() {
        String expected = "value1" + capitalizeFirst(engineFirst("次元", NamingKind.VARIABLE));
        String text = completeAndChooseFirst("class A { void f() { int value1<caret> } }", "次元", expected);
        assertEquals("class A { void f() { int " + expected + " } }", text);
    }

    /** 略語に見える英字(大文字だけ)は、これまでどおり句の一部として訳す。 */
    public void testAbbreviationLeadIsTranslatedAsPartOfPhrase() {
        String expected = engineFirst("IDを取得する", NamingKind.VARIABLE);
        String text = completeAndChooseFirst("class A { void f() { int ID<caret> } }", "を取得する", expected);
        assertEquals("class A { void f() { int " + expected + " } }", text);
    }

    /**
     * 1 語の小文字(id・url・json)の直後が格助詞で始まる日本語なら、略語として全体を句として訳す
     * (idを取得する → getId。engine に句の全体を渡したときと同じ先頭候補)。
     */
    public void testLowercaseAbbreviationBeforeParticleIsTranslatedAsPartOfPhrase() {
        String[][] cases = {{"id", "を取得する"}, {"url", "を生成する"}, {"json", "を解析する"}};
        for (String[] c : cases) {
            String expected = engineFirst(c[0] + c[1], NamingKind.VARIABLE);
            System.out.println("[LEAD] " + c[0] + c[1] + " → " + expected);
            assertFalse("動詞が先頭の名前: " + expected, expected.startsWith(c[0]));
            String text = completeAndChooseFirst("class A { void f() { int " + c[0] + "<caret> } }", c[1], expected);
            assertEquals("class A { void f() { int " + expected + " } }", text);
        }
        assertEquals("getId", engineFirst("idを取得する", NamingKind.VARIABLE));
    }

    /** 全角の英字(ｉｄ)も半角(id)と同じに扱う。 */
    public void testFullWidthLowercaseAbbreviationIsSameAsHalfWidth() {
        String text = completeAndChooseFirst("class A { void f() { int ｉｄ<caret> } }", "を取得する", "getId");
        assertEquals("class A { void f() { int getId } }", text);
    }

    /** 全角の camelCase の英字も半角と同じに lead として残す(挿入は半角)。 */
    public void testFullWidthCamelLeadIsKeptAsHalfWidth() {
        String expected = "pageDecision" + capitalizeFirst(engineFirst("ああ", NamingKind.VARIABLE));
        String text = completeAndChooseFirst("class A { void f() { int ｐａｇｅＤｅｃｉｓｉｏｎ<caret> } }", "ああ", expected);
        assertEquals("class A { void f() { int " + expected + " } }", text);
    }

    /** 1 語の小文字でも、日本語が助詞で始まらなければ lead を残す(user名前 → userName)。 */
    public void testLowercaseWordBeforeNonParticleIsKept() {
        String expected = "user" + capitalizeFirst(engineFirst("名前", NamingKind.VARIABLE));
        String text = completeAndChooseFirst("class A { void f() { int user<caret> } }", "名前", expected);
        assertEquals("class A { void f() { int " + expected + " } }", text);
    }

    /** 助詞で始まる日本語でも、lead が camelCase(1 語の小文字でない)なら lead を残す。 */
    public void testCamelLeadBeforeParticleIsKept() {
        String expected = "pageDecision" + capitalizeFirst(engineFirst("を取得する", NamingKind.VARIABLE));
        String text = completeAndChooseFirst("class A { void f() { int pageDecision<caret> } }", "を取得する", expected);
        assertEquals("class A { void f() { int " + expected + " } }", text);
    }

    /** lead と日本語の部分の先頭の語が同じなら重ねない(get + ページタイトルを取得する → getPageTitle)。 */
    public void testRepeatedLeadingVerbIsNotDoubled() {
        String text = completeAndChooseFirst("class A { void get<caret>() {} }", "ページタイトルを取得する", "getPageTitle");
        assertEquals("class A { void getPageTitle() {} }", text);
        String isText = completeAndChooseFirst("class A { boolean is<caret>() { return true; } }", "有料プランか", "isPaidPlan");
        assertEquals("class A { boolean isPaidPlan() { return true; } }", isText);
    }

    /** 日本語だけの句はこれまでどおり。 */
    public void testJapaneseOnlyIsUnchanged() {
        String text = completeAndChooseFirst("class A { void f() { int <caret> } }", "稼働時刻", "operationTime");
        assertEquals("class A { void f() { int operationTime } }", text);
    }

    /** 先頭の数字を拾う仕組みはこれまでどおり(2 は句の先頭の数詞)。 */
    public void testLeadingDigitIsUnchanged() {
        String text = completeAndChooseFirst("class A { void f() { int 2<caret> } }", "次元配列", "twoDimensionArray");
        assertEquals("class A { void f() { int twoDimensionArray } }", text);
    }

    /** 英字と日本語の句の全体(pageDecisionああ)で絞り込める(右の灰色の文字も打ったとおり)。 */
    public void testTypeTextShowsWholeInput() {
        myFixture.configureByText("A.java", "class A { void f() { int str<caret> } }");
        myFixture.type("稼働時刻");
        myFixture.completeBasic();
        LookupImpl lookup = (LookupImpl) myFixture.getLookup();
        LookupElement first = lookup.getItems().stream()
            .filter(e -> e.getLookupString().equals("strOperationTime")).findFirst().orElseThrow();
        assertTrue(first.getAllLookupStrings().contains("str稼働時刻"));
    }
}
