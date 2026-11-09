package namingassist.plugin;

import junit.framework.TestCase;
import namingassist.engine.NamingCase;

/** 打ち済みの英字(lead)の切り出しと、命名規則ごとのつなぎ方。 */
public class TypedLeadTest extends TestCase {

    public void testLeadIsLeadingAsciiIdentifierWithLowercase() {
        assertEquals("pageDecision", TypedLead.leadOf("pageDecisionああ"));
        assertEquals("str", TypedLead.leadOf("str稼働時刻"));
        assertEquals("id", TypedLead.leadOf("idを取得する"));
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
        assertEquals("", TypedLead.leadOf("ＩＤを取得する")); // 全角は ASCII ではない
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
