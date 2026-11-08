package namingassist.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import namingassist.engine.dictionary.Dictionary;
import namingassist.engine.dictionary.DictionaryEntry;
import namingassist.engine.tokenize.Pos;
import namingassist.engine.tokenize.Token;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 候補生成が有限の時間で終わり、取り消せること(最終レビュー Critical-1 の回帰テスト)。
 * 実際の Kuromoji と同梱辞書({@link NamingEngines#createStandard})で測る。
 */
class BoundedSuggestTest {
    private static final Duration LIMIT = Duration.ofSeconds(2);
    private static NamingEngine engine;

    @BeforeAll
    static void build() {
        engine = NamingEngines.createStandard(word -> Optional.empty());
        engine.suggest("稼働時刻", NamingKind.VARIABLE); // 辞書の読み込みを測定から外す
    }

    private static List<String> rendered(String phrase, NamingKind kind) {
        return assertTimeoutPreemptively(LIMIT,
            () -> engine.suggest(phrase, kind).stream().map(Candidate::rendered).toList(),
            "2 秒以内に終わること: " + phrase);
    }

    private static void assertAllValid(List<String> rendered) {
        assertFalse(rendered.isEmpty(), "候補があること");
        for (String r : rendered) {
            assertTrue(Character.isJavaIdentifierStart(r.codePointAt(0)), "識別子として始まること: " + r);
        }
    }

    // ---- 訳のハイフン・アポストロフィ ----

    @Test
    void hyphenatedTranslationGivesCandidate() {
        List<String> r = rendered("内蔵カメラ", NamingKind.VARIABLE);
        assertEquals("builtInCamera", r.get(0), r.toString());
    }

    @Test
    void timestampGivesCandidate() {
        List<String> r = rendered("タイムスタンプを取得する", NamingKind.METHOD);
        assertAllValid(r);
        assertTrue(r.get(0).startsWith("getTime"), r.toString());
    }

    // ---- 先頭が数字の句 ----

    @Test
    void phraseStartingWithDigitGivesCandidate() {
        List<String> r = rendered("2段階認証の有効期限", NamingKind.VARIABLE);
        assertAllValid(r);
        assertTrue(r.get(0).startsWith("twoStage"), r.toString());
    }

    // ---- 長い句 ----

    @Test
    void longPhrasesFinishInTime() {
        for (String phrase : List.of(
                "最終更新のタイムスタンプの表示位置の初期値の平均値",
                "2次元配列の要素数の最大値の平均値",
                "2次元配列の要素数の最大値の平均値の合計値の表示位置")) {
            assertAllValid(rendered(phrase, NamingKind.VARIABLE));
        }
    }

    // ---- 取り消し ----

    @Test
    void canceledSuggestStopsAndAsksCancellation() {
        AtomicInteger asked = new AtomicInteger();
        List<Candidate> c = engine.suggest("ページタイトルを取得する", NamingKind.METHOD, () -> {
            asked.incrementAndGet();
            return true;
        });
        assertTrue(asked.get() > 0, "取り消しを確かめること");
        assertEquals(List.of(), c);
    }

    @Test
    void notCanceledSuggestIsSameAsPlainSuggest() {
        assertEquals(engine.suggest("ページタイトルを取得する", NamingKind.METHOD),
            engine.suggest("ページタイトルを取得する", NamingKind.METHOD, () -> false));
    }

    // ---- 走査数の上限 ----

    /** 不正な名前(予約語)しか作れない訳が大量にあっても、走査は上限で止まる。 */
    @Test
    void scanningStopsAtLimit() {
        Dictionary onlyReserved = word -> "謎".equals(word)
            ? Optional.of(new DictionaryEntry("謎", Collections.nCopies(50_000, "int")))
            : Optional.empty();
        NamingEngine reserved = new NamingEngine(
            new FakeTokenizer(List.of(new Token("謎", "謎", Pos.NOUN))), onlyReserved);
        AtomicInteger asked = new AtomicInteger();
        List<Candidate> c = reserved.suggest("謎", NamingKind.VARIABLE, () -> {
            asked.incrementAndGet();
            return false;
        });
        assertEquals(List.of(), c);
        assertTrue(asked.get() > 0 && asked.get() <= NamingEngine.MAX_SCANNED,
            "組み合わせごとに取り消しを確かめ、走査が上限で止まること: " + asked.get());
    }
}
