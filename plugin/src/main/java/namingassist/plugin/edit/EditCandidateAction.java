package namingassist.plugin.edit;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupEx;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 候補一覧(Lookup)の本プラグインの候補について、単語の並べ替え・結合の小窓を開くエディタのアクション。
 * 選んでいる候補が本プラグインの候補ならそれを、そうでなければ一覧の先頭の本プラグインの候補を対象にする。
 * Alt+Enter(候補の操作の一覧)は、選んでいる候補が標準の候補だと IDE が何も出さないため、確実な入口として置く。
 */
public final class EditCandidateAction extends AnAction implements DumbAware {
    public static final String ID = "namingassist.EditNamingCandidate";
    /** plugin.xml の既定のキーマップ($default)の割り当てと同じ。 */
    public static final String SHORTCUT = "ctrl shift Q";

    /** lookup に本プラグインの候補があれば、その編集の対象。無ければ null。 */
    static @Nullable EditTarget targetIn(Lookup lookup) {
        var item = EditActionProvider.pluginItemFor(lookup.getCurrentItem(), lookup);
        return item == null ? null : EditActionProvider.targetFor(item, lookup);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null && e.getData(CommonDataKeys.EDITOR) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        LookupEx lookup = LookupManager.getActiveLookup(editor);
        EditTarget target = lookup == null ? null : targetIn(lookup);
        if (target == null) {
            HintManager.getInstance().showInformationHint(editor,
                "日本語の句を打ち、英語名の候補の一覧が出ている間に使ってください");
            return;
        }
        lookup.hideLookup(true);
        // Lookup が閉じてから小窓を開く(閉じる処理と焦点を取り合わない)。
        ApplicationManager.getApplication().invokeLater(() -> NameEditorPopup.show(target),
            target.project().getDisposed());
    }
}
