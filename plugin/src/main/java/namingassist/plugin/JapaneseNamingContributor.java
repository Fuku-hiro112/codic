package namingassist.plugin;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import namingassist.engine.Candidate;
import namingassist.engine.NamingEngine;
import namingassist.engine.NamingKind;
import org.jetbrains.annotations.NotNull;

/**
 * 補完のプレフィックスに日本語が含まれるときだけ、英語の命名候補を足す。英字だけのときは何も足さない
 * (標準の補完をそのまま残す)。候補は日本語の句でも絞り込めるよう、句を lookup string に加える。
 */
public final class JapaneseNamingContributor extends CompletionContributor {
    private static final Logger LOG = Logger.getInstance(JapaneseNamingContributor.class);
    private static final AtomicBoolean engineFailureLogged = new AtomicBoolean();

    private final ContextKindResolver kindResolver = new ContextKindResolver();

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        NamingEngineService service = ApplicationManager.getApplication().getService(NamingEngineService.class);
        String prefix = result.getPrefixMatcher().getPrefix();
        if (!containsJapanese(prefix)) {
            service.preload(); // 日本語が来る前に辞書を読み始めておく(待たない)
            return;
        }
        // 候補は句全体で決まる(「稼」では空、「稼働時刻」で operationTime)。空でも打ち足すたびに作り直す。
        result.restartCompletionOnAnyPrefixChange();
        NamingKind kind = kindResolver.resolve(parameters.getPosition());
        // 先頭の数字(2次元配列の「2」)は識別子の先頭になれずプレフィックスに入らない。句の先頭に含めて解析し、確定で消す。
        CharSequence text = parameters.getEditor().getDocument().getCharsSequence();
        String digits = leadingDigits(text, parameters.getOffset() - prefix.length());
        String phrase = digits + prefix;
        InsertHandler<LookupElement> removeDigits = digits.isEmpty() ? null : (context, item) -> {
            Document document = context.getDocument();
            int end = context.getStartOffset();
            int start = end - digits.length();
            if (start >= 0 && digits.contentEquals(document.getCharsSequence().subSequence(start, end))) {
                document.deleteString(start, end);
            }
        };
        // 候補はすべて同じ句に一致して重みが並ぶので、engine の優先順を降順の優先度で渡す(無いと辞書順に落ちる)。
        List<Candidate> candidates = suggest(service::engine, phrase, kind);
        int count = candidates.size();
        for (int i = 0; i < count; i++) {
            Candidate candidate = candidates.get(i);
            LookupElementBuilder element = LookupElementBuilder.create(candidate, candidate.rendered())
                .withLookupString(prefix) // matcher はプレフィックス(数字を含まない)で絞るので、それと一致する形を持たせる
                .withTypeText(phrase)
                .withInsertHandler(removeDigits);
            result.addElement(PrioritizedLookupElement.withPriority(element, count - i));
        }
    }

    /**
     * prefixStart の直前に続く数字(半角 0-9・全角 ０-９)。その前が識別子の一部(英字など)なら、
     * 数字は別の語に属するので空を返す。
     */
    public static String leadingDigits(CharSequence text, int prefixStart) {
        int start = prefixStart;
        while (start > 0 && isDigit(text.charAt(start - 1))) {
            start--;
        }
        if (start == prefixStart || (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1)))) {
            return "";
        }
        return text.subSequence(start, prefixStart).toString();
    }

    private static boolean isDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= '０' && c <= '９');
    }

    /**
     * 候補を作る。補完が取り消されたら ProcessCanceledException で止まる(組み合わせごとに確かめる)。
     * engine を組み立てられないときは補完へ例外を漏らさず空を返す。ログは最初の 1 回だけ出す。
     */
    static List<Candidate> suggest(Supplier<NamingEngine> engine, String prefix, NamingKind kind) {
        NamingEngine ready;
        try {
            ready = engine.get();
        } catch (IllegalStateException e) {
            if (engineFailureLogged.compareAndSet(false, true)) {
                LOG.warn("命名エンジンを使えないため、日本語の命名候補を出さない", e);
            }
            return List.of();
        }
        return ready.suggest(prefix, kind, () -> {
            ProgressManager.checkCanceled();
            return false;
        });
    }

    static boolean containsJapanese(String text) {
        return text.codePoints().anyMatch(cp -> {
            Character.UnicodeScript script = Character.UnicodeScript.of(cp);
            return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA;
        });
    }
}
