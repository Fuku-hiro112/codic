# jmdict.tsv の出典とライセンス

`jmdict.tsv` は、EDRDG(Electronic Dictionary Research and Development Group)の
**JMdict**(JMdict_e、英語のみ)を命名用に加工した派生物です。

- 原本: JMdict_e.gz(`https://www.edrdg.org/pub/Nihongo/JMdict_e.gz`、案内ページ https://www.edrdg.org/jmdict/j_jmdict.html)
- 原本の版: JMdict created 2026-11-08(取得日 2026-11-08)
- 著作権: JMdict は Electronic Dictionary Research and Development Group の著作物です。
  https://www.edrdg.org/
- ライセンス: **Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)**
  https://creativecommons.org/licenses/by-sa/4.0/
  EDRDG の原本ライセンスページ: https://www.edrdg.org/edrdg/licence.html

## 加工の内容(加工物であることの注記)

この `jmdict.tsv` は原本そのものではなく、本プロジェクトが加工したものです。
加工は `tools/convert-jmdict/convert_jmdict.py` で行いました。

- ke_pri / re_pri のいずれかが ichi1・news1・spec1・gai1 を持つ項目だけを残した(一般的な語に限定)。
- 見出しは漢字表記。かな書きが普通の語(uk)と漢字表記を持たない語は、読みを見出しにした。
- 訳は英語の glosses から、先頭の "to " を除き、小文字の ASCII のみで 3 語・30 文字以内のものだけを残し、
  1 見出しあたり 5 件までにした。括弧書き・長い説明文・大文字を含む訳は捨てた。
- 形式を「日本語<TAB>訳1,訳2,…」(UTF-8)に変えた。品詞・読み・用例などの情報は含まない。

加工者: 本プロジェクト(codic 命名プラグイン)。原本の正確性について EDRDG および加工者は保証しません。

## 同じライセンスでの扱い

この加工物 `jmdict.tsv` は CC BY-SA 4.0 で配布されます。プラグインのコードとは別のライセンスです。
更新するときは、変換器で原本の最新版から作り直し、この文書の版と日付を更新してください。
