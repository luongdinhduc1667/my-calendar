package vn.lichviet.core.zodiac

/** Sources describe the traditional rules, not evidence that they predict real events. */
enum class CulturalSource(val title: String, val url: String, val section: String) {
    ZODIAC_SYMBOLS("British Library · International Dunhuang Programme",
        "https://idp.bl.uk/discover/learning/chinese-astronomy/articles/astrology-and-astronomy/animals-of-the-chinese-zodiac/", "Animals of the Chinese zodiac"),
    VIETNAMESE_ANIMALS("VietnamPlus · Vietnam News Agency",
        "https://en.vietnamplus.vn/why-does-vietnam-celebrate-the-year-of-the-cat-instead-of-the-rabbit-post255154.vnp", "Vietnamese Cat and Water Buffalo"),
    BRANCH_RELATIONS("三命通會 · Tam Mệnh Thông Hội · II",
        "https://zh.wikisource.org/wiki/三命通會_(四庫全書本)/卷02", "論支元六合 · 論支元三合 · 論衝擊"),
    ELEMENTS_NAP_AM("三命通會 · Tam Mệnh Thông Hội · I",
        "https://zh.wikisource.org/wiki/三命通會_(四庫全書本)/卷01", "論五行生尅 · 總論納音 · 論納音取象"),
    STEM_BRANCH_HOURS("Hong Kong Observatory",
        "https://www.weather.gov.hk/en/gts/time/stemsandbranches.htm", "Heavenly Stems and Earthly Branches · Tables 2–5"),
    COMPASS_EIGHT_HOUSES("欽定協紀辨方書 · Hiệp Kỷ Biện Phương Thư · II",
        "https://zh.wikisource.org/wiki/欽定協紀辨方書_(四庫全書本)/卷02", "二十四方位 · 正五行 · 大遊年變卦"),
    HOUR_ALMANAC("欽定協紀辨方書 · Hiệp Kỷ Biện Phương Thư · VII",
        "https://zh.wikisource.org/wiki/欽定協紀辨方書_(四庫全書本)/卷07", "黄道黒道 · 日起時 · 青龍"),
}

object CulturalRuleSet {
    const val ID = "lichviet-cultural-1.1-v1"
    const val SOURCES_REVIEWED_ON = "2026-10-23"
    /** Describes the software's interpretation policy; it is not a passage in a classic. */
    const val FORECAST_EDITORIAL_VERSION = "branch-reflection-v1"
}
