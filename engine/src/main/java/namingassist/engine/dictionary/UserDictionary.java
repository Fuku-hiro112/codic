package namingassist.engine.dictionary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 利用者辞書(ファイル 1 つ)。ファイルが無い・空・壊れているときは空の辞書として動き、
 * 例外を投げない。読めなかった理由は {@link #warnings()} で返す。
 */
public final class UserDictionary implements Dictionary {
    private final Path file;
    private Map<String, DictionaryEntry> entries = Map.of();
    private List<String> warnings = List.of();

    public UserDictionary(Path file) {
        this.file = file;
        reload();
    }

    @Override
    public synchronized Optional<DictionaryEntry> lookup(String japaneseWord) {
        return Optional.ofNullable(entries.get(japaneseWord));
    }

    /** 読み込み時に起きた問題(無し=空)。ファイルが無いのは通常の状態なので警告にしない。 */
    public synchronized List<String> warnings() {
        return warnings;
    }

    public synchronized List<DictionaryEntry> entries() {
        return List.copyOf(entries.values());
    }

    /** 全項目を書き換えて保存し、読み直す。 */
    public synchronized void save(List<DictionaryEntry> newEntries) throws IOException {
        String text = TsvFormat.format(newEntries);
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, text, StandardCharsets.UTF_8);
        reload();
    }

    private void reload() {
        Map<String, DictionaryEntry> loaded = new LinkedHashMap<>();
        List<String> warns = new ArrayList<>();
        try {
            byte[] bytes = Files.readAllBytes(file);
            String text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
            if (text.startsWith("﻿")) {
                text = text.substring(1);
            }
            TsvFormat.Parsed parsed = TsvFormat.parse(text, file.getFileName().toString());
            loaded.putAll(parsed.entries());
            warns.addAll(parsed.warnings());
        } catch (NoSuchFileException e) {
            // 未作成は正常
        } catch (CharacterCodingException e) {
            warns.add("利用者辞書が UTF-8 として読めないため空として扱う: " + file);
        } catch (IOException | RuntimeException e) {
            warns.add("利用者辞書を読めないため空として扱う: " + file + " (" + e.getMessage() + ")");
        }
        this.entries = loaded;
        this.warnings = List.copyOf(warns);
    }
}
