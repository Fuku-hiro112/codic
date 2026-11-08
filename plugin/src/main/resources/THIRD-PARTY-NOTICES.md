# THIRD-PARTY-NOTICES(第三者の著作物の通知)

このプラグイン(日本語→英語 命名)の配布物には、次の第三者の著作物が含まれます。
それぞれのライセンス・著作権表示・無保証条項を、下に**原文のまま**載せます。
(この文書の日本語の説明は案内であり、法的助言ではありません。原文が優先します。)

| 同梱物 | 版 | 配布物の中の場所 | ライセンス |
|---|---|---|---|
| Kuromoji(`com.atilika.kuromoji:kuromoji-core`) | 0.9.0 | `lib/kuromoji-core-0.9.0.jar`(無改変) | Apache License 2.0 |
| Kuromoji IPADIC(`com.atilika.kuromoji:kuromoji-ipadic`) | 0.9.0 | `lib/kuromoji-ipadic-0.9.0.jar`(無改変) | Apache License 2.0(Kuromoji のコード)と mecab-ipadic の条件(辞書データ) |
| mecab-ipadic(上の jar に含まれる辞書データの元) | 2.7.0-20070801 | `lib/kuromoji-ipadic-0.9.0.jar` の中 | 下の §3 の条件(NAIST / ICOT) |
| JMdict(命名用に加工した派生物 `jmdict.tsv`) | JMdict created 2026-11-08 | engine の jar の `dictionary/jmdict.tsv` | CC BY-SA 4.0(§4) |

原文の取得元(2026-11-08 取得):

- Kuromoji の LICENSE.md: https://raw.githubusercontent.com/atilika/kuromoji/master/LICENSE.md
  (Maven Central の `kuromoji-core-0.9.0.jar` と `kuromoji-ipadic-0.9.0.jar` の `META-INF/LICENSE.md` とバイト単位で同一)
