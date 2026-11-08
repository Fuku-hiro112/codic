package namingassist.engine.compose;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import namingassist.engine.Candidate;
import namingassist.engine.FakeTokenizer;
import namingassist.engine.NamePart;
import namingassist.engine.NamingCase;
import namingassist.engine.NamingEngine;
import namingassist.engine.NamingKind;
import namingassist.engine.dictionary.Dictionary;
import namingassist.engine.dictionary.DictionaryEntry;
import namingassist.engine.tokenize.Pos;
import namingassist.engine.tokenize.Token;
import org.junit.jupiter.api.Test;

class PhraseComposerTest {

    /** 固定の小さな辞書。値の並びが優先順。 */
    private static final Map<String, List<String>> WORDS = Map.ofEntries(
        Map.entry("取得", List.of("get", "fetch")),
        Map.entry("数", List.of("count", "number")),
        Map.entry("従業員", List.of("employee")),
        Map.entry("ページ", List.of("page")),
        Map.entry("タイトル", List.of("title")),
        Map.entry("稼働", List.of("operation")),
        Map.entry("時刻", List.of("time")),
        Map.entry("広告", List.of("ad")),
        Map.entry("人", List.of("person")),
        Map.entry("プラン", List.of("plan")),
        Map.entry("持つ", List.of("has")),
        Map.entry("赤", List.of("red", "crimson", "scarlet", "ruby")),
        Map.entry("青", List.of("blue", "azure", "navy", "cyan")),
        Map.entry("箱", List.of("box", "crate", "bin", "chest")));

    private static final Dictionary DICTIONARY = word ->
        Optional.ofNullable(WORDS.get(word)).map(t -> new DictionaryEntry(word, t));

    /** 実際の KuromojiTokenizer の出力と同じ形の Token(task-4-report.md の実測を参照)。 */
    private static final FakeTokenizer TOKENIZER = new FakeTokenizer(List.of(
        noun("従業員"), noun("数"), noun("取得"), noun("ページ"), noun("タイトル"),
        noun("稼働"), noun("時刻"), noun("在庫"), noun("引当"), noun("広告"), noun("人"),
        noun("プラン"), noun("赤"), noun("青"), noun("箱"), noun("謎"), noun("未知"),
        new Token("を", "を", Pos.PARTICLE),
        new Token("の", "の", Pos.PARTICLE),
        new Token("て", "て", Pos.PARTICLE),
        new Token("する", "する", Pos.VERB),
        new Token("持つ", "持つ", Pos.VERB),
        new Token("持っ", "持つ", Pos.VERB),
        new Token("いる", "いる", Pos.VERB),
        new Token("ID", "ID", Pos.OTHER),
        new Token("s", "s", Pos.OTHER),
        new Token("ｓ", "ｓ", Pos.SYMBOL)));

    private final NamingEngine engine = new NamingEngine(TOKENIZER, DICTIONARY);

    private static Token noun(String w) {
        return new Token(w, w, Pos.NOUN);
    }

    private List<String> rendered(String phrase, NamingKind kind) {
        return engine.suggest(phrase, kind).stream().map(Candidate::rendered).toList();
    }

    @Test
    void verbObjectIsReordered() {
        List<Candidate> c = engine.suggest("従業員数を取得する", NamingKind.METHOD);
        assertEquals("get_employee_count", c.get(0).name().render(NamingCase.SNAKE));
        assertEquals("getEmployeeCount", c.get(0).name().render(NamingCase.CAMEL));
        assertEquals("getEmployeeCount", c.get(0).rendered());
    }

    @Test
    void alternativeVerbGivesSecondCandidate() {
        List<String> snake = engine.suggest("ページタイトルを取得する", NamingKind.METHOD).stream()
            .map(c -> c.name().render(NamingCase.SNAKE)).toList();
        assertTrue(snake.contains("get_page_title"), snake.toString());
        assertTrue(snake.contains("fetch_page_title"), snake.toString());
        assertTrue(snake.indexOf("get_page_title") < snake.indexOf("fetch_page_title"), snake.toString());
    }

    @Test
    void nounPhraseKeepsOrder() {
        assertEquals("operationTime", rendered("稼働時刻", NamingKind.VARIABLE).get(0));
    }

    @Test
    void unknownWordIsKeptInJapaneseAtTheEnd() {
        List<Candidate> c = engine.suggest("在庫引当を取得する", NamingKind.METHOD);
        List<String> r = c.stream().map(Candidate::rendered).toList();
        assertTrue(r.contains("get在庫引当"), r.toString());
        Candidate last = c.get(c.size() - 1);
        assertTrue(last.name().parts().stream().anyMatch(p -> !p.resolved()), last.toString());
        Candidate get = c.get(r.indexOf("get在庫引当"));
        NamePart unresolved = get.name().parts().stream().filter(p -> !p.resolved()).findFirst().orElseThrow();
        assertFalse(unresolved.resolved());
        assertEquals("在庫引当", unresolved.text());
    }

    @Test
    void allUnknownGivesNoCandidate() {
        assertEquals(List.of(), engine.suggest("謎の未知", NamingKind.VARIABLE));
    }

    @Test
    void latinIsKeptAsWord() {
        assertEquals("getId", rendered("IDを取得する", NamingKind.METHOD).get(0));
    }

    @Test
    void pluralSuffix() {
        assertEquals("ads", rendered("広告s", NamingKind.VARIABLE).get(0));
        assertEquals("ads", rendered("広告ｓ", NamingKind.VARIABLE).get(0));
    }

    @Test
    void irregularPlural() {
        assertEquals("people", rendered("人s", NamingKind.VARIABLE).get(0));
    }

    @Test
    void limitsToTenCandidates() {
        List<String> r = rendered("赤青箱", NamingKind.VARIABLE);
        assertEquals(10, r.size(), r.toString());
        // 辞書の優先順の辞書式順序: 後ろの語ほど速く変わる
        assertEquals(List.of("redBlueBox", "redBlueCrate", "redBlueBin", "redBlueChest", "redAzureBox"),
            r.subList(0, 5));
    }

    // ---- 結果を分岐させる実装の選択をここで固定する ----

    @Test
    void particleNoKeepsNounOrder() {
        assertEquals("getEmployeeCount", rendered("従業員の数を取得する", NamingKind.METHOD).get(0));
    }

    @Test
    void verbTokenIsLookedUpByBaseForm() {
        assertEquals("hasPlan", rendered("プランを持つ", NamingKind.METHOD).get(0));
    }

    @Test
    void auxiliaryVerbIruIsNotThePredicate() {
        assertEquals("hasPlan", rendered("プランを持っている", NamingKind.METHOD).get(0));
    }

    @Test
    void blankPhraseGivesNoCandidate() {
        assertEquals(List.of(), engine.suggest("", NamingKind.VARIABLE));
        assertEquals(List.of(), engine.suggest(null, NamingKind.VARIABLE));
    }

    @Test
    void alternativesArePerWord() {
        Candidate first = engine.suggest("従業員数を取得する", NamingKind.METHOD).get(0);
        assertEquals(List.of("get", "fetch"), first.name().parts().get(0).alternatives());
        assertEquals(List.of("count", "number"), first.name().parts().get(2).alternatives());
    }

    @Test
    void regularPlurals() {
        assertEquals("categories", Pluralizer.pluralize("category"));
        assertEquals("boxes", Pluralizer.pluralize("box"));
        assertEquals("keys", Pluralizer.pluralize("key"));
        assertEquals("children", Pluralizer.pluralize("child"));
    }
}
