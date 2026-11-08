# 日本語→英語命名 補完プラグイン 実装計画

> **実行者へ:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(推奨)または superpowers:executing-plans で、タスクごとに実施する。手順はチェックボックスで追う。

**Goal:** IntelliJ IDEA Community 2025.2.6.2 上で、日本語を確定した直後に英語の命名候補が自動表示され、選ぶと置き換わるプラグインを作る。

**Architecture:** 純粋な Java の命名エンジン(`engine`)と、IntelliJ 連携の薄い層(`plugin`)に分ける。依存は `plugin → engine` の一方向。IDE 連携は、試作で成り立つと確かめた標準の補完機構(`CompletionContributor`)に乗せる。

**Tech Stack:** Java 21、Gradle 9.8.2、IntelliJ Platform Gradle Plugin 2.20.0、JUnit 5(engine)、IntelliJ テストフレームワーク(plugin)、Kuromoji(第一候補。Task 2 で確定)。

**Spec:** `codic/docs/superpowers/specs/2026-11-08-codic-naming-plugin-design.md`(以下「設計書」)

## Global Constraints

- 永続無料。有料 API を使わない。**外部通信を一切しない**(辞書は同梱)。
- 対象 IDE は IntelliJ IDEA Community Edition `2025.2.6.2` のみ。`sinceBuild` = `252`、`untilBuild` = `252.*`。
- 対象言語は Java のみ。JDK 21。
- 英字入力時の補完は、このプラグインの有無で変えない。日本語を打ったときだけ動く。
- 辞書に無い語は、その語だけ日本語のまま残した候補を最後尾に出す。句の全語が未登録なら候補を出さない。
- 候補は辞書の優先順で最大 10 件。命名規則の既定値は、変数 = camel、定数 = SNAKE、メソッド = camel、クラス = Pascal。判定できなければ変数(camel)。
- 利用者辞書は優先度が最も高い。IDE の設定フォルダに 1 ファイル、1 語に複数の訳を持てる。
- JMdict(CC BY-SA 4.0)を同梱する。出典表示を設定画面に出す。加工した辞書データは `engine/src/main/resources/dictionary/` に、コードと別のライセンス文書を添えて置く。
- `engine` は IntelliJ のクラスを import しない。呼び出し側に散らさず、値の不正は値を作る箇所で防ぐ。
- 一実装しかない箇所に interface を作らない。境界を作るのは、差し替えが実在する辞書の読み出し元だけ。
- コミットはフックが自動で行う(ターン終了時)。手順に commit を書かない。
- 試作 `codic/spike/` は使い捨て。参照だけして、コードを移植しない(テストのケースと期待値は引き継ぐ)。

## Review Focus

設計書が暗黙に求めるが、どのタスクの通常テストにも出ない入力。最も起きやすい順。各行のテストは、担当タスクに追加する。

1. 予約語になる英語名(例: 「クラス」→ `class`)。候補から除く、または使える形にする。 → Task 1
2. 英数字が混じる日本語句(例: 「IDを取得する」)。英数字はそのまま語として残す。 → Task 4
3. 長い句での遅延。補完の候補生成が UI を固めない。辞書の読み込みは初回のみで、候補生成は一定時間内に終わる。 → Task 7
4. 利用者辞書ファイルが空・壊れている。プラグインが起動し、同梱辞書だけで動く。 → Task 3
5. 訳語が複数語・空白を含む(例: `pay attention`)。英語名の語の列として正しく展開する。 → Task 3

---

## ファイル構成

