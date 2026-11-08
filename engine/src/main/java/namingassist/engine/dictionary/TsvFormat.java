package namingassist.engine.dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 「日本語<TAB>訳1,訳2,…」の行形式。読み書きの規則をここに集める。 */
final class TsvFormat {
    private TsvFormat() {}

    /** 解析結果。壊れた行は捨てて warnings に残す。 */
    record Parsed(Map<String, DictionaryEntry> entries, List<String> warnings) {}

    static Parsed parse(String text, String sourceName) {
        Map<String, DictionaryEntry> entries = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int tab = line.indexOf('\t');
            String word = tab < 0 ? "" : line.substring(0, tab).trim();
            List<String> translations = tab < 0
                ? List.of()
                : Arrays.stream(line.substring(tab + 1).split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
            if (word.isEmpty() || translations.isEmpty()) {
                warnings.add(sourceName + ":" + (i + 1) + " を読み飛ばした(形式は「日本語<TAB>訳1,訳2」)");
                continue;
            }
            entries.put(word, new DictionaryEntry(word, translations));
        }
        return new Parsed(entries, warnings);
    }

    static String format(List<DictionaryEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (DictionaryEntry e : entries) {
            if (hasSeparator(e.japanese()) || e.translations().stream().anyMatch(TsvFormat::hasSeparator)) {
                throw new IllegalArgumentException("tab, comma and newline are not allowed: " + e);
            }
            if (e.translations().isEmpty()) {
                throw new IllegalArgumentException("translations must not be empty: " + e.japanese());
            }
            sb.append(e.japanese()).append('\t').append(String.join(",", e.translations())).append('\n');
        }
        return sb.toString();
    }

    private static boolean hasSeparator(String s) {
        return s.indexOf('\t') >= 0 || s.indexOf(',') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
    }
}
