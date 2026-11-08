package namingassist.plugin.edit;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupActionProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.Consumer;
import namingassist.engine.Candidate;
import namingassist.engine.NamingCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 候補一覧で本プラグインの候補を選んでいるとき、その候補の操作(Lookup 表示中の Alt+Enter、または一覧の電球)に
 * 「単語を並べ替え・結合」を足す。選ぶと Lookup を閉じ、日本語の入力を残したまま編集の小窓を開く。
 */
public final class EditActionProvider implements LookupActionProvider {
    public static final String ACTION_TEXT = "単語を並べ替え・結合…";

    @Override
    public void fillActions(@NotNull LookupElement element, @NotNull Lookup lookup,
                            @NotNull Consumer<? super LookupElementAction> consumer) {
        if (element.getObject() instanceof Candidate) {
            consumer.consume(new EditAction(element, lookup));
        }
    }

    /** element が本プラグインの候補なら、日本語の入力の範囲と表記をまとめた編集の対象。それ以外は null。 */
    static @Nullable EditTarget targetFor(LookupElement element, Lookup lookup) {
        if (!(element.getObject() instanceof Candidate candidate)) {
            return null;
        }
        Editor editor = lookup.getEditor();
        int start = lookup.getLookupStart();
        int end = editor.getCaretModel().getOffset();
        if (start < 0 || start > end) {
            return null;
        }
        return new EditTarget(lookup.getProject(), editor, editor.getDocument().createRangeMarker(start, end),
            candidate, caseOf(candidate), editor.getDocument().getText(new TextRange(start, end)));
    }

    /** 候補の表記(rendered)と同じ綴りになる規則。見つからなければ CAMEL。 */
    static NamingCase caseOf(Candidate candidate) {
        for (NamingCase c : NamingCase.values()) {
            if (candidate.name().render(c).equals(candidate.rendered())) {
                return c;
            }
        }
        return NamingCase.CAMEL;
    }

    private static final class EditAction extends LookupElementAction {
        private final LookupElement element;
        private final Lookup lookup;

        EditAction(LookupElement element, Lookup lookup) {
            super(AllIcons.Actions.Edit, ACTION_TEXT);
            this.element = element;
            this.lookup = lookup;
        }

        @Override
        public Result performLookupAction() {
            EditTarget target = targetFor(element, lookup);
            if (target != null) {
                // Lookup が閉じてから小窓を開く(閉じる処理と焦点を取り合わない)。
                ApplicationManager.getApplication().invokeLater(() -> NameEditorPopup.show(target),
                    target.project().getDisposed());
            }
            return Result.HIDE_LOOKUP;
        }
    }
}
