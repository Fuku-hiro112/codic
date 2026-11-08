package namingassist.plugin;

import com.intellij.psi.PsiElement;
import namingassist.engine.NamingKind;

/** カーソル位置の文脈から命名の種別を決める。今は常に変数として扱う(文脈の判定は後で入れる)。 */
public final class ContextKindResolver {

    public NamingKind resolve(PsiElement elementAtCaret) {
        return NamingKind.VARIABLE;
    }
}
