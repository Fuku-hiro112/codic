package namingassist.plugin;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.util.PsiTreeUtil;
import namingassist.engine.NamingKind;

/**
 * カーソル位置の文脈から命名の種別を決める。補完では、補完用コピーのファイルでダミー識別子を含む識別子が渡る。
 * <ul>
 *   <li>クラスの宣言名 → CLASS</li>
 *   <li>メソッドの宣言名 → METHOD。メソッド名の位置では、補完用コピーに「名前;」が入りフィールドとして解析される
 *       (実測)。そのため、フィールドの直後が「(」か、型が void のフィールドをメソッドとみなす。</li>
 *   <li>{@code static final} のフィールド(インタフェースの暗黙のものを含む) → CONSTANT</li>
 *   <li>それ以外(ローカル変数・フィールド・判定できない位置) → VARIABLE</li>
 * </ul>
 */
public final class ContextKindResolver {

    public NamingKind resolve(PsiElement elementAtCaret) {
        if (!(elementAtCaret instanceof PsiIdentifier)) {
            return NamingKind.VARIABLE;
        }
        PsiElement owner = elementAtCaret.getParent();
        if (!(owner instanceof PsiNameIdentifierOwner named) || named.getNameIdentifier() != elementAtCaret) {
            return NamingKind.VARIABLE; // 宣言の名前ではない(参照・式の途中など)
        }
        if (owner instanceof PsiClass) {
            return NamingKind.CLASS;
        }
        if (owner instanceof PsiMethod) {
            return NamingKind.METHOD;
        }
        if (owner instanceof PsiField field) {
            if (looksLikeMethod(field)) {
                return NamingKind.METHOD;
            }
            if (field.hasModifierProperty(PsiModifier.STATIC) && field.hasModifierProperty(PsiModifier.FINAL)) {
                return NamingKind.CONSTANT;
            }
        }
        return NamingKind.VARIABLE;
    }

    /** フィールドに見えるが、実はメソッドの宣言名: 直後が「(」、または型が void(フィールドでは書けない)。 */
    private static boolean looksLikeMethod(PsiField field) {
        PsiElement next = PsiTreeUtil.nextVisibleLeaf(field);
        if (next instanceof PsiJavaToken token && token.getTokenType() == JavaTokenType.LPARENTH) {
            return true;
        }
        return PsiTypes.voidType().equals(field.getType());
    }
}
