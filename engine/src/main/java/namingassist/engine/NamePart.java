package namingassist.engine;

import java.util.List;

/**
 * 名前の部品。
 *
 * @param alternatives text を含む別訳
 * @param resolved     辞書で英語に解決できたか。false なら text は元の日本語など
 */
public record NamePart(String text, List<String> alternatives, boolean resolved) {
    public NamePart {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("text must not be empty");
        }
        if (alternatives == null) {
            throw new IllegalArgumentException("alternatives must not be null");
        }
        alternatives = List.copyOf(alternatives);
    }
}
