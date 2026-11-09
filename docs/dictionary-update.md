# 辞書(JMdict)の更新手順

同梱の辞書 `engine/src/main/resources/dictionary/jmdict.tsv` は、EDRDG(Electronic Dictionary Research and
Development Group)の JMdict を命名用に加工した派生物です。ライセンスは **CC BY-SA 4.0** で、
プラグインのコード(MIT License、`LICENSE`)とは別です。出典・版・加工の内容は
`engine/src/main/resources/dictionary/LICENSE-JMDICT.md` に書きます。

## 方針

- **プラグインのリリースごとに**、JMdict の最新版から `jmdict.tsv` を作り直す。
- JMdict のライセンス(https://www.edrdg.org/edrdg/licence.html)が求める「定期的な更新」の義務が、
  このプラグインのようなオフラインで同梱する配布物にどこまで及ぶかは **未確認** です。
  上の「リリースごとに更新する」は、その確認が済むまでの自主的な方針であり、義務の範囲を満たすと確かめたものではありません。

## 加工した辞書データの公開場所

加工した辞書データ(`jmdict.tsv`、CC BY-SA 4.0)は、次の 2 か所で公開します。

- プラグインの配布物: 配布 zip の `lib/engine-<版>.jar` の中の `dictionary/jmdict.tsv`
  (同じ jar の `dictionary/LICENSE-JMDICT.md` と、zip のプラグインのフォルダ直下の `LICENSE-JMDICT.md` に出典とライセンス)
- リポジトリ: `engine/src/main/resources/dictionary/jmdict.tsv`

## 手順

### 1. 原本を取得する

```
curl -L -o JMdict_e.gz https://www.edrdg.org/pub/Nihongo/JMdict_e.gz
```

案内ページ: https://www.edrdg.org/jmdict/j_jmdict.html 。原本はリポジトリに入れません(作業用のフォルダに置く)。

### 2. 原本の版と SHA-256 を控える

```
sha256sum JMdict_e.gz
gzip -dc JMdict_e.gz | grep -m1 "JMdict created"
```

2 行目の出力(例: `<!-- JMdict created: 2026-11-08 -->`)が原本の版です。

### 3. 変換する

```
python -I tools/convert-jmdict/convert_jmdict.py JMdict_e.gz engine/src/main/resources/dictionary/jmdict.tsv
```

変換器は標準ライブラリだけを使い、通信しません。加工の内容(絞り込み・訳の選び方・形式)は変換器の先頭のコメントと
`LICENSE-JMDICT.md` の「加工の内容」にあります。変換器の規則を変えたときは、`LICENSE-JMDICT.md` の「加工の内容」も直します。

### 4. `LICENSE-JMDICT.md` に記録する

`engine/src/main/resources/dictionary/LICENSE-JMDICT.md` の冒頭の箇条を更新します。

- `原本の版:` に手順 2 の版と取得日(例: `JMdict created 2026-12-01(取得日 2026-12-01)`)
- `原本の SHA-256:` に手順 2 のハッシュ値(この行が無ければ `原本の版` の次に足す)
- 変換日(変換器を実行した日)

2026-11-08 版の原本の SHA-256 は記録されていません(原本が手元に残っていないため)。次の更新から記録します。

`plugin/src/main/resources/THIRD-PARTY-NOTICES.md` の表の JMdict の「版」も同じ版に直します。

### 5. テストを通す

```
./gradlew build
```

失敗 0 であること。特に `GoldenExamplesTest`(命名の回帰例)が外れたときは、期待値を書き換えず、
`curated.tsv` などの辞書か規則を直します。配布 zip は `./gradlew clean :plugin:buildPlugin` で作り直します。

### 6. コミットする

`jmdict.tsv`・`LICENSE-JMDICT.md`・`THIRD-PARTY-NOTICES.md` を一緒にコミットします。
