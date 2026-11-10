# 日本語→英語 命名(Japanese Naming Assist)

An IntelliJ IDEA plugin that suggests English identifier names for Java from a Japanese phrase.
Type a Japanese phrase where a name goes (e.g. `稼働時刻`) and pick an English candidate (`operationTime`) from the completion list.
It works offline with bundled dictionaries and makes no network requests. Made as a replacement for the discontinued naming service codic.

IntelliJ IDEA で、Java の識別子を書く位置に日本語を打つと、英語の命名候補を補完の一覧に出すプラグインです。
提供を終えた命名支援サービス codic の代わりとして作りました。辞書はすべて同梱で、外部へ通信しません。

例: ローカル変数の位置で「稼働時刻」と打って確定 → 一覧から選ぶと `operationTime` に置き換わります。

## 目次

- [特徴](#特徴)
- [命名の例](#命名の例)
- [動作環境](#動作環境)
- [導入](#導入)
- [使い方](#使い方)
- [命名のルール](#命名のルール)
- [仕組み](#仕組み)
- [開発](#開発)
- [確認の範囲](#確認の範囲)
- [既知の制限](#既知の制限)
- [ライセンス](#ライセンス)
- [謝辞と出典](#謝辞と出典)

## 特徴

- 識別子の位置で日本語を確定すると、補完を呼ばなくても候補の一覧が出ます。選ぶと日本語が英語名に置き換わります。
- 英字だけを打っているときは何も足さず、IDE 標準の補完をそのまま残します(英字の入力を妨げません)。
- 辞書は同梱で、外部へ通信しません。オフラインで動き、利用料はかかりません。
- 命名の種別(変数・フィールド・定数・メソッド・クラス)を位置から自動で判定し、表記(camelCase・UPPER_SNAKE_CASE・PascalCase)を合わせます。
- 「〜する」は動詞から始まる名前に、「〜か」で終わる句は真偽値の名前(`is`・`has`・`should` など)になります。
- 同じ語の別訳を組み合わせた候補も並びます(最大 10 件)。
- 名詞の直後の `s`(全角の `ｓ` も可)で複数形にします。例: 「広告s」→ `ads`
- 先頭の数字は数詞として読みます。例: 「2次元配列」→ `twoDimensionArray`(全角の `２` も同じ)
- 候補の一覧が出ている間に Alt+Enter(または Ctrl+Shift+Q)で小窓を開き、単語の並べ替え・結合・別訳への差し替えをしてから置き換えられます。
- 利用者辞書: 設定画面で語と訳を追加できます。利用者辞書は同梱辞書より優先されます。
- 英字の直後に日本語を打っても、英字を崩さずに残し、日本語の部分の英語を後ろにつなぎます。例: `str` の直後に「稼働時刻」→ `strOperationTime`
- 辞書に無い語は、その語だけ日本語のまま残した候補を出します(例: 「在庫引当を取得する」→ `getStock引当`)。

## 命名の例

命名の回帰テスト([`engine/src/test/resources/golden-examples.tsv`](engine/src/test/resources/golden-examples.tsv))で、
同梱辞書を使った先頭の候補が次のとおりになることを確かめています。

| 日本語の句 | 命名の種別 | 先頭の候補 |
|---|---|---|
| 従業員数を取得する | メソッド | `getEmployeeCount` |
| ページタイトルを取得する | メソッド(snake_case) | `get_page_title` |
| 広告を表示するか | メソッド | `shouldShowAd` |
| ファイルは作成されているか | メソッド | `isFileCreated` |
| 有料プランか | メソッド | `isPaidPlan` |
| 正規表現検索は有効か | メソッド | `isRegexpSearchEnabled` |
| ウェブサイトを持っているか | メソッド | `hasWebsite` |
| 通知すべきか | メソッド | `shouldNotify` |
| 稼働時刻 | 変数 | `operationTime` |
| 作成日 | 変数 | `createdOn` |
| 患者名 | 変数 | `patientName` |
| 花粉数 | 変数 | `pollenCount` |
| 外部ID | 変数 | `externalId` |
| 下限値 / 上限値 | 変数 | `lowerBound` / `upperBound` |
| 緯度 / 経度 | 変数 | `latitude` / `longitude` |
| 観測地点 | クラス | `ObservationPoint` |
| 花粉レベル | クラス | `PollenLevel` |

`snake_case` の行は、エンジンが表記の指定を受け付けることの回帰例です。プラグインは種別ごとの表記(下の「命名のルール」)だけを使います。

## 動作環境

- IntelliJ IDEA Community Edition 2025.2.x(開発・テストは 2025.2.6.2)。ビルド 252 系のみ(`sinceBuild` 252 / `untilBuild` 252.*)
- 対象言語は Java のみ
- 作者が確かめたのは Windows です。macOS・Linux では確かめていません。

## 導入

配布済みのリリースはありません。ソースからビルドして zip を作り、IDE に導入します。

1. ビルドする(JDK 21 が要ります。Gradle は同梱の wrapper を使います)。

   ```
   ./gradlew :plugin:buildPlugin
   ```

   zip は `plugin/build/distributions/` にできます(現在の版では `plugin-0.1.0.zip`)。
2. IntelliJ IDEA で Settings > Plugins > 歯車アイコン > Install Plugin from Disk... を選び、上の zip を指定します。
3. IDE を再起動します。

導入すると IDE 全体で有効になり、プロジェクトごとの設定は要りません。
利用者辞書は IDE の設定フォルダに保存され、プロジェクトをまたいで共有されます。

通常の配布版の IDE に zip を導入した状態での動作は、まだ確かめていません(下の「確認の範囲」)。

## 使い方

### 候補を選ぶ

1. 変数名・フィールド名・メソッド名・クラス名などを書く位置で、日本語を打って IME で確定します。
2. 候補の一覧が自動で出ます。上下で選び、Enter で確定すると日本語が英語名に置き換わります。
3. 一覧は日本語の句でも絞り込めます。

### 候補を編集する(小窓)

候補の一覧に本プラグインの候補が並んでいる間に **Alt+Enter** を押すと「単語を並べ替え・結合」の小窓が開きます。
同じ操作は **Ctrl+Shift+Q** にも割り当ててあります。

- 「← 前へ」「後ろへ →」で選んだ単語を動かし、「次の語と結合」で次の語とつなぎ、「別訳」で単語ごとの訳を差し替えます。
- 「確定」で、編集した名前に日本語を置き換えます。Java の識別子にならない名前は確定できません。
- 対象は一覧で選んでいる候補です(選んでいるのが本プラグイン以外の候補なら、一覧の先頭の本プラグインの候補)。
- 本プラグインの候補が無いとき(英字だけの一覧、一覧が出ていないとき)の Alt+Enter は IDE 標準のままです。
- キーは Settings > Keymap の「日本語の命名候補を編集(単語の並べ替え・結合)」で変えられます。
- Alt+Enter で小窓でなく中身の見えない四角形が出たときは、その四角形の 1 行目をクリックすると小窓が開きます
  (runIde の環境で、IDE 内部の例外(idea.log に記録)により、候補の操作の一覧の文字が描かれないことを観察しました。
  通常の配布版の IDE でも同じかは未確認です。2 行目は「候補一覧の電球を表示しない」の切り替えなので押さないでください)。

### 利用者辞書

Settings > Tools > 日本語→英語 命名 で、語と訳(複数可)を追加・編集・削除できます。
利用者辞書は同梱辞書より優先され、複合語の照合にも使われます(形態素解析で分かれる語も、登録すれば 1 語として引きます)。
ファイルは IDE の設定フォルダの `japanese-naming-user-dictionary.tsv` で、保存の前に元のファイルを `.bak` に退避します。
設定画面から、同梱辞書(JMdict 由来)の出典とライセンスも表示できます。

## 命名のルール

### 命名の種別

カーソルの位置から種別を決め、種別ごとの表記で候補を綴ります。

| 位置 | 種別 | 表記 | 例 |
|---|---|---|---|
| ローカル変数・フィールド・判定できない位置 | 変数 | camelCase | `operationTime` |
| `static final` のフィールド・enum の定数 | 定数 | UPPER_SNAKE_CASE | `OPERATION_TIME` |
| メソッドの宣言名 | メソッド | camelCase | `getEmployeeCount` |
| クラスの宣言名 | クラス | PascalCase | `ObservationPoint` |

インタフェースなどの暗黙の `static final` のフィールドは、初期化子があるときだけ定数とみなします(`()` を打つ前のメソッドであることが多いため)。

### 句の訳し方

- 「〜する」は動詞から始まる名前になります(「従業員数を取得する」→ `getEmployeeCount`)。
- 「〜か」で終わる句は真偽値の名前になります(「有料プランか」→ `isPaidPlan`、「広告を表示するか」→ `shouldShowAd`、「ウェブサイトを持っているか」→ `hasWebsite`)。
- 変数の位置では「〜する」を名詞の形に訳すことがあります(「ページを生成する」→ `creationPage`)。

### 英字の直後に日本語を打ったとき

英字(打ち済みの識別子)はそのまま残し、日本語の部分だけを英語にして、種別の表記に沿って後ろにつなぎます。

- `pageDecision` の直後に「ああ」→ `pageDecisionLikeThat`
- `str` の直後に「稼働時刻」→ `strOperationTime`
- `user` の直後に「名前」→ `userName`
- クラス名で `Pollen` の直後に「観測地点」→ `PollenObservationPoint`
- 定数で `max` の直後に「稼働時刻」→ `MAX_OPERATION_TIME`(定数では英字を大文字にし、語の境目と後ろを `_` でつなぎます)

次のときは英字を略語とみなし、英字と日本語の全体を句として訳します。

- 英字に小文字が無い(大文字・数字・`_`・`$` だけ。`ID`・`URL` など)。例: `ID` の直後に「を取得する」→ `getId`
- 英字が 1 語の小文字(小文字で始まり、小文字と数字だけ。`id`・`url`・`json`・`md5` など)で、日本語の部分が
  格助詞(を・の・が・に・で・と・へ・から)で始まる。例: `id` の直後に「を取得する」→ `getId`、
  `url` の直後に「を生成する」→ `creationUrl`(全体を句として訳した結果で、英字の無い「ページを生成する」→ `creationPage` と同じ訳し方です)

それ以外は英字を残します。例: `pageDecision` の直後に「を取得する」→ `pageDecisionGet`(英字が 1 語の小文字ではない)。

- 全角の英字(`ｉｄ`・`ｐａｇｅＤｅｃｉｓｉｏｎ`)は半角と同じに扱い、残す英字は半角で書き込みます。
- 英字と、日本語の部分の英語の先頭の語が同じなら重ねません(`get` の直後に「ページタイトルを取得する」→ `getPageTitle`、
  `is` の直後に「有料プランか」→ `isPaidPlan`)。
- 英字の前に数字があるとき(「2dモデル」など)は、英字を残さず全体を句として訳します。

## 仕組み

Gradle のマルチモジュールで、命名の処理と IDE への組み込みを分けています。依存は `plugin` → `engine` の一方向です。

- `engine/`: 日本語の句を英語名にする部分。IntelliJ に依存しない純粋な Java です。
  - 日本語の解析: Kuromoji(IPADIC)で形態素に分けます。
  - 辞書: 3 層を上から順に引きます。
    1. 利用者辞書(設定画面で編集)
    2. 自前の辞書 `engine/src/main/resources/dictionary/curated.tsv`(命名向けに選んだ訳)
    3. JMdict 由来の辞書 `engine/src/main/resources/dictionary/jmdict.tsv`
  - 組み立て: 句の構造(動詞・真偽値・複数形など)から単語を並べ、種別の表記で綴ります。
- `plugin/`: IntelliJ への組み込み。IDE 標準の補完の仕組み(`completion.contributor`)に候補を足し、
  種別の判定、候補の編集の小窓、設定画面を持ちます。
- `tools/convert-jmdict/`: JMdict の原本から `jmdict.tsv` を作る変換器(Python の標準ライブラリだけを使います)。

## 開発

codic のルート(このリポジトリのルート)で実行します。JDK 21 が要ります。

```
./gradlew build                 # engine と plugin のビルドとテスト
./gradlew :plugin:buildPlugin   # 配布 zip を作る
./gradlew :plugin:runIde        # 開発用の IDE を起動してプラグインを試す
```

- テスト
  - `engine`: JUnit 5。辞書・形態素解析・句の組み立て・真偽値の規則の単体テストと、
    命名の回帰テスト `GoldenExamplesTest`(`golden-examples.tsv` の例を実際の Kuromoji と同梱辞書で評価)。
  - `plugin`: IntelliJ Platform のテストフレームワーク(JUnit 4)。補完・自動表示・英字入力が変わらないこと・
    種別の判定・英字を残す挙動・小窓・利用者辞書の保存を確かめます。
- 回帰例が外れたときは、期待値を書き換えず、辞書か規則を直します。
- `runIde` で起動した IDE は、起動した時点のプラグインで動きます。修正を反映するには、起動中の IDE を閉じてから
  もう一度 `./gradlew :plugin:runIde` で起動します。
- 辞書(JMdict)の更新手順は [`docs/dictionary-update.md`](docs/dictionary-update.md) にあります。

## 確認の範囲

- テスト環境(IntelliJ のテストフレームワーク。1 文字ずつ入力する模擬)で確かめたこと: 自動表示、候補の確定による置き換え、
  英字の入力で補完の結果が変わらないこと、種別の判定、英字を残してつなぐ挙動、Alt+Enter・Ctrl+Shift+Q のキーの割り当てと
  選ばれるアクション、小窓の確定による置き換え、利用者辞書の保存。
- 作者が開発用の IDE(`runIde`、Windows)で確かめたこと: 日本語の確定後に候補が自動で出て置き換わること、
  英字の入力が邪魔されないこと、設定画面で利用者辞書に語を足せること、Alt+Enter と Ctrl+Shift+Q で小窓を開いて語順を編集できること。
- まだ確かめていないこと: 通常の配布版の IDE に zip を導入した状態での動作。英字の直後に日本語を打ったときの挙動の実際の IDE での動作
  (テスト環境の補完と確定で確かめた範囲です)。Windows 以外の OS。

## 既知の制限

- コメントの中・文字列の中では、候補の一覧は自動では出ません。手動の補完(Ctrl+Space)を呼べば出ます
  (テスト環境での観察。`plugin/src/test/java/namingassist/plugin/AutoPopupTest.java` の `testContextObservation`)。
- Kotlin など Java 以外の言語には対応していません。
- フィールド宣言の名前の位置では、IDE 標準の候補(例: `稼働時刻AnInt`)も混ざります。
- 命名の質は辞書次第です。JMdict 由来の訳は一般の辞書の訳で、命名に向かない訳が上位に出ることがあります
  (例: 「人s」の先頭の候補は `men`)。思った訳が出ないときは利用者辞書に足してください。
- 英字の直後の格助詞の判定は日本語の部分の先頭の文字だけを見ます。そのため、1 語の小文字の英字の直後に、ひらがなのまま確定した語が
  格助詞と同じ文字で始まると(例: `id` の直後に「とうろく」)、格助詞とみなして全体を句として訳します。
- 本プラグインの候補が並ぶ間は、Alt+Enter で IDE 標準の候補の操作(補完から除外など)は開けず、小窓が開きます。
- Ctrl+Shift+Q は候補の一覧が出ている間だけ働きます。一覧が無いときは何も起きません。
- Ctrl+Shift+Q は、IntelliJ IDEA Community 2025.2.6.2 の Windows の既定のキーマップ(`$default`)と同梱プラグインでは
  未使用であることを確かめて選びました。macOS・Linux 向けのキーマップや、他のプラグイン・OS・IME の割り当てとは衝突し得ます。
  macOS のキーマップで Ctrl が Cmd に置き換わると、Cmd+Shift+Q は macOS のログアウトの操作と重なります(macOS では確かめていません)。
  衝突するときは Settings > Keymap で変えてください。
- 対応する IDE はビルド 252 系(2025.2.x)だけです。

## ライセンス

ライセンスは、自作のコードと同梱物ごとに分かれます。

| 対象 | ライセンス | 全文・出典 |
|---|---|---|
| このプラグインのコード(engine・plugin・tools の自作部分) | MIT License | [`LICENSE`](LICENSE) |
| 辞書データ `jmdict.tsv`(JMdict/EDICT 由来の加工物) | CC BY-SA 4.0(コードとは別) | [`engine/src/main/resources/dictionary/LICENSE-JMDICT.md`](engine/src/main/resources/dictionary/LICENSE-JMDICT.md) |
| Kuromoji 0.9.0 | Apache License 2.0 | [`plugin/src/main/resources/THIRD-PARTY-NOTICES.md`](plugin/src/main/resources/THIRD-PARTY-NOTICES.md) §1・§2 |
| IPADIC(Kuromoji の辞書データ) | mecab-ipadic の条件(NAIST / ICOT) | [`plugin/src/main/resources/THIRD-PARTY-NOTICES.md`](plugin/src/main/resources/THIRD-PARTY-NOTICES.md) §3 |

配布 zip では、プラグインのフォルダ(`lib/` と同じ階層)に `LICENSE`・`LICENSE-JMDICT.md`・`THIRD-PARTY-NOTICES.md` を置いています
(`THIRD-PARTY-NOTICES.md` は plugin の jar の中、`LICENSE-JMDICT.md` は engine の jar の中にもあります)。

### 辞書データの出典

This package uses the JMdict/EDICT dictionary files (processed for naming).
JMdict/EDICT (c) Electronic Dictionary Research and Development Group, CC BY-SA 4.0.
https://www.edrdg.org/edrdg/licence.html

同梱の `jmdict.tsv` は JMdict を命名用に加工したもので、原本そのものではありません。
原本の版と加工の内容は `LICENSE-JMDICT.md` にあります。

この節はライセンスの概要で、法的助言ではありません。このプラグインやその改変物を配布する場合は、各ライセンスの条件を全文で確認してください。

## 謝辞と出典

- **codic**: 提供を終えた命名支援サービス codic に着想を得ました。codic について公開されている記載(使い方や命名の例)をもとに仕様を決めましたが、
  codic のコードや辞書は使っておらず、そのデータも持っていません。
- **Kuromoji**(Atilika Inc. and contributors): 日本語の形態素解析。Apache License 2.0。
- **IPADIC**(mecab-ipadic-2.7.0-20070801、Nara Institute of Science and Technology。項目の多くは ICOT Free Software 由来):
  Kuromoji の辞書データとして同梱。
- **JMdict**(Electronic Dictionary Research and Development Group): 命名用に加工した辞書 `jmdict.tsv` として同梱。CC BY-SA 4.0。