```
codic/
  settings.gradle.kts         ルート。engine と plugin を include(spike は含めない)
  build.gradle.kts            共通設定(Java 21)
  engine/                     純粋な Java
    src/main/java/namingassist/engine/
      NamingKind.java  NamingCase.java  NamePart.java  EnglishName.java
      Candidate.java  NamingEngine.java
      tokenize/  Token.java  Tokenizer.java  KuromojiTokenizer.java
      dictionary/  DictionaryEntry.java  Dictionary.java  LayeredDictionary.java
                   CuratedDictionary.java  JmdictDictionary.java  UserDictionary.java
      compose/  PhraseComposer.java  BooleanRule.java  Pluralizer.java
    src/main/resources/dictionary/   curated.tsv  jmdict.tsv  LICENSE-JMDICT.md
    src/test/java/...
  plugin/                     IntelliJ 連携
    src/main/java/namingassist/plugin/
      JapaneseNamingContributor.java  ContextKindResolver.java
      settings/  UserDictionaryConfigurable.java  UserDictionaryStore.java
      edit/  NameEditorPopup.java  EditActionProvider.java
    src/main/resources/META-INF/plugin.xml
    src/test/java/...
  tools/convert-jmdict/       JMdict を jmdict.tsv に変換する一回きりのスクリプト
  spike/                      使い捨て(触らない)
```

---

### Task 1: 命名の値(種別・規則・英語名)

**Files:**
- Create: `engine/src/main/java/namingassist/engine/{NamingKind,NamingCase,NamePart,EnglishName,Candidate}.java`
- Test: `engine/src/test/java/namingassist/engine/EnglishNameTest.java`

**Interfaces:**
- Produces:
  - `enum NamingKind { VARIABLE, CONSTANT, METHOD, CLASS; NamingCase defaultCase(); }`
  - `enum NamingCase { CAMEL, PASCAL, SNAKE, UPPER_SNAKE, KEBAB }`
  - `record NamePart(String text, List<String> alternatives, boolean resolved)`(`alternatives` は `text` を含む別訳。未解決は `resolved=false`)
  - `record EnglishName(List<NamePart> parts)`: `parts` が空なら `IllegalArgumentException`。`String render(NamingCase c)`、`boolean isValidJavaIdentifier(NamingCase c)`。
  - `record Candidate(EnglishName name, String rendered)`

- [ ] **Step 1: 失敗するテストを書く**
  `EnglishNameTest` に次を書く。
  - `rendersAllCases`: parts = get / employee / count のとき CAMEL=`getEmployeeCount`、PASCAL=`GetEmployeeCount`、SNAKE=`get_employee_count`、UPPER_SNAKE=`GET_EMPLOYEE_COUNT`、KEBAB=`get-employee-count`。
  - `rejectsEmptyParts`: `new EnglishName(List.of())` が `IllegalArgumentException`。
  - `defaultCasePerKind`: VARIABLE→CAMEL、CONSTANT→UPPER_SNAKE、METHOD→CAMEL、CLASS→PASCAL。
  - `reservedWordIsNotValidIdentifier`: parts = `class` 一語のとき CAMEL で `isValidJavaIdentifier` が false。
  - `leadingDigitIsNotValidIdentifier`: parts = `3d` / `view` のとき CAMEL で false。
  - `japaneseTextIsValidIdentifier`: 未解決の部品 `在庫引当` を含む名前(`get` + `在庫引当`)で CAMEL=`get在庫引当`、有効。
- [ ] **Step 2: 実行して失敗を確認する** — `./gradlew :engine:test --tests '*EnglishNameTest'`。期待: コンパイル失敗(型が未定義)。
- [ ] **Step 3: 実装する** — 上の Interfaces どおり。有効性の判定は `Character.isJavaIdentifierStart/Part` と予約語の集合で行う。
- [ ] **Step 4: 実行して通ることを確認する** — 同じコマンド。期待: 6 件 PASS。

---

### Task 2: 日本語の解析(形態素解析の選定とアダプタ)

**Files:**
- Create: `engine/src/main/java/namingassist/engine/tokenize/{Token,Tokenizer,KuromojiTokenizer}.java`
- Create: `docs/superpowers/specs/tokenizer-decision.md`(選定結果の記録)
- Test: `engine/src/test/java/namingassist/engine/tokenize/KuromojiTokenizerTest.java`

