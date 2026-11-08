package namingassist.plugin.settings;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableEP;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.table.DefaultTableModel;
import namingassist.engine.NamingKind;
import namingassist.engine.dictionary.DictionaryEntry;
import namingassist.engine.dictionary.UserDictionary;
import namingassist.plugin.NamingEngineService;
import namingassist.plugin.PluginLookups;

/**
 * 利用者辞書の保存・読み込みと補完への反映。補完のサービスはアプリ全体で 1 つなので、各テストの後に
 * 設定フォルダの利用者辞書を消し、空の利用者層に戻す。
 */
public class UserDictionaryStoreTest extends BasePlatformTestCase {
    private static final String PAGE_TITLE = "ページタイトルを取得する";
    private Path tempDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tempDir = Files.createTempDirectory("user-dictionary-test");
        Files.deleteIfExists(UserDictionaryStore.defaultFile());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Files.deleteIfExists(UserDictionaryStore.defaultFile());
            NamingEngineService.getInstance().replaceUserLayer(new UserDictionary(UserDictionaryStore.defaultFile()));
        } catch (Throwable e) {
            addSuppressedException(e);
        } finally {
            super.tearDown();
        }
    }

    private UserDictionaryStore tempStore() {
        return new UserDictionaryStore(tempDir.resolve(UserDictionaryStore.FILE_NAME), NamingEngineService.getInstance());
    }

    public void testDefaultFileIsInConfigDir() {
        Path file = UserDictionaryStore.defaultFile();
        assertEquals("japanese-naming-user-dictionary.tsv", file.getFileName().toString());
        assertEquals(Path.of(com.intellij.openapi.application.PathManager.getConfigPath()), file.getParent());
    }

    public void testSaveThenReloadGivesSameEntries() throws IOException {
        List<DictionaryEntry> entries = List.of(
            new DictionaryEntry("取得", List.of("fetch", "retrieve")),
            new DictionaryEntry("稼働", List.of("uptime")));
        tempStore().save(entries);

        UserDictionary reloaded = tempStore().load();
        assertEquals(entries, reloaded.entries());
        assertEquals(List.of(), reloaded.warnings());
    }

    /** 保存した語が、再起動なしで次の補完から先頭に出る。 */
    public void testSavedWordComesFirstOnNextCompletion() throws IOException {
        assertEquals("保存前は同梱辞書の get が先頭", "getPageTitle", firstPluginItem(PAGE_TITLE));

        tempStore().save(List.of(new DictionaryEntry("取得", List.of("fetch"))));

        assertEquals("保存後は利用者辞書の fetch が先頭", "fetchPageTitle", firstPluginItem(PAGE_TITLE));
    }

    /** 保存した複合語(Kuromoji は 在庫/引当 に分ける)が、1 語として次の補完の先頭に出る。 */
    public void testSavedCompoundWordComesFirstOnNextCompletion() throws IOException {
        tempStore().save(List.of(new DictionaryEntry("在庫引当", List.of("stock allocation"))));

        assertEquals("getStockAllocation", firstPluginItem("在庫引当を取得する"));
    }

    /** 起動時(サービスの最初の組み立て)に、設定フォルダに保存済みの利用者辞書を読む。 */
    public void testSavedDictionaryIsUsedAtStartup() throws IOException {
        Files.writeString(UserDictionaryStore.defaultFile(), "取得\tfetch\n", StandardCharsets.UTF_8);

        NamingEngineService fresh = new NamingEngineService();
        String top = fresh.engine().suggest(PAGE_TITLE, NamingKind.VARIABLE).get(0).rendered();
        assertEquals("fetchPageTitle", top);
    }

    /** 利用者辞書が壊れていても、起動(組み立て)は失敗せず同梱辞書だけで補完が動く。警告は読み込み結果で分かる。 */
    public void testBrokenFileStillStartsAndCompletes() throws IOException {
        Files.write(UserDictionaryStore.defaultFile(), new byte[] {(byte) 0xff, (byte) 0xfe, (byte) 0xc3, 0x28});

        NamingEngineService fresh = new NamingEngineService();
        String top = fresh.engine().suggest("稼働時刻", NamingKind.VARIABLE).get(0).rendered();
        assertEquals("operationTime", top);

        UserDictionaryStore store = UserDictionaryStore.inConfigDir();
        UserDictionary loaded = store.load();
        assertFalse("壊れていることが警告で分かる", loaded.warnings().isEmpty());
        NamingEngineService.getInstance().replaceUserLayer(loaded);
        assertEquals("operationTime", firstPluginItem("稼働時刻"));
    }

    /** 壊れたファイルを保存で上書きしても、元のバイト列が .bak に残る。 */
    public void testSaveKeepsBrokenFileAsBackup() throws IOException {
        UserDictionaryStore store = tempStore();
        byte[] broken = {(byte) 0xff, (byte) 0xfe, (byte) 0xc3, 0x28};
        Files.write(store.file(), broken);

        store.save(List.of(new DictionaryEntry("取得", List.of("fetch"))));

        assertOrderedEquals(Files.readAllBytes(store.backupFile()), broken);
        assertEquals("取得\tfetch\n", Files.readString(store.file(), StandardCharsets.UTF_8));
    }

    /** 手で書いたコメント行は保存で消えるが、.bak に残る。 */
    public void testSaveKeepsCommentedFileAsBackup() throws IOException {
        UserDictionaryStore store = tempStore();
        String original = "# 自分用のメモ\n取得\tfetch\n\n";
        Files.writeString(store.file(), original, StandardCharsets.UTF_8);

        store.save(List.of(new DictionaryEntry("取得", List.of("retrieve"))));

        assertEquals(original, Files.readString(store.backupFile(), StandardCharsets.UTF_8));
    }

    /** ファイルが無いときの保存では .bak を作らない。 */
    public void testSaveWithoutFileCreatesNoBackup() throws IOException {
        UserDictionaryStore store = tempStore();
        assertFalse(Files.exists(store.file()));

        store.save(List.of(new DictionaryEntry("取得", List.of("fetch"))));

        assertTrue(Files.exists(store.file()));
        assertFalse(Files.exists(store.backupFile()));
        assertEquals("japanese-naming-user-dictionary.tsv.bak", store.backupFile().getFileName().toString());
    }

    public void testRowsToEntriesSplitsTranslationsByComma() throws ConfigurationException {
        List<DictionaryEntry> entries = UserDictionaryStore.toEntries(List.of(
            new UserDictionaryStore.Row(" 取得 ", "fetch, retrieve ,"),
            new UserDictionaryStore.Row("", ""))); // 空の行は無視する
        assertEquals(List.of(new DictionaryEntry("取得", List.of("fetch", "retrieve"))), entries);
        assertEquals(List.of(new UserDictionaryStore.Row("取得", "fetch, retrieve")), UserDictionaryStore.toRows(entries));
    }

    public void testRowsWithoutTranslationOrDuplicatedAreRejected() {
        assertRejected(new UserDictionaryStore.Row("取得", " , "));
        assertRejected(new UserDictionaryStore.Row("", "fetch"));
        assertRejected(new UserDictionaryStore.Row("取得", "fetch"), new UserDictionaryStore.Row("取得", "get"));
        assertRejected(new UserDictionaryStore.Row("取,得", "fetch"));
    }

    private static void assertRejected(UserDictionaryStore.Row... rows) {
        try {
            UserDictionaryStore.toEntries(List.of(rows));
            fail("受け付けないこと: " + List.of(rows));
        } catch (ConfigurationException expected) {
            System.out.println("[REJECT] " + List.of(rows) + " -> " + expected.getMessage());
        }
    }

    /** 画面の一覧表に行を足して適用すると、ファイルに保存され、次の補完から反映される(画面を目で見る代わりのロジック確認)。 */
    public void testConfigurableApplySavesAndReflects() throws Exception {
        UserDictionaryStore store = tempStore();
        UserDictionaryConfigurable configurable = new UserDictionaryConfigurable(store);
        try {
            JComponent component = configurable.createComponent();
            configurable.reset();
            JBTable table = UIUtil.findComponentOfType(component, JBTable.class);
            assertNotNull("一覧表がある", table);
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            assertEquals(0, model.getRowCount());
            assertFalse(configurable.isModified());

            model.addRow(new Object[] {"取得", "fetch,get"});
            assertTrue(configurable.isModified());
            configurable.apply();

            assertFalse(configurable.isModified());
            assertEquals("取得\tfetch,get\n", Files.readString(store.file(), StandardCharsets.UTF_8));
            assertEquals("fetchPageTitle", firstPluginItem(PAGE_TITLE));

            model.removeRow(0);
            configurable.apply();
            assertEquals("", Files.readString(store.file(), StandardCharsets.UTF_8));
            assertEquals("getPageTitle", firstPluginItem(PAGE_TITLE));
        } finally {
            configurable.disposeUIResources();
        }
    }

    /** Settings > Tools に登録され、IDE が画面を生成できる。 */
    public void testConfigurableIsRegisteredUnderTools() {
        ConfigurableEP<Configurable> ep = Configurable.APPLICATION_CONFIGURABLE.getExtensionList().stream()
            .filter(e -> UserDictionaryConfigurable.class.getName().equals(e.instanceClass))
            .findFirst().orElse(null);
        assertNotNull("applicationConfigurable として登録されている", ep);
        assertEquals("tools", ep.parentId);
        Configurable created = ep.createConfigurable();
        assertNotNull(created);
        assertEquals("日本語→英語 命名", created.getDisplayName());
        assertTrue("同梱のライセンス文書を読める", UserDictionaryConfigurable.readLicense().contains("CC BY-SA 4.0"));
    }

    private String firstPluginItem(String phrase) {
        myFixture.configureByText("A.java", "class A { void f() { int <caret> } }");
        myFixture.type(phrase);
        myFixture.completeBasic();
        LookupImpl lookup = (LookupImpl) myFixture.getLookup();
        assertNotNull("Lookup が出ること", lookup);
        List<String> items = PluginLookups.fromPlugin(lookup.getItems().toArray(LookupElement[]::new));
        System.out.println("[STORE] " + phrase + "=" + items);
        assertFalse(items.isEmpty());
        return items.get(0);
    }
}
