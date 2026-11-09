package namingassist.plugin.settings;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import namingassist.plugin.NamingEngineService;

/**
 * 設定画面に JMdict の出典(誰の著作物か・ライセンス)が表示されること。headless なので見た目は確かめられず、
 * 画面のコンポーネントの木にある文言(ラベル・リンク)だけを確かめる。
 */
public class AttributionDisplayTest extends BasePlatformTestCase {

    public void testSettingsPanelShowsJmdictAttribution() throws Exception {
        Path dir = Files.createTempDirectory("attribution-test");
        UserDictionaryConfigurable configurable = new UserDictionaryConfigurable(
            new UserDictionaryStore(dir.resolve(UserDictionaryStore.FILE_NAME), NamingEngineService.getInstance()));
        try {
            JComponent component = configurable.createComponent();
            String texts = String.join("\n", texts(component));
            System.out.println("[ATTRIBUTION] " + texts);
            assertTrue("JMdict の名前: " + texts, texts.contains("JMdict"));
            assertTrue("著作権者 EDRDG: " + texts,
                texts.contains("Electronic Dictionary Research and Development Group"));
            assertTrue("ライセンス CC BY-SA 4.0: " + texts, texts.contains("CC BY-SA 4.0"));
            assertTrue("同梱のライセンス文書への導線: " + texts, texts.contains("LICENSE-JMDICT.md"));
        } finally {
            configurable.disposeUIResources();
        }
    }

    /** コンポーネントの木にあるラベルとボタン(リンク)の文言。HTML のタグは除く。 */
    private static List<String> texts(Component root) {
        List<String> out = new ArrayList<>();
        collect(root, out);
        return out;
    }

    private static void collect(Component c, List<String> out) {
        String text = null;
        if (c instanceof JLabel label) {
            text = label.getText();
        } else if (c instanceof AbstractButton button) {
            text = button.getText();
        }
        if (text != null && !text.isBlank()) {
            out.add(text.replaceAll("<br>", " ").replaceAll("<[^>]+>", ""));
        }
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                collect(child, out);
            }
        }
    }
}