**Interfaces:**
- Produces:
  - `record Token(String surface, String baseForm, Pos pos)` と `enum Pos { NOUN, VERB, ADJECTIVE, PARTICLE, AUXILIARY, SYMBOL, OTHER }`
  - `interface Tokenizer { List<Token> tokenize(String japanese); }`(ここは実装が 2 つ以上になり得ないが、`engine` を解析製品から独立させるため残す。テストでは固定の `FakeTokenizer` を使えるようにする)

- [ ] **Step 1: 選定の小さな試作** — Kuromoji(`com.atilika.kuromoji:kuromoji-ipadic`)で、次の 3 句が語に分かれることを確かめる。「従業員数を取得する」「広告を表示するか」「ウェブサイトを持っているか」。結果は `tokenizer-decision.md` に書く。
- [ ] **Step 2: ライセンスを原本で確認する** — Kuromoji の `NOTICE.md` と IPADIC の条件を読み、配布(プラグイン同梱)が可能かを `tokenizer-decision.md` に書く。**不可、または分けられない場合は、他のライブラリ(Sudachi など)を同じ手順で評価し、利用者に報告して止まる。**
- [ ] **Step 3: 失敗するテストを書く** — `KuromojiTokenizerTest`:
  - `splitsEmployeeCountPhrase`: 「従業員数を取得する」のトークンの surface に「従業員」「数」「を」「取得」が含まれる。
  - `marksVerbBaseForm`: 「持っている」のトークンに baseForm=`持つ`、pos=VERB がある。
  - `keepsLatinAsOther`: 「IDを取得する」で `ID` のトークンの surface が `ID`。
- [ ] **Step 4: 実装して通す** — `KuromojiTokenizer implements Tokenizer`。品詞は IPADIC の品詞名を `Pos` に対応させる。`./gradlew :engine:test --tests '*KuromojiTokenizerTest'` で 3 件 PASS。
- [ ] **Step 5: 初回のロード時間を測る** — 初回の `tokenize` 呼び出しの所要時間を `tokenizer-decision.md` に記録する(Task 7 の遅延対策の入力)。

---

### Task 3: 辞書(同梱 2 層 + 利用者辞書 + 層の合成)

**Files:**
- Create: `engine/src/main/java/namingassist/engine/dictionary/*.java`(上の構成のとおり)
- Create: `engine/src/main/resources/dictionary/curated.tsv`、`jmdict.tsv`、`LICENSE-JMDICT.md`
- Create: `tools/convert-jmdict/`(JMdict の XML を `jmdict.tsv` に変換)
- Test: `engine/src/test/java/namingassist/engine/dictionary/DictionaryTest.java`

**Interfaces:**
- Produces:
  - `record DictionaryEntry(String japanese, List<String> translations)`(`translations` は優先順。1 要素に空白を含んでよい)
  - `interface Dictionary { Optional<DictionaryEntry> lookup(String japaneseWord); }`(読み出し元が複数あるので境界とする)
  - `LayeredDictionary(List<Dictionary> layersInPriorityOrder)`: 先に見つけた層を採用。ただし訳語は、上位の層の訳を先に、下位の層の訳を後ろに連結し、重複を除く。
  - `UserDictionary`: ファイルから読む。ファイルが無い・空・壊れているときは**空の辞書として動き、例外を投げない**(警告を返せる形にする)。書き込み用に `save(List<DictionaryEntry>)`。

- [ ] **Step 1: 失敗するテストを書く** — `DictionaryTest`:
  - `userDictionaryWinsOverCurated`: 同じ語を両方に置くと、利用者の訳が先頭。
  - `layersAreConcatenatedWithoutDuplicates`: 上位 `[fetch]`、下位 `[get, fetch]` → `[fetch, get]`。
  - `missingUserFileGivesEmptyDictionary`、`emptyUserFileGivesEmptyDictionary`、`corruptUserFileGivesEmptyDictionary`(壊れたバイト列)。いずれも例外なし、同梱辞書では引ける。
  - `translationWithSpaceExpandsToWords`: 訳 `pay attention` が 2 語の `NamePart` 列に展開される(展開は Task 4 が呼ぶ `EnglishWords.split(String)` として本タスクで作る)。
