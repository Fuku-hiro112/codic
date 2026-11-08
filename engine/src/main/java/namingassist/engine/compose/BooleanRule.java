package namingassist.engine.compose;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import namingassist.engine.tokenize.Pos;
import namingassist.engine.tokenize.Token;

/**
 * 「〜か」で終わる句を真偽値の名前の形に読む。
 *
 * <ul>
 *   <li>受け身(「ファイルは作成されているか」)→ is + 主語 + 過去分詞(is file created)
 *   <li>述語が「持つ」(「ウェブサイトを持っているか」)→ has + 目的語。「持つ」の訳は使わない
 *   <li>その他の動詞が述語(「広告を表示するか」)→ should + 動詞 + 目的語
 *   <li>述語の動詞が無い(「有料プランか」「正規表現検索は有効か」)→ is + 句(日本語の順のまま)
 * </ul>
 */
final class BooleanRule {
    private static final String KA = "か";
    private static final String HAVE = "持つ";
    /** 受け身の接尾(「作成さ・れ・て・いる」の「れ」)。語にも述語にもしない。 */
    private static final Set<String> PASSIVE_SUFFIXES = Set.of("れる", "られる");

    /** 述語の動詞の置き場所。 */
    enum VerbPlace { FRONT, LAST_AS_PAST_PARTICIPLE, DROPPED }

    /**
     * @param prefix    先頭に付ける語(should / is / has)
     * @param body      「か」と受け身の接尾を除いた語の列
     * @param verbPlace 述語の動詞をどこに置くか
     */
    record Form(String prefix, List<Token> body, VerbPlace verbPlace) {
    }

    private BooleanRule() {
    }

    /** 「か」で終わる句なら真偽値の形。そうでなければ空。 */
    static Optional<Form> of(List<Token> tokens) {
        if (tokens.isEmpty() || !isKa(tokens.get(tokens.size() - 1))) {
            return Optional.empty();
        }
        List<Token> withoutKa = tokens.subList(0, tokens.size() - 1);
        List<Token> body = withoutKa.stream().filter(t -> !isPassiveSuffix(t)).toList();
        boolean passive = body.size() < withoutKa.size();
        int predicate = PhraseComposer.predicateIndex(body);
        if (predicate < 0) {
            return Optional.of(new Form("is", body, VerbPlace.FRONT));
        }
        if (passive) {
            return Optional.of(new Form("is", body, VerbPlace.LAST_AS_PAST_PARTICIPLE));
        }
        Token verb = body.get(predicate);
        if (verb.pos() == Pos.VERB && HAVE.equals(verb.baseForm())) {
            return Optional.of(new Form("has", body, VerbPlace.DROPPED));
        }
        return Optional.of(new Form("should", body, VerbPlace.FRONT));
    }

    private static boolean isKa(Token t) {
        return t.pos() == Pos.PARTICLE && KA.equals(t.surface());
    }

    private static boolean isPassiveSuffix(Token t) {
        return t.pos() == Pos.VERB && PASSIVE_SUFFIXES.contains(t.baseForm());
    }

    // ---- 過去分詞 ----

    private static final Map<String, String> IRREGULAR = Map.ofEntries(
        Map.entry("begin", "begun"), Map.entry("bind", "bound"), Map.entry("break", "broken"),
        Map.entry("bring", "brought"), Map.entry("build", "built"), Map.entry("buy", "bought"),
        Map.entry("choose", "chosen"), Map.entry("cut", "cut"), Map.entry("do", "done"),
        Map.entry("draw", "drawn"), Map.entry("find", "found"), Map.entry("forget", "forgotten"),
        Map.entry("freeze", "frozen"), Map.entry("get", "got"), Map.entry("give", "given"),
        Map.entry("go", "gone"), Map.entry("have", "had"), Map.entry("hide", "hidden"),
        Map.entry("hit", "hit"), Map.entry("hold", "held"), Map.entry("keep", "kept"),
        Map.entry("know", "known"), Map.entry("lay", "laid"), Map.entry("leave", "left"),
        Map.entry("let", "let"), Map.entry("lose", "lost"), Map.entry("make", "made"),
        Map.entry("mean", "meant"), Map.entry("meet", "met"), Map.entry("override", "overridden"),
        Map.entry("pay", "paid"), Map.entry("put", "put"), Map.entry("read", "read"),
        Map.entry("rebuild", "rebuilt"), Map.entry("reset", "reset"), Map.entry("rewrite", "rewritten"),
        Map.entry("run", "run"), Map.entry("see", "seen"), Map.entry("sell", "sold"),
        Map.entry("send", "sent"), Map.entry("set", "set"), Map.entry("show", "shown"),
        Map.entry("shut", "shut"), Map.entry("spend", "spent"), Map.entry("split", "split"),
        Map.entry("take", "taken"), Map.entry("tell", "told"), Map.entry("throw", "thrown"),
        Map.entry("understand", "understood"), Map.entry("win", "won"), Map.entry("write", "written"));

    /** 語尾の子音を重ねてから ed を付ける動詞(stop → stopped)。規則で判定せず表で持つ。 */
    private static final Set<String> DOUBLED = Set.of(
        "commit", "control", "drop", "log", "map", "permit", "pin", "plan", "plug", "prefer",
        "refer", "scan", "ship", "skip", "stop", "submit", "tag", "transfer", "wrap");

    /** 英語の動詞(小文字の一語)の過去分詞。不規則表 → 子音重ね表 → 規則(e→d、子音+y→ied、他は ed)。 */
    static String pastParticiple(String verb) {
        String irregular = IRREGULAR.get(verb);
        if (irregular != null) {
            return irregular;
        }
        if (DOUBLED.contains(verb)) {
            return verb + verb.charAt(verb.length() - 1) + "ed";
        }
        if (verb.endsWith("e")) {
            return verb + "d";
        }
        if (verb.length() > 1 && verb.endsWith("y") && "aeiou".indexOf(verb.charAt(verb.length() - 2)) < 0) {
            return verb.substring(0, verb.length() - 1) + "ied";
        }
        return verb + "ed";
    }
}
