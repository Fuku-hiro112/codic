package namingassist.engine;

/** 補完候補。rendered は name を規則に沿って綴った文字列。 */
public record Candidate(EnglishName name, String rendered) {
}
