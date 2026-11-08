package namingassist.engine.compose;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import namingassist.engine.EnglishName;
import namingassist.engine.NamePart;
import namingassist.engine.dictionary.Dictionary;
import namingassist.engine.dictionary.DictionaryEntry;
import namingassist.engine.dictionary.EnglishWords;
import namingassist.engine.tokenize.Pos;
import namingassist.engine.tokenize.Token;

/**
 * 解析済みの語の列を英語名の候補に組み立てる。ここでは名詞句と「〜する」動詞句を扱う。
 *
 * <ul>
 *   <li>助詞(を・は・が・の など)は語にしない。名詞の並びは日本語の順のまま残す。
 *   <li>述語の動詞があれば先頭に置く(「従業員数を取得する」→ get employee count)。
 *       述語は末尾から探した最初の自立した動詞。「する」なら直前の名詞(サ変名詞)を動詞として使う。
 *   <li>英数字だけの語は辞書を引かず、小文字にして語にする。
 *   <li>名詞の直後の「s」「ｓ」は、その名詞の訳を複数形にする印として読む。
 *   <li>辞書に無い語は日本語のまま残す(resolved=false)。隣り合う未登録語は一語にまとめる。
 * </ul>
 *
 * 候補は各語の訳の組み合わせを、辞書の優先順の辞書式順序(後ろの語ほど速く変わる)で遅延生成する。
 * 未登録語の有無は句ごとに全候補で共通なので、未登録語を含む候補は常に他の候補の後ろに来る。
 */
public final class PhraseComposer {
    private static final String SURU = "する";
    /** 補助動詞として付く動詞(「持っている」の「いる」)。述語にしない。 */
    private static final Set<String> NON_INDEPENDENT_VERBS = Set.of("いる", "しまう", "おく");
    private static final Set<String> PLURAL_MARKERS = Set.of("s", "ｓ");

    private final Dictionary dictionary;

    public PhraseComposer(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    /** 候補を優先順に返す。全語が未登録なら空。 */
    public Stream<EnglishName> compose(List<Token> tokens) {
        List<Slot> slots = slots(tokens);
        if (slots.isEmpty() || slots.stream().noneMatch(Slot::resolved)) {
            return Stream.empty();
        }
        return Stream.iterate(new int[slots.size()], c -> c != null, c -> next(c, slots))
            .map(choice -> toName(choice, slots));
    }

    // ---- 語の列 → 語ごとの訳の枠 ----

    private List<Slot> slots(List<Token> tokens) {
        int predicate = predicateIndex(tokens);
        Slot verb = null;
        List<Slot> rest = new ArrayList<>();
        int lastSlotToken = -2;
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (PLURAL_MARKERS.contains(t.surface()) && lastSlotToken == i - 1
                    && tokens.get(i - 1).pos() == Pos.NOUN && !rest.isEmpty()) {
                rest.set(rest.size() - 1, rest.get(rest.size() - 1).pluralized());
                continue;
            }
            if (i != predicate && !isWord(t)) {
                continue;
            }
            Slot slot = toSlot(t);
            if (i == predicate) {
                verb = slot;
            } else if (!slot.resolved() && lastSlotToken == i - 1 && !rest.isEmpty()
                    && !rest.get(rest.size() - 1).resolved()) {
                rest.set(rest.size() - 1, rest.get(rest.size() - 1).joinedWith(slot));
                lastSlotToken = i;
            } else {
                rest.add(slot);
                lastSlotToken = i;
            }
        }
        List<Slot> ordered = new ArrayList<>();
        if (verb != null) {
            ordered.add(verb);
        }
        ordered.addAll(rest);
        return ordered;
    }

    /** 述語の動詞として訳す語の位置。「名詞+する」なら名詞の位置。無ければ -1。 */
    private static int predicateIndex(List<Token> tokens) {
        for (int i = tokens.size() - 1; i >= 0; i--) {
            Token t = tokens.get(i);
            if (t.pos() != Pos.VERB || NON_INDEPENDENT_VERBS.contains(t.baseForm())) {
                continue;
            }
            if (!SURU.equals(t.baseForm())) {
                return i;
            }
            return i > 0 && tokens.get(i - 1).pos() == Pos.NOUN ? i - 1 : -1;
        }
        return -1;
    }

    private static boolean isWord(Token t) {
        return switch (t.pos()) {
            case NOUN, ADJECTIVE, OTHER -> true;
            case VERB -> !SURU.equals(t.baseForm()) && !NON_INDEPENDENT_VERBS.contains(t.baseForm());
            case PARTICLE, AUXILIARY, SYMBOL -> false;
        };
    }

    private Slot toSlot(Token t) {
        if (isLatin(t.surface())) {
            return new Slot(List.of(List.of(t.surface().toLowerCase(Locale.ROOT))), true);
        }
        String key = t.baseForm() == null || t.baseForm().isBlank() ? t.surface() : t.baseForm();
        List<List<String>> choices = dictionary.lookup(key)
            .map(DictionaryEntry::translations).orElse(List.of()).stream()
            .filter(tr -> tr != null && !tr.isBlank()) // EnglishWords.split は null を受けない
            .map(EnglishWords::split)
            .filter(words -> !words.isEmpty())
            .toList();
        return choices.isEmpty() ? new Slot(List.of(List.of(key)), false) : new Slot(choices, true);
    }

    private static boolean isLatin(String s) {
        return !s.isEmpty() && s.chars().allMatch(c -> c < 0x80 && Character.isLetterOrDigit(c));
    }

    // ---- 枠 → 英語名(辞書式順序) ----

    /** 次の組み合わせ。末尾の枠から繰り上げる。尽きたら null。 */
    private static int[] next(int[] current, List<Slot> slots) {
        int[] n = current.clone();
        for (int j = n.length - 1; j >= 0; j--) {
            if (++n[j] < slots.get(j).choices().size()) {
                return n;
            }
            n[j] = 0;
        }
        return null;
    }

    private static EnglishName toName(int[] choice, List<Slot> slots) {
        List<NamePart> parts = new ArrayList<>();
        for (int j = 0; j < choice.length; j++) {
            Slot slot = slots.get(j);
            List<String> words = slot.choices().get(choice[j]);
            List<String> singleWordAlternatives = slot.choices().stream()
                .filter(c -> c.size() == 1).map(c -> c.get(0)).toList();
            for (String word : words) {
                List<String> alternatives = words.size() == 1 ? singleWordAlternatives : List.of(word);
                parts.add(new NamePart(word, alternatives, slot.resolved()));
            }
        }
        return new EnglishName(parts);
    }

    /**
     * 日本語の一語に対応する訳の枠。
     *
     * @param choices 優先順の訳。各訳は英単語の並び。未登録語なら日本語一つだけ
     */
    private record Slot(List<List<String>> choices, boolean resolved) {
        Slot pluralized() {
            if (!resolved) {
                return this;
            }
            Set<List<String>> plural = new LinkedHashSet<>();
            for (List<String> words : choices) {
                List<String> w = new ArrayList<>(words);
                w.set(w.size() - 1, Pluralizer.pluralize(w.get(w.size() - 1)));
                plural.add(List.copyOf(w));
            }
            return new Slot(List.copyOf(plural), true);
        }

        /** 隣り合う未登録語を一語にまとめる。 */
        Slot joinedWith(Slot next) {
            return new Slot(List.of(List.of(choices.get(0).get(0) + next.choices().get(0).get(0))), false);
        }
    }
}
