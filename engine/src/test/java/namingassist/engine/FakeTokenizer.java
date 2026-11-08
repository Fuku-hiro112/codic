package namingassist.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import namingassist.engine.tokenize.Token;
import namingassist.engine.tokenize.Tokenizer;

/**
 * テスト用の {@link Tokenizer}。渡した Token の表層形を語の表として持ち、入力を最長一致で区切る。
 * 表に無い文字に当たったら、テストの書き損じとして例外にする。
 *
 * <p>Token の形は実際の KuromojiTokenizer の出力に合わせて渡すこと
 * (例: 「取得する」は 取得=NOUN + する=VERB、半角 s は OTHER、全角 ｓ は SYMBOL)。
 */
public final class FakeTokenizer implements Tokenizer {
    private final Map<String, Token> bySurface;
    private final int longest;

    public FakeTokenizer(List<Token> vocabulary) {
        this.bySurface = vocabulary.stream()
            .collect(Collectors.toMap(Token::surface, Function.identity(), (a, b) -> b));
        this.longest = bySurface.keySet().stream().mapToInt(String::length).max().orElse(0);
    }

    @Override
    public List<Token> tokenize(String japanese) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        while (i < japanese.length()) {
            Token hit = null;
            for (int len = Math.min(longest, japanese.length() - i); len > 0; len--) {
                hit = bySurface.get(japanese.substring(i, i + len));
                if (hit != null) {
                    break;
                }
            }
            if (hit == null) {
                throw new IllegalArgumentException("FakeTokenizer has no word at: " + japanese.substring(i));
            }
            out.add(hit);
            i += hit.surface().length();
        }
        return out;
    }
}
