#!/usr/bin/env python3
"""JMdict_e(.gz または .xml)を jmdict.tsv(日本語<TAB>訳1,訳2,...)に変換する。開発時だけ使う。

使い方: python -I convert_jmdict.py <JMdict_e.gz> <出力 jmdict.tsv>
絞り込み: ke_pri/re_pri のいずれかが ichi1・news1・spec1・gai1 を持つ項目だけ。
訳は 1 語あたり 5 件まで。"to " を除き、小文字の ASCII の短い語句だけ残す。
"""
import gzip
import re
import sys
import xml.etree.ElementTree as ET

COMMON = {"ichi1", "news1", "spec1", "gai1"}
MAX_TRANSLATIONS = 5
MAX_WORDS = 3
MAX_LEN = 30
OK_GLOSS = re.compile(r"^[a-z][a-z' -]*$")


def clean(gloss):
    g = gloss.strip()
    if g.startswith("to "):
        g = g[3:]
    g = re.sub(r"\s+", " ", g)
    if len(g) > MAX_LEN or len(g.split(" ")) > MAX_WORDS or not OK_GLOSS.match(g):
        return None
    return g


def main(src, dst):
    opener = gzip.open if src.endswith(".gz") else open
    table = {}
    with opener(src, "rb") as f:
        for _, e in ET.iterparse(f):
            if e.tag != "entry":
                continue
            pris = {p.text for p in e.iter() if p.tag in ("ke_pri", "re_pri")}
            if pris & COMMON:
                keys = [k.findtext("keb") for k in e.findall("k_ele")]
                uk = any(m.text == "word usually written using kana alone"
                         for s in e.findall("sense") for m in s.findall("misc"))
                if not keys or uk:
                    keys += [r.findtext("reb") for r in e.findall("r_ele")]
                glosses = []
                for s in e.findall("sense"):
                    for g in s.findall("gloss"):
                        c = clean(g.text or "")
                        if c and c not in glosses:
                            glosses.append(c)
                if glosses:
                    for k in dict.fromkeys(keys):
                        cur = table.setdefault(k, [])
                        for g in glosses:
                            if g not in cur:
                                cur.append(g)
            e.clear()
    with open(dst, "w", encoding="utf-8", newline="\n") as out:
        for k in sorted(table):
            out.write(k + "\t" + ",".join(table[k][:MAX_TRANSLATIONS]) + "\n")
    print(f"{len(table)} entries -> {dst}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
