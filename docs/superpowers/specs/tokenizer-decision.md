# 形態素解析の選定記録

決定: Kuromoji(`com.atilika.kuromoji:kuromoji-ipadic:0.9.0`、Maven Central)を採用する。

## 試作の実測(Step 1)

| 入力 | 分割結果(表層形/品詞) |
|---|---|
| 従業員数を取得する | 従業(名詞) / 員数(名詞) / を / 取得 / する(動詞) |
| 広告を表示するか | 広告 / を / 表示 / する / か |
| ウェブサイトを持っているか | ウェブサイト / を / 持っ(基本形 持つ・動詞) / て / いる / か |
| IDを取得する | ID(名詞・基本形 `*`) / を / 取得 / する |

事実と是正:
- 既定辞書では「従業員数」が「従業」+「員数」に割れ、期待の「従業員」+「数」にならない。
  ユーザー辞書(`user-dictionary.csv`)に `従業員数,従業員 数,...` を足して解消した。
  今後も辞書で割れ方が悪い業務語はこのファイルに足す運用とする。
- 外来語・英字(ウェブサイト、ID)は基本形が `*` になる。アダプタで表層形に置き換える。
- 英数字(ASCII のみ)のトークンは `Pos.OTHER` にし、`ID` が語としてそのまま残るようにした。

## ライセンス確認(Step 2)

原本を WebFetch で読んだ。

- Kuromoji 本体: https://raw.githubusercontent.com/atilika/kuromoji/master/LICENSE.md
  Apache License 2.0。頒布は可。条件は、ライセンス文の添付、変更表示、著作権・帰属表示の保持。
- 通知: https://raw.githubusercontent.com/atilika/kuromoji/master/NOTICE.md
  著作権表示(Atilika Inc. and contributors)と、同梱の第三者辞書データについて
  `kuromoji-ipadic/NOTICE.md` を参照せよ、とある。
- IPADIC: https://raw.githubusercontent.com/atilika/kuromoji/master/kuromoji-ipadic/NOTICE.md
  (mecab-ipadic-2.7.0-20070801 Notice)。原形でも改変後でも第三者への頒布は自由。
  条件: 「NO WARRANTY」(第3項)を常に付すこと、ほぼ同じ形式で頒布すること、各国の法令に反しないこと。
  頒布者は利用者への損害責任を免れる(別途の書面保証を除く)。

判断: プラグインへの同梱・再配布は可能(条件付き)。
- 条件を満たす作業(Task 7 の配布物で行う): Apache-2.0 の LICENSE と Kuromoji の NOTICE、
  IPADIC の NOTICE 全文(NO WARRANTY 条項を含む)を配布物に同梱する。
- 未確認: WebFetch は要約で返るため、IPADIC 通知の全文の逐語確認はしていない。
  配布前に NOTICE.md の全文をそのまま同梱物へコピーすること。
  また「バイナリ同梱」への言及は通知の抜粋に無かった(一般の「Program の頒布」条件を適用と解釈=推測)。

## 初回ロード時間の実測(Step 5)

JDK 21、Windows 11、Gradle テスト JVM、3回計測:
- `new KuromojiTokenizer()`(辞書ロード): 約 148〜149 ms
- 最初の `tokenize`: 約 3.8〜4.5 ms
- 2回目の `tokenize`: 約 0.2 ms

Task 7 への入力: 遅延は主にコンストラクタ(約 150 ms)。補完の呼び出しスレッドで初回に作ると
体感できる遅れになり得るので、バックグラウンドで先に生成しておく(遅延対策)。
