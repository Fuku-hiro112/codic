package namingassist.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import namingassist.engine.compose.PhraseComposer;
import namingassist.engine.dictionary.Dictionary;
import namingassist.engine.tokenize.Tokenizer;

/** 日本語句から命名候補を作る。状態を持たない(同じ入力と辞書から同じ候補を返す)。 */
public final class NamingEngine {
    private static final int MAX_CANDIDATES = 10;
    /** 1 回の候補生成で調べる組み合わせの上限。不正な名前ばかりの句でも、ここで打ち切る。 */
    static final int MAX_SCANNED = 1_000;

    private final Tokenizer tokenizer;
    private final PhraseComposer composer;

    public NamingEngine(Tokenizer tokenizer, Dictionary dictionary) {
        this.tokenizer = tokenizer;
        this.composer = new PhraseComposer(dictionary);
    }

    /** 候補を優先順に最大 10 件返す。作れないときは空。Java の識別子にならない名前は除く。 */
    public List<Candidate> suggest(String japanesePhrase, NamingKind kind) {
        return suggest(japanesePhrase, kind, () -> false);
    }

    /**
     * {@link #suggest(String, NamingKind)} と同じ。組み合わせを 1 つ調べるたびに canceled を確かめ、
     * true ならそこで打ち切って、それまでの候補を返す。canceled が例外を投げれば、そのまま呼び出し側へ伝わる
     * (IntelliJ の取り消しを呼び出し側で投げられるように)。
     */
    public List<Candidate> suggest(String japanesePhrase, NamingKind kind, BooleanSupplier canceled) {
        if (japanesePhrase == null || japanesePhrase.isBlank()) {
            return List.of();
        }
        NamingCase c = kind.defaultCase();
        List<Candidate> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Iterator<EnglishName> names = composer.compose(tokenizer.tokenize(japanesePhrase.strip()))
            .limit(MAX_SCANNED).iterator();
        while (candidates.size() < MAX_CANDIDATES && names.hasNext()) {
            if (canceled.getAsBoolean()) {
                break;
            }
            EnglishName name = names.next();
            String rendered = name.render(c);
            if (name.isValidJavaIdentifier(c) && seen.add(rendered)) {
                candidates.add(new Candidate(name, rendered));
            }
        }
        return List.copyOf(candidates);
    }
}
