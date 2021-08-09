package com.pnd.android.loop.alarm

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.pnd.android.loop.R


const val NO_ALARMS = -1

// MAX ALARMS FOR EACH CATEGORY : 1024
private const val SHIFT_ALARM_COUNT = 10
private const val ALARM_COUNT_MASK = (1 shl SHIFT_ALARM_COUNT) - 1

// MAX CATEGORY : 32
private const val SHIFT_CATEGORY = 5
private const val CATEGORY_MASK = (1 shl SHIFT_CATEGORY) - 1

fun rawToUri(context: Context, @RawRes rawResId: Int): Uri? {
    return if (rawResId == NO_ALARMS) null
    else Uri.parse("android.resource://${context.packageName}/$rawResId")
}

enum class AlarmCategory(
    @StringRes val titleResId: Int,
    val items: Map<String, Int>
) {
    Sounds(
        R.string.alarm_category_sounds,
        ALARM_SOUNDS
    ),

    Vibe(
        R.string.alarm_category_vibes,
        ALARM_VIBES
    )
    ;

    fun key(alarmIndex: Int): Int {
        if (alarmIndex == 0) return NO_ALARMS

        val categoryInt = ALL_ALARM_CATEGORIES.indexOf(this) and CATEGORY_MASK
        return (categoryInt shl SHIFT_ALARM_COUNT) + alarmIndex
    }

    fun nameOf(key: Int): String {
        return items.entries.elementAt(indexOf(key)).key
    }

    fun indexOf(key: Int): Int {
        return if (key == NO_ALARMS) 0 else (key and ALARM_COUNT_MASK)
    }

    companion object {

        @RawRes
        fun alarm(key: Int): Int {
            if (key == NO_ALARMS) return NO_ALARMS

            val category = category(key)
            return category?.items?.values?.elementAt(key and ALARM_COUNT_MASK) ?: NO_ALARMS
        }


        fun category(key: Int): AlarmCategory? {
            if (key == NO_ALARMS) return null

            val categoryInt = (key shr SHIFT_ALARM_COUNT) and CATEGORY_MASK
            return ALL_ALARM_CATEGORIES[categoryInt]
        }
    }
}

// All Alarm categories
val ALL_ALARM_CATEGORIES by lazy { AlarmCategory.values() }


private val ALARM_SOUNDS = mapOf(
    "none" to NO_ALARMS,
    "clicking" to R.raw._412_clicking,
    "oringz" to R.raw._425_oringz_w447,
    "wet" to R.raw._431_wet,
    "code" to R.raw._435_code,
    "electronic" to R.raw._444_electronic,
    "tiny bell" to R.raw._448_tiny_bell,
    "long chime sound" to R.raw._455_long_chime_sound,
    "chimes glassy" to R.raw._456_chimes_glassy,
    "all eyes on me" to R.raw._465_all_eyes_on_me,
    "arpeggio" to R.raw._467_arpeggio,
    "awareness" to R.raw._469_awareness,
    "twirl" to R.raw._470_twirl,
    "pizzicato" to R.raw._471_pizzicato,
    "obey" to R.raw._479_obey,
    "attention seeker" to R.raw._480_attention_seeker,
    "hiccup" to R.raw._483_hiccup,
    "are you kidding" to R.raw._488_are_you_kidding,
    "alarm frenzy" to R.raw._493_alarm_frenzy,
    "job done" to R.raw._501_job_done,
    "good morning" to R.raw._502_good_morning,
    "get outta here" to R.raw._505_get_outta_here,
    "you wouldn't believe" to R.raw._510_you_wouldnt_believe,
    "credulous" to R.raw._512_credulous,
    "a happy new year" to R.raw._513_a_happy_new_year,
    "nice cut" to R.raw._514_nice_cut,
    "demonstrative" to R.raw._516_demonstrative,
    "system fault" to R.raw._518_system_fault,
    "communication channel" to R.raw._519_communication_channel,
    "capisci" to R.raw._521_capisci,
    "jingle bells" to R.raw._523_jingle_bells,
    "12 days of christmas" to R.raw._526_12_days_of_christmas,
    "cheerful" to R.raw._527_cheerful,
    "bubbling up" to R.raw._530_bubbling_up,
    "case closed" to R.raw._531_case_closed,
    "closure" to R.raw._542_closure,
    "long expected" to R.raw._548_long_expected,
    "definite" to R.raw._555_definite,
    "intuition" to R.raw._561_intuition,
    "slow spring board longer tail" to R.raw._571_slow_spring_board_longer_tail,
    "hollow" to R.raw._582_hollow,
    "open up" to R.raw._587_open_up,
    "point blank" to R.raw._589_point_blank,
    "inflicted" to R.raw._601_inflicted,
    "clearly" to R.raw._602_clearly,
    "when" to R.raw._604_when,
    "juntos" to R.raw._607_juntos,
    "goes without saying" to R.raw._608_goes_without_saying,
    "pristine" to R.raw._609_pristine,
    "swiftly" to R.raw._610_swiftly,
    "piece of cake" to R.raw._611_piece_of_cake,
    "done for you" to R.raw._612_done_for_you,
    "got it done" to R.raw._613_got_it_done,
    "percussion sound" to R.raw._614_percussion_sound
)

private val ALARM_VIBES = mapOf<String, Int>()