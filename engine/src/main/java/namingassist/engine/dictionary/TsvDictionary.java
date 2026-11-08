package namingassist.engine.dictionary;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/** 同梱の読み取り専用辞書(クラスパス上の TSV)。 */
public final class TsvDictionary implements Dictionary {
    private final Map<String, DictionaryEntry> entries;

    private TsvDictionary(Map<String, DictionaryEntry> entries) {
        this.entries = entries;
    }

    /** 自前のプログラミング用語辞書。 */
    public static TsvDictionary curated() {
        return fromResource("/dictionary/curated.tsv");
    }

    /** JMdict から命名用に加工した汎用辞書。 */
    public static TsvDictionary jmdict() {
        return fromResource("/dictionary/jmdict.tsv");
    }

    private static TsvDictionary fromResource(String path) {
        try (InputStream in = TsvDictionary.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("bundled dictionary missing: " + path);
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return new TsvDictionary(TsvFormat.parse(text, path).entries());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<DictionaryEntry> lookup(String japaneseWord) {
        return Optional.ofNullable(entries.get(japaneseWord));
    }
}
