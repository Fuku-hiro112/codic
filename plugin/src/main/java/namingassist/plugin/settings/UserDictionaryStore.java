package namingassist.plugin.settings;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.options.ConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import namingassist.engine.dictionary.DictionaryEntry;
import namingassist.engine.dictionary.UserDictionary;
import namingassist.plugin.NamingEngineService;

/**
 * 利用者辞書のファイル(IDE の設定フォルダに 1 つ)の読み書きと、補完への反映。
 * 形式は engine の {@link UserDictionary} と同じ「日本語&lt;TAB&gt;訳1,訳2」の TSV。
 */
public final class UserDictionaryStore {
    public static final String FILE_NAME = "japanese-naming-user-dictionary.tsv";

    /** 一覧表の 1 行。translations はカンマ区切りの文字列のまま持つ。 */
    public record Row(String japanese, String translations) {
    }

    private final Path file;
    private final NamingEngineService engines;

    public UserDictionaryStore(Path file, NamingEngineService engines) {
        this.file = file;
        this.engines = engines;
    }

    /** IDE の設定フォルダの利用者辞書。 */
    public static UserDictionaryStore inConfigDir() {
        return new UserDictionaryStore(defaultFile(), NamingEngineService.getInstance());
    }

    public static Path defaultFile() {
        return Path.of(PathManager.getConfigPath(), FILE_NAME);
    }

    public Path file() {
        return file;
    }

    /** 保存の直前に元のファイルを退避する先(直前 1 世代)。 */
    public Path backupFile() {
        return file.resolveSibling(file.getFileName() + ".bak");
    }

    /** 読む。無い・壊れているときは空で、理由は {@link UserDictionary#warnings()}。 */
    public UserDictionary load() {
        return new UserDictionary(file);
    }

    /**
     * 全項目を書き換えて保存し、次の補完から使う(再起動は要らない)。保存は一覧の内容で置き換えるため、
     * 読めなかった行・コメント行は消える。元のファイルがあれば {@link #backupFile()} にそのまま退避する。
     */
    public void save(List<DictionaryEntry> entries) throws IOException {
        if (Files.exists(file)) {
            Files.copy(file, backupFile(), StandardCopyOption.REPLACE_EXISTING);
        }
        UserDictionary dictionary = new UserDictionary(file);
        dictionary.save(entries);
        engines.replaceUserLayer(dictionary);
    }

    public static List<Row> toRows(List<DictionaryEntry> entries) {
        return entries.stream().map(e -> new Row(e.japanese(), String.join(", ", e.translations()))).toList();
    }

    /**
     * 一覧表の行を辞書の項目にする。語と訳が両方空の行は無視する。訳はカンマで分け、前後の空白を除く。
     *
     * @throws ConfigurationException 訳が無い・語が無い・語が重複・区切り文字を含む行があるとき
     */
    public static List<DictionaryEntry> toEntries(List<Row> rows) throws ConfigurationException {
        List<DictionaryEntry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            String japanese = strip(rows.get(i).japanese());
            List<String> translations = Arrays.stream(strip(rows.get(i).translations()).split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).toList();
            String where = (i + 1) + " 行目";
            if (japanese.isEmpty() && translations.isEmpty()) {
                continue;
            }
            if (japanese.isEmpty()) {
                throw new ConfigurationException(where + ": 日本語の語が空です");
            }
            if (translations.isEmpty()) {
                throw new ConfigurationException(where + "(" + japanese + "): 訳が空です");
            }
            if (hasSeparator(japanese) || translations.stream().anyMatch(UserDictionaryStore::hasSeparator)) {
                throw new ConfigurationException(where + "(" + japanese + "): 語にカンマ・タブ・改行は使えません");
            }
            if (!seen.add(japanese)) {
                throw new ConfigurationException(where + ": 「" + japanese + "」が 2 回あります。1 行にまとめてください");
            }
            entries.add(new DictionaryEntry(japanese, translations));
        }
        return entries;
    }

    private static String strip(String s) {
        return s == null ? "" : s.strip();
    }

    private static boolean hasSeparator(String s) {
        return s.indexOf(',') >= 0 || s.indexOf('\t') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
    }
}