- Kuromoji の NOTICE.md: https://raw.githubusercontent.com/atilika/kuromoji/master/NOTICE.md
- Kuromoji IPADIC の NOTICE.md: `kuromoji-ipadic-0.9.0.jar` の `META-INF/NOTICE.md`
  (GitHub の https://raw.githubusercontent.com/atilika/kuromoji/master/kuromoji-ipadic/NOTICE.md とは著作権表示の年 `2010-2015` / `2010-2018` だけが異なる)
- Kuromoji の CONTRIBUTORS.md: 上の 2 つの jar の `META-INF/CONTRIBUTORS.md`(NOTICE が参照している文書)
- mecab-ipadic の COPYING: `mecab-ipadic-2.7.0-20070801.tar.gz`(http://atilika.com/releases/mecab-ipadic/mecab-ipadic-2.7.0-20070801.tar.gz 、Kuromoji IPADIC の NOTICE が示す入手先)の `COPYING`
  (https://raw.githubusercontent.com/taku910/mecab/master/mecab-ipadic/COPYING とバイト単位で同一)

---

## 1. Kuromoji — Apache License 2.0(全文)

Kuromoji(kuromoji-core / kuromoji-ipadic 0.9.0)は Apache License 2.0 で提供されています。
次は Kuromoji の LICENSE.md の全文です(原文のまま。行頭の空白も原文どおり)。

```text
    
                                     Apache License
                               Version 2.0, January 2004
                            http://www.apache.org/licenses/
    
       TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION
    
       1. Definitions.
    
          "License" shall mean the terms and conditions for use, reproduction,
          and distribution as defined by Sections 1 through 9 of this document.
    
          "Licensor" shall mean the copyright owner or entity authorized by
          the copyright owner that is granting the License.
    
          "Legal Entity" shall mean the union of the acting entity and all
          other entities that control, are controlled by, or are under common
          control with that entity. For the purposes of this definition,
          "control" means (i) the power, direct or indirect, to cause the
          direction or management of such entity, whether by contract or
          otherwise, or (ii) ownership of fifty percent (50%) or more of the
          outstanding shares, or (iii) beneficial ownership of such entity.
    
          "You" (or "Your") shall mean an individual or Legal Entity
          exercising permissions granted by this License.
    
          "Source" form shall mean the preferred form for making modifications,
          including but not limited to software source code, documentation
          source, and configuration files.
    
          "Object" form shall mean any form resulting from mechanical
          transformation or translation of a Source form, including but
          not limited to compiled object code, generated documentation,
          and conversions to other media types.
    
          "Work" shall mean the work of authorship, whether in Source or
          Object form, made available under the License, as indicated by a
          copyright notice that is included in or attached to the work
          (an example is provided in the Appendix below).
    
          "Derivative Works" shall mean any work, whether in Source or Object
          form, that is based on (or derived from) the Work and for which the
          editorial revisions, annotations, elaborations, or other modifications
          represent, as a whole, an original work of authorship. For the purposes
          of this License, Derivative Works shall not include works that remain
          separable from, or merely link (or bind by name) to the interfaces of,
          the Work and Derivative Works thereof.
    
          "Contribution" shall mean any work of authorship, including
          the original version of the Work and any modifications or additions
          to that Work or Derivative Works thereof, that is intentionally
          submitted to Licensor for inclusion in the Work by the copyright owner
          or by an individual or Legal Entity authorized to submit on behalf of
          the copyright owner. For the purposes of this definition, "submitted"
          means any form of electronic, verbal, or written communication sent
          to the Licensor or its representatives, including but not limited to
          communication on electronic mailing lists, source code control systems,
          and issue tracking systems that are managed by, or on behalf of, the
          Licensor for the purpose of discussing and improving the Work, but
          excluding communication that is conspicuously marked or otherwise
          designated in writing by the copyright owner as "Not a Contribution."
    
          "Contributor" shall mean Licensor and any individual or Legal Entity
          on behalf of whom a Contribution has been received by Licensor and
          subsequently incorporated within the Work.
    
       2. Grant of Copyright License. Subject to the terms and conditions of
          this License, each Contributor hereby grants to You a perpetual,
          worldwide, non-exclusive, no-charge, royalty-free, irrevocable
          copyright license to reproduce, prepare Derivative Works of,
          publicly display, publicly perform, sublicense, and distribute the
          Work and such Derivative Works in Source or Object form.
    
       3. Grant of Patent License. Subject to the terms and conditions of
          this License, each Contributor hereby grants to You a perpetual,
          worldwide, non-exclusive, no-charge, royalty-free, irrevocable
          (except as stated in this section) patent license to make, have made,
          use, offer to sell, sell, import, and otherwise transfer the Work,
          where such license applies only to those patent claims licensable
          by such Contributor that are necessarily infringed by their
          Contribution(s) alone or by combination of their Contribution(s)
          with the Work to which such Contribution(s) was submitted. If You
          institute patent litigation against any entity (including a
          cross-claim or counterclaim in a lawsuit) alleging that the Work
          or a Contribution incorporated within the Work constitutes direct
          or contributory patent infringement, then any patent licenses
          granted to You under this License for that Work shall terminate
          as of the date such litigation is filed.
    
       4. Redistribution. You may reproduce and distribute copies of the
          Work or Derivative Works thereof in any medium, with or without
          modifications, and in Source or Object form, provided that You
          meet the following conditions:
    
          (a) You must give any other recipients of the Work or
              Derivative Works a copy of this License; and
    
          (b) You must cause any modified files to carry prominent notices
              stating that You changed the files; and
    
          (c) You must retain, in the Source form of any Derivative Works
              that You distribute, all copyright, patent, trademark, and
              attribution notices from the Source form of the Work,
              excluding those notices that do not pertain to any part of
              the Derivative Works; and
    
          (d) If the Work includes a "NOTICE" text file as part of its
              distribution, then any Derivative Works that You distribute must
              include a readable copy of the attribution notices contained
              within such NOTICE file, excluding those notices that do not
              pertain to any part of the Derivative Works, in at least one
              of the following places: within a NOTICE text file distributed
              as part of the Derivative Works; within the Source form or
              documentation, if provided along with the Derivative Works; or,
              within a display generated by the Derivative Works, if and
              wherever such third-party notices normally appear. The contents
              of the NOTICE file are for informational purposes only and
              do not modify the License. You may add Your own attribution
              notices within Derivative Works that You distribute, alongside
              or as an addendum to the NOTICE text from the Work, provided
              that such additional attribution notices cannot be construed
              as modifying the License.
    
          You may add Your own copyright statement to Your modifications and
          may provide additional or different license terms and conditions
          for use, reproduction, or distribution of Your modifications, or
          for any such Derivative Works as a whole, provided Your use,
          reproduction, and distribution of the Work otherwise complies with
          the conditions stated in this License.
    
       5. Submission of Contributions. Unless You explicitly state otherwise,
          any Contribution intentionally submitted for inclusion in the Work
          by You to the Licensor shall be under the terms and conditions of
          this License, without any additional terms or conditions.
          Notwithstanding the above, nothing herein shall supersede or modify
          the terms of any separate license agreement you may have executed
          with Licensor regarding such Contributions.
    
       6. Trademarks. This License does not grant permission to use the trade
          names, trademarks, service marks, or product names of the Licensor,
          except as required for reasonable and customary use in describing the
          origin of the Work and reproducing the content of the NOTICE file.
    
       7. Disclaimer of Warranty. Unless required by applicable law or
          agreed to in writing, Licensor provides the Work (and each
          Contributor provides its Contributions) on an "AS IS" BASIS,
          WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
          implied, including, without limitation, any warranties or conditions
          of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
          PARTICULAR PURPOSE. You are solely responsible for determining the
          appropriateness of using or redistributing the Work and assume any
          risks associated with Your exercise of permissions under this License.
    
       8. Limitation of Liability. In no event and under no legal theory,
          whether in tort (including negligence), contract, or otherwise,
          unless required by applicable law (such as deliberate and grossly
          negligent acts) or agreed to in writing, shall any Contributor be
          liable to You for damages, including any direct, indirect, special,
          incidental, or consequential damages of any character arising as a
          result of this License or out of the use or inability to use the
          Work (including but not limited to damages for loss of goodwill,
          work stoppage, computer failure or malfunction, or any and all
          other commercial damages or losses), even if such Contributor
          has been advised of the possibility of such damages.
    
       9. Accepting Warranty or Additional Liability. While redistributing
          the Work or Derivative Works thereof, You may choose to offer,
          and charge a fee for, acceptance of support, warranty, indemnity,
          or other liability obligations and/or rights consistent with this
          License. However, in accepting such obligations, You may act only
          on Your own behalf and on Your sole responsibility, not on behalf
          of any other Contributor, and only if You agree to indemnify,
          defend, and hold each Contributor harmless for any liability
          incurred by, or claims asserted against, such Contributor by reason
          of your accepting any such warranty or additional liability.
    
       END OF TERMS AND CONDITIONS
    
       APPENDIX: How to apply the Apache License to your work.
    
          To apply the Apache License to your work, attach the following
          boilerplate notice, with the fields enclosed by brackets "[]"
          replaced with your own identifying information. (Don't include
          the brackets!)  The text should be enclosed in the appropriate
          comment syntax for the file format. We also recommend that a
          file or class name and description of purpose be included on the
          same "printed page" as the copyright notice for easier
          identification within third-party archives.
    
       Copyright [yyyy] [name of copyright owner]
    
       Licensed under the Apache License, Version 2.0 (the "License");
       you may not use this file except in compliance with the License.
       You may obtain a copy of the License at
    
           http://www.apache.org/licenses/LICENSE-2.0
    
       Unless required by applicable law or agreed to in writing, software
       distributed under the License is distributed on an "AS IS" BASIS,
       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       See the License for the specific language governing permissions and
       limitations under the License.
    
```

---

## 2. Kuromoji の NOTICE と CONTRIBUTORS(全文)

### 2-1. kuromoji-ipadic-0.9.0.jar の `META-INF/NOTICE.md`(配布している版の NOTICE)

```text
# Kuromoji Japanese Morphological Analyzer

Copyright 2010-2015 Atilika Inc. and contributors (see `CONTRIBUTORS.md`)

This software also includes a binary and/or source version of data from
`mecab-ipadic-2.7.0-20070801`, which can be obtained from 

- http://atilika.com/releases/mecab-ipadic/mecab-ipadic-2.7.0-20070801.tar.gz
- http://jaist.dl.sourceforge.net/project/mecab/mecab-ipadic/2.7.0-20070801/mecab-ipadic-2.7.0-20070801.tar.gz

## mecab-ipadic-2.7.0-20070801 Notice

    Nara Institute of Science and Technology (NAIST),
    the copyright holders, disclaims all warranties with regard to this
    software, including all implied warranties of merchantability and
    fitness, in no event shall NAIST be liable for
    any special, indirect or consequential damages or any damages
    whatsoever resulting from loss of use, data or profits, whether in an
    action of contract, negligence or other tortuous action, arising out
    of or in connection with the use or performance of this software.

    A large portion of the dictionary entries
    originate from ICOT Free Software.  The following conditions for ICOT
    Free Software applies to the current dictionary as well.

    Each User may also freely distribute the Program, whether in its
    original form or modified, to any third party or parties, PROVIDED
    that the provisions of Section 3 ("NO WARRANTY") will ALWAYS appear
    on, or be attached to, the Program, which is distributed substantially
    in the same form as set out herein and that such intended
    distribution, if actually made, will neither violate or otherwise
    contravene any of the laws and regulations of the countries having
    jurisdiction over the User or the intended distribution itself.

    NO WARRANTY

    The program was produced on an experimental basis in the course of the
    research and development conducted during the project and is provided
    to users as so produced on an experimental basis.  Accordingly, the
    program is provided without any warranty whatsoever, whether express,
    implied, statutory or otherwise.  The term "warranty" used herein
    includes, but is not limited to, any warranty of the quality,
    performance, merchantability and fitness for a particular purpose of
    the program and the nonexistence of any infringement or violation of
    any right of any third party.

    Each user of the program will agree and understand, and be deemed to
    have agreed and understood, that there is no warranty whatsoever for
    the program and, accordingly, the entire risk arising from or
    otherwise connected with the program is assumed by the user.

    Therefore, neither ICOT, the copyright holder, or any other
    organization that participated in or was otherwise related to the
    development of the program and their respective officials, directors,
    officers and other employees shall be held liable for any and all
    damages, including, without limitation, general, special, incidental
    and consequential damages, arising out of or otherwise in connection
    with the use or inability to use the program or any product, material
    or result produced or otherwise obtained by using the program,
    regardless of whether they have been advised of, or otherwise had
    knowledge of, the possibility of such damages at any time during the
    project or thereafter.  Each user will be deemed to have agreed to the
    foregoing by his or her commencement of use of the program.  The term
    "use" as used herein includes, but is not limited to, the use,
    modification, copying and distribution of the program and the
    production of secondary products from the program.

    In the case where the program, whether in its original form or
    modified, was distributed or delivered to or received by a user from
    any person, organization or entity other than ICOT, unless it makes or
    grants independently of ICOT any specific warranty to the user in
    writing, such person, organization or entity, will also be exempted
    from and not be held liable to the user for any such damages as noted
    above as far as the program is concerned.
```

### 2-2. Kuromoji リポジトリ直下の `NOTICE.md`(GitHub の master)

`kuromoji-core-0.9.0.jar` には NOTICE が含まれていません。参考として、リポジトリ直下の NOTICE を載せます。
このプラグインが同梱する辞書は IPADIC だけです(他の辞書の jar は含みません)。

```text
# Kuromoji Japanese Morphological Analyzer

Copyright 2010-2018 Atilika Inc. and contributors (see `CONTRIBUTORS.md`)

This software also includes a binary and/or source version of data from various 3rd party dictionaries.  See:

- `kuromoji-entities/NOTICE.md`
- `kuromoji-ipadic/NOTICE.md`
- `kuromoji-ipadic-neologd/NOTICE.md`
- `kuromoji-jumandic/NOTICE.md`
- `kuromoji-naist-jdic/NOTICE.md`
- `kuromoji-unidic/NOTICE.md`
- `kuromoji-unidic-kanaccent/NOTICE.md`
- `kuromoji-unidic-neologd/NOTICE.md`
```

### 2-3. `META-INF/CONTRIBUTORS.md`(kuromoji-core / kuromoji-ipadic 0.9.0 の jar。2 つは同一)

```text
# Authors

* Gaute Lambertsen
* Masaru Hasegawa
* Christian Moen


# Contributors

* Dawid Weiss
* Gerrard Hocks
* Masaru Hasegawa
* Yong-woon Lee
* Yungho Yu
```

---

## 3. mecab-ipadic(IPADIC)— 著作権表示・許諾条件・無保証条項(全文)

`kuromoji-ipadic-0.9.0.jar` の辞書データは、Kuromoji の NOTICE によれば mecab-ipadic-2.7.0-20070801 に由来します。
次は mecab-ipadic-2.7.0-20070801 の `COPYING` の全文です(原文のまま)。
原本ファイルの最終行は文字ではない 2 バイト(0xF7 0xF7)だけの行で、テキストとして表せないため省きました。それ以外は原本とバイト単位で同一です。

上の §2-1 の NOTICE にも同じ条項の一部が載っていますが、冒頭の著作権表示(Copyright ... Nara Institute of Science and Technology)と
許諾の段落(Use, reproduction, and distribution ...)は含まれていません。この §3 がそれを補います。

```text
Copyright 2000, 2001, 2002, 2003 Nara Institute of Science
and Technology.  All Rights Reserved.

Use, reproduction, and distribution of this software is permitted.
Any copy of this software, whether in its original form or modified,
must include both the above copyright notice and the following
paragraphs.

Nara Institute of Science and Technology (NAIST),
the copyright holders, disclaims all warranties with regard to this
software, including all implied warranties of merchantability and
fitness, in no event shall NAIST be liable for
any special, indirect or consequential damages or any damages
whatsoever resulting from loss of use, data or profits, whether in an
action of contract, negligence or other tortuous action, arising out
of or in connection with the use or performance of this software.

A large portion of the dictionary entries
originate from ICOT Free Software.  The following conditions for ICOT
Free Software applies to the current dictionary as well.

Each User may also freely distribute the Program, whether in its
original form or modified, to any third party or parties, PROVIDED
that the provisions of Section 3 ("NO WARRANTY") will ALWAYS appear
on, or be attached to, the Program, which is distributed substantially
in the same form as set out herein and that such intended
distribution, if actually made, will neither violate or otherwise
contravene any of the laws and regulations of the countries having
jurisdiction over the User or the intended distribution itself.

NO WARRANTY

The program was produced on an experimental basis in the course of the
research and development conducted during the project and is provided
to users as so produced on an experimental basis.  Accordingly, the
program is provided without any warranty whatsoever, whether express,
implied, statutory or otherwise.  The term "warranty" used herein
includes, but is not limited to, any warranty of the quality,
performance, merchantability and fitness for a particular purpose of
the program and the nonexistence of any infringement or violation of
any right of any third party.

Each user of the program will agree and understand, and be deemed to
have agreed and understood, that there is no warranty whatsoever for
the program and, accordingly, the entire risk arising from or
otherwise connected with the program is assumed by the user.

Therefore, neither ICOT, the copyright holder, or any other
organization that participated in or was otherwise related to the
development of the program and their respective officials, directors,
officers and other employees shall be held liable for any and all
damages, including, without limitation, general, special, incidental
and consequential damages, arising out of or otherwise in connection
with the use or inability to use the program or any product, material
or result produced or otherwise obtained by using the program,
regardless of whether they have been advised of, or otherwise had
knowledge of, the possibility of such damages at any time during the
project or thereafter.  Each user will be deemed to have agreed to the
foregoing by his or her commencement of use of the program.  The term
"use" as used herein includes, but is not limited to, the use,
modification, copying and distribution of the program and the
production of secondary products from the program.

In the case where the program, whether in its original form or
modified, was distributed or delivered to or received by a user from
any person, organization or entity other than ICOT, unless it makes or
grants independently of ICOT any specific warranty to the user in
writing, such person, organization or entity, will also be exempted
from and not be held liable to the user for any such damages as noted
above as far as the program is concerned.
```

---

## 4. JMdict(命名用の加工辞書 `jmdict.tsv`)

engine の jar の `dictionary/jmdict.tsv` は、EDRDG(Electronic Dictionary Research and Development Group)の
JMdict を命名用に加工した派生物で、CC BY-SA 4.0 で配布されます(プラグインのコードとは別のライセンスです)。
出典・版・加工の内容・ライセンスは、同じ jar の `dictionary/LICENSE-JMDICT.md` に書いてあります
(設定画面 Settings > Tools > 日本語→英語 命名 からも表示できます)。

- 著作権: JMdict は Electronic Dictionary Research and Development Group の著作物です。 https://www.edrdg.org/
- ライセンス: Creative Commons Attribution-ShareAlike 4.0 International — https://creativecommons.org/licenses/by-sa/4.0/
- EDRDG のライセンスページ: https://www.edrdg.org/edrdg/licence.html
