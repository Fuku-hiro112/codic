package namingassist.engine.dictionary;

import java.util.Optional;

/** 日本語の語から訳を引く。読み出し元が同梱・利用者・合成と複数あるので境界とする。 */
public interface Dictionary {
    Optional<DictionaryEntry> lookup(String japaneseWord);
}
