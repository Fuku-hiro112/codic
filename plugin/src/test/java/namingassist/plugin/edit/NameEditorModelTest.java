package namingassist.plugin.edit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import namingassist.engine.EnglishName;
import namingassist.engine.NamePart;
import namingassist.engine.NamingCase;
import org.junit.Test;

/** 語順の入れ替え・結合・別訳への差し替え(IntelliJ に依存しない純粋なロジック)。 */
public class NameEditorModelTest {

    private static NameEditorModel getEmployeeCount() {
        return new NameEditorModel(new EnglishName(List.of(
            new NamePart("get", List.of("get", "fetch", "acquire"), true),
            new NamePart("employee", List.of("employee", "staff"), true),
            new NamePart("count", List.of("count", "number"), true))));
    }

    @Test
    public void moveChangesWordOrder() {
        NameEditorModel model = getEmployeeCount();
        model.move(2, 1);
        assertEquals("get_count_employee", model.name().render(NamingCase.SNAKE));
    }

    @Test
    public void moveForwardChangesWordOrder() {
        NameEditorModel model = getEmployeeCount();
        model.move(0, 2);
        assertEquals("employee_count_get", model.name().render(NamingCase.SNAKE));
    }

    @Test
    public void joinMakesOneWord() {
        NameEditorModel model = getEmployeeCount();
        model.join(1, 2);
        List<NamePart> parts = model.name().parts();
        assertEquals(2, parts.size());
        assertEquals("employeecount", parts.get(1).text());
        assertEquals("get_employeecount", model.name().render(NamingCase.SNAKE));
        assertEquals("getEmployeecount", model.name().render(NamingCase.CAMEL));
    }

    @Test
    public void replaceWordUsesAlternative() {
        NameEditorModel model = getEmployeeCount();
        model.replaceWord(0, "fetch");
        assertEquals("fetch_employee_count", model.name().render(NamingCase.SNAKE));
        assertTrue("別訳の候補は残る", model.name().parts().get(0).alternatives().contains("get"));
    }

    @Test
    public void editsAccumulate() {
        NameEditorModel model = getEmployeeCount();
        model.replaceWord(2, "number");
        model.move(2, 1);
        assertEquals("getNumberEmployee", model.name().render(NamingCase.CAMEL));
    }

    @Test
    public void outOfRangeIndexesThrow() {
        NameEditorModel model = getEmployeeCount();
        assertThrows(IndexOutOfBoundsException.class, () -> model.move(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> model.move(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> model.move(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> model.join(1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> model.join(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> model.replaceWord(3, "x"));
        assertEquals("例外の後も元のまま", "get_employee_count", model.name().render(NamingCase.SNAKE));
    }

    /** 確定できるのは Java の識別子になる名前だけ(予約語 1 語・先頭が数字は不可)。 */
    @Test
    public void onlyValidIdentifierIsCommittable() {
        NameEditorModel model = getEmployeeCount();
        assertTrue(model.isCommittable(NamingCase.CAMEL));

        NameEditorModel digitFirst = new NameEditorModel(new EnglishName(List.of(
            new NamePart("array", List.of("array"), true),
            new NamePart("2d", List.of("2d"), true))));
        assertTrue(digitFirst.isCommittable(NamingCase.CAMEL));
        digitFirst.move(1, 0);
        assertFalse("先頭が数字", digitFirst.isCommittable(NamingCase.CAMEL));

        NameEditorModel reserved = new NameEditorModel(new EnglishName(List.of(
            new NamePart("value", List.of("value", "new"), true))));
        reserved.replaceWord(0, "new");
        assertFalse("予約語 1 語", reserved.isCommittable(NamingCase.CAMEL));
        assertTrue("PascalCase の New は識別子になる", reserved.isCommittable(NamingCase.PASCAL));
    }

    @Test
    public void joinNeedsAscendingRange() {
        NameEditorModel model = getEmployeeCount();
        assertThrows(IllegalArgumentException.class, () -> model.join(2, 1));
        assertThrows(IllegalArgumentException.class, () -> model.join(1, 1));
    }
}
