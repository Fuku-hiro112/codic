package namingassist.plugin;

import junit.framework.TestCase;
import namingassist.engine.NamingCase;

/** 打ち済みの英字(lead)の切り出しと、命名規則ごとのつなぎ方。 */
public class TypedLeadTest extends TestCase {

    public void testLeadIsLeadingAsciiIdentifierWithLowercase() {
        assertEquals("pageDecision", TypedLead.leadOf("pageDecisionああ"));
        assertEquals("str", TypedLead.leadOf("str稼働時刻"));
        assertEquals("user", TypedLead.leadOf("user名前"));
        assertEquals("pageDecision", TypedLead.leadOf("pageDecisionを取得する"));
        assertEquals("user_id", TypedLead.leadOf("user_idを取得する")); // _ を含むのは 1 語でない
        assertEquals("pageDecision", TypedLead.leadOf("ｐａｇｅＤｅｃｉｓｉｏｎああ")); // 全角は半角にそろえる
        assertEquals("Page", TypedLead.leadOf("Page観測地点"));
        assertEquals("value1", TypedLead.leadOf("value1次元"));
        assertEquals("_tmp", TypedLead.leadOf("_tmp時刻"));
        assertEquals("$x", TypedLead.leadOf("$x時刻"));
        // 英字の後ろの日本語の中にある英字は lead に入れない
        assertEquals("page", TypedLead.leadOf("page外部ID"));
    }

    /** 略語に見える(小文字を含まない)連なりは lead にしない(句の一部として訳す)。 */
    public void testAbbreviationIsNotLead() {
        assertEquals("", TypedLead.leadOf("IDを取得する"));
        assertEquals("", TypedLead.leadOf("URL一覧"));
        assertEquals("", TypedLead.leadOf("Dモデル"));
        assertEquals("", TypedLead.leadOf("MAX_件数"));
        assertEquals("", TypedLead.leadOf("ＩＤを取得する"));
        assertEquals("", TypedLead.leadOf("A$B時刻"));
    }

    /** 1 語の小文字(数字は後ろに続いてよい)+ 格助詞で始まる日本語は、略語として句の一部にする。 */
    public void testLowercaseWordBeforeParticleIsNotLead() {
        for (String particle : new String[] {"を", "の", "が", "に", "で", "と", "へ", "から"}) {
            assertEquals(particle, "", TypedLead.leadOf("id" + particle + "取得"));
        }
        assertEquals("", TypedLead.leadOf("urlを生成する"));
        assertEquals("", TypedLead.leadOf("md5を計算する"));
        assertEquals("", TypedLead.leadOf("ｉｄを取得する"));
    }

    public void testJoinDoesNotRepeatLeadingWord() {
        assertEquals("getPageTitle", TypedLead.join("get", "getPageTitle", NamingCase.CAMEL));
        assertEquals("isPaidPlan", TypedLead.join("is", "isPaidPlan", NamingCase.CAMEL));
        assertEquals("get_page_title", TypedLead.join("get", "get_page_title", NamingCase.SNAKE));
        assertEquals("GET_PAGE_TITLE", TypedLead.join("get", "GET_PAGE_TITLE", NamingCase.UPPER_SNAKE));
        assertEquals("GetPageTitle", TypedLead.join("Get", "GetPageTitle", NamingCase.PASCAL));
        // 1 語だけの英語は削らない(lead だけにならない)
        assertEquals("getGet", TypedLead.join("get", "get", NamingCase.CAMEL));
        // 先頭の語の一部が一致するだけなら削らない
        assertEquals("getGetterName", TypedLead.join("get", "getterName", NamingCase.CAMEL));
    }

    public void testNoLeadForJapaneseOnlyOrAsciiOnly() {
        assertEquals("", TypedLead.leadOf("稼働時刻"));
        assertEquals("", TypedLead.leadOf("pageDecision"));
        assertEquals("", TypedLead.leadOf(""));
    }

    public void testJoinPerNamingCase() {
        assertEquals("pageDecisionLikeThat", TypedLead.join("pageDecision", "likeThat", NamingCase.CAMEL));
        assertEquals("PollenObservationPoint", TypedLead.join("Pollen", "ObservationPoint", NamingCase.PASCAL));
        assertEquals("page_like_that", TypedLead.join("page", "like_that", NamingCase.SNAKE));
        assertEquals("page_like_that", TypedLead.join("page_", "like_that", NamingCase.SNAKE));
        assertEquals("MAX_OPERATION_TIME", TypedLead.join("max", "OPERATION_TIME", NamingCase.UPPER_SNAKE));
        assertEquals("MAX_VALUE_OPERATION_TIME", TypedLead.join("maxValue", "OPERATION_TIME", NamingCase.UPPER_SNAKE));
        assertEquals("page-like-that", TypedLead.join("page", "like-that", NamingCase.KEBAB));
    }

    public void testJoinWithoutLeadIsRenderedAsIs() {
        for (NamingCase c : NamingCase.values()) {
            assertEquals("operationTime", TypedLead.join("", "operationTime", c));
        }
    }
}
