package namingassist.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EnglishNameTest {

    private static NamePart part(String text) {
        return new NamePart(text, List.of(text), true);
    }

    private static EnglishName name(String... texts) {
        return new EnglishName(java.util.Arrays.stream(texts).map(EnglishNameTest::part).toList());
    }

    @Test
    void rendersAllCases() {
        EnglishName n = name("get", "employee", "count");
        assertEquals("getEmployeeCount", n.render(NamingCase.CAMEL));
        assertEquals("GetEmployeeCount", n.render(NamingCase.PASCAL));
        assertEquals("get_employee_count", n.render(NamingCase.SNAKE));
        assertEquals("GET_EMPLOYEE_COUNT", n.render(NamingCase.UPPER_SNAKE));
        assertEquals("get-employee-count", n.render(NamingCase.KEBAB));
    }

    /** 部品の text が空白区切りの複数語(結合した語)なら、語ごとに区切って綴る。 */
    @Test
    void rendersMultiWordPartAsSeparateWords() {
        EnglishName n = name("get", "employee count");
        assertEquals("getEmployeeCount", n.render(NamingCase.CAMEL));
        assertEquals("GetEmployeeCount", n.render(NamingCase.PASCAL));
        assertEquals("get_employee_count", n.render(NamingCase.SNAKE));
        assertEquals("GET_EMPLOYEE_COUNT", n.render(NamingCase.UPPER_SNAKE));
        assertEquals("get-employee-count", n.render(NamingCase.KEBAB));
        assertTrue(n.isValidJavaIdentifier(NamingCase.CAMEL));
        assertEquals("employeeCount", name("employee count").render(NamingCase.CAMEL));
    }

    @Test
    void rejectsEmptyParts() {
        assertThrows(IllegalArgumentException.class, () -> new EnglishName(List.of()));
    }

    @Test
    void defaultCasePerKind() {
        assertEquals(NamingCase.CAMEL, NamingKind.VARIABLE.defaultCase());
        assertEquals(NamingCase.UPPER_SNAKE, NamingKind.CONSTANT.defaultCase());
        assertEquals(NamingCase.CAMEL, NamingKind.METHOD.defaultCase());
        assertEquals(NamingCase.PASCAL, NamingKind.CLASS.defaultCase());
    }

    @Test
    void reservedWordIsNotValidIdentifier() {
        assertFalse(name("class").isValidJavaIdentifier(NamingCase.CAMEL));
    }

    @Test
    void leadingDigitIsNotValidIdentifier() {
        assertFalse(name("3d", "view").isValidJavaIdentifier(NamingCase.CAMEL));
    }

    @Test
    void japaneseTextIsValidIdentifier() {
        EnglishName n = new EnglishName(List.of(
                part("get"),
                new NamePart("在庫引当", List.of("在庫引当"), false)));
        assertEquals("get在庫引当", n.render(NamingCase.CAMEL));
        assertTrue(n.isValidJavaIdentifier(NamingCase.CAMEL));
    }
}
