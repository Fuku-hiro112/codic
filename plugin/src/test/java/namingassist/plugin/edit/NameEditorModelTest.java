package namingassist.plugin.edit;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void joinNeedsAscendingRange() {
        NameEditorModel model = getEmployeeCount();
        assertThrows(IllegalArgumentException.class, () -> model.join(2, 1));
        assertThrows(IllegalArgumentException.class, () -> model.join(1, 1));
    }
}
