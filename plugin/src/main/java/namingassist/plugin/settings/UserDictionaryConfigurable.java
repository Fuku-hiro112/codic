package namingassist.plugin.settings;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import namingassist.engine.dictionary.DictionaryEntry;
import namingassist.engine.dictionary.TsvDictionary;
import namingassist.engine.dictionary.UserDictionary;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

/**
 * Settings &gt; Tools &gt; 日本語→英語 命名。利用者辞書を一覧表で追加・編集・削除し、下部に JMdict の出典を示す。
 */
public final class UserDictionaryConfigurable implements Configurable {
    /** engine の jar に同梱されているライセンス文書。 */
    static final String LICENSE_RESOURCE = "/dictionary/LICENSE-JMDICT.md";
    private static final String EDRDG_LICENCE_URL = "https://www.edrdg.org/edrdg/licence.html";
    private static final String CC_BY_SA_URL = "https://creativecommons.org/licenses/by-sa/4.0/";

    private final UserDictionaryStore store;
    private List<UserDictionaryStore.Row> savedRows = List.of();
    private DefaultTableModel model;
    private JBTable table;
    private JBLabel warningLabel;

    public UserDictionaryConfigurable() {
        this(UserDictionaryStore.inConfigDir());
    }

    UserDictionaryConfigurable(UserDictionaryStore store) {
        this.store = store;
    }

    @Override
    public @Nls String getDisplayName() {
        return "日本語→英語 命名";
    }

    @Override
    public @Nullable JComponent createComponent() {
        model = new DefaultTableModel(new Object[] {"日本語", "訳(カンマ区切り。先頭が優先)"}, 0);
        table = new JBTable(model);
        table.getEmptyText().setText("利用者辞書は空です。+ で語を追加します");
        JPanel tablePanel = ToolbarDecorator.createDecorator(table)
            .setAddAction(button -> addRow())
            .setRemoveAction(button -> removeSelectedRows())
            .disableUpDownActions()
            .createPanel();

        warningLabel = new JBLabel();
        warningLabel.setForeground(UIUtil.getErrorForeground());
        warningLabel.setVisible(false);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(new JBLabel("ここに登録した訳は、同梱の辞書より優先されます。保存先: " + store.file()));
        top.add(warningLabel);

        JPanel root = new JPanel(new BorderLayout(0, JBUI.scale(8)));
        root.add(top, BorderLayout.NORTH);
        root.add(tablePanel, BorderLayout.CENTER);
        root.add(createAttributionPanel(), BorderLayout.SOUTH);
        return root;
    }

    private JComponent createAttributionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.emptyTop(8));
        panel.add(new JBLabel("<html>同梱の辞書には、JMdict/EDICT を命名用に加工したデータを含みます。<br>"
            + "JMdict/EDICT (c) Electronic Dictionary Research and Development Group, CC BY-SA 4.0<br>"
            + "This package uses the JMdict/EDICT dictionary files. These files are the property of the "
            + "Electronic Dictionary Research and Development Group, and are used in conformance with the "
            + "Group's licence.</html>"));
        JPanel links = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        links.add(new ActionLink("同梱のライセンス文書 (LICENSE-JMDICT.md) を表示", (ActionListener) e -> showLicense()));
        links.add(new JBLabel("   "));
        links.add(new ActionLink("EDRDG ライセンス", (ActionListener) e -> BrowserUtil.browse(EDRDG_LICENCE_URL)));
        links.add(new JBLabel("   "));
        links.add(new ActionLink("CC BY-SA 4.0", (ActionListener) e -> BrowserUtil.browse(CC_BY_SA_URL)));
        links.setAlignmentX(0f);
        panel.add(links);
        return panel;
    }

    private void showLicense() {
        URL url = TsvDictionary.class.getResource(LICENSE_RESOURCE);
        JBTextArea text = new JBTextArea(readLicense(), 25, 90);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setCaretPosition(0);
        DialogBuilder builder = new DialogBuilder(table);
        builder.setTitle("LICENSE-JMDICT.md");
        JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
        panel.add(new JBLabel("同梱場所: " + (url == null ? LICENSE_RESOURCE : url)), BorderLayout.NORTH);
        panel.add(new JBScrollPane(text), BorderLayout.CENTER);
        builder.setCenterPanel(panel);
        builder.addOkAction();
        builder.show();
    }

    /** 同梱のライセンス文書の本文。見つからないときは原本ページの案内を返す。 */
    static String readLicense() {
        try (InputStream in = TsvDictionary.class.getResourceAsStream(LICENSE_RESOURCE)) {
            if (in == null) {
                return "同梱のライセンス文書が見つかりません。原本: " + EDRDG_LICENCE_URL;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "同梱のライセンス文書を読めません (" + e.getMessage() + ")。原本: " + EDRDG_LICENCE_URL;
        }
    }

    private void addRow() {
        stopEditing();
        model.addRow(new Object[] {"", ""});
        int row = model.getRowCount() - 1;
        table.getSelectionModel().setSelectionInterval(row, row);
        table.editCellAt(row, 0);
    }

    private void removeSelectedRows() {
        stopEditing();
        int[] selected = table.getSelectedRows();
        for (int i = selected.length - 1; i >= 0; i--) {
            model.removeRow(selected[i]);
        }
    }

    private void stopEditing() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
    }

    private List<UserDictionaryStore.Row> currentRows() {
        List<UserDictionaryStore.Row> rows = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            rows.add(new UserDictionaryStore.Row(cell(i, 0), cell(i, 1)));
        }
        return rows;
    }

    private String cell(int row, int column) {
        Object value = model.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private void showRows(List<UserDictionaryStore.Row> rows) {
        model.setRowCount(0);
        for (UserDictionaryStore.Row row : rows) {
            model.addRow(new Object[] {row.japanese(), row.translations()});
        }
        savedRows = rows;
    }

    @Override
    public boolean isModified() {
        return model != null && !currentRows().equals(savedRows);
    }

    @Override
    public void apply() throws ConfigurationException {
        stopEditing();
        List<DictionaryEntry> entries = UserDictionaryStore.toEntries(currentRows());
        try {
            store.save(entries);
        } catch (IOException e) {
            throw new ConfigurationException("利用者辞書を保存できません: " + store.file() + " (" + e.getMessage() + ")");
        }
        showRows(UserDictionaryStore.toRows(entries));
        warningLabel.setVisible(false);
    }

    @Override
    public void reset() {
        UserDictionary loaded = store.load();
        showRows(UserDictionaryStore.toRows(loaded.entries()));
        List<String> warnings = loaded.warnings();
        warningLabel.setText(warnings.isEmpty() ? "" : "<html>" + String.join("<br>", warnings)
            + "<br>読めた語だけを表示しています。保存するとファイルはこの一覧の内容で置き換わります。"
            + "<br>元のファイルは " + store.backupFile() + " に残ります。</html>");
        warningLabel.setVisible(!warnings.isEmpty());
    }

    @Override
    public void disposeUIResources() {
        model = null;
        table = null;
        warningLabel = null;
    }
}