- [ ] **Step 2: 失敗を確認** — `./gradlew :engine:test --tests '*DictionaryTest'`。
- [ ] **Step 3: 実装する** — TSV は「日本語<TAB>訳1,訳2,…」。`CuratedDictionary` は、まず次の語を含める: 取得→`get,fetch`、設定→`set`、数→`count,number`、従業員→`employee`、ページ→`page`、タイトル→`title`、表示→`show,display`、広告→`ad`、作成→`create`、有料→`paid`、プラン→`plan`、有効→`enabled`、持つ→`has`、正規表現→`regexp`、検索→`search`、ウェブサイト→`website`、稼働→`operation`、時刻→`time`。
- [ ] **Step 4: JMdict を変換する** — `tools/convert-jmdict` で、原本の最新版から `jmdict.tsv` を作る。変換の日付と版を `LICENSE-JMDICT.md` に書く。出力は 1 語あたり訳 5 件まで(仮置き)。
- [ ] **Step 5: 通ることを確認** — 同じコマンド。全 PASS。

---

### Task 4: 句の組み立て(語順・助詞・未登録語・複数形)

**Files:**
- Create: `engine/src/main/java/namingassist/engine/compose/{PhraseComposer,Pluralizer}.java`、`NamingEngine.java`
- Test: `engine/src/test/java/namingassist/engine/compose/PhraseComposerTest.java`

**Interfaces:**
- Consumes: `Tokenizer`、`Dictionary`、Task 1 の値。
- Produces:
  - `NamingEngine(Tokenizer t, Dictionary d)` と `List<Candidate> suggest(String japanesePhrase, NamingKind kind)`。候補は最大 10 件、`kind.defaultCase()` で綴る。
  - `Pluralizer.pluralize(String englishWord)`: 規則複数形 + 不規則(`person`→`people` など)。
  - `PhraseComposer` は、ここでは名詞句と「〜する」動詞句を扱う。真偽値は Task 5。

- [ ] **Step 1: 失敗するテストを書く** — 固定の小さな辞書と `FakeTokenizer` を使い、次を assert:
  - `verbObjectIsReordered`: 「従業員数を取得する」(VARIABLE でなく METHOD)→ 先頭候補の SNAKE 表記が `get_employee_count`、CAMEL が `getEmployeeCount`。
  - `alternativeVerbGivesSecondCandidate`: 「ページタイトルを取得する」→ 候補に `get_page_title` と `fetch_page_title` の両方があり、`get` が先。
  - `nounPhraseKeepsOrder`: 「稼働時刻」(VARIABLE)→ 先頭候補が `operationTime`。
  - `unknownWordIsKeptInJapaneseAtTheEnd`: 「在庫引当を取得する」(辞書に引当が無い)→ 最後尾の候補に `get在庫引当` を含み、未解決の部品の `resolved` が false。
  - `allUnknownGivesNoCandidate`: 全語が未登録なら空リスト。
  - `latinIsKeptAsWord`: 「IDを取得する」→ `getId`(英数字は語としてそのまま、小文字化して規則に従う)。
  - `pluralSuffix`: 「広告s」(半角)と「広告ｓ」(全角)がどちらも `ads`。
  - `irregularPlural`: 「人s」に対応する語 `person` が `people` になる(辞書に 人=`person` を置く)。
  - `limitsToTenCandidates`: 訳が 4 つずつの語が 3 つあるとき、候補が 10 件に切られる。
- [ ] **Step 2: 失敗を確認** — `./gradlew :engine:test --tests '*PhraseComposerTest'`。
- [ ] **Step 3: 実装する** — 助詞「を・は・が・の」で句を区切り、動詞があれば「動詞 + 目的語の名詞句」の順にする。候補の列挙は、各語の別訳の組み合わせを、辞書の優先順の辞書式順序で作る。未解決の語は日本語のまま残し、その候補は最後に並べる。
- [ ] **Step 4: 通ることを確認** — 同じコマンド。全 PASS。

