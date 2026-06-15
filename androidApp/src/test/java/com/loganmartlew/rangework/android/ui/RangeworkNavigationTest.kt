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
    fun refreshOnEnterOnlyTargetsNonEditorPages() {
        assertTrue(RangeworkRoutes.Overview.shouldRefreshPlanningOnEnter())
        assertTrue(RangeworkRoutes.Units.shouldRefreshPlanningOnEnter())
        assertTrue(RangeworkRoutes.unitDetail("unit-1").shouldRefreshPlanningOnEnter())
        assertTrue(RangeworkRoutes.sessionDetail("session-1").shouldRefreshPlanningOnEnter())
        assertFalse(RangeworkRoutes.UnitCreate.shouldRefreshPlanningOnEnter())
        assertFalse(RangeworkRoutes.unitEdit("unit-1").shouldRefreshPlanningOnEnter())
        assertFalse(RangeworkRoutes.SessionCreate.shouldRefreshPlanningOnEnter())
        assertFalse(RangeworkRoutes.sessionEdit("session-1").shouldRefreshPlanningOnEnter())
    }
}
