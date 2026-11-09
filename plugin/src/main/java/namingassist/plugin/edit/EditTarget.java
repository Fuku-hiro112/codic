package namingassist.plugin.edit;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import namingassist.engine.Candidate;
import namingassist.engine.EnglishName;
import namingassist.engine.NamingCase;
import namingassist.plugin.TypedLead;

/**
 * 編集の対象。range は Lookup を開いたときの日本語の入力の範囲。namingCase は Lookup で選ばれた候補の表記。
 *
 * @param range  文書の変更に追従する範囲(RangeMarker)
 * @param phrase 編集を始めたときの範囲の文字列(日本語の入力)。確定時に同じままかを確かめる
 * @param lead   範囲の先頭の打ち済みの英字({@link TypedLead#leadOf})。訳さずに残し、編集した名前の前につなぐ。無ければ空
 */
public record EditTarget(Project project, Editor editor, RangeMarker range, Candidate candidate, NamingCase namingCase,
                         String phrase, String lead) {

    /** 置き換え前の文字列(日本語の入力)。 */
    public String originalText() {
        return phrase;
    }

    /** name を namingCase で綴り、lead の後ろにつないだ文字列(確定で書き込む文字列)。 */
    public String textFor(EnglishName name) {
        return TypedLead.join(lead, name.render(namingCase), namingCase);
    }

    /**
     * 日本語の入力を、name を namingCase で綴った文字列に置き換え、キャレットをその末尾に置く。
     * 範囲が消えた・中身が変わった・文書に書き込めないときは何もせず false。
     */
    public boolean replaceWith(EnglishName name) {
        Document document = editor.getDocument();
        if (!range.isValid() || !phrase.equals(document.getText(range.getTextRange()))) {
            return false;
        }
        if (!FileDocumentManager.getInstance().requestWriting(document, project) || !document.isWritable()) {
            return false;
        }
        String text = textFor(name);
        WriteCommandAction.runWriteCommandAction(project, EditActionProvider.ACTION_TEXT, null, () -> {
            int start = range.getStartOffset();
            document.replaceString(start, range.getEndOffset(), text);
            editor.getCaretModel().moveToOffset(start + text.length());
        });
        return true;
    }
}
