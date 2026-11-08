package namingassist.engine.tokenize;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/** Kuromoji(IPADIC)による {@link Tokenizer}。品詞は IPADIC の第1階層を {@link Pos} に対応させる。 */
public final class KuromojiTokenizer implements Tokenizer {
  private static final String USER_DICTIONARY = "user-dictionary.csv";

  private final com.atilika.kuromoji.ipadic.Tokenizer delegate;

  public KuromojiTokenizer() {
    try (InputStream in = KuromojiTokenizer.class.getResourceAsStream(USER_DICTIONARY)) {
      var builder = new com.atilika.kuromoji.ipadic.Tokenizer.Builder();
      if (in != null) {
        builder.userDictionary(in);
      }
      this.delegate = builder.build();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public List<Token> tokenize(String japanese) {
    return delegate.tokenize(japanese).stream().map(KuromojiTokenizer::toToken).toList();
  }

  private static Token toToken(com.atilika.kuromoji.ipadic.Token t) {
    String surface = t.getSurface();
    // 未知語・外来語は baseForm が "*" になるので表層形で代用する
    String base = "*".equals(t.getBaseForm()) ? surface : t.getBaseForm();
    return new Token(surface, base, toPos(surface, t.getPartOfSpeechLevel1()));
  }

  private static Pos toPos(String surface, String ipadicPos) {
    if (surface.chars().allMatch(c -> c < 0x80 && Character.isLetterOrDigit(c))) {
      return Pos.OTHER; // ID など英数字はそのまま残る語
    }
    return switch (ipadicPos) {
      case "名詞" -> Pos.NOUN;
      case "動詞" -> Pos.VERB;
      case "形容詞" -> Pos.ADJECTIVE;
      case "助詞" -> Pos.PARTICLE;
      case "助動詞" -> Pos.AUXILIARY;
      case "記号" -> Pos.SYMBOL;
      default -> Pos.OTHER;
    };
  }
}
