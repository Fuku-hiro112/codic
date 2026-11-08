package namingassist.plugin;

import com.intellij.codeInsight.lookup.LookupElement;
import java.util.Arrays;
import java.util.List;
import namingassist.engine.Candidate;

/** テストの共通処理。本プラグイン由来の候補は、lookup object が {@link Candidate} のものとして見分ける。 */
final class PluginLookups {
    private PluginLookups() {
    }

    static List<String> fromPlugin(LookupElement[] elements) {
        if (elements == null) {
            return List.of();
        }
        return Arrays.stream(elements)
            .filter(e -> e.getObject() instanceof Candidate)
            .map(LookupElement::getLookupString)
            .toList();
    }

    static List<String> all(LookupElement[] elements) {
        if (elements == null) {
            return List.of();
        }
        return Arrays.stream(elements).map(LookupElement::getLookupString).toList();
    }
}
