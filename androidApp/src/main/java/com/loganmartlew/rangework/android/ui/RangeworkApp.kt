package com.loganmartlew.rangework.android.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.loganmartlew.rangework.android.auth.AndroidGoogleIdTokenProvider
import com.loganmartlew.rangework.android.config.baselineAndroidAppAuthConfig
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.config.AppEnvironment
import com.loganmartlew.rangework.shared.config.isAuthConfigured
import com.loganmartlew.rangework.shared.data.createRangeworkFoundation
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.usecase.AppBootstrapMessage
import com.loganmartlew.rangework.shared.usecase.AppBootstrapMessageUseCase

@Composable
fun RangeworkApp(
    activity: ComponentActivity,
) {
    val androidAuthConfig = remember { baselineAndroidAppAuthConfig() }
    val rangeworkFoundation = remember(androidAuthConfig.environment.supabaseConfig) {
        createRangeworkFoundation(androidAuthConfig.environment.supabaseConfig)
    }
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(
        factory = remember(androidAuthConfig, rangeworkFoundation) {
            AuthViewModel.factory(
                androidAuthConfig = androidAuthConfig,
                authFoundation = rangeworkFoundation?.authFoundation,
            )
        },
    )
    val plannerViewModel: PracticePlannerViewModel = viewModel(
        factory = remember(androidAuthConfig.environment, rangeworkFoundation) {
            PracticePlannerViewModel.factory(
                environment = androidAuthConfig.environment,
                dataFoundation = rangeworkFoundation?.dataFoundation,
            )
        },
    )
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val plannerUiState by plannerViewModel.uiState
    val bootstrapMessage = remember(androidAuthConfig.environment) {
        AppBootstrapMessageUseCase().invoke(androidAuthConfig.environment)
    }
    val googleIdTokenProvider = remember(activity, androidAuthConfig.googleWebClientId) {
        AndroidGoogleIdTokenProvider(
            activity = activity,
            webClientId = androidAuthConfig.googleWebClientId,
        )
    }
    val rootRoute = remember(authUiState.authState) {
        rootRouteForAuthState(authUiState.authState)
    }

    LaunchedEffect(authUiState.authState) {
        plannerViewModel.onAuthStateChanged(authUiState.authState)
    }

    LaunchedEffect(rootRoute) {
        val currentRoute = navController.currentDestination?.route
        if (rootRoute == RangeworkRoutes.Overview && currentRoute != RangeworkRoutes.Overview) {
            navController.navigate(RangeworkRoutes.Overview) {
                popUpTo(RangeworkRoutes.SignIn) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        } else if (rootRoute == RangeworkRoutes.SignIn && currentRoute != RangeworkRoutes.SignIn) {
            navController.navigate(RangeworkRoutes.SignIn) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    RangeworkTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = RangeworkRoutes.SignIn,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(RangeworkRoutes.SignIn) {
                    UnauthenticatedEntryScreen(
                        uiState = authUiState,
                        bootstrapMessage = bootstrapMessage,
                        onRestoreSession = authViewModel::restoreSession,
                        onSignIn = { authViewModel.signInWithGoogle(googleIdTokenProvider) },
                    )
                }
                composable(RangeworkRoutes.Overview) {
                    AuthenticatedAppShell(
                        authUiState = authUiState,
                        plannerUiState = plannerUiState,
                        onSignOut = authViewModel::signOut,
                        onRefreshPlanning = plannerViewModel::refreshPlanning,
                        onBeginNewUnit = plannerViewModel::beginNewUnit,
                        onEditUnit = plannerViewModel::editUnit,
                        onDeleteUnit = plannerViewModel::deleteUnit,
                        onUpdateUnitTitle = plannerViewModel::updateUnitTitle,
                        onUpdateUnitNotes = plannerViewModel::updateUnitNotes,
                        onUpdateUnitFocus = plannerViewModel::updateUnitFocus,
                        onUpdateUnitDefaultClubReference = plannerViewModel::updateUnitDefaultClubReference,
                        onUpdateUnitTags = plannerViewModel::updateUnitTags,
                        onUpdateUnitDefaultBallCount = plannerViewModel::updateUnitDefaultBallCount,
                        onAddInstruction = plannerViewModel::addInstruction,
                        onUpdateInstructionText = plannerViewModel::updateInstructionText,
                        onUpdateInstructionClubReference = plannerViewModel::updateInstructionClubReference,
                        onUpdateInstructionRepCount = plannerViewModel::updateInstructionRepCount,
                        onUpdateInstructionBallCount = plannerViewModel::updateInstructionBallCount,
                        onMoveInstructionUp = plannerViewModel::moveInstructionUp,
                        onMoveInstructionDown = plannerViewModel::moveInstructionDown,
                        onRemoveInstruction = plannerViewModel::removeInstruction,
                        onSaveUnit = plannerViewModel::saveUnit,
                        onBeginNewSession = plannerViewModel::beginNewSession,
                        onEditSession = plannerViewModel::editSession,
                        onDeleteSession = plannerViewModel::deleteSession,
                        onUpdateSessionName = plannerViewModel::updateSessionName,
                        onUpdateSessionNotes = plannerViewModel::updateSessionNotes,
                        onAddSessionItem = plannerViewModel::addSessionItem,
                        onUpdateSessionItemUnit = plannerViewModel::updateSessionItemUnit,
                        onUpdateSessionItemNotes = plannerViewModel::updateSessionItemNotes,
                        onUpdateSessionItemFocusCue = plannerViewModel::updateSessionItemFocusCue,
                        onUpdateSessionItemRestSeconds = plannerViewModel::updateSessionItemRestSeconds,
                        onUpdateSessionItemOverrideBallCount = plannerViewModel::updateSessionItemOverrideBallCount,
                        onMoveSessionItemUp = plannerViewModel::moveSessionItemUp,
                        onMoveSessionItemDown = plannerViewModel::moveSessionItemDown,
                        onRemoveSessionItem = plannerViewModel::removeSessionItem,
                        onSaveSession = plannerViewModel::saveSession,
                        navController = navController,
                    )
                }
                composable(RangeworkRoutes.Units) {
                    AuthenticatedAppShell(
                        authUiState = authUiState,
                        plannerUiState = plannerUiState,
                        onSignOut = authViewModel::signOut,
                        onRefreshPlanning = plannerViewModel::refreshPlanning,
                        onBeginNewUnit = plannerViewModel::beginNewUnit,
                        onEditUnit = plannerViewModel::editUnit,
                        onDeleteUnit = plannerViewModel::deleteUnit,
                        onUpdateUnitTitle = plannerViewModel::updateUnitTitle,
                        onUpdateUnitNotes = plannerViewModel::updateUnitNotes,
                        onUpdateUnitFocus = plannerViewModel::updateUnitFocus,
                        onUpdateUnitDefaultClubReference = plannerViewModel::updateUnitDefaultClubReference,
                        onUpdateUnitTags = plannerViewModel::updateUnitTags,
                        onUpdateUnitDefaultBallCount = plannerViewModel::updateUnitDefaultBallCount,
                        onAddInstruction = plannerViewModel::addInstruction,
                        onUpdateInstructionText = plannerViewModel::updateInstructionText,
                        onUpdateInstructionClubReference = plannerViewModel::updateInstructionClubReference,
                        onUpdateInstructionRepCount = plannerViewModel::updateInstructionRepCount,
                        onUpdateInstructionBallCount = plannerViewModel::updateInstructionBallCount,
                        onMoveInstructionUp = plannerViewModel::moveInstructionUp,
                        onMoveInstructionDown = plannerViewModel::moveInstructionDown,
                        onRemoveInstruction = plannerViewModel::removeInstruction,
                        onSaveUnit = plannerViewModel::saveUnit,
                        onBeginNewSession = plannerViewModel::beginNewSession,
                        onEditSession = plannerViewModel::editSession,
                        onDeleteSession = plannerViewModel::deleteSession,
                        onUpdateSessionName = plannerViewModel::updateSessionName,
                        onUpdateSessionNotes = plannerViewModel::updateSessionNotes,
                        onAddSessionItem = plannerViewModel::addSessionItem,
                        onUpdateSessionItemUnit = plannerViewModel::updateSessionItemUnit,
                        onUpdateSessionItemNotes = plannerViewModel::updateSessionItemNotes,
                        onUpdateSessionItemFocusCue = plannerViewModel::updateSessionItemFocusCue,
                        onUpdateSessionItemRestSeconds = plannerViewModel::updateSessionItemRestSeconds,
                        onUpdateSessionItemOverrideBallCount = plannerViewModel::updateSessionItemOverrideBallCount,
                        onMoveSessionItemUp = plannerViewModel::moveSessionItemUp,
                        onMoveSessionItemDown = plannerViewModel::moveSessionItemDown,
                        onRemoveSessionItem = plannerViewModel::removeSessionItem,
                        onSaveSession = plannerViewModel::saveSession,
                        navController = navController,
                    )
                }
                composable(RangeworkRoutes.Sessions) {
                    AuthenticatedAppShell(
                        authUiState = authUiState,
                        plannerUiState = plannerUiState,
                        onSignOut = authViewModel::signOut,
                        onRefreshPlanning = plannerViewModel::refreshPlanning,
                        onBeginNewUnit = plannerViewModel::beginNewUnit,
                        onEditUnit = plannerViewModel::editUnit,
                        onDeleteUnit = plannerViewModel::deleteUnit,
                        onUpdateUnitTitle = plannerViewModel::updateUnitTitle,
                        onUpdateUnitNotes = plannerViewModel::updateUnitNotes,
                        onUpdateUnitFocus = plannerViewModel::updateUnitFocus,
                        onUpdateUnitDefaultClubReference = plannerViewModel::updateUnitDefaultClubReference,
                        onUpdateUnitTags = plannerViewModel::updateUnitTags,
                        onUpdateUnitDefaultBallCount = plannerViewModel::updateUnitDefaultBallCount,
                        onAddInstruction = plannerViewModel::addInstruction,
                        onUpdateInstructionText = plannerViewModel::updateInstructionText,
                        onUpdateInstructionClubReference = plannerViewModel::updateInstructionClubReference,
                        onUpdateInstructionRepCount = plannerViewModel::updateInstructionRepCount,
                        onUpdateInstructionBallCount = plannerViewModel::updateInstructionBallCount,
                        onMoveInstructionUp = plannerViewModel::moveInstructionUp,
                        onMoveInstructionDown = plannerViewModel::moveInstructionDown,
                        onRemoveInstruction = plannerViewModel::removeInstruction,
                        onSaveUnit = plannerViewModel::saveUnit,
                        onBeginNewSession = plannerViewModel::beginNewSession,
                        onEditSession = plannerViewModel::editSession,
                        onDeleteSession = plannerViewModel::deleteSession,
                        onUpdateSessionName = plannerViewModel::updateSessionName,
                        onUpdateSessionNotes = plannerViewModel::updateSessionNotes,
                        onAddSessionItem = plannerViewModel::addSessionItem,
                        onUpdateSessionItemUnit = plannerViewModel::updateSessionItemUnit,
                        onUpdateSessionItemNotes = plannerViewModel::updateSessionItemNotes,
                        onUpdateSessionItemFocusCue = plannerViewModel::updateSessionItemFocusCue,
                        onUpdateSessionItemRestSeconds = plannerViewModel::updateSessionItemRestSeconds,
                        onUpdateSessionItemOverrideBallCount = plannerViewModel::updateSessionItemOverrideBallCount,
                        onMoveSessionItemUp = plannerViewModel::moveSessionItemUp,
                        onMoveSessionItemDown = plannerViewModel::moveSessionItemDown,
                        onRemoveSessionItem = plannerViewModel::removeSessionItem,
                        onSaveSession = plannerViewModel::saveSession,
                        navController = navController,
                    )
                }
                composable(RangeworkRoutes.Settings) {
                    AuthenticatedAppShell(
                        authUiState = authUiState,
                        plannerUiState = plannerUiState,
                        onSignOut = authViewModel::signOut,
                        onRefreshPlanning = plannerViewModel::refreshPlanning,
                        onBeginNewUnit = plannerViewModel::beginNewUnit,
                        onEditUnit = plannerViewModel::editUnit,
                        onDeleteUnit = plannerViewModel::deleteUnit,
                        onUpdateUnitTitle = plannerViewModel::updateUnitTitle,
                        onUpdateUnitNotes = plannerViewModel::updateUnitNotes,
                        onUpdateUnitFocus = plannerViewModel::updateUnitFocus,
                        onUpdateUnitDefaultClubReference = plannerViewModel::updateUnitDefaultClubReference,
                        onUpdateUnitTags = plannerViewModel::updateUnitTags,
                        onUpdateUnitDefaultBallCount = plannerViewModel::updateUnitDefaultBallCount,
                        onAddInstruction = plannerViewModel::addInstruction,
                        onUpdateInstructionText = plannerViewModel::updateInstructionText,
                        onUpdateInstructionClubReference = plannerViewModel::updateInstructionClubReference,
                        onUpdateInstructionRepCount = plannerViewModel::updateInstructionRepCount,
                        onUpdateInstructionBallCount = plannerViewModel::updateInstructionBallCount,
                        onMoveInstructionUp = plannerViewModel::moveInstructionUp,
                        onMoveInstructionDown = plannerViewModel::moveInstructionDown,
                        onRemoveInstruction = plannerViewModel::removeInstruction,
                        onSaveUnit = plannerViewModel::saveUnit,
                        onBeginNewSession = plannerViewModel::beginNewSession,
                        onEditSession = plannerViewModel::editSession,
                        onDeleteSession = plannerViewModel::deleteSession,
                        onUpdateSessionName = plannerViewModel::updateSessionName,
                        onUpdateSessionNotes = plannerViewModel::updateSessionNotes,
                        onAddSessionItem = plannerViewModel::addSessionItem,
                        onUpdateSessionItemUnit = plannerViewModel::updateSessionItemUnit,
                        onUpdateSessionItemNotes = plannerViewModel::updateSessionItemNotes,
                        onUpdateSessionItemFocusCue = plannerViewModel::updateSessionItemFocusCue,
                        onUpdateSessionItemRestSeconds = plannerViewModel::updateSessionItemRestSeconds,
                        onUpdateSessionItemOverrideBallCount = plannerViewModel::updateSessionItemOverrideBallCount,
                        onMoveSessionItemUp = plannerViewModel::moveSessionItemUp,
                        onMoveSessionItemDown = plannerViewModel::moveSessionItemDown,
                        onRemoveSessionItem = plannerViewModel::removeSessionItem,
                        onSaveSession = plannerViewModel::saveSession,
                        navController = navController,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnauthenticatedEntryScreen(
    uiState: AuthUiState,
    bootstrapMessage: AppBootstrapMessage,
    onRestoreSession: () -> Unit,
    onSignIn: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Rangework",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = bootstrapMessage.headline,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = bootstrapMessage.detail,
                style = MaterialTheme.typography.bodyLarge,
            )
            EntryHighlightCard(
                title = "Planning workflow",
                body = "Sign in to create practice units, compose reusable sessions, and keep the baseline planning workflow synced through Supabase.",
            )
            ConfigurationStatusCard(
                environment = uiState.environment,
                authState = uiState.authState,
            )
            SignInActionsCard(
                uiState = uiState,
                onRestoreSession = onRestoreSession,
                onSignIn = onSignIn,
            )
            uiState.statusMessage?.let { statusMessage ->
                EntryHighlightCard(
                    title = "Status",
                    body = statusMessage,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAppShell(
    authUiState: AuthUiState,
    plannerUiState: PracticePlannerUiState,
    onSignOut: () -> Unit,
    onRefreshPlanning: () -> Unit,
    onBeginNewUnit: () -> Unit,
    onEditUnit: (String) -> Unit,
    onDeleteUnit: (String) -> Unit,
    onUpdateUnitTitle: (String) -> Unit,
    onUpdateUnitNotes: (String) -> Unit,
    onUpdateUnitFocus: (String) -> Unit,
    onUpdateUnitDefaultClubReference: (String) -> Unit,
    onUpdateUnitTags: (String) -> Unit,
    onUpdateUnitDefaultBallCount: (String) -> Unit,
    onAddInstruction: () -> Unit,
    onUpdateInstructionText: (Int, String) -> Unit,
    onUpdateInstructionClubReference: (Int, String) -> Unit,
    onUpdateInstructionRepCount: (Int, String) -> Unit,
    onUpdateInstructionBallCount: (Int, String) -> Unit,
    onMoveInstructionUp: (Int) -> Unit,
    onMoveInstructionDown: (Int) -> Unit,
    onRemoveInstruction: (Int) -> Unit,
    onSaveUnit: () -> Unit,
    onBeginNewSession: () -> Unit,
    onEditSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onUpdateSessionName: (String) -> Unit,
    onUpdateSessionNotes: (String) -> Unit,
    onAddSessionItem: () -> Unit,
    onUpdateSessionItemUnit: (Int, String) -> Unit,
    onUpdateSessionItemNotes: (Int, String) -> Unit,
    onUpdateSessionItemFocusCue: (Int, String) -> Unit,
    onUpdateSessionItemRestSeconds: (Int, String) -> Unit,
    onUpdateSessionItemOverrideBallCount: (Int, String) -> Unit,
    onMoveSessionItemUp: (Int) -> Unit,
    onMoveSessionItemDown: (Int) -> Unit,
    onRemoveSessionItem: (Int) -> Unit,
    onSaveSession: () -> Unit,
    navController: NavHostController,
) {
    if (authUiState.authState !is AuthState.SignedIn) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val configuration = LocalConfiguration.current
    val navigationType = remember(configuration.screenWidthDp) {
        navigationTypeForScreenWidth(configuration.screenWidthDp)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: RangeworkRoutes.Overview

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = topLevelDestinations.firstOrNull { destination ->
                            destination.route == currentRoute
                        }?.label ?: "Rangework",
                    )
                },
                navigationIcon = {
                    Text(
                        text = "RW",
                        modifier = Modifier.padding(start = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                actions = {
                    TextButton(onClick = onRefreshPlanning) {
                        Text("Refresh")
                    }
                    TextButton(onClick = onSignOut) {
                        Text("Sign out")
                    }
                },
            )
        },
        bottomBar = {
            if (navigationType == RangeworkNavigationType.BottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination.isRouteSelected(destination.route),
                            onClick = {
                                navController.navigate(destination.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                }
                            },
                            icon = { Text(destination.glyph) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (navigationType == RangeworkNavigationType.NavigationRail) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    topLevelDestinations.forEach { destination ->
                        NavigationRailItem(
                            selected = currentDestination.isRouteSelected(destination.route),
                            onClick = {
                                navController.navigate(destination.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                }
                            },
                            icon = { Text(destination.glyph) },
                            label = { Text(destination.label) },
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (plannerUiState.isWorking || authUiState.actionInProgress) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                when (currentRoute) {
                    RangeworkRoutes.Overview -> OverviewScreen(
                        authUiState = authUiState,
                        plannerUiState = plannerUiState,
                        isExpandedLayout = navigationType == RangeworkNavigationType.NavigationRail,
                    )

                    RangeworkRoutes.Units -> UnitsScreen(
                        plannerUiState = plannerUiState,
                        isExpandedLayout = navigationType == RangeworkNavigationType.NavigationRail,
                        onBeginNewUnit = onBeginNewUnit,
                        onEditUnit = onEditUnit,
                        onDeleteUnit = onDeleteUnit,
                        onUpdateUnitTitle = onUpdateUnitTitle,
                        onUpdateUnitNotes = onUpdateUnitNotes,
                        onUpdateUnitFocus = onUpdateUnitFocus,
                        onUpdateUnitDefaultClubReference = onUpdateUnitDefaultClubReference,
                        onUpdateUnitTags = onUpdateUnitTags,
                        onUpdateUnitDefaultBallCount = onUpdateUnitDefaultBallCount,
                        onAddInstruction = onAddInstruction,
                        onUpdateInstructionText = onUpdateInstructionText,
                        onUpdateInstructionClubReference = onUpdateInstructionClubReference,
                        onUpdateInstructionRepCount = onUpdateInstructionRepCount,
                        onUpdateInstructionBallCount = onUpdateInstructionBallCount,
                        onMoveInstructionUp = onMoveInstructionUp,
                        onMoveInstructionDown = onMoveInstructionDown,
                        onRemoveInstruction = onRemoveInstruction,
                        onSaveUnit = onSaveUnit,
                    )

                    RangeworkRoutes.Sessions -> SessionsScreen(
                        plannerUiState = plannerUiState,
                        isExpandedLayout = navigationType == RangeworkNavigationType.NavigationRail,
                        onBeginNewSession = onBeginNewSession,
                        onEditSession = onEditSession,
                        onDeleteSession = onDeleteSession,
                        onUpdateSessionName = onUpdateSessionName,
                        onUpdateSessionNotes = onUpdateSessionNotes,
                        onAddSessionItem = onAddSessionItem,
                        onUpdateSessionItemUnit = onUpdateSessionItemUnit,
                        onUpdateSessionItemNotes = onUpdateSessionItemNotes,
                        onUpdateSessionItemFocusCue = onUpdateSessionItemFocusCue,
                        onUpdateSessionItemRestSeconds = onUpdateSessionItemRestSeconds,
                        onUpdateSessionItemOverrideBallCount = onUpdateSessionItemOverrideBallCount,
                        onMoveSessionItemUp = onMoveSessionItemUp,
                        onMoveSessionItemDown = onMoveSessionItemDown,
                        onRemoveSessionItem = onRemoveSessionItem,
                        onSaveSession = onSaveSession,
                    )

                    RangeworkRoutes.Settings -> SettingsScreen(
                        authUiState = authUiState,
                        plannerUiState = plannerUiState,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewScreen(
    authUiState: AuthUiState,
    plannerUiState: PracticePlannerUiState,
    isExpandedLayout: Boolean,
) {
    val signedInState = authUiState.authState as AuthState.SignedIn

    Text(
        text = "Welcome back",
        style = MaterialTheme.typography.headlineMedium,
    )
    Text(
        text = signedInState.userEmail ?: signedInState.userId,
        style = MaterialTheme.typography.bodyLarge,
    )

    val summaryCards = listOf(
        "Practice units" to "${plannerUiState.units.size} saved unit${if (plannerUiState.units.size == 1) "" else "s"} ready for reuse.",
        "Session templates" to "${plannerUiState.sessions.size} reusable session${if (plannerUiState.sessions.size == 1) "" else "s"} composed from live units.",
        "Editing focus" to "Unit instructions support club references, reps, and ball counts; session items support notes, rest timers, focus cues, and overrides.",
    )

    if (isExpandedLayout) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                summaryCards.take(2).forEach { (title, body) ->
                    EntryHighlightCard(title = title, body = body)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EntryHighlightCard(title = summaryCards.last().first, body = summaryCards.last().second)
                ConfigurationStatusCard(
                    environment = authUiState.environment,
                    authState = authUiState.authState,
                )
            }
        }
    } else {
        summaryCards.forEach { (title, body) ->
            EntryHighlightCard(title = title, body = body)
        }
        ConfigurationStatusCard(
            environment = authUiState.environment,
            authState = authUiState.authState,
        )
    }

    plannerUiState.statusMessage?.let { status ->
        EntryHighlightCard(
            title = "Status",
            body = status,
        )
    }
}

@Composable
private fun UnitsScreen(
    plannerUiState: PracticePlannerUiState,
    isExpandedLayout: Boolean,
    onBeginNewUnit: () -> Unit,
    onEditUnit: (String) -> Unit,
    onDeleteUnit: (String) -> Unit,
    onUpdateUnitTitle: (String) -> Unit,
    onUpdateUnitNotes: (String) -> Unit,
    onUpdateUnitFocus: (String) -> Unit,
    onUpdateUnitDefaultClubReference: (String) -> Unit,
    onUpdateUnitTags: (String) -> Unit,
    onUpdateUnitDefaultBallCount: (String) -> Unit,
    onAddInstruction: () -> Unit,
    onUpdateInstructionText: (Int, String) -> Unit,
    onUpdateInstructionClubReference: (Int, String) -> Unit,
    onUpdateInstructionRepCount: (Int, String) -> Unit,
    onUpdateInstructionBallCount: (Int, String) -> Unit,
    onMoveInstructionUp: (Int) -> Unit,
    onMoveInstructionDown: (Int) -> Unit,
    onRemoveInstruction: (Int) -> Unit,
    onSaveUnit: () -> Unit,
) {
    Text(
        text = "Practice units",
        style = MaterialTheme.typography.headlineMedium,
    )
    Text(
        text = "Create reusable practice units with ordered instructions, club references, notes, and ball-count defaults.",
        style = MaterialTheme.typography.bodyLarge,
    )

    if (!plannerUiState.dataConfigured) {
        EntryHighlightCard(
            title = "Planning unavailable",
            body = plannerUiState.statusMessage ?: planningUnavailableMessage(plannerUiState.environment),
        )
        return
    }

    PlannerActionRow(
        primaryActionLabel = "New unit",
        onPrimaryAction = onBeginNewUnit,
        primaryEnabled = !plannerUiState.isWorking,
        secondaryActionLabel = "Save unit",
        onSecondaryAction = onSaveUnit,
        secondaryEnabled = !plannerUiState.isWorking,
    )

    if (isExpandedLayout) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                UnitListCard(
                    units = plannerUiState.units,
                    selectedUnitId = plannerUiState.selectedUnitId,
                    onEditUnit = onEditUnit,
                    onDeleteUnit = onDeleteUnit,
                )
            }
            Column(
                modifier = Modifier.weight(1.15f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                UnitEditorCard(
                    editorState = plannerUiState.unitEditor,
                    isWorking = plannerUiState.isWorking,
                    onUpdateTitle = onUpdateUnitTitle,
                    onUpdateNotes = onUpdateUnitNotes,
                    onUpdateFocus = onUpdateUnitFocus,
                    onUpdateDefaultClubReference = onUpdateUnitDefaultClubReference,
                    onUpdateTags = onUpdateUnitTags,
                    onUpdateDefaultBallCount = onUpdateUnitDefaultBallCount,
                    onAddInstruction = onAddInstruction,
                    onUpdateInstructionText = onUpdateInstructionText,
                    onUpdateInstructionClubReference = onUpdateInstructionClubReference,
                    onUpdateInstructionRepCount = onUpdateInstructionRepCount,
                    onUpdateInstructionBallCount = onUpdateInstructionBallCount,
                    onMoveInstructionUp = onMoveInstructionUp,
                    onMoveInstructionDown = onMoveInstructionDown,
                    onRemoveInstruction = onRemoveInstruction,
                    onSaveUnit = onSaveUnit,
                )
            }
        }
    } else {
        UnitListCard(
            units = plannerUiState.units,
            selectedUnitId = plannerUiState.selectedUnitId,
            onEditUnit = onEditUnit,
            onDeleteUnit = onDeleteUnit,
        )
        UnitEditorCard(
            editorState = plannerUiState.unitEditor,
            isWorking = plannerUiState.isWorking,
            onUpdateTitle = onUpdateUnitTitle,
            onUpdateNotes = onUpdateUnitNotes,
            onUpdateFocus = onUpdateUnitFocus,
            onUpdateDefaultClubReference = onUpdateUnitDefaultClubReference,
            onUpdateTags = onUpdateUnitTags,
            onUpdateDefaultBallCount = onUpdateUnitDefaultBallCount,
            onAddInstruction = onAddInstruction,
            onUpdateInstructionText = onUpdateInstructionText,
            onUpdateInstructionClubReference = onUpdateInstructionClubReference,
            onUpdateInstructionRepCount = onUpdateInstructionRepCount,
            onUpdateInstructionBallCount = onUpdateInstructionBallCount,
            onMoveInstructionUp = onMoveInstructionUp,
            onMoveInstructionDown = onMoveInstructionDown,
            onRemoveInstruction = onRemoveInstruction,
            onSaveUnit = onSaveUnit,
        )
    }

    plannerUiState.statusMessage?.let { status ->
        EntryHighlightCard(
            title = "Status",
            body = status,
        )
    }
}

@Composable
private fun SessionsScreen(
    plannerUiState: PracticePlannerUiState,
    isExpandedLayout: Boolean,
    onBeginNewSession: () -> Unit,
    onEditSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onUpdateSessionName: (String) -> Unit,
    onUpdateSessionNotes: (String) -> Unit,
    onAddSessionItem: () -> Unit,
    onUpdateSessionItemUnit: (Int, String) -> Unit,
    onUpdateSessionItemNotes: (Int, String) -> Unit,
    onUpdateSessionItemFocusCue: (Int, String) -> Unit,
    onUpdateSessionItemRestSeconds: (Int, String) -> Unit,
    onUpdateSessionItemOverrideBallCount: (Int, String) -> Unit,
    onMoveSessionItemUp: (Int) -> Unit,
    onMoveSessionItemDown: (Int) -> Unit,
    onRemoveSessionItem: (Int) -> Unit,
    onSaveSession: () -> Unit,
) {
    Text(
        text = "Session templates",
        style = MaterialTheme.typography.headlineMedium,
    )
    Text(
        text = "Compose reusable sessions from live unit references, reorder them, and add notes, rest timers, focus cues, and ball-count overrides.",
        style = MaterialTheme.typography.bodyLarge,
    )

    if (!plannerUiState.dataConfigured) {
        EntryHighlightCard(
            title = "Planning unavailable",
            body = plannerUiState.statusMessage ?: planningUnavailableMessage(plannerUiState.environment),
        )
        return
    }

    PlannerActionRow(
        primaryActionLabel = "New session",
        onPrimaryAction = onBeginNewSession,
        primaryEnabled = !plannerUiState.isWorking,
        secondaryActionLabel = "Save session",
        onSecondaryAction = onSaveSession,
        secondaryEnabled = !plannerUiState.isWorking,
    )

    if (plannerUiState.units.isEmpty()) {
        EntryHighlightCard(
            title = "Create a unit first",
            body = "Sessions reference live practice units. Add at least one unit on the Units screen before composing session items.",
        )
    }

    if (isExpandedLayout) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SessionListCard(
                    sessions = plannerUiState.sessions,
                    units = plannerUiState.units,
                    selectedSessionId = plannerUiState.selectedSessionId,
                    onEditSession = onEditSession,
                    onDeleteSession = onDeleteSession,
                )
            }
            Column(
                modifier = Modifier.weight(1.15f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SessionEditorCard(
                    editorState = plannerUiState.sessionEditor,
                    availableUnits = plannerUiState.units,
                    isWorking = plannerUiState.isWorking,
                    onUpdateName = onUpdateSessionName,
                    onUpdateNotes = onUpdateSessionNotes,
                    onAddSessionItem = onAddSessionItem,
                    onUpdateSessionItemUnit = onUpdateSessionItemUnit,
                    onUpdateSessionItemNotes = onUpdateSessionItemNotes,
                    onUpdateSessionItemFocusCue = onUpdateSessionItemFocusCue,
                    onUpdateSessionItemRestSeconds = onUpdateSessionItemRestSeconds,
                    onUpdateSessionItemOverrideBallCount = onUpdateSessionItemOverrideBallCount,
                    onMoveSessionItemUp = onMoveSessionItemUp,
                    onMoveSessionItemDown = onMoveSessionItemDown,
                    onRemoveSessionItem = onRemoveSessionItem,
                    onSaveSession = onSaveSession,
                )
            }
        }
    } else {
        SessionListCard(
            sessions = plannerUiState.sessions,
            units = plannerUiState.units,
            selectedSessionId = plannerUiState.selectedSessionId,
            onEditSession = onEditSession,
            onDeleteSession = onDeleteSession,
        )
        SessionEditorCard(
            editorState = plannerUiState.sessionEditor,
            availableUnits = plannerUiState.units,
            isWorking = plannerUiState.isWorking,
            onUpdateName = onUpdateSessionName,
            onUpdateNotes = onUpdateSessionNotes,
            onAddSessionItem = onAddSessionItem,
            onUpdateSessionItemUnit = onUpdateSessionItemUnit,
            onUpdateSessionItemNotes = onUpdateSessionItemNotes,
            onUpdateSessionItemFocusCue = onUpdateSessionItemFocusCue,
            onUpdateSessionItemRestSeconds = onUpdateSessionItemRestSeconds,
            onUpdateSessionItemOverrideBallCount = onUpdateSessionItemOverrideBallCount,
            onMoveSessionItemUp = onMoveSessionItemUp,
            onMoveSessionItemDown = onMoveSessionItemDown,
            onRemoveSessionItem = onRemoveSessionItem,
            onSaveSession = onSaveSession,
        )
    }

    plannerUiState.statusMessage?.let { status ->
        EntryHighlightCard(
            title = "Status",
            body = status,
        )
    }
}

@Composable
private fun SettingsScreen(
    authUiState: AuthUiState,
    plannerUiState: PracticePlannerUiState,
) {
    Text(
        text = "Settings",
        style = MaterialTheme.typography.headlineMedium,
    )
    Text(
        text = "The shell is wired to shared auth and planning state. Measurement preferences can plug into this route without restructuring the app.",
        style = MaterialTheme.typography.bodyLarge,
    )
    ConfigurationStatusCard(
        environment = authUiState.environment,
        authState = authUiState.authState,
    )
    EntryHighlightCard(
        title = "Planning summary",
        body = "${plannerUiState.units.size} units and ${plannerUiState.sessions.size} session templates are currently loaded into the app shell.",
    )
    plannerUiState.statusMessage?.let { status ->
        EntryHighlightCard(
            title = "Status",
            body = status,
        )
    }
}

@Composable
private fun PlannerActionRow(
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    primaryEnabled: Boolean,
    secondaryActionLabel: String,
    onSecondaryAction: () -> Unit,
    secondaryEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilledTonalButton(
            modifier = Modifier.weight(1f),
            enabled = primaryEnabled,
            onClick = onPrimaryAction,
        ) {
            Text(primaryActionLabel)
        }
        Button(
            modifier = Modifier.weight(1f),
            enabled = secondaryEnabled,
            onClick = onSecondaryAction,
        ) {
            Text(secondaryActionLabel)
        }
    }
}

@Composable
private fun UnitListCard(
    units: List<PracticeUnit>,
    selectedUnitId: String?,
    onEditUnit: (String) -> Unit,
    onDeleteUnit: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Saved units",
                style = MaterialTheme.typography.titleMedium,
            )
            if (units.isEmpty()) {
                Text(
                    text = "No units yet. Start by defining a reusable practice block.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                units.forEachIndexed { index, unit ->
                    SelectableEntityCard(
                        title = unit.title,
                        subtitle = unit.instructions.joinToString(
                            separator = "  •  ",
                            transform = { instruction -> instruction.text },
                        ),
                        supportingText = buildString {
                            append("${unit.instructions.size} instruction")
                            if (unit.instructions.size != 1) append("s")
                            unit.defaultBallCount?.let { count ->
                                append("  •  $count balls")
                            }
                            unit.defaultClubReference?.let { club ->
                                append("  •  $club")
                            }
                        },
                        selected = selectedUnitId == unit.id,
                        onSelect = { onEditUnit(unit.id) },
                        onDelete = { onDeleteUnit(unit.id) },
                    )
                    if (index != units.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionListCard(
    sessions: List<PracticeSession>,
    units: List<PracticeUnit>,
    selectedSessionId: String?,
    onEditSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
) {
    val unitTitles = remember(units) {
        units.associateBy(PracticeUnit::id)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Saved sessions",
                style = MaterialTheme.typography.titleMedium,
            )
            if (sessions.isEmpty()) {
                Text(
                    text = "No session templates yet. Combine units into a reusable practice plan when you're ready.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                sessions.forEachIndexed { index, session ->
                    val itemSummary = session.items.joinToString(
                        separator = "  •  ",
                        transform = { item ->
                            unitTitles[item.practiceUnitId]?.title ?: "Missing unit"
                        },
                    )
                    SelectableEntityCard(
                        title = session.name,
                        subtitle = if (itemSummary.isBlank()) {
                            "No session items yet."
                        } else {
                            itemSummary
                        },
                        supportingText = buildString {
                            append("${session.items.size} item")
                            if (session.items.size != 1) append("s")
                            session.notes?.takeIf(String::isNotBlank)?.let { notes ->
                                append("  •  $notes")
                            }
                        },
                        selected = selectedSessionId == session.id,
                        onSelect = { onEditSession(session.id) },
                        onDelete = { onDeleteSession(session.id) },
                    )
                    if (index != sessions.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitEditorCard(
    editorState: PracticeUnitEditorState,
    isWorking: Boolean,
    onUpdateTitle: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdateFocus: (String) -> Unit,
    onUpdateDefaultClubReference: (String) -> Unit,
    onUpdateTags: (String) -> Unit,
    onUpdateDefaultBallCount: (String) -> Unit,
    onAddInstruction: () -> Unit,
    onUpdateInstructionText: (Int, String) -> Unit,
    onUpdateInstructionClubReference: (Int, String) -> Unit,
    onUpdateInstructionRepCount: (Int, String) -> Unit,
    onUpdateInstructionBallCount: (Int, String) -> Unit,
    onMoveInstructionUp: (Int) -> Unit,
    onMoveInstructionDown: (Int) -> Unit,
    onRemoveInstruction: (Int) -> Unit,
    onSaveUnit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (editorState.unitId == null) "New unit" else "Editing unit",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = editorState.title,
                onValueChange = onUpdateTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                enabled = !isWorking,
                singleLine = true,
            )
            OutlinedTextField(
                value = editorState.notes,
                onValueChange = onUpdateNotes,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                enabled = !isWorking,
                minLines = 3,
            )
            OutlinedTextField(
                value = editorState.focus,
                onValueChange = onUpdateFocus,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Focus") },
                enabled = !isWorking,
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = editorState.defaultClubReference,
                    onValueChange = onUpdateDefaultClubReference,
                    modifier = Modifier.weight(1f),
                    label = { Text("Default club") },
                    enabled = !isWorking,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = editorState.defaultBallCount,
                    onValueChange = onUpdateDefaultBallCount,
                    modifier = Modifier.weight(1f),
                    label = { Text("Default balls") },
                    enabled = !isWorking,
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = editorState.tags,
                onValueChange = onUpdateTags,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tags (comma separated)") },
                enabled = !isWorking,
                singleLine = true,
            )

            Text(
                text = "Instructions",
                style = MaterialTheme.typography.titleMedium,
            )
            editorState.instructions.forEachIndexed { index, instruction ->
                InstructionEditorCard(
                    instruction = instruction,
                    isWorking = isWorking,
                    onUpdateText = { onUpdateInstructionText(index, it) },
                    onUpdateClubReference = { onUpdateInstructionClubReference(index, it) },
                    onUpdateRepCount = { onUpdateInstructionRepCount(index, it) },
                    onUpdateBallCount = { onUpdateInstructionBallCount(index, it) },
                    onMoveUp = { onMoveInstructionUp(index) },
                    onMoveDown = { onMoveInstructionDown(index) },
                    onRemove = { onRemoveInstruction(index) },
                    canMoveUp = index > 0,
                    canMoveDown = index < editorState.instructions.lastIndex,
                )
            }
            FilledTonalButton(
                enabled = !isWorking,
                onClick = onAddInstruction,
            ) {
                Text("Add instruction")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isWorking,
                onClick = onSaveUnit,
            ) {
                Text("Save unit")
            }
        }
    }
}

@Composable
private fun InstructionEditorCard(
    instruction: PracticeInstructionEditorState,
    isWorking: Boolean,
    onUpdateText: (String) -> Unit,
    onUpdateClubReference: (String) -> Unit,
    onUpdateRepCount: (String) -> Unit,
    onUpdateBallCount: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Instruction ${instruction.order}",
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedTextField(
                value = instruction.text,
                onValueChange = onUpdateText,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Instruction") },
                enabled = !isWorking,
                minLines = 2,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = instruction.clubReference,
                    onValueChange = onUpdateClubReference,
                    modifier = Modifier.weight(1f),
                    label = { Text("Club") },
                    enabled = !isWorking,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = instruction.repCount,
                    onValueChange = onUpdateRepCount,
                    modifier = Modifier.weight(1f),
                    label = { Text("Reps") },
                    enabled = !isWorking,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = instruction.ballCount,
                    onValueChange = onUpdateBallCount,
                    modifier = Modifier.weight(1f),
                    label = { Text("Balls") },
                    enabled = !isWorking,
                    singleLine = true,
                )
            }
            EditorReorderActions(
                isWorking = isWorking,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onRemove = onRemove,
            )
        }
    }
}

@Composable
private fun SessionEditorCard(
    editorState: PracticeSessionEditorState,
    availableUnits: List<PracticeUnit>,
    isWorking: Boolean,
    onUpdateName: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onAddSessionItem: () -> Unit,
    onUpdateSessionItemUnit: (Int, String) -> Unit,
    onUpdateSessionItemNotes: (Int, String) -> Unit,
    onUpdateSessionItemFocusCue: (Int, String) -> Unit,
    onUpdateSessionItemRestSeconds: (Int, String) -> Unit,
    onUpdateSessionItemOverrideBallCount: (Int, String) -> Unit,
    onMoveSessionItemUp: (Int) -> Unit,
    onMoveSessionItemDown: (Int) -> Unit,
    onRemoveSessionItem: (Int) -> Unit,
    onSaveSession: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (editorState.sessionId == null) "New session" else "Editing session",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = editorState.name,
                onValueChange = onUpdateName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                enabled = !isWorking,
                singleLine = true,
            )
            OutlinedTextField(
                value = editorState.notes,
                onValueChange = onUpdateNotes,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Session notes") },
                enabled = !isWorking,
                minLines = 3,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Session items",
                    style = MaterialTheme.typography.titleMedium,
                )
                FilledTonalButton(
                    enabled = !isWorking && availableUnits.isNotEmpty(),
                    onClick = onAddSessionItem,
                ) {
                    Text("Add item")
                }
            }
            editorState.items.forEachIndexed { index, item ->
                SessionItemEditorCard(
                    item = item,
                    availableUnits = availableUnits,
                    isWorking = isWorking,
                    onSelectUnit = { onUpdateSessionItemUnit(index, it) },
                    onUpdateNotes = { onUpdateSessionItemNotes(index, it) },
                    onUpdateFocusCue = { onUpdateSessionItemFocusCue(index, it) },
                    onUpdateRestSeconds = { onUpdateSessionItemRestSeconds(index, it) },
                    onUpdateOverrideBallCount = { onUpdateSessionItemOverrideBallCount(index, it) },
                    onMoveUp = { onMoveSessionItemUp(index) },
                    onMoveDown = { onMoveSessionItemDown(index) },
                    onRemove = { onRemoveSessionItem(index) },
                    canMoveUp = index > 0,
                    canMoveDown = index < editorState.items.lastIndex,
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isWorking,
                onClick = onSaveSession,
            ) {
                Text("Save session")
            }
        }
    }
}

@Composable
private fun SessionItemEditorCard(
    item: PracticeSessionItemEditorState,
    availableUnits: List<PracticeUnit>,
    isWorking: Boolean,
    onSelectUnit: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdateFocusCue: (String) -> Unit,
    onUpdateRestSeconds: (String) -> Unit,
    onUpdateOverrideBallCount: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
) {
    var unitMenuExpanded by remember(item.order, item.practiceUnitId) { mutableStateOf(false) }
    val selectedUnit = availableUnits.firstOrNull { unit -> unit.id == item.practiceUnitId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Session item ${item.order}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Practice unit",
                style = MaterialTheme.typography.labelLarge,
            )
            Box {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isWorking,
                    onClick = { unitMenuExpanded = true },
                ) {
                    Text(selectedUnit?.title ?: "Select a unit")
                }
                DropdownMenu(
                    expanded = unitMenuExpanded,
                    onDismissRequest = { unitMenuExpanded = false },
                ) {
                    availableUnits.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit.title) },
                            onClick = {
                                onSelectUnit(unit.id)
                                unitMenuExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = item.notes,
                onValueChange = onUpdateNotes,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Item notes") },
                enabled = !isWorking,
                minLines = 2,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = item.focusCue,
                    onValueChange = onUpdateFocusCue,
                    modifier = Modifier.weight(1f),
                    label = { Text("Focus cue") },
                    enabled = !isWorking,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = item.restSeconds,
                    onValueChange = onUpdateRestSeconds,
                    modifier = Modifier.weight(1f),
                    label = { Text("Rest seconds") },
                    enabled = !isWorking,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = item.overrideBallCount,
                    onValueChange = onUpdateOverrideBallCount,
                    modifier = Modifier.weight(1f),
                    label = { Text("Override balls") },
                    enabled = !isWorking,
                    singleLine = true,
                )
            }
            EditorReorderActions(
                isWorking = isWorking,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onRemove = onRemove,
            )
        }
    }
}

@Composable
private fun EditorReorderActions(
    isWorking: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(
            enabled = !isWorking && canMoveUp,
            onClick = onMoveUp,
        ) {
            Text("Move up")
        }
        TextButton(
            enabled = !isWorking && canMoveDown,
            onClick = onMoveDown,
        ) {
            Text("Move down")
        }
        TextButton(
            enabled = !isWorking,
            onClick = onRemove,
        ) {
            Text("Remove")
        }
    }
}

@Composable
private fun SelectableEntityCard(
    title: String,
    subtitle: String,
    supportingText: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = CardDefaults.shape,
            )
            .clickable(onClick = onSelect)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onSelect) {
                Text("Edit")
            }
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun SignInActionsCard(
    uiState: AuthUiState,
    onRestoreSession: () -> Unit,
    onSignIn: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.actionInProgress &&
                    uiState.environment.supabaseConfig.isConfigured,
                onClick = onRestoreSession,
            ) {
                Text("Restore session")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.actionInProgress && uiState.environment.isAuthConfigured,
                onClick = onSignIn,
            ) {
                Text("Sign in with Google")
            }
            if (uiState.actionInProgress || uiState.authState is AuthState.Restoring) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ConfigurationStatusCard(
    environment: AppEnvironment,
    authState: AuthState,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Auth foundation",
                style = MaterialTheme.typography.titleMedium,
            )
            StatusLine(
                label = "Supabase URL",
                value = if (environment.supabaseConfig.hasProjectUrl) "Configured" else "Missing",
            )
            StatusLine(
                label = "Supabase anon key",
                value = if (environment.supabaseConfig.hasAnonKey) "Configured" else "Missing",
            )
            StatusLine(
                label = "Google web client ID",
                value = if (environment.googleAuthConfig.isConfigured) "Configured" else "Missing",
            )
            StatusLine(
                label = "Session",
                value = authStateMessage(authState),
            )
        }
    }
}

@Composable
private fun EntryHighlightCard(
    title: String,
    body: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
    )
}

private fun NavDestination?.isRouteSelected(route: String): Boolean =
    this?.hierarchy?.any { destination -> destination.route == route } == true
