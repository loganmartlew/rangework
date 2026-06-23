package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.auth.AuthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RangeworkNavigationTest {
    @Test
    fun signedInUsersStartInOverview() {
        assertEquals(
            RangeworkRoutes.Overview,
            rootRouteForAuthState(
                AuthState.SignedIn(
                    userId = "user-1",
                    userEmail = "logan@example.com",
                ),
            ),
        )
    }

    @Test
    fun signedOutStatesStartInSignIn() {
        assertEquals(RangeworkRoutes.SignIn, rootRouteForAuthState(AuthState.SignedOut))
        assertEquals(RangeworkRoutes.SignIn, rootRouteForAuthState(AuthState.Restoring))
        assertEquals(
            RangeworkRoutes.SignIn,
            rootRouteForAuthState(AuthState.Error("Auth failed.")),
        )
    }

    @Test
    fun navigationTypeSwitchesAtTabletWidth() {
        assertEquals(RangeworkNavigationType.BottomBar, navigationTypeForScreenWidth(600))
        assertEquals(RangeworkNavigationType.NavigationRail, navigationTypeForScreenWidth(840))
    }

    @Test
    fun titlesCoverDynamicPlannerRoutes() {
        assertEquals("New unit", titleForRoute(RangeworkRoutes.UnitCreate))
        assertEquals("Unit", titleForRoute(RangeworkRoutes.unitDetail("unit-1")))
        assertEquals("Edit unit", titleForRoute(RangeworkRoutes.unitEdit("unit-1")))
        assertEquals("Session", titleForRoute(RangeworkRoutes.sessionDetail("session-1")))
        assertEquals("Edit session", titleForRoute(RangeworkRoutes.sessionEdit("session-1")))
        assertEquals("Rangework", titleForRoute("unknown"))
    }

    @Test
    fun aiSessionPlansIsASettingsSubScreen() {
        assertEquals("AI Session Plans", titleForRoute(RangeworkRoutes.AiSessionPlans))
        assertFalse(RangeworkRoutes.AiSessionPlans.isTopLevelRoute())
    }

    @Test
    fun topLevelRouteDetectionMatchesShellTabs() {
        assertEquals(true, RangeworkRoutes.Overview.isTopLevelRoute())
        assertEquals(true, RangeworkRoutes.Units.isTopLevelRoute())
        assertEquals(true, RangeworkRoutes.Sessions.isTopLevelRoute())
        assertEquals(true, RangeworkRoutes.Settings.isTopLevelRoute())
        assertEquals(false, RangeworkRoutes.UnitCreate.isTopLevelRoute())
        assertEquals(false, RangeworkRoutes.sessionDetail("session-1").isTopLevelRoute())
    }

    @Test
    fun listFabStyleUsesStage12Thresholds() {
        assertEquals(ListFabStyle.Hidden, listFabStyleForCount(0))
        assertEquals(ListFabStyle.Hidden, listFabStyleForCount(-1))
        assertEquals(ListFabStyle.Extended, listFabStyleForCount(1))
        assertEquals(ListFabStyle.Extended, listFabStyleForCount(2))
        assertEquals(ListFabStyle.Compact, listFabStyleForCount(3))
    }
}
