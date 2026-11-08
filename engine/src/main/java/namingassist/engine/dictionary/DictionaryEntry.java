package namingassist.engine.dictionary;

import java.util.List;

/**
 * 辞書の 1 項目。
 *
 * @param translations 優先順の訳。1 要素に空白を含んでよい(例: "pay attention")
 */
public record DictionaryEntry(String japanese, List<String> translations) {
    public DictionaryEntry {
        if (japanese == null || japanese.isEmpty()) {
            throw new IllegalArgumentException("japanese must not be empty");
        }
        translations = List.copyOf(translations);
    }
}
