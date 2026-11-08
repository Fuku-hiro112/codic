package namingassist.engine.compose;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import namingassist.engine.FakeTokenizer;
import namingassist.engine.NamingCase;
import namingassist.engine.NamingEngine;
import namingassist.engine.NamingKind;
import namingassist.engine.dictionary.Dictionary;
import namingassist.engine.dictionary.DictionaryEntry;
import namingassist.engine.tokenize.Pos;
import namingassist.engine.tokenize.Token;
import org.junit.jupiter.api.Test;

/** 「〜か」で終わる句を真偽値の名前にする。例は https://nelog.jp/codic の記載。 */
class BooleanRuleTest {

    /** 同梱の curated.tsv と同じ訳。 */
    private static final Map<String, List<String>> WORDS = Map.ofEntries(
        Map.entry("広告", List.of("ad")),
        Map.entry("表示", List.of("show", "display")),
        Map.entry("ファイル", List.of("file")),
        Map.entry("作成", List.of("create")),
        Map.entry("有料", List.of("paid")),
        Map.entry("プラン", List.of("plan")),
        Map.entry("正規表現", List.of("regexp")),
        Map.entry("検索", List.of("search")),
        Map.entry("有効", List.of("enabled")),
        Map.entry("ウェブサイト", List.of("website")),
        Map.entry("持つ", List.of("has")));

    /**
     * 実際の KuromojiTokenizer の出力と同じ形の Token(task-5-report.md の実測を参照)。
     * 「作成されている」は 作成/NOUN さ(する)/VERB れ(れる)/VERB て/PARTICLE いる/VERB、
     * 「有効」は形容動詞語幹で NOUN、「正規表現」は user-dictionary で一語。
     */
    private static final List<Token> VOCABULARY = List.of(
        noun("広告"), noun("表示"), noun("ファイル"), noun("作成"), noun("有料"), noun("プラン"),
        noun("正規表現"), noun("検索"), noun("有効"), noun("ウェブサイト"),
        new Token("を", "を", Pos.PARTICLE),
        new Token("は", "は", Pos.PARTICLE),
        new Token("て", "て", Pos.PARTICLE),
        new Token("か", "か", Pos.PARTICLE),
        new Token("する", "する", Pos.VERB),
        new Token("さ", "する", Pos.VERB),
        new Token("れ", "れる", Pos.VERB),
        new Token("いる", "いる", Pos.VERB),
        new Token("持っ", "持つ", Pos.VERB),
        new Token("s", "s", Pos.OTHER));

    private static Token noun(String w) {
        return new Token(w, w, Pos.NOUN);
    }

    private static NamingEngine engine(Map<String, List<String>> words) {
        Dictionary dictionary = word -> Optional.ofNullable(words.get(word)).map(t -> new DictionaryEntry(word, t));
        return new NamingEngine(new FakeTokenizer(VOCABULARY), dictionary);
    }

    private final NamingEngine engine = engine(WORDS);

    private String first(String phrase) {
        return engine.suggest(phrase, NamingKind.METHOD).get(0).name().render(NamingCase.SNAKE);
    }

    @Test
    void suruKaIsShould() {
        assertEquals("should_show_ad", first("広告を表示するか"));
    }

    @Test
    void passiveKaIsSubjectAndPastParticiple() {
        assertEquals("is_file_created", first("ファイルは作成されているか"));
    }

    @Test
    void nounKaIsIs() {
        assertEquals("is_paid_plan", first("有料プランか"));
    }

    @Test
    void nounWaAdjectiveKaIsIs() {
        assertEquals("is_regexp_search_enabled", first("正規表現検索は有効か"));
    }

    @Test
    void motteiruKaIsHas() {
        assertEquals("has_website", first("ウェブサイトを持っているか"));
    }

    @Test
    void pluralWithShould() {
        assertEquals("should_show_ads", first("広告sを表示するか"));
    }

    // ---- 結果を分岐させる実装の選択をここで固定する ----

    @Test
    void hasDoesNotDependOnTheVerbTranslation() {
        NamingEngine possess = engine(Map.of("ウェブサイト", List.of("website"), "持つ", List.of("have", "own")));
        assertEquals("has_website",
            possess.suggest("ウェブサイトを持っているか", NamingKind.METHOD).get(0).name().render(NamingCase.SNAKE));
    }

    @Test
    void alternativeVerbStillGetsPrefix() {
        List<String> snake = engine.suggest("広告を表示するか", NamingKind.METHOD).stream()
            .map(c -> c.name().render(NamingCase.SNAKE)).toList();
        assertEquals(List.of("should_show_ad", "should_display_ad"), snake);
    }

    @Test
    void kaAloneGivesNoCandidate() {
        assertEquals(List.of(), engine.suggest("か", NamingKind.METHOD));
    }

    @Test
    void pastParticiples() {
        assertEquals("created", BooleanRule.pastParticiple("create"));
        assertEquals("enabled", BooleanRule.pastParticiple("enable"));
        assertEquals("opened", BooleanRule.pastParticiple("open"));
        assertEquals("copied", BooleanRule.pastParticiple("copy"));
        assertEquals("played", BooleanRule.pastParticiple("play"));
        assertEquals("sent", BooleanRule.pastParticiple("send"));
        assertEquals("shown", BooleanRule.pastParticiple("show"));
        assertEquals("stopped", BooleanRule.pastParticiple("stop"));
    }
}
