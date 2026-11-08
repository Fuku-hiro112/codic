package namingassist.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 命名でよく使う基本語は、JMdict の先頭訳(値 → price、一覧 → look など)ではなく curated の訳が先頭に来る
 * (最終レビュー Important-4)。実際の Kuromoji と同梱辞書で確かめる。
 */
class BasicWordsTest {
    private static NamingEngine engine;

    @BeforeAll
    static void build() {
        engine = NamingEngines.createStandard(word -> Optional.empty());
    }

    private static String top(String phrase, NamingKind kind) {
        List<Candidate> c = engine.suggest(phrase, kind);
        assertFalse(c.isEmpty(), "候補があること: " + phrase);
        return c.get(0).rendered();
    }

    @Test
    void upperBoundPairsWithLowerBound() {
        assertEquals("upperBound", top("上限値", NamingKind.VARIABLE));
        assertEquals("lowerBound", top("下限値", NamingKind.VARIABLE));
    }

    @Test
    void valueIsValue() {
        assertEquals("maxValue", top("最大値", NamingKind.VARIABLE));
        assertEquals("minValue", top("最小値", NamingKind.VARIABLE));
        assertEquals("averageValue", top("平均値", NamingKind.VARIABLE));
        assertEquals("totalValue", top("合計値", NamingKind.VARIABLE));
    }

    @Test
    void listIsList() {
        assertEquals("notifyList", top("通知一覧", NamingKind.VARIABLE));
    }

    /** 予約語 class にしかならない語は、Java の慣習の clazz にする(候補を 0 件にしない)。 */
    @Test
    void reservedClassBecomesClazz() {
        assertEquals("clazz", top("クラス", NamingKind.VARIABLE));
        assertEquals("Class", top("クラス", NamingKind.CLASS));
        assertEquals("CLASS", top("クラス", NamingKind.CONSTANT));
    }

    @Test
    void otherBasicWords() {
        assertEquals("getTimeStamp", top("タイムスタンプを取得する", NamingKind.METHOD));
        assertEquals("twoDimensionArray", top("2次元配列", NamingKind.VARIABLE));
        assertEquals("showPosition", top("表示位置", NamingKind.VARIABLE));
        assertEquals("newUser", top("新規ユーザー", NamingKind.VARIABLE));
        assertEquals("userAuthentication", top("ユーザー認証", NamingKind.VARIABLE));
    }
}
