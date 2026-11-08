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

判断(Task 2 時点): プラグインへの同梱・再配布は可能(条件付き)と見込んだ。
上の要約は WebFetch の要約で、逐語確認ではなかった。Task 11a で次のとおり原文を確かめた。

### 逐語確認(Task 11a、2026-11-08)

原文を curl で取得し、バイト列で比較した(要約ツールは使っていない)。

確認済み:
- Kuromoji の LICENSE.md(GitHub master)は、Maven Central の `kuromoji-core-0.9.0.jar` と
  `kuromoji-ipadic-0.9.0.jar` の `META-INF/LICENSE.md` とバイト単位で同一(SHA-256 76b21bab…)。
  中身は Apache License 2.0 の全文(行頭 4 字下げ。https://www.apache.org/licenses/LICENSE-2.0.txt と空白を除き一致)。
- jar は Maven Central の `.sha1` と一致し、Gradle キャッシュの jar とも同一。
- `kuromoji-ipadic-0.9.0.jar` には `META-INF/NOTICE.md` がある。GitHub の `kuromoji-ipadic/NOTICE.md` とは
  著作権表示の年(jar: 2010-2015、GitHub: 2010-2018)だけが異なる。`kuromoji-core-0.9.0.jar` に NOTICE は無い。
- Kuromoji の IPADIC NOTICE は mecab-ipadic の `COPYING` の**抜粋**だった。冒頭の著作権表示
  「Copyright 2000, 2001, 2002, 2003 Nara Institute of Science and Technology. All Rights Reserved.」と、
  許諾の段落「Use, reproduction, and distribution of this software is permitted. Any copy of this software,
  whether in its original form or modified, must include both the above copyright notice and the following paragraphs.」
  が NOTICE には無い。原本は NOTICE が示す入手先の `mecab-ipadic-2.7.0-20070801.tar.gz` の `COPYING` で確認し、
  https://raw.githubusercontent.com/taku910/mecab/master/mecab-ipadic/COPYING とバイト単位で同一だった。
- 条件(原文から): (1) NAIST: どの複製にも、著作権表示とそれに続く段落を含めること。
  (2) ICOT: 第三者への頒布は自由。ただし「NO WARRANTY」の条項を常にプログラムに表示または添付すること、
  ほぼ同じ形式で頒布すること、関係国の法令に反しないこと。「バイナリ」という語は原文に無い。
- 対応: 上の全文(Apache 2.0、NOTICE、CONTRIBUTORS、mecab-ipadic の COPYING 全文)を
  `plugin/src/main/resources/THIRD-PARTY-NOTICES.md` に原文のまま入れ、配布 zip の plugin の jar に含めた。
  Kuromoji の jar は無改変で `lib/` に入り、jar 自身の `META-INF/LICENSE.md`・`NOTICE.md` もそのまま配布される。

判断: 同梱・再配布は、上の通知を添えることで条件を満たすと解釈する(法的助言ではない)。

未確認(または解釈に依る):
- jar の辞書データが本当に mecab-ipadic-2.7.0-20070801 から作られたかは、Kuromoji の NOTICE の記述に依る(自分で作り直して照合していない)。
- ICOT Free Software の原本の許諾条件(COPYING が「Section 3」と呼ぶ元の文書)は入手していない。COPYING に引用された部分だけを確認した。
- 「プログラムに表示または添付」を、zip 内の jar に入った通知文書で満たすか。jar のバイナリ辞書を「ほぼ同じ形式」と言えるか。
  いずれも解釈の問題で、利用者に要確認。

## 初回ロード時間の実測(Step 5)

JDK 21、Windows 11、Gradle テスト JVM、3回計測:
- `new KuromojiTokenizer()`(辞書ロード): 約 148〜149 ms
- 最初の `tokenize`: 約 3.8〜4.5 ms
- 2回目の `tokenize`: 約 0.2 ms

Task 7 への入力: 遅延は主にコンストラクタ(約 150 ms)。補完の呼び出しスレッドで初回に作ると
体感できる遅れになり得るので、バックグラウンドで先に生成しておく(遅延対策)。
