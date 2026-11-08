package namingassist.engine.dictionary;

import java.util.Arrays;
import java.util.List;

/** 辞書の訳(空白を含みうる)を英単語に展開する。 */
public final class EnglishWords {
    private EnglishWords() {}

    public static List<String> split(String translation) {
        return Arrays.stream(translation.trim().split("\\s+")).filter(w -> !w.isEmpty()).toList();
    }
}