---

### Task 5: 真偽値の命名(「〜か」)

**Files:**
- Create: `engine/src/main/java/namingassist/engine/compose/BooleanRule.java`
- Modify: `engine/src/main/java/namingassist/engine/compose/PhraseComposer.java`
- Test: `engine/src/test/java/namingassist/engine/compose/BooleanRuleTest.java`

**Interfaces:**
- Consumes: Task 4 の `NamingEngine`。
- Produces: 句が「か」で終わるとき、真偽値の接頭辞(`should` / `is` / `has`)を先頭に付けた候補を返す。

- [ ] **Step 1: 失敗するテストを書く** — 記事(nelog.jp/codic)の 5 例を、METHOD の SNAKE 表記で assert:
  - 「広告を表示するか」→ `should_show_ad`
  - 「ファイルは作成されているか」→ `is_file_created`
  - 「有料プランか」→ `is_paid_plan`
  - 「正規表現検索は有効か」→ `is_regexp_search_enabled`
  - 「ウェブサイトを持っているか」→ `has_website`
  - 追加: 「広告sを表示するか」→ `should_show_ads`(複数形との併用)
- [ ] **Step 2: 失敗を確認** — `./gradlew :engine:test --tests '*BooleanRuleTest'`。
- [ ] **Step 3: 実装する** — 規則: 「〜するか」→ `should` + 動詞 + 目的語。「〜されているか」→ `is` + 主語 + 過去分詞(辞書に過去分詞形を持たせる。無い動詞は `ed` を規則で付ける)。「Nか」「Nは形容詞か」→ `is` + N(+ 形容詞)。「〜を持っているか」→ `has` + 目的語。
- [ ] **Step 4: 通ることを確認** — 全 PASS。

---

### Task 6: 命名の質を固定する(回帰テスト)

**Files:**
- Create: `engine/src/test/resources/golden-examples.tsv`
- Test: `engine/src/test/java/namingassist/engine/GoldenExamplesTest.java`

- [ ] **Step 1: 例を固定する** — `golden-examples.tsv` に「日本語句 / 命名種別 / 期待する先頭候補(SNAKE か CAMEL)」を置く。内容は、記事の例(Task 4・5 の 7 件)、利用者の実例「稼働時刻→`operationTime`」。**利用者に、日ごろ命名する日本語句とその採用名を 10 件以上出してもらい、追加する。**(出してもらうまで、タスクは未完了)
- [ ] **Step 2: テストを書く** — `GoldenExamplesTest`: 全行を `NamingEngine`(実際の Kuromoji と同梱辞書)で評価し、先頭候補が期待と一致することを assert。
- [ ] **Step 3: 実行** — `./gradlew :engine:test --tests '*GoldenExamplesTest'`。外れた行は、辞書か規則を直す。期待値を実装に合わせて書き換えない。

---

### Task 7: IDE 連携(補完 contributor・日本語のみ・英字は不変)

**Files:**
- Create: `plugin/build.gradle.kts`、`plugin/src/main/resources/META-INF/plugin.xml`、`plugin/src/main/java/namingassist/plugin/JapaneseNamingContributor.java`
- Test: `plugin/src/test/java/namingassist/plugin/{JapaneseCompletionTest,AutoPopupTest,EnglishUnchangedTest}.java`

**Interfaces:**
- Consumes: `NamingEngine#suggest(String, NamingKind)`、Task 8 の `ContextKindResolver`(この時点では固定で VARIABLE を返すスタブ)。
- Produces: 日本語プレフィックスのときだけ、`LookupElementBuilder.create(rendered).withLookupString(日本語句)` を返す contributor。`NamingEngine` はアプリケーションサービスとして遅延生成し、辞書の読み込みは初回のみ。

