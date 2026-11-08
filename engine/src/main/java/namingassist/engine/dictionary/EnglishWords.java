package namingassist.engine.dictionary;

import java.util.Arrays;
import java.util.List;

/** 辞書の訳(空白を含みうる)を英単語に展開する。 */
public final class EnglishWords {
    private EnglishWords() {}

    /** 空白とハイフンで分け、アポストロフィは落とす(time-stamp → time stamp、o'clock → oclock)。 */
    public static List<String> split(String translation) {
        String normalized = translation.replace('-', ' ').replace("'", "");
        return Arrays.stream(normalized.trim().split("\\s+")).filter(w -> !w.isEmpty()).toList();
    }
}
