package namingassist.plugin.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import namingassist.engine.EnglishName;
import namingassist.engine.NamePart;

/**
 * 英語名の部品(単語)の編集。codic の「単語をクリックで別訳」「Shift+クリックで結合」「Ctrl+ドラッグで並べ替え」
 * に当たる操作を、添え字で行う。IntelliJ に依存しない。添え字が範囲外なら IndexOutOfBoundsException で、
 * そのとき名前は変わらない。
 */
public final class NameEditorModel {
    private EnglishName name;

    public NameEditorModel(EnglishName name) {
        this.name = Objects.requireNonNull(name);
    }

    /** 現在の英語名。 */
    public EnglishName name() {
        return name;
    }

    /** from の単語を取り出し、to の位置へ入れる(to は取り出した後の並びでの位置)。 */
    public void move(int from, int to) {
        List<NamePart> parts = new ArrayList<>(name.parts());
        Objects.checkIndex(from, parts.size());
        Objects.checkIndex(to, parts.size());
        parts.add(to, parts.remove(from));
        name = new EnglishName(parts);
    }

    /** from から to まで(両端を含む)の単語を 1 語につなげる。綴りは区切り無しの連結(employee + count → employeecount)。 */
    public void join(int from, int to) {
        List<NamePart> parts = new ArrayList<>(name.parts());
        Objects.checkIndex(from, parts.size());
        Objects.checkIndex(to, parts.size());
        if (from >= to) {
            throw new IllegalArgumentException("from must be less than to: " + from + ", " + to);
        }
        List<NamePart> joined = parts.subList(from, to + 1);
        StringBuilder text = new StringBuilder();
        boolean resolved = true;
        for (NamePart part : joined) {
            text.append(part.text());
            resolved &= part.resolved();
        }
        NamePart one = new NamePart(text.toString(), List.of(text.toString()), resolved);
        joined.clear();
        parts.add(from, one);
        name = new EnglishName(parts);
    }

    /** index の単語を別訳に差し替える。別訳の候補(alternatives)はそのまま残す。 */
    public void replaceWord(int index, String alternative) {
        List<NamePart> parts = new ArrayList<>(name.parts());
        Objects.checkIndex(index, parts.size());
        NamePart old = parts.get(index);
        parts.set(index, new NamePart(alternative, old.alternatives(), true));
        name = new EnglishName(parts);
    }
}
