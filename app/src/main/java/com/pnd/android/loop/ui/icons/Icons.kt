package com.pnd.android.loop.ui.icons

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.pnd.android.loop.R


// MAX ICONS FOR EACH CATEGORY : 1024
private const val SHIFT_ICON_COUNT = 10
private const val ICON_COUNT_MASK = (1 shl SHIFT_ICON_COUNT) - 1

// MAX CATEGORY : 32
private const val SHIFT_CATEGORY = 5
private const val CATEGORY_MASK = (1 shl SHIFT_CATEGORY) - 1


enum class IconCategory(
    @StringRes val titleResId: Int,
    val items: List<ImageVector>
) {
    Check(
        titleResId = R.string.icon_category_check,
        items = CHECK_ICONS
    ),
    Navigation(
        titleResId = R.string.icon_category_navigation,
        items = NAVIGATION_ICONS
    ),
    Place(
        titleResId = R.string.icon_category_place,
        items = PLACE_ICONS
    ),
    People(
        titleResId = R.string.icon_category_people,
        items = PEOPLE_ICONS
    ),
    Social(
        titleResId = R.string.icon_category_social,
        items = SOCIAL_ICONS
    ),
    Sports(
        titleResId = R.string.icon_category_sports,
        items = SPORTS_ICONS
    ),
    ;

    fun key(iconIndex: Int): Int {
        val categoryInt = ALL_ICON_CATEGORIES.indexOf(this) and CATEGORY_MASK
        return (categoryInt shl SHIFT_ICON_COUNT) + iconIndex
    }
}

// All Icon categories
val ALL_ICON_CATEGORIES by lazy { IconCategory.values() }


fun icon(key: Int): ImageVector {
    val category = category(key)
    return category.items[key and ICON_COUNT_MASK]
}


fun category(key: Int): IconCategory {
    val categoryInt = (key shr SHIFT_ICON_COUNT) and CATEGORY_MASK
    return ALL_ICON_CATEGORIES[categoryInt]
}

fun categoryIndex(key:Int) : Int {
    return (key shr SHIFT_ICON_COUNT) and CATEGORY_MASK
}


private val CHECK_ICONS = listOf(
    Icons.Outlined.CheckBox,
    Icons.Outlined.CheckBoxOutlineBlank,
    Icons.Outlined.IndeterminateCheckBox,
    Icons.Outlined.RadioButtonChecked,
    Icons.Outlined.RadioButtonUnchecked,
    Icons.Outlined.StarBorder,
    Icons.Outlined.StarHalf,
    Icons.Outlined.StarPurple500,
    Icons.Outlined.ToggleOff,
    Icons.Outlined.ToggleOn,
)

private val NAVIGATION_ICONS = listOf(
    Icons.Outlined.ArrowBack,
    Icons.Outlined.ArrowForward,
    Icons.Outlined.ArrowDownward,
    Icons.Outlined.ArrowUpward,
    Icons.Outlined.ArrowBackIos,
    Icons.Outlined.ArrowForwardIos,
    Icons.Outlined.ArrowLeft,
    Icons.Outlined.ArrowRight,
    Icons.Outlined.ArrowDropDown,
    Icons.Outlined.ArrowDropUp,
    Icons.Outlined.West,
    Icons.Outlined.East,
    Icons.Outlined.North,
    Icons.Outlined.South,
    Icons.Outlined.NorthWest,
    Icons.Outlined.NorthEast,
    Icons.Outlined.SouthWest,
    Icons.Outlined.SouthEast,
    Icons.Outlined.ChevronLeft,
    Icons.Outlined.ChevronRight,
    Icons.Outlined.ExpandMore,
    Icons.Outlined.ExpandLess,
    Icons.Outlined.Fullscreen,
    Icons.Outlined.FullscreenExit,
    Icons.Outlined.FirstPage,
    Icons.Outlined.LastPage,
    Icons.Outlined.MoreHoriz,
    Icons.Outlined.MoreVert,
    Icons.Outlined.Refresh,
    Icons.Outlined.SwitchLeft,
    Icons.Outlined.SwitchRight,
    Icons.Outlined.UnfoldLess,
    Icons.Outlined.UnfoldMore,
    Icons.Outlined.Close,
    Icons.Outlined.Menu,
    Icons.Outlined.Apps,
)

