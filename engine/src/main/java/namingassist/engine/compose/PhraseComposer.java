package namingassist.engine.compose;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
 *   <li>「か」で終わる句は should / is / has を先頭に付けた真偽値の名前にする({@link BooleanRule})。
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

    /** 候補を優先順に返す。全語が未登録なら空。「か」で終わる句は真偽値の形にする({@link BooleanRule})。 */
    public Stream<EnglishName> compose(List<Token> tokens) {
        Optional<BooleanRule.Form> bool = BooleanRule.of(tokens);
        Clause clause = clause(bool.map(BooleanRule.Form::body).orElse(tokens));
        List<Slot> slots = bool.map(form -> booleanSlots(form, clause)).orElseGet(clause::ordered);
        if (slots.isEmpty() || slots.stream().noneMatch(Slot::resolved)) {
            return Stream.empty();
        }
        bool.ifPresent(form -> slots.add(0, new Slot(List.of(List.of(form.prefix())), true)));
        if (!makeLeadingSlotStartIdentifier(slots)) {
            return Stream.empty();
        }
        return Stream.iterate(new int[slots.size()], c -> c != null, c -> next(c, slots))
            .map(choice -> toName(choice, slots));
    }

    // ---- 先頭の枠: 識別子の先頭に置ける訳だけにする ----

    private static final List<String> NUMBER_WORDS = List.of(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve");

    /**
     * 先頭の枠を、識別子の先頭に置ける訳(数字で始まらない)だけにする。どの訳も置けないとき、
     * 12 以下の数は英語の数詞にし(2段階 → two stage)、それ以外はその枠を次の枠の後ろへ回す。
     * 名前を作れないなら false。
     */
    private static boolean makeLeadingSlotStartIdentifier(List<Slot> slots) {
        for (int moved = 0; moved < slots.size(); moved++) {
            Slot first = slots.get(0);
            List<List<String>> startable = first.choices().stream()
                .filter(words -> Character.isJavaIdentifierStart(words.get(0).codePointAt(0))).toList();
            if (!startable.isEmpty()) {
                slots.set(0, new Slot(startable, first.resolved()));
                return true;
            }
            Optional<String> number = numberWord(first);
            if (number.isPresent()) {
                slots.set(0, new Slot(List.of(List.of(number.get())), true));
                return true;
            }
            if (slots.size() < 2) {
                return false;
            }
            slots.add(1, slots.remove(0));
        }
        return false;
    }

    private static Optional<String> numberWord(Slot slot) {
        if (slot.choices().size() != 1 || slot.choices().get(0).size() != 1) {
            return Optional.empty();
        }
        String s = slot.choices().get(0).get(0);
        if (s.isEmpty() || s.length() > 2 || !s.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return Optional.empty();
        }
        int n = Integer.parseInt(s);
        return n < NUMBER_WORDS.size() ? Optional.of(NUMBER_WORDS.get(n)) : Optional.empty();
    }

    /** 接頭辞を除いた真偽値の名前の枠。 */
    private static List<Slot> booleanSlots(BooleanRule.Form form, Clause clause) {
        return switch (form.verbPlace()) {
            case FRONT -> clause.ordered();
            case DROPPED -> new ArrayList<>(clause.rest());
            case LAST_AS_PAST_PARTICIPLE -> {
                List<Slot> s = new ArrayList<>(clause.rest());
                s.add(clause.verb().pastParticiple());
                yield s;
            }
        };
    }

    // ---- 語の列 → 語ごとの訳の枠 ----

    /** 述語の動詞の枠(無ければ null)と、それ以外の語の枠(日本語の順)。 */
    private record Clause(Slot verb, List<Slot> rest) {
        /** 述語を先頭に置いた並び。変更できるリスト。 */
        List<Slot> ordered() {
            List<Slot> ordered = new ArrayList<>();
            if (verb != null) {
                ordered.add(verb);
            }
            ordered.addAll(rest);
            return ordered;
        }
    }

    private Clause clause(List<Token> tokens) {
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
        return new Clause(verb, rest);
    }

    /** 述語の動詞として訳す語の位置。「名詞+する」なら名詞の位置。無ければ -1。 */
    static int predicateIndex(List<Token> tokens) {
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
            .filter(words -> !words.isEmpty() && words.stream().allMatch(PhraseComposer::isIdentifierPart))
            .toList();
        return choices.isEmpty() ? new Slot(List.of(List.of(key)), false) : new Slot(choices, true);
    }

    /** 識別子の途中に置ける語か。置けない訳は枠に入れない(組み合わせを作った後で除くと積を全部走査しうる)。 */
    private static boolean isIdentifierPart(String word) {
        return word.codePoints().allMatch(Character::isJavaIdentifierPart);
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

        /** 動詞の訳を過去分詞にする(複数語の訳は最初の語)。 */
        Slot pastParticiple() {
            if (!resolved) {
                return this;
            }
            Set<List<String>> participles = new LinkedHashSet<>();
            for (List<String> words : choices) {
                List<String> w = new ArrayList<>(words);
                w.set(0, BooleanRule.pastParticiple(w.get(0)));
                participles.add(List.copyOf(w));
            }
            return new Slot(List.copyOf(participles), true);
        }

        /** 隣り合う未登録語を一語にまとめる。 */
        Slot joinedWith(Slot next) {
            return new Slot(List.of(List.of(choices.get(0).get(0) + next.choices().get(0).get(0))), false);
        }
    }
}
