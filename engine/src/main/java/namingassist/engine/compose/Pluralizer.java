package namingassist.engine.compose;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 英単語を複数形にする。不規則形と不可算名詞は表で、それ以外は規則で作る。 */
public final class Pluralizer {
    private static final Map<String, String> IRREGULAR = Map.ofEntries(
        Map.entry("person", "people"),
        Map.entry("child", "children"),
        Map.entry("man", "men"),
        Map.entry("woman", "women"),
        Map.entry("foot", "feet"),
        Map.entry("tooth", "teeth"),
        Map.entry("mouse", "mice"),
        Map.entry("goose", "geese"),
        Map.entry("datum", "data"),
        Map.entry("medium", "media"),
        Map.entry("criterion", "criteria"),
        Map.entry("index", "indices"),
        Map.entry("matrix", "matrices"),
        Map.entry("vertex", "vertices"),
        Map.entry("analysis", "analyses"),
        Map.entry("leaf", "leaves"),
        Map.entry("half", "halves"),
        Map.entry("knife", "knives"),
        Map.entry("life", "lives"),
        Map.entry("wife", "wives"));

    private static final Set<String> UNCOUNTABLE = Set.of(
        "data", "information", "equipment", "software", "hardware", "metadata",
        "news", "series", "species", "sheep", "fish", "feedback", "advice", "people");

    private Pluralizer() {}

    public static String pluralize(String englishWord) {
        String w = englishWord.toLowerCase(Locale.ROOT);
        if (w.isEmpty() || UNCOUNTABLE.contains(w)) {
            return w;
        }
        String irregular = IRREGULAR.get(w);
        if (irregular != null) {
            return irregular;
        }
        if (w.endsWith("s") || w.endsWith("x") || w.endsWith("z") || w.endsWith("ch") || w.endsWith("sh")) {
            return w + "es";
        }
        if (w.length() >= 2 && w.endsWith("y") && !isVowel(w.charAt(w.length() - 2))) {
            return w.substring(0, w.length() - 1) + "ies";
        }
        return w + "s";
    }

    private static boolean isVowel(char c) {
        return "aeiou".indexOf(c) >= 0;
    }
}
