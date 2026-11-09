package namingassist.plugin.edit;

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
 *
 * <p>既定のキーマップで Alt+Enter と Ctrl+Shift+Q に割り当てる。IDE 2025.2 の Lookup 表示中の Alt+Enter(候補の操作の一覧)は、
 * 一覧の描画中に IDE 内部で例外が出て文字が描かれない(空の四角形に見える)。そのため本プラグインの候補が並ぶ間は、
 * {@link EditCandidateActionPromoter} で IDE の Alt+Enter より先にこのアクションを選ばせ、小窓を直接開く。
 * それ以外のとき(一覧が無い、英字だけの一覧)は無効にして、Alt+Enter を IDE に任せる。
 */
public final class EditCandidateAction extends AnAction implements DumbAware {
    public static final String ID = "namingassist.EditNamingCandidate";
    /** plugin.xml の既定のキーマップ($default)の割り当てと同じ(もう 1 つは alt ENTER)。 */
    public static final String SHORTCUT = "ctrl shift Q";

    /** lookup に本プラグインの候補があれば、その編集の対象。無ければ null。 */
    static @Nullable EditTarget targetIn(Lookup lookup) {
        var item = EditActionProvider.pluginItemFor(lookup.getCurrentItem(), lookup);
        return item == null ? null : EditActionProvider.targetFor(item, lookup);
    }

    /** e の editor に表示中の Lookup があり、本プラグインの候補が並んでいればその Lookup。無ければ null。 */
    private static @Nullable LookupEx lookupWithPluginItem(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        LookupEx lookup = editor == null ? null : LookupManager.getActiveLookup(editor);
        return lookup != null && EditActionProvider.pluginItemFor(lookup.getCurrentItem(), lookup) != null ? lookup : null;
    }

    /** Lookup は EDT で扱う(一覧の中身と選択は EDT で更新される)。 */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    /** 本プラグインの候補が並ぶ間だけ有効。無効のとき Alt+Enter は IDE の意図アクションに渡る。 */
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null && lookupWithPluginItem(e) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LookupEx lookup = lookupWithPluginItem(e);
        EditTarget target = lookup == null ? null : targetIn(lookup);
        if (target == null) {
            return;
        }
        lookup.hideLookup(true);
        // Lookup が閉じてから小窓を開く(閉じる処理と焦点を取り合わない)。
        ApplicationManager.getApplication().invokeLater(() -> NameEditorPopup.show(target),
            target.project().getDisposed());
    }
}
