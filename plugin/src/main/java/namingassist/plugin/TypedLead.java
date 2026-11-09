package namingassist.plugin;

import java.util.Locale;
import namingassist.engine.NamingCase;

/**
 * 補完のプレフィックスの先頭にある、打ち済みの識別子(lead)。pageDecisionああ の pageDecision。
 * lead は訳さずにそのまま残し、日本語の部分の英語を命名規則に沿って後ろにつなぐ。
 */
public final class TypedLead {
    private TypedLead() {
    }

    /**
     * prefix の先頭の ASCII の識別子の連なり(英字・数字・_・$)のうち、後ろに日本語などが続き、
     * 小文字を 1 つ以上含むもの。無ければ空。ID・URL など大文字・数字・_・$ だけの連なりは略語とみなし、
     * 句の一部として訳すので空を返す(IDを取得する → getId のまま)。
     */
    public static String leadOf(String prefix) {
        int end = 0;
        boolean hasLower = false;
        while (end < prefix.length() && isAsciiIdentifierPart(prefix.charAt(end))) {
            hasLower |= prefix.charAt(end) >= 'a' && prefix.charAt(end) <= 'z';
            end++;
        }
        if (end == 0 || end == prefix.length() || !hasLower || !Character.isJavaIdentifierStart(prefix.charAt(0))) {
            return "";
        }
        return prefix.substring(0, end);
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
     */
    public static String join(String lead, String rendered, NamingCase c) {
        if (lead.isEmpty() || rendered.isEmpty()) {
            return lead + rendered;
        }
        return switch (c) {
            case CAMEL -> lead + Character.toUpperCase(rendered.charAt(0)) + rendered.substring(1);
            case PASCAL -> lead + rendered;
            case SNAKE -> lead + (lead.endsWith("_") ? "" : "_") + rendered;
            case UPPER_SNAKE -> upperSnake(lead) + (lead.endsWith("_") ? "" : "_") + rendered;
            case KEBAB -> lead + "-" + rendered;
        };
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
