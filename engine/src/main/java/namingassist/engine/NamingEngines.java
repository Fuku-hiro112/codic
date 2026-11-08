package namingassist.engine;

import java.util.List;
import namingassist.engine.dictionary.Dictionary;
import namingassist.engine.dictionary.LayeredDictionary;
import namingassist.engine.dictionary.TsvDictionary;
import namingassist.engine.tokenize.KuromojiTokenizer;

/** 標準の {@link NamingEngine} の組み立て。回帰テストとプラグインが同じ組み立てを使う。 */
public final class NamingEngines {
    private NamingEngines() {
    }

    /**
     * Kuromoji と、層 利用者 → curated → jmdict の辞書で組み立てる。同梱辞書はクラスパスから読む。
     *
     * @param userLayer 最優先の利用者の層(呼び出し側が持つ。無ければ空の辞書を渡す)
     */
    public static NamingEngine createStandard(Dictionary userLayer) {
        Dictionary dictionary = new LayeredDictionary(
            List.of(userLayer, TsvDictionary.curated(), TsvDictionary.jmdict()));
        return new NamingEngine(new KuromojiTokenizer(), dictionary);
    }
}
