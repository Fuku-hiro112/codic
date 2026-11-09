package namingassist.engine;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 部品の並びとしての英語名。綴りは render で規則ごとに決める。 */
public record EnglishName(List<NamePart> parts) {

    private static final Set<String> RESERVED = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "_",
            "true", "false", "null");

    public EnglishName {
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("parts must not be empty");
        }
        parts = List.copyOf(parts);
    }

    /**
     * 規則 c で綴る。部品の text が空白区切りの複数語(結合した語)なら、語ごとに区切って綴る
     * (employee count → CAMEL では EmployeeCount の形、SNAKE では employee_count)。
     */
    public String render(NamingCase c) {
        List<String> words = parts.stream()
                .flatMap(p -> Arrays.stream(p.text().split(" ")))
                .filter(w -> !w.isEmpty())
                .toList();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            String t = words.get(i);
            switch (c) {
                case CAMEL -> sb.append(i == 0 ? lower(t) : capitalize(t));
                case PASCAL -> sb.append(capitalize(t));
                case SNAKE -> sb.append(i == 0 ? "" : "_").append(lower(t));
                case UPPER_SNAKE -> sb.append(i == 0 ? "" : "_").append(t.toUpperCase(Locale.ROOT));
                case KEBAB -> sb.append(i == 0 ? "" : "-").append(lower(t));
            }
        }
        return sb.toString();
    }

    public boolean isValidJavaIdentifier(NamingCase c) {
        String s = render(c);
        if (s.isEmpty() || RESERVED.contains(s)) {
            return false;
        }
        int first = s.codePointAt(0);
        if (!Character.isJavaIdentifierStart(first)) {
            return false;
        }
        for (int i = Character.charCount(first); i < s.length(); ) {
            int cp = s.codePointAt(i);
            if (!Character.isJavaIdentifierPart(cp)) {
                return false;
            }
            i += Character.charCount(cp);
        }
        return true;
    }

    private static String lower(String t) {
        return t.toLowerCase(Locale.ROOT);
    }

    private static String capitalize(String t) {
        if (t.isEmpty()) {
            return t;
        }
        int n = Character.charCount(t.codePointAt(0));
        return t.substring(0, n).toUpperCase(Locale.ROOT) + t.substring(n).toLowerCase(Locale.ROOT);
    }
}