- [ ] **Step 1: 失敗するテストを書く** — 試作のテストのケースと期待値を引き継ぐ。
  - `JapaneseCompletionTest`: 変数名の位置で「稼働時刻」を打つと、候補に `operationTime`。
  - `AutoPopupTest`(`JavaCompletionAutoPopupTestCase`): 日本語入力の直後に、補完を呼ばなくても Lookup が出る。
  - `EnglishUnchangedTest`: `str` / `oper` / `Str` / `a` で、本プラグインの contributor が有効でも無効でも補完結果が一致。日本語を含まない入力で、本プラグイン由来の候補が 0 件。
  - `longPhraseDoesNotBlock`: 30 文字の句で候補生成が 1 秒以内(閾値は仮置き。Task 2 の初回ロード時間と比べて見直す)。
  - `contextObservation`: コメント内・文字列内・`@Scheduled(cron = <ここ>)`・フィールド名の 4 か所の挙動を表にして出力する(設計書 §1 の観測の再現)。
- [ ] **Step 2: 失敗を確認** — `./gradlew :plugin:test`。
- [ ] **Step 3: 実装する** — 試作の `JapaneseNamingContributor` の構造を参考に新規作成する(コードは移植しない)。`plugin.xml` の `since-build`=`252`、`until-build`=`252.*`。
- [ ] **Step 4: 通ることを確認** — `./gradlew :plugin:test`、`./gradlew buildPlugin`。全 PASS、zip ができる。

---

### Task 8: 命名種別の自動判定(PSI)

**Files:**
- Create: `plugin/src/main/java/namingassist/plugin/ContextKindResolver.java`
- Test: `plugin/src/test/java/namingassist/plugin/ContextKindResolverTest.java`

**Interfaces:**
- Produces: `NamingKind resolve(PsiElement elementAtCaret)`。ローカル変数・フィールド → VARIABLE、`static final` のフィールド → CONSTANT、メソッド宣言名 → METHOD、クラス宣言名 → CLASS。判定できなければ VARIABLE。

- [ ] **Step 1: 失敗するテストを書く** — 次の位置で「稼働時刻」を打ったとき `operationTime` / `OPERATION_TIME` / `operationTime`(メソッド)/ `OperationTime`(クラス)の先頭候補になる。`static final` フィールド、メソッド宣言名、クラス宣言名、ローカル変数の 4 か所に加え、判定できない位置(式の途中)で VARIABLE になること。
- [ ] **Step 2〜4: 実装して通す** — `./gradlew :plugin:test --tests '*ContextKindResolverTest'`。判定が実機で成り立たない文脈があれば、その文脈は VARIABLE に落とし、結果を利用者に報告する。Task 7 のスタブをこの実装に差し替える。

---

### Task 9: 利用者辞書の設定画面と出典表示

**Files:**
- Create: `plugin/src/main/java/namingassist/plugin/settings/{UserDictionaryConfigurable,UserDictionaryStore}.java`
- Modify: `plugin/src/main/resources/META-INF/plugin.xml`(設定画面の登録)
- Test: `plugin/src/test/java/namingassist/plugin/settings/UserDictionaryStoreTest.java`

**Interfaces:**
- Consumes: `UserDictionary`(Task 3)。
- Produces: 設定画面(Settings > Tools に「日本語→英語 命名」)。一覧表で語の追加・編集・削除。1 語に複数の訳(カンマ区切り)。保存先は `PathManager.getConfigPath()` 配下の 1 ファイル。下部に JMdict の出典表示とライセンス文書へのリンク。

- [ ] **Step 1: 失敗するテストを書く** — `UserDictionaryStoreTest`: 追加→保存→再読込で同じ内容。利用者辞書に追加した語が、次の補完から候補の先頭に出る(再起動なしで反映)。ファイルが壊れていても IDE が起動する。
- [ ] **Step 2〜3: 実装して通す** — `./gradlew :plugin:test --tests '*UserDictionaryStoreTest'`。画面そのものは `runIde` で目視確認し、結果を記録する。

