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

    private static String top(NamingEngine engine) {
        List<Candidate> candidates = engine.suggest(PHRASE, NamingKind.VARIABLE);
        return candidates.get(0).rendered();
    }
}