private val PLACE_ICONS = listOf(
    Icons.Outlined.LocationCity,
    Icons.Outlined.Domain,
    Icons.Outlined.Apartment,
    Icons.Outlined.Deck,
    Icons.Outlined.Checkroom,
    Icons.Outlined.Bathroom,
    Icons.Outlined.BedroomBaby,
    Icons.Outlined.BedroomChild,
    Icons.Outlined.BedroomParent,
    Icons.Outlined.Dining,
    Icons.Outlined.Storefront,
    Icons.Outlined.Garage,
    Icons.Outlined.Living,
    Icons.Outlined.Elevator,
    Icons.Outlined.Escalator,
    Icons.Outlined.Stairs,
    Icons.Outlined.Yard,
    Icons.Outlined.FoodBank,
    Icons.Outlined.House,
    Icons.Outlined.HouseSiding,
    Icons.Outlined.Gite,
    Icons.Outlined.GolfCourse,
    Icons.Outlined.Kitchen,
    Icons.Outlined.SmokingRooms,
    Icons.Outlined.FitnessCenter
)

private val PEOPLE_ICONS = listOf(
    Icons.Outlined.EmojiPeople,
    Icons.Outlined.Accessibility,
    Icons.Outlined.AccessibilityNew,
    Icons.Outlined.SelfImprovement,
    Icons.Outlined.PregnantWoman,
    Icons.Outlined.Wc,
    Icons.Outlined.EscalatorWarning,
    Icons.Outlined.FamilyRestroom,
    Icons.Outlined.Bathtub,
    Icons.Outlined.BabyChangingStation,
    Icons.Outlined.AirlineSeatReclineNormal,
    Icons.Outlined.AirlineSeatReclineExtra,
    Icons.Outlined.TransferWithinAStation,
    Icons.Outlined.Hail,
    Icons.Outlined.DirectionsWalk,
    Icons.Outlined.DirectionsRun,
    Icons.Outlined.DirectionsBike,
    Icons.Outlined.Engineering,
    Icons.Outlined.Accessible,
    Icons.Outlined.AccessibleForward,
    Icons.Outlined.Elderly,
)

private val SOCIAL_ICONS = listOf(
    Icons.Outlined.Android,
    Icons.Outlined.Facebook,
    Icons.Outlined.Podcasts,
    Icons.Outlined.Whatshot,
    Icons.Outlined.CatchingPokemon,
    Icons.Outlined.TravelExplore,
    Icons.Outlined.Share,
    Icons.Outlined.SafetyDivider,
    Icons.Outlined.OutdoorGrill
)

private val SPORTS_ICONS = listOf(
    Icons.Outlined.IceSkating,
    Icons.Outlined.Kitesurfing,
    Icons.Outlined.NordicWalking,
    Icons.Outlined.Kayaking,
    Icons.Outlined.DownhillSkiing,
    Icons.Outlined.Surfing,
    Icons.Outlined.SportsKabaddi,
    Icons.Outlined.SportsHandball,
    Icons.Outlined.Snowshoeing,
    Icons.Outlined.Snowboarding,
    Icons.Outlined.Sledding,
    Icons.Outlined.Skateboarding,
    Icons.Outlined.Paragliding,
    Icons.Outlined.SportsBaseball,
    Icons.Outlined.SportsBasketball,
    Icons.Outlined.SportsCricket,
    Icons.Outlined.SportsEsports,
    Icons.Outlined.SportsFootball,
    Icons.Outlined.SportsGolf,
    Icons.Outlined.SportsVolleyball,
    Icons.Outlined.SportsTennis,
    Icons.Outlined.SportsSoccer,
    Icons.Outlined.SportsRugby,
    Icons.Outlined.SportsMotorsports,
    Icons.Outlined.SportsMma,
    Icons.Outlined.SportsHockey
)