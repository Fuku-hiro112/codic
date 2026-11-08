package namingassist.plugin;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import namingassist.engine.Candidate;
import namingassist.engine.NamingKind;
import org.jetbrains.annotations.NotNull;

/**
 * 補完のプレフィックスに日本語が含まれるときだけ、英語の命名候補を足す。英字だけのときは何も足さない
 * (標準の補完をそのまま残す)。候補は日本語の句でも絞り込めるよう、句を lookup string に加える。
 */
public final class JapaneseNamingContributor extends CompletionContributor {
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
        for (Candidate candidate : service.engine().suggest(prefix, kind)) {
            result.addElement(LookupElementBuilder.create(candidate, candidate.rendered())
                .withLookupString(prefix)
                .withTypeText(prefix));
        }
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
