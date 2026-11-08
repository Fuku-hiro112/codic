package namingassist.engine;

/** 命名の対象の種別。 */
public enum NamingKind {
    VARIABLE(NamingCase.CAMEL),
    CONSTANT(NamingCase.UPPER_SNAKE),
    METHOD(NamingCase.CAMEL),
    CLASS(NamingCase.PASCAL);

    private final NamingCase defaultCase;

    NamingKind(NamingCase defaultCase) {
        this.defaultCase = defaultCase;
    }

    public NamingCase defaultCase() {
        return defaultCase;
    }
}
