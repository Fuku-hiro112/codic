package namingassist.plugin.edit;

import com.intellij.openapi.actionSystem.ActionPromoter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * 同じキー(Alt+Enter)に割り当てられたアクションのうち、{@link EditCandidateAction} を先に試させる。
 * 同アクションは本プラグインの候補が並ぶ間しか有効にならないので、それ以外のときは IDE の Alt+Enter がそのまま動く。
 */
public final class EditCandidateActionPromoter implements ActionPromoter {
    @Override
    public List<AnAction> promote(@NotNull List<? extends AnAction> actions, @NotNull DataContext context) {
        return actions.stream().filter(a -> a instanceof EditCandidateAction).map(a -> (AnAction) a).toList();
    }
}
