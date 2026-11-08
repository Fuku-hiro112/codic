package namingassist.plugin.edit;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import namingassist.engine.Candidate;
import namingassist.engine.EnglishName;
import namingassist.engine.NamingCase;

/**
 * 編集の対象。range は Lookup を開いたときの日本語の入力の範囲。namingCase は Lookup で選ばれた候補の表記。
 *
 * @param range 文書の変更に追従する範囲(RangeMarker)
 */
public record EditTarget(Project project, Editor editor, RangeMarker range, Candidate candidate, NamingCase namingCase) {

    /** 置き換え前の文字列(日本語の入力)。 */
    public String originalText() {
        return editor.getDocument().getText(range.getTextRange());
    }

    /** 日本語の入力を、name を namingCase で綴った文字列に置き換え、キャレットをその末尾に置く。 */
    public void replaceWith(EnglishName name) {
        String text = name.render(namingCase);
        WriteCommandAction.runWriteCommandAction(project, EditActionProvider.ACTION_TEXT, null, () -> {
            int start = range.getStartOffset();
            editor.getDocument().replaceString(start, range.getEndOffset(), text);
            editor.getCaretModel().moveToOffset(start + text.length());
        });
    }
}
