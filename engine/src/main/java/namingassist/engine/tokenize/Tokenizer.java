package namingassist.engine.tokenize;

import java.util.List;

public interface Tokenizer {
  List<Token> tokenize(String japanese);
}