---

### Task 10: 語順の入れ替え・結合の編集(項目 6)

**Files:**
- Create: `plugin/src/main/java/namingassist/plugin/edit/{NameEditorPopup,EditActionProvider}.java`
- Test: `plugin/src/test/java/namingassist/plugin/edit/NameEditorModelTest.java`

**Interfaces:**
- Produces: `NameEditorModel`(`move(int from, int to)`、`join(int from, int to)`、`replaceWord(int index, String alternative)` と、現在の英語名の取得)。IDE 連携は `LookupActionProvider` で、選択中の候補に「単語を並べ替え・結合」の操作を足す。

- [ ] **Step 1: 実現手段の確認** — `LookupActionProvider` で候補の操作を足せることを、2025.2.6.2 の API で**実機に近いテストで確かめる**。足せない場合は、候補の挿入後に使える別の操作(意図アクション等)に切り替え、利用者に報告する。
- [ ] **Step 2: 失敗するテストを書く(モデル)** — `NameEditorModelTest`: `get / employee / count` で `move(2, 1)` すると `get_count_employee`。`join(1, 2)` すると `employee count` が 1 つの部品になる。`replaceWord(0, "fetch")` で `fetch_employee_count`。範囲外の添え字は例外。
- [ ] **Step 3: 実装して通す** — `./gradlew :plugin:test --tests '*NameEditorModelTest'`。画面は `runIde` で目視確認する。

---

### Task 11: 全体の検証と引き渡し

**Files:**
- Create: `tasks/contracts/2026-11-XX-codic-naming-plugin.json`(**実装の前に**書く。下の check を含める)
- Modify: `codic/README.md`(導入と使い方)

- [ ] **Step 1: 契約を先に書く** — 少なくとも次を含める(`how` は実行できる手段で書く)。
  - 2025.2.6.2 を対象にビルドとテストが通る(`./gradlew build`)。
  - 外部通信が無い: ソースと生成物に `java.net`、`HttpClient`、`URL(` の使用が 0 件(`grep -rn`)。
  - 英字入力の補完が不変(Task 7 のテスト)。
  - 記事の例と利用者の実例が全件一致(Task 6)。
  - 実 IDE(IME 経由)で、利用者本人が、日本語確定後に自動表示・置き換え・語順の編集・利用者辞書の追加を確認する。
  - JMdict の出典が設定画面に出る。ライセンス文書が配布物に含まれる(zip の中身を一覧)。
- [ ] **Step 2: 採点** — 別の評価役(`contract-evaluator`)に採点させる。実装者が `passes` を書き換えない。
- [ ] **Step 3: 利用者に引き渡す** — zip の場所と導入手順、確認してほしい点を伝える。
- [ ] **Step 4: 試作の整理** — 利用者に確認の上、`codic/spike/` を削除するか決める。

---

## 自己点検

- **設計書の網羅**: 項目 1〜7 → Task 3〜5、9、10。Q1 → Task 4。Q2 → Task 3、9。Q3 → Task 4(複数形)、10(編集)。命名種別の自動判定 → Task 8。成功条件・拒否条件 → Task 7。品質の表(§6)→ Task 6、7、11。§7-1b の未確認事項 → Task 3 Step 4(変換日付と版の記録)、Task 9(出典表示)、更新の手順は **Task 11 の引き渡しで利用者に提示する**(原本で確認できなければ未確認として報告する)。
- **型の整合**: `NamingKind`、`NamingCase`、`NamePart`、`EnglishName`、`Candidate`、`Dictionary`、`DictionaryEntry`、`Tokenizer`、`Token`、`NamingEngine#suggest` の名前は全タスクで同一。
- **未検証のまま計画に入れたもの**: PSI による種別判定(Task 8)、`LookupActionProvider` による編集操作(Task 10 Step 1)、Kuromoji 同梱辞書のライセンス(Task 2 Step 2)。いずれも最初に確かめる手順があり、外れたら利用者に報告して止まる。
