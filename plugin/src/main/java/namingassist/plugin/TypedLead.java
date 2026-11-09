package namingassist.plugin;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import namingassist.engine.NamingCase;

/**
 * 補完のプレフィックスの先頭にある、打ち済みの識別子(lead)。pageDecisionああ の pageDecision。
 * lead は訳さずにそのまま残し、日本語の部分の英語を命名規則に沿って後ろにつなぐ。
 */
public final class TypedLead {
    private TypedLead() {
    }

    /** 略語の直後にあれば、略語を句の一部として訳す格助詞。 */
    private static final List<String> PARTICLES = List.of("を", "の", "が", "に", "で", "と", "へ", "から");
    /** 1 語の小文字(小文字で始まり、小文字と数字だけ。id・url・json・md5)。 */
    private static final Pattern ONE_LOWERCASE_WORD = Pattern.compile("[a-z][a-z0-9]*");

    /**
     * prefix の先頭の英字の連なり(英字・数字・_・$。全角は NFKC で半角にそろえて判定する)のうち、
     * 後ろに日本語などが続くもの。半角にそろえた文字列を返す(prefix の先頭の同じ文字数に当たる)。
     * 次のときは略語とみなし、句の一部として訳すので空を返す。
     * <ul>
     *   <li>小文字を含まない(ID・URL など大文字・数字・_・$ だけ。IDを取得する → getId)</li>
     *   <li>1 語の小文字で、後ろの日本語が格助詞で始まる(idを取得する → getId、urlを生成する)</li>
     * </ul>
     */
    public static String leadOf(String prefix) {
        StringBuilder lead = new StringBuilder();
        boolean hasLower = false;
        while (lead.length() < prefix.length()) {
            char ch = asciiOf(prefix.charAt(lead.length()));
            if (!isAsciiIdentifierPart(ch)) {
                break;
            }
            hasLower |= ch >= 'a' && ch <= 'z';
            lead.append(ch);
        }
        int end = lead.length();
        if (end == 0 || end == prefix.length() || !hasLower || !Character.isJavaIdentifierStart(lead.charAt(0))) {
            return "";
        }
        String rest = prefix.substring(end);
        if (ONE_LOWERCASE_WORD.matcher(lead).matches() && PARTICLES.stream().anyMatch(rest::startsWith)) {
            return "";
        }
        return lead.toString();
    }

    /** 1 文字を NFKC で正規化し、ASCII の 1 文字になればそれ、ならなければ 0(全角の ｉ → i)。 */
    private static char asciiOf(char ch) {
        if (ch < 0x80) {
            return ch;
        }
        String n = Normalizer.normalize(String.valueOf(ch), Normalizer.Form.NFKC);
        return n.length() == 1 && n.charAt(0) < 0x80 ? n.charAt(0) : 0;
    }

    /**
     * lead の後ろに、規則 c で綴った英語 rendered をつなぐ。lead が空なら rendered のまま。
     * <ul>
     *   <li>CAMEL: lead + rendered の先頭を大文字に(pageDecision + likeThat → pageDecisionLikeThat)</li>
     *   <li>PASCAL: lead + rendered(rendered は既に先頭が大文字)</li>
     *   <li>SNAKE: lead + _ + rendered</li>
     *   <li>UPPER_SNAKE: lead を大文字にし(語の境目に _ を入れる)、_ + rendered
     *       (max + OPERATION_TIME → MAX_OPERATION_TIME、maxValue → MAX_VALUE_…)</li>
     *   <li>KEBAB: lead + - + rendered</li>
     * </ul>
     * SNAKE・UPPER_SNAKE で lead が _ で終わるときは、_ を重ねない。
     * rendered の先頭の語が lead と同じ(大文字小文字を問わない)で、後ろに語が続くなら、その語を重ねない
     * (get + getPageTitle → getPageTitle、is + isPaidPlan → isPaidPlan)。
     */
    public static String join(String lead, String rendered, NamingCase c) {
        if (lead.isEmpty() || rendered.isEmpty()) {
            return lead + rendered;
        }
        rendered = withoutRepeatedWord(lead, rendered, c);
        return switch (c) {
            case CAMEL -> lead + Character.toUpperCase(rendered.charAt(0)) + rendered.substring(1);
            case PASCAL -> lead + rendered;
            case SNAKE -> lead + (lead.endsWith("_") ? "" : "_") + rendered;
            case UPPER_SNAKE -> upperSnake(lead) + (lead.endsWith("_") ? "" : "_") + rendered;
            case KEBAB -> lead + "-" + rendered;
        };
    }

    /** rendered の先頭の語が lead と同じなら、その語(と区切り)を除いた残り。1 語だけなら除かない。 */
    private static String withoutRepeatedWord(String lead, String rendered, NamingCase c) {
        int end = switch (c) {
            case CAMEL, PASCAL -> firstUpperFrom(rendered, 1);
            case SNAKE, UPPER_SNAKE -> rendered.indexOf('_');
            case KEBAB -> rendered.indexOf('-');
        };
        if (end <= 0 || !rendered.substring(0, end).equalsIgnoreCase(lead)) {
            return rendered;
        }
        String rest = c == NamingCase.CAMEL || c == NamingCase.PASCAL ? rendered.substring(end) : rendered.substring(end + 1);
        return rest.isEmpty() ? rendered : rest;
    }

    private static int firstUpperFrom(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (Character.isUpperCase(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /** 大文字にし、語の境目(小文字・数字の直後の大文字)に _ を入れる(maxValue → MAX_VALUE)。 */
    private static String upperSnake(String lead) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lead.length(); i++) {
            char ch = lead.charAt(i);
            if (i > 0 && Character.isUpperCase(ch) && isLowerOrDigit(lead.charAt(i - 1))) {
                sb.append('_');
            }
            sb.append(ch);
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }

    private static boolean isLowerOrDigit(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
    }

    private static boolean isAsciiIdentifierPart(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '$';
    }
}
