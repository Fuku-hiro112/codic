package namingassist.engine.dictionary;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** 優先順の層を合成する。訳は上位の層を先に、下位の層を後ろに連結し、重複を除く。 */
public final class LayeredDictionary implements Dictionary {
    private final List<Dictionary> layers;

    public LayeredDictionary(List<Dictionary> layersInPriorityOrder) {
        this.layers = List.copyOf(layersInPriorityOrder);
    }

    @Override
    public Optional<DictionaryEntry> lookup(String japaneseWord) {
        Set<String> merged = new LinkedHashSet<>();
        for (Dictionary layer : layers) {
            layer.lookup(japaneseWord).ifPresent(e -> merged.addAll(e.translations()));
        }
        return merged.isEmpty()
            ? Optional.empty()
            : Optional.of(new DictionaryEntry(japaneseWord, List.copyOf(merged)));
    }
}
