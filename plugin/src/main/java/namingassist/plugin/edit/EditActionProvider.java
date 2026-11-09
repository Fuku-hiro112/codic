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
import namingassist.plugin.JapaneseNamingContributor;
import namingassist.engine.NamingCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 候補一覧に本プラグインの候補があるとき、候補の操作(一覧の電球)に「単語を並べ替え・結合」を足す。
 * 選ぶと Lookup を閉じ、日本語の入力を残したまま編集の小窓を開く。
 * IDE 2025.2 ではこの操作の一覧の文字が描かれない(描画中に IDE 内部で例外)ため、主な入口は
 * Alt+Enter で小窓を直接開く {@link EditCandidateAction}。
 */
public final class EditActionProvider implements LookupActionProvider {
    public static final String ACTION_TEXT = "単語を並べ替え・結合…";

    @Override
    public void fillActions(@NotNull LookupElement element, @NotNull Lookup lookup,
                            @NotNull Consumer<? super LookupElementAction> consumer) {
        LookupElement item = pluginItemFor(element, lookup);
        if (item != null) {
            consumer.consume(new EditAction(item, lookup));
        }
    }

    /**
     * 編集の対象にする候補。element が本プラグインの候補ならそれ、そうでなければ一覧の先頭の本プラグインの候補。
     * 一覧に本プラグインの候補が無ければ null。標準の候補(ML の予測など)が選ばれていても編集に入れるようにする
     * (選んでいる候補に操作が 1 つも無いと、IDE は Alt+Enter で何も出さない)。
     */
    static @Nullable LookupElement pluginItemFor(@Nullable LookupElement element, Lookup lookup) {
        if (element != null && element.getObject() instanceof Candidate) {
            return element;
        }
        return lookup.getItems().stream()
            .filter(e -> e.getObject() instanceof Candidate)
            .findFirst().orElse(null);
    }

    /** element が本プラグインの候補なら、日本語の入力の範囲と表記をまとめた編集の対象。それ以外は null。 */
    static @Nullable EditTarget targetFor(LookupElement element, Lookup lookup) {
        if (!(element.getObject() instanceof Candidate candidate)) {
            return null;
        }
        Editor editor = lookup.getEditor();
        int lookupStart = lookup.getLookupStart();
        int end = editor.getCaretModel().getOffset();
        if (lookupStart < 0 || lookupStart > end) {
            return null;
        }
        // 先頭の数字(2次元配列の「2」)はプレフィックスの外にあるが、句の一部として解析したので範囲に含める
        int start = lookupStart - JapaneseNamingContributor.leadingDigits(
            editor.getDocument().getCharsSequence(), lookupStart).length();
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
