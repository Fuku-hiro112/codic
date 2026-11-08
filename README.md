# 日本語→英語 命名(Japanese Naming Assist)

IntelliJ IDEA で、識別子を書く位置に日本語を打つと、英語の命名候補を補完の一覧に出すプラグインです。
提供を終えた codic の代わりとして作りました。辞書はすべて同梱で、外部へ通信しません。

## できること

- 変数名などの位置で日本語を打つと、補完を呼ばなくても候補の一覧が出ます。選ぶと日本語が英語名に置き換わります。
  例: ローカル変数の位置で「稼働時刻」→ `operationTime`
- 命名の種別は位置から自動で決まります: 変数・フィールド(`camelCase`)、メソッド名(`camelCase`)、
  定数 `static final`(`SNAKE_CASE`)、クラス名(`PascalCase`)。
- 「〜する」は動詞から始まる名前に、「〜か」で終わる句は真偽値の名前になります。
  例: 「従業員数を取得する」→ `getEmployeeCount`、「広告を表示するか」→ `shouldShowAd`、
  「有料プランか」→ `isPaidPlan`、「ウェブサイトを持っているか」→ `hasWebsite`
- 辞書に無い語は、その語だけ日本語のまま残した候補を最後に出します(例: `get在庫引当`)。
- 候補は最大 10 件。一覧の下の方に同じ語の別訳を組み合わせた候補も並びます。
- 候補を選ぶ前に Alt+Enter(または一覧の候補の電球)から「単語を並べ替え・結合…」を開くと、
  単語ごとの別訳の選択・並べ替え・結合をしてから置き換えられます。

## 動作条件

- IntelliJ IDEA Community Edition 2025.2.6.2(ビルド 252 系のみ。`sinceBuild` 252 / `untilBuild` 252.*)
- 対象言語は Java のみ

## 導入

1. ビルドする(JDK 21 が要ります。Gradle は同梱の wrapper を使います)。

   ```
   ./gradlew :plugin:buildPlugin
   ```

   zip は `plugin/build/distributions/plugin-0.1.0.zip` にできます。
2. IntelliJ IDEA で Settings > Plugins > 歯車アイコン > Install Plugin from Disk... を選び、上の zip を指定します。
3. IDE を再起動します。

## 利用者辞書

Settings > Tools > 日本語→英語 命名 で、語と訳(複数可)を追加・編集・削除できます。
利用者辞書は同梱辞書より優先されます。ファイルは IDE の設定フォルダの
`japanese-naming-user-dictionary.tsv` で、保存の前に元のファイルを `.bak` に退避します。

## 既知の制限

- コメントの中・文字列の中では、候補の一覧は自動では出ません。手動の補完(Ctrl+Space)を呼べば出ます
  (テスト環境での観察。`plugin/src/test/java/namingassist/plugin/AutoPopupTest.java` の `testContextObservation`)。
- Kotlin など Java 以外の言語には対応していません。
- フィールド宣言の名前の位置では、IDE 標準の候補(例: `稼働時刻AnInt`)も混ざります。
- 自動で一覧が出ることはテスト環境(1 文字ずつ入力する模擬)で確かめたものです。
  実際の IDE で IME から日本語を確定したときの動作、設定画面と「単語を並べ替え・結合…」の小窓の見た目は、まだ確認していません。
- 命名の質は辞書次第です。思った訳が出ないときは利用者辞書に足してください。

## ライセンスと出典

配布物には次の第三者の著作物が含まれます。全文(原文のまま)は
`plugin/src/main/resources/THIRD-PARTY-NOTICES.md`(配布物では plugin の jar の中の `THIRD-PARTY-NOTICES.md`)にあります。

- **JMdict**(EDRDG): 命名用に加工した辞書 `jmdict.tsv` として同梱。CC BY-SA 4.0。
  加工辞書は CC BY-SA 4.0 で配布され、プラグインのコードとは別のライセンスです。
  出典と加工の内容は `engine/src/main/resources/dictionary/LICENSE-JMDICT.md`(設定画面からも表示できます)。
  https://www.edrdg.org/edrdg/licence.html
- **Kuromoji** 0.9.0(Atilika Inc. and contributors): 日本語の形態素解析。Apache License 2.0。
- **IPADIC**(mecab-ipadic-2.7.0-20070801、Nara Institute of Science and Technology。項目の多くは ICOT Free Software 由来):
  Kuromoji の辞書データとして同梱。著作権表示・許諾条件・無保証条項(NO WARRANTY)の全文は THIRD-PARTY-NOTICES.md の §3。

## 開発

```
./gradlew build                 # engine と plugin のビルドとテスト
./gradlew :plugin:buildPlugin   # 配布 zip を作る
./gradlew :plugin:runIde        # 開発用の IDE で試す
```

- `engine/`: 日本語の句を英語名にする部分(IDE に依存しない)。
- `plugin/`: IntelliJ への組み込み(補完・種別の判定・編集の小窓・設定画面)。
- `tools/convert-jmdict/`: JMdict の原本から `jmdict.tsv` を作る変換器。辞書の更新はこれで作り直します。
