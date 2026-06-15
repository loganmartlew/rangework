package com.loganmartlew.rangework.android.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.loganmartlew.rangework.android.R
import com.loganmartlew.rangework.android.auth.AndroidGoogleIdTokenProvider
import com.loganmartlew.rangework.android.config.baselineAndroidAppAuthConfig
import com.loganmartlew.rangework.android.BuildConfig
import com.loganmartlew.rangework.android.ui.theme.DataStoreThemePreferenceStore
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme
import com.loganmartlew.rangework.android.ui.theme.ThemeMode
import com.loganmartlew.rangework.android.ui.theme.ThemePreferenceStore
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.config.isAuthConfigured
import com.loganmartlew.rangework.shared.data.createRangeworkFoundation
import com.loganmartlew.rangework.shared.model.DistanceUnit
import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.SpeedUnit
import com.loganmartlew.rangework.shared.model.derivedBallCount
import com.loganmartlew.rangework.shared.usecase.AppBootstrapMessage
import com.loganmartlew.rangework.shared.usecase.AppBootstrapMessageUseCase

private const val UnitIdArg = "unitId"
private const val SessionIdArg = "sessionId"

@Composable
fun RangeworkApp(
    activity: ComponentActivity,
) {
    val androidAuthConfig = remember { baselineAndroidAppAuthConfig() }
    val rangeworkFoundation = remember(androidAuthConfig.environment.supabaseConfig) {
        createRangeworkFoundation(androidAuthConfig.environment.supabaseConfig)
    }
    val rootNavController = rememberNavController()
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
    val themePreferenceStore = remember(activity) { DataStoreThemePreferenceStore(activity) }
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = remember(rangeworkFoundation) {
            SettingsViewModel.factory(
                dataFoundation = rangeworkFoundation?.dataFoundation,
                themePreferenceStore = themePreferenceStore,
            )
        },
    )
    val settingsUiState by settingsViewModel.uiState
    val rootRoute = remember(authUiState.authState) {
        rootRouteForAuthState(authUiState.authState)
    }

    LaunchedEffect(authUiState.authState) {
        plannerViewModel.onAuthStateChanged(authUiState.authState)
        settingsViewModel.onAuthStateChanged(authUiState.authState)
    }

    LaunchedEffect(rootRoute) {
        val target = if (rootRoute == RangeworkRoutes.SignIn) {
            RangeworkRoutes.SignIn
        } else {
            RangeworkRoutes.Authenticated
        }
        if (rootNavController.currentDestination?.route != target) {
            rootNavController.navigate(target) {
                popUpTo(rootNavController.graph.findStartDestination().id) {
                    inclusive = target == RangeworkRoutes.SignIn
                }
                launchSingleTop = true
            }
        }
    }

    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settingsUiState.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    RangeworkTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = rootNavController,
                startDestination = RangeworkRoutes.SignIn,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(RangeworkRoutes.SignIn) {
                    UnauthenticatedEntryScreen(
                        uiState = authUiState,
                        bootstrapMessage = bootstrapMessage,
                        onSignIn = { authViewModel.signInWithGoogle(googleIdTokenProvider) },
                    )
                }
                composable(RangeworkRoutes.Authenticated) {
                    AuthenticatedAppShell(
                        authUiState = authUiState,
                        plannerUiState = plannerUiState,
                        settingsUiState = settingsUiState,
                        onSignOut = authViewModel::signOut,
                        onSetThemeMode = settingsViewModel::setThemeMode,
                        onSelectDistanceUnit = settingsViewModel::selectDistanceUnit,
                        onSelectSpeedUnit = settingsViewModel::selectSpeedUnit,
                        onRefreshPlanning = plannerViewModel::refreshPlanning,
                        onRefreshPlanningOnNavigation = plannerViewModel::refreshPlanningOnNavigation,
                        onBeginNewUnit = plannerViewModel::beginNewUnit,
                        onEditUnit = plannerViewModel::editUnit,
                        onDeleteUnit = plannerViewModel::deleteUnit,
                        onConsumeSavedUnitId = plannerViewModel::consumeSavedUnitId,
                        onUpdateUnitTitle = plannerViewModel::updateUnitTitle,
                        onUpdateUnitNotes = plannerViewModel::updateUnitNotes,
                        onUpdateUnitFocus = plannerViewModel::updateUnitFocus,
                        onUpdateUnitDefaultClubReference = plannerViewModel::updateUnitDefaultClubReference,
                        onAddInstruction = plannerViewModel::addInstruction,
                        onUpdateInstructionText = plannerViewModel::updateInstructionText,
                        onUpdateInstructionRepCount = plannerViewModel::updateInstructionRepCount,
                        onUpdateInstructionBallCount = plannerViewModel::updateInstructionBallCount,
                        onMoveInstructionUp = plannerViewModel::moveInstructionUp,
                        onMoveInstructionDown = plannerViewModel::moveInstructionDown,
                        onRemoveInstruction = plannerViewModel::removeInstruction,
                        onSaveUnit = plannerViewModel::saveUnit,
                        onBeginNewSession = plannerViewModel::beginNewSession,
                        onEditSession = plannerViewModel::editSession,
                        onDeleteSession = plannerViewModel::deleteSession,
                        onConsumeSavedSessionId = plannerViewModel::consumeSavedSessionId,
                        onUpdateSessionName = plannerViewModel::updateSessionName,
                        onUpdateSessionNotes = plannerViewModel::updateSessionNotes,
                        onAddSessionItem = plannerViewModel::addSessionItem,
                        onUpdateSessionItemUnit = plannerViewModel::updateSessionItemUnit,
                        onUpdateSessionItemRepeatCount = plannerViewModel::updateSessionItemRepeatCount,
                        onUpdateSessionItemClubReference = plannerViewModel::updateSessionItemClubReference,
                        onUpdateSessionItemNotes = plannerViewModel::updateSessionItemNotes,
                        onUpdateSessionItemFocusCue = plannerViewModel::updateSessionItemFocusCue,
                        onUpdateSessionItemRestSeconds = plannerViewModel::updateSessionItemRestSeconds,
                        onMoveSessionItem = plannerViewModel::moveSessionItem,
                        onRemoveSessionItem = plannerViewModel::removeSessionItem,
                        onSaveSession = plannerViewModel::saveSession,
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
    onSignIn: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        ScrollableScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Text(
                text = "Rangework",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OnboardingHeroCard(
                headline = bootstrapMessage.headline,
                detail = bootstrapMessage.detail,
            )
            SignInActionsCard(
                uiState = uiState,
                onSignIn = onSignIn,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAppShell(
    authUiState: AuthUiState,
    plannerUiState: PracticePlannerUiState,
    settingsUiState: SettingsUiState,
    onSignOut: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSelectDistanceUnit: (DistanceUnit) -> Unit,
    onSelectSpeedUnit: (SpeedUnit) -> Unit,
    onRefreshPlanning: () -> Unit,
    onRefreshPlanningOnNavigation: () -> Unit,
    onBeginNewUnit: () -> Unit,
    onEditUnit: (String) -> Unit,
    onDeleteUnit: (String) -> Unit,
    onConsumeSavedUnitId: () -> Unit,
    onUpdateUnitTitle: (String) -> Unit,
    onUpdateUnitNotes: (String) -> Unit,
    onUpdateUnitFocus: (String) -> Unit,
    onUpdateUnitDefaultClubReference: (String) -> Unit,
    onAddInstruction: () -> Unit,
    onUpdateInstructionText: (Int, String) -> Unit,
    onUpdateInstructionRepCount: (Int, String) -> Unit,
    onUpdateInstructionBallCount: (Int, String) -> Unit,
    onMoveInstructionUp: (Int) -> Unit,
    onMoveInstructionDown: (Int) -> Unit,
    onRemoveInstruction: (Int) -> Unit,
    onSaveUnit: () -> Unit,
    onBeginNewSession: () -> Unit,
    onEditSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onConsumeSavedSessionId: () -> Unit,
    onUpdateSessionName: (String) -> Unit,
    onUpdateSessionNotes: (String) -> Unit,
    onAddSessionItem: () -> Unit,
    onUpdateSessionItemUnit: (Int, String) -> Unit,
    onUpdateSessionItemRepeatCount: (Int, String) -> Unit,
    onUpdateSessionItemClubReference: (Int, String) -> Unit,
    onUpdateSessionItemNotes: (Int, String) -> Unit,
    onUpdateSessionItemFocusCue: (Int, String) -> Unit,
    onUpdateSessionItemRestSeconds: (Int, String) -> Unit,
    onMoveSessionItem: (Int, Int) -> Unit,
    onRemoveSessionItem: (Int) -> Unit,
    onSaveSession: () -> Unit,
) {
    if (authUiState.authState !is AuthState.SignedIn) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val shellNavController = rememberNavController()
    val configuration = LocalConfiguration.current
    val navigationType = remember(configuration.screenWidthDp) {
        navigationTypeForScreenWidth(configuration.screenWidthDp)
    }
    val navBackStackEntry by shellNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: RangeworkRoutes.Overview
    val canNavigateBack = shellNavController.previousBackStackEntry != null && !currentRoute.isTopLevelRoute()
    val snackbarHostState = remember { SnackbarHostState() }
    var lastSnackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(plannerUiState.statusMessage, plannerUiState.dataConfigured) {
        val message = plannerUiState.statusMessage
        if (message != null && message != lastSnackbarMessage && shouldShowPlannerSnackbar(message, plannerUiState)) {
            lastSnackbarMessage = message
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute.shouldRefreshPlanningOnEnter()) {
            onRefreshPlanningOnNavigation()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = titleForRoute(currentRoute))
                },
                navigationIcon = {
                    if (canNavigateBack) {
                        TextButton(
                            onClick = { shellNavController.popBackStack() },
                        ) {
                            Text("Back")
                        }
                    } else {
                        BrandWordmark(modifier = Modifier.padding(start = 12.dp))
                    }
                }
            )
        },
        bottomBar = {
            if (navigationType == RangeworkNavigationType.BottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = navBackStackEntry?.destination.isRouteSelected(destination.route),
                            onClick = {
                                shellNavController.navigate(destination.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(shellNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                }
                            },
                            icon = { Icon(imageVector = destination.icon, contentDescription = null) },
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
                            selected = navBackStackEntry?.destination.isRouteSelected(destination.route),
                            onClick = {
                                shellNavController.navigate(destination.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(shellNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                }
                            },
                            icon = { Icon(imageVector = destination.icon, contentDescription = null) },
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                val anyWorking = plannerUiState.isWorking || authUiState.actionInProgress || settingsUiState.isWorking
                if (anyWorking) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                NavHost(
                    navController = shellNavController,
                    startDestination = RangeworkRoutes.Overview,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (anyWorking) 12.dp else 0.dp),
                ) {
                    composable(RangeworkRoutes.Overview) {
                        OverviewScreen(
                            authUiState = authUiState,
                            plannerUiState = plannerUiState,
                            isExpandedLayout = navigationType == RangeworkNavigationType.NavigationRail,
                            onCreateUnit = {
                                onBeginNewUnit()
                                shellNavController.navigate(RangeworkRoutes.UnitCreate)
                            },
                            onCreateSession = {
                                onBeginNewSession()
                                shellNavController.navigate(RangeworkRoutes.SessionCreate)
                            },
                        )
                    }
                    composable(RangeworkRoutes.Units) {
                        UnitListScreen(
                            plannerUiState = plannerUiState,
                            onRefresh = onRefreshPlanning,
                            onCreateUnit = {
                                onBeginNewUnit()
                                shellNavController.navigate(RangeworkRoutes.UnitCreate)
                            },
                            onViewUnit = { unitId ->
                                shellNavController.navigate(RangeworkRoutes.unitDetail(unitId))
                            },
                            onEditUnit = { unitId ->
                                onEditUnit(unitId)
                                shellNavController.navigate(RangeworkRoutes.unitEdit(unitId))
                            },
                            onDeleteUnit = onDeleteUnit,
                        )
                    }
                    composable(RangeworkRoutes.UnitCreate) {
                        LaunchedEffect(Unit) {
                            onBeginNewUnit()
                        }
                        LaunchedEffect(plannerUiState.savedUnitId) {
                            plannerUiState.savedUnitId?.let { unitId ->
                                onConsumeSavedUnitId()
                                shellNavController.navigate(RangeworkRoutes.unitDetail(unitId)) {
                                    popUpTo(RangeworkRoutes.UnitCreate) { inclusive = true }
                                }
                            }
                        }
                        UnitEditorScreen(
                            plannerUiState = plannerUiState,
                            title = "New unit",
                            onSaveUnit = onSaveUnit,
                            onUpdateTitle = onUpdateUnitTitle,
                            onUpdateNotes = onUpdateUnitNotes,
                            onUpdateFocus = onUpdateUnitFocus,
                            onUpdateDefaultClubReference = onUpdateUnitDefaultClubReference,
                            onAddInstruction = onAddInstruction,
                            onUpdateInstructionText = onUpdateInstructionText,
                            onUpdateInstructionRepCount = onUpdateInstructionRepCount,
                            onUpdateInstructionBallCount = onUpdateInstructionBallCount,
                            onMoveInstructionUp = onMoveInstructionUp,
                            onMoveInstructionDown = onMoveInstructionDown,
                            onRemoveInstruction = onRemoveInstruction,
                        )
                    }
                    composable(RangeworkRoutes.UnitDetail) { backStackEntry ->
                        val unitId = backStackEntry.arguments?.getString(UnitIdArg).orEmpty()
                        UnitDetailScreen(
                            plannerUiState = plannerUiState,
                            unitId = unitId,
                            onCreateUnit = {
                                onBeginNewUnit()
                                shellNavController.navigate(RangeworkRoutes.UnitCreate)
                            },
                            onEditUnit = {
                                onEditUnit(unitId)
                                shellNavController.navigate(RangeworkRoutes.unitEdit(unitId))
                            },
                            onDeleteUnit = {
                                onDeleteUnit(unitId)
                                shellNavController.popBackStack()
                            },
                        )
                    }
                    composable(RangeworkRoutes.UnitEdit) { backStackEntry ->
                        val unitId = backStackEntry.arguments?.getString(UnitIdArg).orEmpty()
                        LaunchedEffect(unitId) {
                            onEditUnit(unitId)
                        }
                        LaunchedEffect(plannerUiState.savedUnitId) {
                            plannerUiState.savedUnitId?.let { savedUnitId ->
                                onConsumeSavedUnitId()
                                shellNavController.navigate(RangeworkRoutes.unitDetail(savedUnitId)) {
                                    popUpTo(RangeworkRoutes.UnitEdit) { inclusive = true }
                                }
                            }
                        }
                        UnitEditorScreen(
                            plannerUiState = plannerUiState,
                            title = "Edit unit",
                            onSaveUnit = onSaveUnit,
                            onUpdateTitle = onUpdateUnitTitle,
                            onUpdateNotes = onUpdateUnitNotes,
                            onUpdateFocus = onUpdateUnitFocus,
                            onUpdateDefaultClubReference = onUpdateUnitDefaultClubReference,
                            onAddInstruction = onAddInstruction,
                            onUpdateInstructionText = onUpdateInstructionText,
                            onUpdateInstructionRepCount = onUpdateInstructionRepCount,
                            onUpdateInstructionBallCount = onUpdateInstructionBallCount,
                            onMoveInstructionUp = onMoveInstructionUp,
                            onMoveInstructionDown = onMoveInstructionDown,
                            onRemoveInstruction = onRemoveInstruction,
                        )
                    }
                    composable(RangeworkRoutes.Sessions) {
                        SessionListScreen(
                            plannerUiState = plannerUiState,
                            onRefresh = onRefreshPlanning,
                            onCreateSession = {
                                onBeginNewSession()
                                shellNavController.navigate(RangeworkRoutes.SessionCreate)
                            },
                            onViewSession = { sessionId ->
                                shellNavController.navigate(RangeworkRoutes.sessionDetail(sessionId))
                            },
                            onEditSession = { sessionId ->
                                onEditSession(sessionId)
                                shellNavController.navigate(RangeworkRoutes.sessionEdit(sessionId))
                            },
                            onDeleteSession = onDeleteSession,
                        )
                    }
                    composable(RangeworkRoutes.SessionCreate) {
                        LaunchedEffect(Unit) {
                            onBeginNewSession()
                        }
                        LaunchedEffect(plannerUiState.savedSessionId) {
                            plannerUiState.savedSessionId?.let { sessionId ->
                                onConsumeSavedSessionId()
                                shellNavController.navigate(RangeworkRoutes.sessionDetail(sessionId)) {
                                    popUpTo(RangeworkRoutes.SessionCreate) { inclusive = true }
                                }
                            }
                        }
                        SessionEditorScreen(
                            plannerUiState = plannerUiState,
                            title = "New session",
                            onSaveSession = onSaveSession,
                            onUpdateSessionName = onUpdateSessionName,
                            onUpdateSessionNotes = onUpdateSessionNotes,
                            onAddSessionItem = onAddSessionItem,
                            onUpdateSessionItemUnit = onUpdateSessionItemUnit,
                            onUpdateSessionItemRepeatCount = onUpdateSessionItemRepeatCount,
                            onUpdateSessionItemClubReference = onUpdateSessionItemClubReference,
                            onUpdateSessionItemNotes = onUpdateSessionItemNotes,
                            onUpdateSessionItemFocusCue = onUpdateSessionItemFocusCue,
                            onUpdateSessionItemRestSeconds = onUpdateSessionItemRestSeconds,
                            onMoveSessionItem = onMoveSessionItem,
                            onRemoveSessionItem = onRemoveSessionItem,
                        )
                    }
                    composable(RangeworkRoutes.SessionDetail) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getString(SessionIdArg).orEmpty()
                        SessionDetailScreen(
                            plannerUiState = plannerUiState,
                            sessionId = sessionId,
                            onCreateSession = {
                                onBeginNewSession()
                                shellNavController.navigate(RangeworkRoutes.SessionCreate)
                            },
                            onEditSession = {
                                onEditSession(sessionId)
                                shellNavController.navigate(RangeworkRoutes.sessionEdit(sessionId))
                            },
                            onDeleteSession = {
                                onDeleteSession(sessionId)
                                shellNavController.popBackStack()
                            },
                        )
                    }
                    composable(RangeworkRoutes.SessionEdit) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getString(SessionIdArg).orEmpty()
                        LaunchedEffect(sessionId) {
                            onEditSession(sessionId)
                        }
                        LaunchedEffect(plannerUiState.savedSessionId) {
                            plannerUiState.savedSessionId?.let { savedSessionId ->
                                onConsumeSavedSessionId()
                                shellNavController.navigate(RangeworkRoutes.sessionDetail(savedSessionId)) {
                                    popUpTo(RangeworkRoutes.SessionEdit) { inclusive = true }
                                }
                            }
                        }
                        SessionEditorScreen(
                            plannerUiState = plannerUiState,
                            title = "Edit session",
                            onSaveSession = onSaveSession,
                            onUpdateSessionName = onUpdateSessionName,
                            onUpdateSessionNotes = onUpdateSessionNotes,
                            onAddSessionItem = onAddSessionItem,
                            onUpdateSessionItemUnit = onUpdateSessionItemUnit,
                            onUpdateSessionItemRepeatCount = onUpdateSessionItemRepeatCount,
                            onUpdateSessionItemClubReference = onUpdateSessionItemClubReference,
                            onUpdateSessionItemNotes = onUpdateSessionItemNotes,
                            onUpdateSessionItemFocusCue = onUpdateSessionItemFocusCue,
                            onUpdateSessionItemRestSeconds = onUpdateSessionItemRestSeconds,
                            onMoveSessionItem = onMoveSessionItem,
                            onRemoveSessionItem = onRemoveSessionItem,
                        )
                    }
                    composable(RangeworkRoutes.Settings) {
                        SettingsScreen(
                            authUiState = authUiState,
                            settingsUiState = settingsUiState,
                            onSignOut = onSignOut,
                            onSetThemeMode = onSetThemeMode,
                            onSelectDistanceUnit = onSelectDistanceUnit,
                            onSelectSpeedUnit = onSelectSpeedUnit,
                        )
                    }
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
    onCreateUnit: () -> Unit,
    onCreateSession: () -> Unit,
) {
    val signedInState = authUiState.authState as AuthState.SignedIn
    val primaryActionLabel = if (plannerUiState.units.isEmpty()) "Create your first unit" else "New unit"
    val secondaryActionLabel = if (plannerUiState.sessions.isEmpty()) "Start a session template" else "New session"

    ScrollableScreen {
        WelcomeHomeCard(
            signedInLabel = signedInState.userEmail ?: signedInState.userId,
            body = if (plannerUiState.units.isEmpty()) {
                "Start by shaping one repeatable practice unit. Once the building blocks are in place, your full range sessions come together fast."
            } else {
                "Your planning workspace is ready. Build full sessions from saved units and arrive at the range with structure already mapped out."
            },
            primaryActionLabel = primaryActionLabel,
            onPrimaryAction = onCreateUnit,
            secondaryActionLabel = secondaryActionLabel,
            onSecondaryAction = onCreateSession,
            secondaryEnabled = plannerUiState.dataConfigured && plannerUiState.units.isNotEmpty(),
        )
        if (!plannerUiState.dataConfigured) {
            EntryHighlightCard(
                title = "Planning unavailable",
                body = planningUnavailableMessage(plannerUiState.environment),
            )
            return@ScrollableScreen
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
                    SnapshotMetricCard(
                        label = "Practice units",
                        value = plannerUiState.units.size.toString(),
                        body = "Saved building blocks ready to reuse.",
                    )
                    SnapshotMetricCard(
                        label = "Session templates",
                        value = plannerUiState.sessions.size.toString(),
                        body = "Structured plans assembled from your live units.",
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SnapshotMetricCard(
                        label = "Range rhythm",
                        value = if (plannerUiState.sessions.isEmpty()) "Draft" else "Live",
                        body = "Ball totals stay derived from explicit instruction counts and session repeat values.",
                    )
                    EntryHighlightCard(
                        title = "Next move",
                        body = if (plannerUiState.sessions.isEmpty()) {
                            "Build a first session template once your units feel solid."
                        } else {
                            "Tighten the unit details first, then use sessions to string them into a repeatable practice block."
                        },
                    )
                }
            }
        } else {
            SnapshotMetricCard(
                label = "Practice units",
                value = plannerUiState.units.size.toString(),
                body = "Saved building blocks ready to reuse.",
            )
            SnapshotMetricCard(
                label = "Session templates",
                value = plannerUiState.sessions.size.toString(),
                body = "Structured plans assembled from your live units.",
            )
            SnapshotMetricCard(
                label = "Range rhythm",
                value = if (plannerUiState.sessions.isEmpty()) "Draft" else "Live",
                body = "Ball totals stay derived from explicit instruction counts and session repeat values.",
            )
            EntryHighlightCard(
                title = "Next move",
                body = if (plannerUiState.sessions.isEmpty()) {
                    "Build a first session template once your units feel solid."
                } else {
                    "Tighten the unit details first, then use sessions to string them into a repeatable practice block."
                },
            )
        }
    }
}

@Composable
private fun UnitListScreen(
    plannerUiState: PracticePlannerUiState,
    onRefresh: () -> Unit,
    onCreateUnit: () -> Unit,
    onViewUnit: (String) -> Unit,
    onEditUnit: (String) -> Unit,
    onDeleteUnit: (String) -> Unit,
) {
    RefreshableScrollableScreen(
        isRefreshing = plannerUiState.isLoading,
        onRefresh = {
            if (!plannerUiState.isWorking) {
                onRefresh()
            }
        },
    ) {
        Text(
            text = "Practice units",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Reusable drills with ordered instructions, explicit ball counts, and a unit-level default club.",
            style = MaterialTheme.typography.bodyLarge,
        )

        if (!plannerUiState.dataConfigured) {
            EntryHighlightCard(
                title = "Planning unavailable",
                body = plannerUiState.statusMessage ?: planningUnavailableMessage(plannerUiState.environment),
            )
        } else {
            FilledTonalButton(
                enabled = !plannerUiState.isWorking,
                onClick = onCreateUnit,
            ) {
                Text("New unit")
            }

            if (plannerUiState.units.isEmpty()) {
                EntryHighlightCard(
                    title = "No units yet",
                    body = "Create a unit to start building reusable drills.",
                )
            } else {
                plannerUiState.units.forEachIndexed { index, unit ->
                    SummaryEntityCard(
                        title = unit.title,
                        subtitle = "${unit.instructions.size} instruction${if (unit.instructions.size == 1) "" else "s"}  •  ${ballSummary(unit.derivedBallCount())}",
                        supportingText = buildString {
                            unit.defaultClubReference?.let { append("$it  •  ") }
                            append(unit.instructions.joinToString("  •  ") { instruction -> instruction.text })
                        },
                        onView = { onViewUnit(unit.id) },
                        onEdit = { onEditUnit(unit.id) },
                        onDelete = { onDeleteUnit(unit.id) },
                    )
                    if (index != plannerUiState.units.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }

    }
}

@Composable
private fun UnitDetailScreen(
    plannerUiState: PracticePlannerUiState,
    unitId: String,
    onCreateUnit: () -> Unit,
    onEditUnit: () -> Unit,
    onDeleteUnit: () -> Unit,
) {
    val unit = plannerUiState.units.firstOrNull { it.id == unitId }
    ScrollableScreen {
        if (unit == null) {
            EntryHighlightCard(
                title = "Unit not found",
                body = "This unit no longer exists in the current planner data.",
            )
            FilledTonalButton(onClick = onCreateUnit) {
                Text("New unit")
            }
            return@ScrollableScreen
        }

        Text(
            text = unit.title,
            style = MaterialTheme.typography.headlineMedium,
        )
        ActionRow(
            primaryLabel = "Edit unit",
            onPrimary = onEditUnit,
            secondaryLabel = "Delete unit",
            onSecondary = onDeleteUnit,
            primaryEnabled = !plannerUiState.isWorking,
            secondaryEnabled = !plannerUiState.isWorking,
        )
        EntryHighlightCard(
            title = "Summary",
            body = buildString {
                append("${unit.instructions.size} instruction")
                if (unit.instructions.size != 1) append("s")
                append("  •  ${ballSummary(unit.derivedBallCount())}")
                unit.defaultClubReference?.let { append("  •  Default club: $it") }
            },
        )
        unit.notes?.takeIf(String::isNotBlank)?.let { notes ->
            EntryHighlightCard(title = "Notes", body = notes)
        }
        unit.focus?.takeIf(String::isNotBlank)?.let { focus ->
            EntryHighlightCard(title = "Focus", body = focus)
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.titleMedium,
                )
                unit.instructions.forEachIndexed { index, instruction ->
                    DetailListCard(
                        title = "Instruction ${instruction.order}",
                        subtitle = instruction.text,
                        supportingText = buildString {
                            instruction.repCount?.let { append("Reps: $it") }
                            instruction.ballCount?.let {
                                if (isNotEmpty()) append("  •  ")
                                append("Balls: $it")
                            }
                            if (isEmpty()) {
                                append("No explicit reps or ball count")
                            }
                        },
                    )
                    if (index != unit.instructions.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitEditorScreen(
    plannerUiState: PracticePlannerUiState,
    title: String,
    onSaveUnit: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdateFocus: (String) -> Unit,
    onUpdateDefaultClubReference: (String) -> Unit,
    onAddInstruction: () -> Unit,
    onUpdateInstructionText: (Int, String) -> Unit,
    onUpdateInstructionRepCount: (Int, String) -> Unit,
    onUpdateInstructionBallCount: (Int, String) -> Unit,
    onMoveInstructionUp: (Int) -> Unit,
    onMoveInstructionDown: (Int) -> Unit,
    onRemoveInstruction: (Int) -> Unit,
) {
    ScrollableScreen {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Units no longer store tags, default balls, or per-instruction club selections.",
            style = MaterialTheme.typography.bodyLarge,
        )
        UnitEditorCard(
            editorState = plannerUiState.unitEditor,
            isWorking = plannerUiState.isWorking,
            onUpdateTitle = onUpdateTitle,
            onUpdateNotes = onUpdateNotes,
            onUpdateFocus = onUpdateFocus,
            onUpdateDefaultClubReference = onUpdateDefaultClubReference,
            onAddInstruction = onAddInstruction,
            onUpdateInstructionText = onUpdateInstructionText,
            onUpdateInstructionRepCount = onUpdateInstructionRepCount,
            onUpdateInstructionBallCount = onUpdateInstructionBallCount,
            onMoveInstructionUp = onMoveInstructionUp,
            onMoveInstructionDown = onMoveInstructionDown,
            onRemoveInstruction = onRemoveInstruction,
            onSaveUnit = onSaveUnit,
        )
    }
}

@Composable
private fun SessionListScreen(
    plannerUiState: PracticePlannerUiState,
    onRefresh: () -> Unit,
    onCreateSession: () -> Unit,
    onViewSession: (String) -> Unit,
    onEditSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
) {
    val unitsById = remember(plannerUiState.units) {
        plannerUiState.units.associateBy(PracticeUnit::id)
    }
    RefreshableScrollableScreen(
        isRefreshing = plannerUiState.isLoading,
        onRefresh = {
            if (!plannerUiState.isWorking) {
                onRefresh()
            }
        },
    ) {
        Text(
            text = "Session templates",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Compose sessions from live unit references, repeat counts, session-specific clubs, and drag-and-drop ordering.",
            style = MaterialTheme.typography.bodyLarge,
        )

        if (!plannerUiState.dataConfigured) {
            EntryHighlightCard(
                title = "Planning unavailable",
                body = plannerUiState.statusMessage ?: planningUnavailableMessage(plannerUiState.environment),
            )
        } else {
            FilledTonalButton(
                enabled = !plannerUiState.isWorking && plannerUiState.units.isNotEmpty(),
                onClick = onCreateSession,
            ) {
                Text("New session")
            }

            if (plannerUiState.units.isEmpty()) {
                EntryHighlightCard(
                    title = "Create a unit first",
                    body = "Sessions are built from live unit references, so add at least one unit before creating a session.",
                )
            }

            if (plannerUiState.sessions.isEmpty()) {
                EntryHighlightCard(
                    title = "No sessions yet",
                    body = "Create a session template once you have units ready.",
                )
            } else {
                plannerUiState.sessions.forEachIndexed { index, session ->
                    SummaryEntityCard(
                        title = session.name,
                        subtitle = "${session.items.size} item${if (session.items.size == 1) "" else "s"}  •  ${ballSummary(session.derivedBallCount(unitsById))}",
                        supportingText = session.items.joinToString("  •  ") { item ->
                            unitsById[item.practiceUnitId]?.title ?: "Missing unit"
                        }.ifBlank { "No items yet." },
                        onView = { onViewSession(session.id) },
                        onEdit = { onEditSession(session.id) },
                        onDelete = { onDeleteSession(session.id) },
                    )
                    if (index != plannerUiState.sessions.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }

    }
}

@Composable
private fun SessionDetailScreen(
    plannerUiState: PracticePlannerUiState,
    sessionId: String,
    onCreateSession: () -> Unit,
    onEditSession: () -> Unit,
    onDeleteSession: () -> Unit,
) {
    val session = plannerUiState.sessions.firstOrNull { it.id == sessionId }
    val unitsById = remember(plannerUiState.units) {
        plannerUiState.units.associateBy(PracticeUnit::id)
    }
    ScrollableScreen {
        if (session == null) {
            EntryHighlightCard(
                title = "Session not found",
                body = "This session no longer exists in the current planner data.",
            )
            FilledTonalButton(onClick = onCreateSession) {
                Text("New session")
            }
            return@ScrollableScreen
        }

        Text(
            text = session.name,
            style = MaterialTheme.typography.headlineMedium,
        )
        ActionRow(
            primaryLabel = "Edit session",
            onPrimary = onEditSession,
            secondaryLabel = "Delete session",
            onSecondary = onDeleteSession,
            primaryEnabled = !plannerUiState.isWorking,
            secondaryEnabled = !plannerUiState.isWorking,
        )
        EntryHighlightCard(
            title = "Summary",
            body = "${session.items.size} item${if (session.items.size == 1) "" else "s"}  •  ${ballSummary(session.derivedBallCount(unitsById))}",
        )
        session.notes?.takeIf(String::isNotBlank)?.let { notes ->
            EntryHighlightCard(title = "Notes", body = notes)
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Session items",
                    style = MaterialTheme.typography.titleMedium,
                )
                session.items.forEachIndexed { index, item ->
                    SessionItemDetailCard(
                        item = item,
                        unit = unitsById[item.practiceUnitId],
                    )
                    if (index != session.items.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionEditorScreen(
    plannerUiState: PracticePlannerUiState,
    title: String,
    onSaveSession: () -> Unit,
    onUpdateSessionName: (String) -> Unit,
    onUpdateSessionNotes: (String) -> Unit,
    onAddSessionItem: () -> Unit,
    onUpdateSessionItemUnit: (Int, String) -> Unit,
    onUpdateSessionItemRepeatCount: (Int, String) -> Unit,
    onUpdateSessionItemClubReference: (Int, String) -> Unit,
    onUpdateSessionItemNotes: (Int, String) -> Unit,
    onUpdateSessionItemFocusCue: (Int, String) -> Unit,
    onUpdateSessionItemRestSeconds: (Int, String) -> Unit,
    onMoveSessionItem: (Int, Int) -> Unit,
    onRemoveSessionItem: (Int) -> Unit,
) {
    val unitsById = remember(plannerUiState.units) {
        plannerUiState.units.associateBy(PracticeUnit::id)
    }
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val sessionItemStartIndex = if (plannerUiState.units.isEmpty()) 4 else 3

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            Text(
                text = "Session item ordering uses drag and drop. Ball totals come from explicit instruction ball counts multiplied by repeat count.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (plannerUiState.units.isEmpty()) {
            item {
                EntryHighlightCard(
                    title = "Create a unit first",
                    body = "Sessions need at least one unit before you can add items.",
                )
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = plannerUiState.sessionEditor.name,
                        onValueChange = onUpdateSessionName,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name") },
                        enabled = !plannerUiState.isWorking,
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = plannerUiState.sessionEditor.notes,
                        onValueChange = onUpdateSessionNotes,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Session notes") },
                        enabled = !plannerUiState.isWorking,
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
                            enabled = !plannerUiState.isWorking && plannerUiState.units.isNotEmpty(),
                            onClick = onAddSessionItem,
                        ) {
                            Text("Add item")
                        }
                    }
                    if (plannerUiState.sessionEditor.items.isEmpty()) {
                        Text(
                            text = "No session items yet.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        itemsIndexed(
            items = plannerUiState.sessionEditor.items,
            key = { _, item -> "${item.order}-${item.practiceUnitId}-${item.notes}" },
        ) { index, item ->
            val isDragging = draggingIndex == index
            SessionItemEditorCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffsetY else 0f
                    }
                    .pointerInput(plannerUiState.sessionEditor.items.size, plannerUiState.isWorking) {
                        if (!plannerUiState.isWorking) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragOffsetY = 0f
                                },
                                onDragEnd = {
                                    draggingIndex = null
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val currentIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                    val currentListIndex = sessionItemStartIndex + currentIndex
                                    dragOffsetY += dragAmount.y
                                    val currentItemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                        info.index == currentListIndex
                                    } ?: return@detectDragGesturesAfterLongPress
                                    val currentMidpoint = currentItemInfo.offset + currentItemInfo.size / 2f + dragOffsetY
                                    val targetItemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                        info.index != currentListIndex &&
                                            info.index in sessionItemStartIndex until (sessionItemStartIndex + plannerUiState.sessionEditor.items.size) &&
                                            currentMidpoint in info.offset.toFloat()..(info.offset + info.size).toFloat()
                                    } ?: return@detectDragGesturesAfterLongPress
                                    val targetIndex = targetItemInfo.index - sessionItemStartIndex
                                    onMoveSessionItem(currentIndex, targetIndex)
                                    draggingIndex = targetIndex
                                    dragOffsetY -= (targetItemInfo.offset - currentItemInfo.offset).toFloat()
                                },
                            )
                        }
                    },
                item = item,
                availableUnits = plannerUiState.units,
                selectedUnit = unitsById[item.practiceUnitId],
                isWorking = plannerUiState.isWorking,
                onSelectUnit = { onUpdateSessionItemUnit(index, it) },
                onUpdateRepeatCount = { onUpdateSessionItemRepeatCount(index, it) },
                onUpdateClubReference = { onUpdateSessionItemClubReference(index, it) },
                onUpdateNotes = { onUpdateSessionItemNotes(index, it) },
                onUpdateFocusCue = { onUpdateSessionItemFocusCue(index, it) },
                onUpdateRestSeconds = { onUpdateSessionItemRestSeconds(index, it) },
                onMoveUp = { onMoveSessionItem(index, index - 1) },
                onMoveDown = { onMoveSessionItem(index, index + 1) },
                onRemove = { onRemoveSessionItem(index) },
                canMoveUp = index > 0,
                canMoveDown = index < plannerUiState.sessionEditor.items.lastIndex,
            )
        }
        item {
            EntryHighlightCard(
                title = "Derived balls",
                body = "${ballSummary(plannerUiState.sessionEditor.items.sumOf { item -> item.derivedBallCount(unitsById[item.practiceUnitId]) ?: 0 })}. Drag and drop items to reorder them.",
            )
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !plannerUiState.isWorking,
                onClick = onSaveSession,
            ) {
                Text("Save session")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    authUiState: AuthUiState,
    settingsUiState: SettingsUiState,
    onSignOut: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSelectDistanceUnit: (DistanceUnit) -> Unit,
    onSelectSpeedUnit: (SpeedUnit) -> Unit,
) {
    val signedInState = authUiState.authState as? AuthState.SignedIn
    var showSignOutDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to sign in again to access your planning workspace.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                ) {
                    Text("Sign out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    ScrollableScreen {
        SettingsSectionHeader("Account")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Signed in as",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = signedInState?.userEmail ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                HorizontalDivider()
                TextButton(
                    onClick = { showSignOutDialog = true },
                    modifier = Modifier.padding(vertical = 4.dp),
                    enabled = !authUiState.actionInProgress,
                ) {
                    Text(
                        text = "Sign out",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        SettingsSectionHeader("Appearance")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
                    val labels = listOf("System", "Light", "Dark")
                    options.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settingsUiState.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) {
                            Text(labels[index])
                        }
                    }
                }
            }
        }

        SettingsSectionHeader("Units")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Distance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val distOptions = listOf(DistanceUnit.YARDS, DistanceUnit.METERS)
                    val distLabels = listOf("Yards", "Meters")
                    distOptions.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = settingsUiState.measurementPreferences.distanceUnit == unit,
                            onClick = { onSelectDistanceUnit(unit) },
                            shape = SegmentedButtonDefaults.itemShape(index, distOptions.size),
                            enabled = !settingsUiState.isWorking && settingsUiState.dataConfigured,
                        ) {
                            Text(distLabels[index])
                        }
                    }
                }
                Text(
                    text = "Speed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val speedOptions = listOf(
                        SpeedUnit.MILES_PER_HOUR,
                        SpeedUnit.KILOMETRES_PER_HOUR,
                        SpeedUnit.METRES_PER_SECOND,
                    )
                    val speedLabels = listOf("mph", "km/h", "m/s")
                    speedOptions.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = settingsUiState.measurementPreferences.speedUnit == unit,
                            onClick = { onSelectSpeedUnit(unit) },
                            shape = SegmentedButtonDefaults.itemShape(index, speedOptions.size),
                            enabled = !settingsUiState.isWorking && settingsUiState.dataConfigured,
                        ) {
                            Text(speedLabels[index])
                        }
                    }
                }

                if (!settingsUiState.dataConfigured) {
                    Text(
                        text = "Unit preferences are not available in this build.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SettingsSectionHeader("About")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                SettingsReadOnlyRow(label = "Version", value = BuildConfig.VERSION_NAME)
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsReadOnlyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
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
    onAddInstruction: () -> Unit,
    onUpdateInstructionText: (Int, String) -> Unit,
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
            OutlinedTextField(
                value = editorState.defaultClubReference,
                onValueChange = onUpdateDefaultClubReference,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Default club") },
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
            ReorderButtons(
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
private fun SessionItemEditorCard(
    modifier: Modifier = Modifier,
    item: PracticeSessionItemEditorState,
    availableUnits: List<PracticeUnit>,
    selectedUnit: PracticeUnit?,
    isWorking: Boolean,
    onSelectUnit: (String) -> Unit,
    onUpdateRepeatCount: (String) -> Unit,
    onUpdateClubReference: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdateFocusCue: (String) -> Unit,
    onUpdateRestSeconds: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
) {
    var unitMenuExpanded by remember(item.order, item.practiceUnitId) { mutableStateOf(false) }

    Card(
        modifier = modifier,
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
                text = "Long press and drag to reorder",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = item.repeatCount,
                    onValueChange = onUpdateRepeatCount,
                    modifier = Modifier.weight(1f),
                    label = { Text("Repeat count") },
                    enabled = !isWorking,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = item.clubReference,
                    onValueChange = onUpdateClubReference,
                    modifier = Modifier.weight(1f),
                    label = { Text("Session club") },
                    enabled = !isWorking,
                    singleLine = true,
                )
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
            }
            Text(
                text = buildString {
                    append(ballSummary(item.derivedBallCount(selectedUnit)))
                    val effectiveClub = item.clubReference.ifBlank {
                        selectedUnit?.defaultClubReference.orEmpty()
                    }
                    if (effectiveClub.isNotBlank()) {
                        append("  •  Club: $effectiveClub")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReorderButtons(
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
private fun SessionItemDetailCard(
    item: PracticeSessionItem,
    unit: PracticeUnit?,
) {
    DetailListCard(
        title = unit?.title ?: "Missing unit",
        subtitle = buildString {
            append("Repeat ${item.repeatCount}x")
            item.clubReference?.takeIf(String::isNotBlank)?.let { append("  •  Club: $it") }
            if (item.clubReference.isNullOrBlank()) {
                unit?.defaultClubReference?.let { append("  •  Club: $it") }
            }
        },
        supportingText = buildString {
            append(ballSummary(item.derivedBallCount(unit)))
            item.focusCue?.takeIf(String::isNotBlank)?.let { append("  •  Focus: $it") }
            item.restSeconds?.let { append("  •  Rest: ${it}s") }
            item.notes?.takeIf(String::isNotBlank)?.let { append("  •  $it") }
        },
    )
}

@Composable
private fun ReorderButtons(
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
private fun SummaryEntityCard(
    title: String,
    subtitle: String,
    supportingText: String,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = CardDefaults.shape,
            )
            .clickable(onClick = onView)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
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
            TextButton(onClick = onView) {
                Text("View")
            }
            TextButton(onClick = onEdit) {
                Text("Edit")
            }
            TextButton(onClick = onDelete) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun DetailListCard(
    title: String,
    subtitle: String,
    supportingText: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
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
    }
}

@Composable
private fun ActionRow(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    primaryEnabled: Boolean,
    secondaryEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilledTonalButton(
            modifier = Modifier.weight(1f),
            enabled = primaryEnabled,
            onClick = onPrimary,
        ) {
            Text(primaryLabel)
        }
        Button(
            modifier = Modifier.weight(1f),
            enabled = secondaryEnabled,
            onClick = onSecondary,
        ) {
            Text(secondaryLabel)
        }
    }
}

@Composable
private fun ScrollableScreen(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshableScrollableScreen(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        ScrollableScreen(
            modifier = Modifier.fillMaxSize(),
            content = content,
        )
    }
}

@Composable
private fun SignInActionsCard(
    uiState: AuthUiState,
    onSignIn: () -> Unit,
) {
    val supportText = when {
        !uiState.environment.isAuthConfigured -> missingConfigMessage(uiState.environment)
        uiState.actionInProgress || uiState.authState is AuthState.Restoring ->
            "Checking your account and preparing the planning workspace."

        uiState.statusMessage == null ||
            uiState.statusMessage == authStateMessage(AuthState.SignedOut) ||
            uiState.statusMessage == authStateMessage(AuthState.Restoring) ->
            "Use the Google account connected to your practice planning workspace."

        else -> uiState.statusMessage
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Badge(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text("Google sign-in")
            }
            Text(
                text = "Pick up where you left off",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = supportText.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun OnboardingHeroCard(
    headline: String,
    detail: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            BrandMarkContainer(size = 96.dp, markSize = 72.dp, twoColor = true)
            Badge(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Text("Welcome")
            }
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EntryHighlightCard(
                title = "What you'll do here",
                body = "Shape reusable practice units, turn them into session templates, and keep everything ready for your next range block.",
            )
        }
    }
}

@Composable
private fun WelcomeHomeCard(
    signedInLabel: String,
    body: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String,
    onSecondaryAction: () -> Unit,
    secondaryEnabled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BrandWordmark()
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = signedInLabel,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onPrimaryAction,
                ) {
                    Text(primaryActionLabel)
                }
                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    enabled = secondaryEnabled,
                    onClick = onSecondaryAction,
                ) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun SnapshotMetricCard(
    label: String,
    value: String,
    body: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = RangeworkMono.large,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun BrandWordmark(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMarkContainer(size = 40.dp, markSize = 24.dp, twoColor = false)
        Text(
            text = "Rangework",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BrandMarkContainer(
    size: androidx.compose.ui.unit.Dp,
    markSize: androidx.compose.ui.unit.Dp,
    twoColor: Boolean,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        CenteredBox {
            Image(
                painter = painterResource(
                    if (twoColor) R.drawable.ic_rangework_mark_twocolor else R.drawable.ic_rangework_mark,
                ),
                contentDescription = "Rangework mark",
                modifier = Modifier.size(markSize),
            )
        }
    }
}

@Composable
private fun CenteredBox(
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

private fun shouldShowPlannerSnackbar(
    message: String,
    plannerUiState: PracticePlannerUiState,
): Boolean = message != planningUnavailableMessage(plannerUiState.environment) &&
    message != planningSchemaUnavailableMessage() &&
    message != "Planning workspace ready." &&
    !message.startsWith("Editing ")

internal fun titleForRoute(route: String): String = when {
    route == RangeworkRoutes.Overview -> "Overview"
    route == RangeworkRoutes.Units -> "Units"
    route == RangeworkRoutes.UnitCreate -> "New unit"
    route == RangeworkRoutes.UnitEdit -> "Edit unit"
    route == RangeworkRoutes.UnitDetail -> "Unit"
    route.startsWith("units/") && route.endsWith("/edit") -> "Edit unit"
    route.startsWith("units/") -> "Unit"
    route == RangeworkRoutes.Sessions -> "Sessions"
    route == RangeworkRoutes.SessionCreate -> "New session"
    route == RangeworkRoutes.SessionEdit -> "Edit session"
    route == RangeworkRoutes.SessionDetail -> "Session"
    route.startsWith("sessions/") && route.endsWith("/edit") -> "Edit session"
    route.startsWith("sessions/") -> "Session"
    route == RangeworkRoutes.Settings -> "Settings"
    else -> "Rangework"
}

internal fun String.shouldRefreshPlanningOnEnter(): Boolean = when {
    this == RangeworkRoutes.Overview -> true
    this == RangeworkRoutes.Units -> true
    this == RangeworkRoutes.Sessions -> true
    this == RangeworkRoutes.Settings -> true
    this == RangeworkRoutes.UnitDetail -> true
    this == RangeworkRoutes.SessionDetail -> true
    startsWith("units/") && this != RangeworkRoutes.UnitCreate && !endsWith("/edit") -> true
    startsWith("sessions/") && this != RangeworkRoutes.SessionCreate && !endsWith("/edit") -> true
    else -> false
}

private fun String.isTopLevelRoute(): Boolean = this == RangeworkRoutes.Overview ||
    this == RangeworkRoutes.Units ||
    this == RangeworkRoutes.Sessions ||
    this == RangeworkRoutes.Settings

private fun NavDestination?.isRouteSelected(route: String): Boolean = this?.hierarchy?.any { destination ->
    val destinationRoute = destination.route ?: return@any false
    destinationRoute == route || destinationRoute.startsWith("$route/")
} == true

private fun PracticeSessionItemEditorState.derivedBallCount(unit: PracticeUnit?): Int? {
    val repeats = repeatCount.trim().toIntOrNull() ?: return unit?.derivedBallCount()
    return unit?.derivedBallCount()?.times(repeats)
}

private fun ballSummary(ballCount: Int?): String = when (ballCount) {
    null -> "Ball total unavailable"
    0 -> "0 derived balls"
    1 -> "1 derived ball"
    else -> "$ballCount derived balls"
}
