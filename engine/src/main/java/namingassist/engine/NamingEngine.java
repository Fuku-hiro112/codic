package namingassist.engine;

import java.util.List;
import namingassist.engine.compose.PhraseComposer;
import namingassist.engine.dictionary.Dictionary;
import namingassist.engine.tokenize.Tokenizer;

/** 日本語句から命名候補を作る。状態を持たない(同じ入力と辞書から同じ候補を返す)。 */
public final class NamingEngine {
    private static final int MAX_CANDIDATES = 10;

    private final Tokenizer tokenizer;
    private final PhraseComposer composer;

    public NamingEngine(Tokenizer tokenizer, Dictionary dictionary) {
        this.tokenizer = tokenizer;
        this.composer = new PhraseComposer(dictionary);
    }

    /** 候補を優先順に最大 10 件返す。作れないときは空。Java の識別子にならない名前は除く。 */
    public List<Candidate> suggest(String japanesePhrase, NamingKind kind) {
        if (japanesePhrase == null || japanesePhrase.isBlank()) {
            return List.of();
        }
        NamingCase c = kind.defaultCase();
        return composer.compose(tokenizer.tokenize(japanesePhrase.strip()))
            .filter(name -> name.isValidJavaIdentifier(c))
            .limit(MAX_CANDIDATES)
            .map(name -> new Candidate(name, name.render(c)))
            .toList();
    }
}
