package com.loganmartlew.rangework.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import com.loganmartlew.rangework.shared.auth.AuthState

internal enum class RangeworkNavigationType {
    BottomBar,
    NavigationRail,
}

internal data class RangeworkDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

internal object RangeworkRoutes {
    const val SignIn = "sign_in"
    const val Authenticated = "authenticated"
    const val Overview = "overview"
    const val Units = "units"
    const val UnitCreate = "units/create"
    const val UnitDetail = "units/{unitId}"
    const val UnitEdit = "units/{unitId}/edit"
    const val Sessions = "sessions"
    const val SessionCreate = "sessions/create"
    const val SessionDetail = "sessions/{sessionId}"
    const val SessionEdit = "sessions/{sessionId}/edit"
    const val Settings = "settings"
    const val ManageClubs = "settings/clubs"
    const val DeleteAccount = "settings/delete-account"
    const val LegalPage = "settings/legal/{page}"
    const val RangeSession = "range-sessions/{rangeSessionId}"

    fun unitDetail(unitId: String): String = "units/$unitId"

    fun unitEdit(unitId: String): String = "units/$unitId/edit"

    fun sessionDetail(sessionId: String): String = "sessions/$sessionId"

    fun sessionEdit(sessionId: String): String = "sessions/$sessionId/edit"

    fun legalPage(page: String): String = "settings/legal/$page"

    fun rangeSession(rangeSessionId: String): String = "range-sessions/$rangeSessionId"
}

internal val topLevelDestinations = listOf(
    RangeworkDestination(
        route = RangeworkRoutes.Overview,
        label = "Overview",
        icon = Icons.Rounded.Home,
    ),
    RangeworkDestination(
        route = RangeworkRoutes.Units,
        label = "Units",
        icon = Icons.Rounded.Widgets,
    ),
    RangeworkDestination(
        route = RangeworkRoutes.Sessions,
        label = "Sessions",
        icon = Icons.AutoMirrored.Rounded.EventNote,
    ),
    RangeworkDestination(
        route = RangeworkRoutes.Settings,
        label = "Settings",
        icon = Icons.Rounded.Tune,
    ),
)

internal fun rootRouteForAuthState(authState: AuthState): String = when (authState) {
    is AuthState.SignedIn -> RangeworkRoutes.Overview
    AuthState.Restoring -> RangeworkRoutes.SignIn
    AuthState.SignedOut -> RangeworkRoutes.SignIn
    is AuthState.Error -> RangeworkRoutes.SignIn
}

internal fun navigationTypeForScreenWidth(screenWidthDp: Int): RangeworkNavigationType =
    if (screenWidthDp >= 840) {
        RangeworkNavigationType.NavigationRail
    } else {
        RangeworkNavigationType.BottomBar
    }

internal fun String.isTopLevelRoute(): Boolean = this == RangeworkRoutes.Overview ||
    this == RangeworkRoutes.Units ||
    this == RangeworkRoutes.Sessions ||
    this == RangeworkRoutes.Settings

internal enum class EditorType { Unit, Session }

internal fun String.isEditorRoute(): Boolean = editorType() != null

internal fun String.editorType(): EditorType? = when {
    this == RangeworkRoutes.UnitCreate ||
    this == RangeworkRoutes.UnitEdit ||
    (startsWith("units/") && endsWith("/edit")) -> EditorType.Unit
    this == RangeworkRoutes.SessionCreate ||
    this == RangeworkRoutes.SessionEdit ||
    (startsWith("sessions/") && endsWith("/edit")) -> EditorType.Session
    else -> null
}
