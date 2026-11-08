package namingassist.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import namingassist.engine.dictionary.Dictionary;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * 命名の質の回帰テスト。golden-examples.tsv の各行を、実際の Kuromoji と同梱辞書
 * ({@link NamingEngines#createStandard})で評価し、先頭候補が期待と一致することを確かめる。
 */
class GoldenExamplesTest {
    private static final String EXAMPLES = "/golden-examples.tsv";
    private static final Dictionary EMPTY_USER_LAYER = word -> Optional.empty();

    private record Example(int line, String phrase, NamingKind kind, NamingCase namingCase, String expected) {
    }

    @TestFactory
    Stream<DynamicTest> topCandidateMatchesExpected() throws IOException {
        NamingEngine engine = NamingEngines.createStandard(EMPTY_USER_LAYER);
        List<Example> examples = load();
        assertFalse(examples.isEmpty(), "例が 1 件も無い: " + EXAMPLES);
        return examples.stream().map(ex -> DynamicTest.dynamicTest(
            ex.line() + ": " + ex.phrase() + " → " + ex.expected(),
            () -> {
                List<Candidate> candidates = engine.suggest(ex.phrase(), ex.kind());
                List<String> rendered = candidates.stream()
                    .map(c -> c.name().render(ex.namingCase())).toList();
                assertFalse(rendered.isEmpty(), "候補が無い: " + ex.phrase());
                assertEquals(ex.expected(), rendered.get(0), "候補: " + rendered);
            }));
    }

    private static List<Example> load() throws IOException {
        String text;
        try (InputStream in = GoldenExamplesTest.class.getResourceAsStream(EXAMPLES)) {
            if (in == null) {
                throw new IllegalStateException("missing: " + EXAMPLES);
            }
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<Example> examples = new ArrayList<>();
        String[] lines = text.split("\r?\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] cols = line.split("\t", -1);
            if (cols.length != 4) {
                throw new IllegalStateException(EXAMPLES + ":" + (i + 1) + " は 4 列ではない: " + line);
            }
            examples.add(new Example(i + 1, cols[0].strip(), NamingKind.valueOf(cols[1].strip()),
                NamingCase.valueOf(cols[2].strip()), cols[3].strip()));
        }
        return examples;
    }
}
