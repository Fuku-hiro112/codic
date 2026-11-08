package namingassist.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import namingassist.engine.dictionary.Dictionary;
import namingassist.engine.dictionary.DictionaryEntry;
import org.junit.jupiter.api.Test;

/** {@link NamingEngines#createStandard} の層の順序。利用者の層が同梱辞書(curated)より優先される。 */
class NamingEnginesTest {
    private static final String PHRASE = "ページタイトルを取得する";

    @Test
    void emptyUserLayerUsesCuratedFirstTranslation() {
        NamingEngine engine = NamingEngines.createStandard(word -> Optional.empty());
        assertEquals("getPageTitle", top(engine));
    }

    @Test
    void nonEmptyUserLayerTakesPrecedenceOverCurated() {
        Dictionary user = word -> "取得".equals(word)
            ? Optional.of(new DictionaryEntry("取得", List.of("fetch")))
            : Optional.empty();
        NamingEngine engine = NamingEngines.createStandard(user);
        assertEquals("fetchPageTitle", top(engine));
    }

    /** 利用者の層に登録した複合語は、Kuromoji が分けても 1 語として引く(最終レビュー Important-1)。 */
    @Test
    void userCompoundWordIsUsedAsOneWord() {
        Dictionary user = word -> "在庫引当".equals(word)
            ? Optional.of(new DictionaryEntry("在庫引当", List.of("stock allocation")))
            : Optional.empty();
        NamingEngine engine = NamingEngines.createStandard(user);
        assertEquals("getStockAllocation",
            engine.suggest("在庫引当を取得する", NamingKind.METHOD).get(0).rendered());
        assertEquals("stockAllocation", engine.suggest("在庫引当", NamingKind.VARIABLE).get(0).rendered());
        assertEquals("stockAllocationCount",
            engine.suggest("在庫引当数", NamingKind.VARIABLE).get(0).rendered());
    }

    /** 複合語の照合は最長一致。短い方の登録があっても、長い方の登録が勝つ。 */
    @Test
    void longestUserCompoundWins() {
        java.util.Map<String, List<String>> words = java.util.Map.of(
            "在庫引当", List.of("stock allocation"),
            "在庫引当日", List.of("allocation date"));
        Dictionary user = word -> Optional.ofNullable(words.get(word)).map(t -> new DictionaryEntry(word, t));
        NamingEngine engine = NamingEngines.createStandard(user);
        assertEquals("allocationDate", engine.suggest("在庫引当日", NamingKind.VARIABLE).get(0).rendered());
    }

    /** 全角英数字・半角カタカナは NFKC で正規化してから解析する(最終レビュー Important-3)。 */
    @Test
    void fullWidthLatinAndHalfWidthKanaAreNormalized() {
        NamingEngine engine = NamingEngines.createStandard(word -> Optional.empty());
        assertEquals("getId", engine.suggest("ＩＤを取得する", NamingKind.METHOD).get(0).rendered());
        assertEquals("getPageTitle", engine.suggest("ﾍﾟｰｼﾞﾀｲﾄﾙを取得する", NamingKind.METHOD).get(0).rendered());
    }

    private static String top(NamingEngine engine) {
        List<Candidate> candidates = engine.suggest(PHRASE, NamingKind.VARIABLE);
        return candidates.get(0).rendered();
    }
}
