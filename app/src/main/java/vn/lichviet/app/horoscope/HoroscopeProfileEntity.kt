package vn.lichviet.app.horoscope

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.json.JSONObject
import vn.lichviet.core.LunarDate
import vn.lichviet.core.horoscope.BirthDateInput
import vn.lichviet.core.horoscope.BirthProfile
import vn.lichviet.core.horoscope.CivilDayRule
import vn.lichviet.core.horoscope.HoroscopeChartEngine
import vn.lichviet.core.horoscope.TraditionalSex

/** Only the user's inputs are persisted; chart results are recomputed with the documented rule set. */
@Entity(tableName = "horoscope_profiles")
data class HoroscopeProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "",
    val group: String = "SELF",
    val dateKind: String = "SOLAR",
    val year: Int = 1990,
    val month: Int = 1,
    val day: Int = 1,
    val isLeap: Boolean = false,
    val hour: Int = 0,
    val minute: Int = 0,
    val traditionalSex: String = "MALE",
    val civilDayRule: String = "CIVIL_MIDNIGHT",
    val utcOffsetMinutes: Int = 420,
)

object HoroscopeProfileValidation {
    fun birthProfile(entity: HoroscopeProfileEntity): BirthProfile = BirthProfile(
        date = when (entity.dateKind) {
            "SOLAR" -> BirthDateInput.Solar(LocalDate.of(entity.year, entity.month, entity.day))
            "LUNAR" -> BirthDateInput.Lunar(LunarDate(entity.day, entity.month, entity.year, entity.isLeap))
            else -> throw IllegalArgumentException("Unsupported birth date kind")
        },
        time = LocalTime.of(entity.hour, entity.minute),
        traditionalSex = TraditionalSex.valueOf(entity.traditionalSex),
        civilDayRule = CivilDayRule.valueOf(entity.civilDayRule),
        utcOffset = ZoneOffset.ofTotalSeconds(entity.utcOffsetMinutes * 60),
        label = entity.label.takeIf(String::isNotEmpty),
    )

    fun validated(entity: HoroscopeProfileEntity): HoroscopeProfileEntity {
        require(entity.id >= 0 && entity.label.length <= 80)
        require(entity.group in setOf("SELF", "FAMILY", "FRIEND"))
        require(entity.utcOffsetMinutes == 420)
        require(entity.dateKind != "SOLAR" || !entity.isLeap)
        HoroscopeChartEngine.calculate(birthProfile(entity))
        return entity.copy(label = entity.label.trim())
    }
}

/** Shared by the manual backup transport. JSON is private birth data, not a public chart link. */
object HoroscopeProfileCodec {
    fun encode(entity: HoroscopeProfileEntity): JSONObject {
        val value = HoroscopeProfileValidation.validated(entity)
        return JSONObject().apply {
            put("label", value.label); put("group", value.group); put("dateKind", value.dateKind)
            put("year", value.year); put("month", value.month); put("day", value.day)
            put("isLeap", value.isLeap); put("hour", value.hour); put("minute", value.minute)
            put("traditionalSex", value.traditionalSex); put("civilDayRule", value.civilDayRule)
            put("utcOffsetMinutes", value.utcOffsetMinutes)
        }
    }

    fun decode(json: JSONObject): HoroscopeProfileEntity {
        fun string(key: String) = json.get(key) as? String ?: throw IllegalArgumentException(key)
        fun number(key: String): Int {
            val value = (json.get(key) as? Number)?.toDouble() ?: throw IllegalArgumentException(key)
            require(value.isFinite() && value % 1.0 == 0.0 && value in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble())
            return value.toInt()
        }
        return HoroscopeProfileValidation.validated(HoroscopeProfileEntity(
            label = string("label"), group = string("group"), dateKind = string("dateKind"),
            year = number("year"), month = number("month"), day = number("day"),
            isLeap = json.get("isLeap") as? Boolean ?: throw IllegalArgumentException("isLeap"),
            hour = number("hour"), minute = number("minute"), traditionalSex = string("traditionalSex"),
            civilDayRule = string("civilDayRule"), utcOffsetMinutes = number("utcOffsetMinutes"),
        ))
    }
}
