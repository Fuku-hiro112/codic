package namingassist.plugin.edit;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import namingassist.engine.NamePart;

/**
 * 単語の並びを編集する小窓。単語を選び、別訳への差し替え・前後への移動・次の語との結合ができる。
 * 確定(ボタンか Enter)で日本語の入力を編集後の名前に置き換える。Esc で何も変えずに閉じる。
 */
final class NameEditorPopup {
    private final EditTarget target;
    private final NameEditorModel model;
    private final DefaultListModel<String> words = new DefaultListModel<>();
    private final JBList<String> list = new JBList<>(words);
    private final DefaultComboBoxModel<String> alternatives = new DefaultComboBoxModel<>();
    private final ComboBox<String> alternativeBox = new ComboBox<>(alternatives);
    private final JButton left = new JButton("← 前へ");
    private final JButton right = new JButton("後ろへ →");
    private final JButton join = new JButton("次の語と結合");
    private final JBLabel preview = new JBLabel();
    private final JButton ok = new JButton("確定");
    private JBPopup popup;
    private boolean refreshing;

    private NameEditorPopup(EditTarget target) {
        this.target = target;
        this.model = new NameEditorModel(target.candidate().name());
    }

    static void show(EditTarget target) {
        new NameEditorPopup(target).open();
    }

    private void open() {
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setVisibleRowCount(1);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> {
            if (!refreshing) {
                refresh(list.getSelectedIndex());
            }
        });
        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    commit();
                    e.consume();
                }
            }
        });
        alternativeBox.addActionListener(e -> {
            int index = list.getSelectedIndex();
            Object chosen = alternativeBox.getSelectedItem();
            if (!refreshing && index >= 0 && chosen != null) {
                model.replaceWord(index, chosen.toString());
                refresh(index);
            }
        });
        left.addActionListener(e -> {
            int index = list.getSelectedIndex();
            model.move(index, index - 1);
            refresh(index - 1);
        });
        right.addActionListener(e -> {
            int index = list.getSelectedIndex();
            model.move(index, index + 1);
            refresh(index + 1);
        });
        join.addActionListener(e -> {
            int index = list.getSelectedIndex();
            model.join(index, index + 1);
            refresh(index);
        });
        ok.addActionListener(e -> commit());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(new JBLabel("別訳:"));
        buttons.add(alternativeBox);
        buttons.add(left);
        buttons.add(right);
        buttons.add(join);
        JPanel bottom = new JPanel(new BorderLayout(4, 0));
        bottom.add(preview, BorderLayout.CENTER);
        bottom.add(ok, BorderLayout.EAST);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(JBUI.Borders.empty(8));
        panel.add(list, BorderLayout.NORTH);
        panel.add(buttons, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        refresh(0);
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, list)
            .setTitle(EditActionProvider.ACTION_TEXT.replace("…", "") + "(" + target.originalText() + ")")
            .setRequestFocus(true)
            .setFocusable(true)
            .setMovable(true)
            .setCancelOnClickOutside(true)
            .createPopup();
        popup.showInBestPositionFor(target.editor());    }

    /** モデルの中身を画面に写し、selected の単語を選ぶ。 */
    private void refresh(int selected) {
        refreshing = true;
        try {
            List<NamePart> parts = model.name().parts();
            int index = Math.max(0, Math.min(selected, parts.size() - 1));
            words.clear();
            parts.forEach(p -> words.addElement(p.text()));
            list.setSelectedIndex(index);

            NamePart part = parts.get(index);
            List<String> choices = new ArrayList<>(part.alternatives());
            if (!choices.contains(part.text())) {
                choices.add(0, part.text());
            }
            alternatives.removeAllElements();
            alternatives.addAll(choices);
            alternatives.setSelectedItem(part.text());

            left.setEnabled(index > 0);
            right.setEnabled(index < parts.size() - 1);
            join.setEnabled(index < parts.size() - 1);
            boolean committable = model.isCommittable(target.namingCase());
            String rendered = target.textFor(model.name());
            preview.setText(committable ? rendered : rendered + "(Java の識別子にならないため確定できません)");
            ok.setEnabled(committable);
        } finally {
            refreshing = false;
        }
    }

    /** 識別子にならない名前は確定しない(小窓は開いたまま)。書き込めなかったら理由を出す。小窓は必ず閉じる。 */
    private void commit() {
        if (!model.isCommittable(target.namingCase())) {
            return;
        }
        boolean replaced = false;
        try {
            replaced = target.replaceWith(model.name());
        } finally {
            popup.closeOk(null);
        }
        if (!replaced) {
            HintManager.getInstance().showErrorHint(target.editor(),
                "日本語の入力が変わったか、ファイルに書き込めないため、置き換えませんでした");
        }
    }
}
