package namingassist.engine.tokenize;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class KuromojiTokenizerTest {
  private final Tokenizer tokenizer = new KuromojiTokenizer();

  private static List<String> surfaces(List<Token> tokens) {
    return tokens.stream().map(Token::surface).toList();
  }

  @Test
  void splitsEmployeeCountPhrase() {
    List<String> s = surfaces(tokenizer.tokenize("従業員数を取得する"));
    assertTrue(s.containsAll(List.of("従業員", "数", "を", "取得")), s.toString());
  }

  @Test
  void marksVerbBaseForm() {
    List<Token> tokens = tokenizer.tokenize("持っている");
    assertTrue(
        tokens.stream().anyMatch(t -> t.baseForm().equals("持つ") && t.pos() == Pos.VERB),
        tokens.toString());
  }

  @Test
  void keepsLatinAsOther() {
    List<Token> tokens = tokenizer.tokenize("IDを取得する");
    assertTrue(tokens.stream().anyMatch(t -> t.surface().equals("ID")), tokens.toString());
  }
}
