package namingassist.engine.dictionary;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import namingassist.engine.NamePart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DictionaryTest {

    @TempDir
    Path dir;

    private static List<String> translations(Dictionary d, String word) {
        return d.lookup(word).map(DictionaryEntry::translations).orElseThrow();
    }

    @Test
    void userDictionaryWinsOverCurated() throws IOException {
        UserDictionary user = new UserDictionary(dir.resolve("user.tsv"));
        user.save(List.of(new DictionaryEntry("取得", List.of("acquire"))));
        Dictionary layered = new LayeredDictionary(List.of(user, TsvDictionary.curated()));
        assertEquals(List.of("acquire", "get", "fetch"), translations(layered, "取得"));
    }

    @Test
    void layersAreConcatenatedWithoutDuplicates() {
        Dictionary upper = word -> Optional.of(new DictionaryEntry(word, List.of("fetch")));
        Dictionary lower = word -> Optional.of(new DictionaryEntry(word, List.of("get", "fetch")));
        assertEquals(List.of("fetch", "get"), translations(new LayeredDictionary(List.of(upper, lower)), "x"));
    }

    @Test
    void unknownWordIsEmptyInAllLayers() {
        assertTrue(new LayeredDictionary(List.of(TsvDictionary.curated())).lookup("zzz存在しない語").isEmpty());
    }

    @Test
    void missingUserFileGivesEmptyDictionary() {
        UserDictionary user = assertDoesNotThrow(() -> new UserDictionary(dir.resolve("none.tsv")));
        assertTrue(user.lookup("取得").isEmpty());
        assertCuratedStillWorks(user);
    }

    @Test
    void emptyUserFileGivesEmptyDictionary() throws IOException {
        Path f = dir.resolve("empty.tsv");
        Files.write(f, new byte[0]);
        UserDictionary user = assertDoesNotThrow(() -> new UserDictionary(f));
        assertTrue(user.lookup("取得").isEmpty());
        assertCuratedStillWorks(user);
    }

    @Test
    void corruptUserFileGivesEmptyDictionary() throws IOException {
        Path f = dir.resolve("corrupt.tsv");
        Files.write(f, new byte[] {(byte) 0xFF, (byte) 0xFE, (byte) 0x80, 0x00, (byte) 0xC3, 0x28});
        UserDictionary user = assertDoesNotThrow(() -> new UserDictionary(f));
        assertTrue(user.lookup("取得").isEmpty());
        assertFalse(user.warnings().isEmpty());
        assertCuratedStillWorks(user);
    }

    @Test
    void brokenLinesAreSkippedWithWarning() throws IOException {
        Path f = dir.resolve("partial.tsv");
        Files.writeString(f, "壊れた行\n在庫\tstock,inventory\n");
        UserDictionary user = new UserDictionary(f);
        assertEquals(List.of("stock", "inventory"), translations(user, "在庫"));
        assertEquals(1, user.warnings().size());
    }

    @Test
    void saveThenReloadRoundTrips() throws IOException {
        Path f = dir.resolve("sub/user.tsv");
        UserDictionary user = new UserDictionary(f);
        user.save(List.of(new DictionaryEntry("引当", List.of("allocate", "pay attention"))));
        assertEquals(List.of("allocate", "pay attention"), translations(new UserDictionary(f), "引当"));
        assertEquals(List.of("allocate", "pay attention"), translations(user, "引当"));
    }

    @Test
    void saveRejectsSeparatorsInTranslations() {
        UserDictionary user = new UserDictionary(dir.resolve("u.tsv"));
        assertThrows(IllegalArgumentException.class,
            () -> user.save(List.of(new DictionaryEntry("x", List.of("a,b")))));
    }

    @Test
    void curatedContainsBriefWords() {
        Dictionary curated = TsvDictionary.curated();
        assertEquals(List.of("get", "fetch"), translations(curated, "取得"));
        assertEquals(List.of("regexp"), translations(curated, "正規表現"));
        assertEquals(List.of("count", "number"), translations(curated, "数"));
    }

    @Test
    void jmdictLayerLooksUpGeneralWords() {
        assertTrue(TsvDictionary.jmdict().lookup("従業員").isPresent());
    }

    @Test
    void translationWithSpaceExpandsToWords() {
        assertEquals(List.of("pay", "attention"), EnglishWords.split("pay attention"));
        assertEquals(List.of("get"), EnglishWords.split("get"));
        List<NamePart> parts = EnglishWords.split("pay attention").stream()
            .map(w -> new NamePart(w, List.of(w), true)).toList();
        assertEquals(2, parts.size());
    }

    private void assertCuratedStillWorks(Dictionary user) {
        Dictionary layered = new LayeredDictionary(List.of(user, TsvDictionary.curated()));
        assertEquals(List.of("get", "fetch"), translations(layered, "取得"));
    }
}
