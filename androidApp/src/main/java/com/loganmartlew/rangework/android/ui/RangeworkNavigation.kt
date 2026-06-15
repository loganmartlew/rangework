package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.auth.AuthState

internal enum class RangeworkNavigationType {
    BottomBar,
    NavigationRail,
}

internal data class RangeworkDestination(
    val route: String,
    val label: String,
    val glyph: String,
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

    fun unitDetail(unitId: String): String = "units/$unitId"

    fun unitEdit(unitId: String): String = "units/$unitId/edit"

    fun sessionDetail(sessionId: String): String = "sessions/$sessionId"

    fun sessionEdit(sessionId: String): String = "sessions/$sessionId/edit"
}

internal val topLevelDestinations = listOf(
    RangeworkDestination(
        route = RangeworkRoutes.Overview,
        label = "Overview",
        glyph = "O",
    ),
    RangeworkDestination(
        route = RangeworkRoutes.Units,
        label = "Units",
        glyph = "U",
    ),
    RangeworkDestination(
        route = RangeworkRoutes.Sessions,
        label = "Sessions",
        glyph = "S",
    ),
    RangeworkDestination(
        route = RangeworkRoutes.Settings,
        label = "Settings",
        glyph = "P",
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
