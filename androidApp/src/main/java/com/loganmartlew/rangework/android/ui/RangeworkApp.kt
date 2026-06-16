package com.loganmartlew.rangework.android.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.painterResource
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
import com.loganmartlew.rangework.android.ui.components.BrandWordmark
import com.loganmartlew.rangework.android.ui.components.BrandMarkContainer
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.android.ui.screens.OverviewScreen
import com.loganmartlew.rangework.android.ui.screens.SessionDetailScreen
import com.loganmartlew.rangework.android.ui.screens.SessionEditorScreen
import com.loganmartlew.rangework.android.ui.screens.SessionListScreen
import com.loganmartlew.rangework.android.ui.screens.SettingsScreen
import com.loganmartlew.rangework.android.ui.screens.UnitDetailScreen
import com.loganmartlew.rangework.android.ui.screens.UnitEditorScreen
import com.loganmartlew.rangework.android.ui.screens.UnitListScreen
import com.loganmartlew.rangework.android.ui.theme.DataStoreThemePreferenceStore
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme
import com.loganmartlew.rangework.android.ui.theme.ThemeMode
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.config.isAuthConfigured
import com.loganmartlew.rangework.shared.data.createRangeworkFoundation
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
    val plannerUiState by plannerViewModel.uiState.collectAsStateWithLifecycle()
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
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
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

    val unitActions = remember(plannerViewModel) {
        UnitEditorActions(
            onBeginNew = plannerViewModel::beginNewUnit,
            onEdit = plannerViewModel::editUnit,
            onDelete = plannerViewModel::deleteUnit,
            onClearBaselines = plannerViewModel::clearEditorBaselines,
            onConsumeSavedId = plannerViewModel::consumeSavedUnitId,
            onUpdateTitle = plannerViewModel::updateUnitTitle,
            onUpdateNotes = plannerViewModel::updateUnitNotes,
            onUpdateFocus = plannerViewModel::updateUnitFocus,
            onUpdateDefaultClubReference = plannerViewModel::updateUnitDefaultClubReference,
            onAddInstruction = plannerViewModel::addInstruction,
            onUpdateInstructionText = plannerViewModel::updateInstructionText,
            onUpdateInstructionBallCount = plannerViewModel::updateInstructionBallCount,
            onMoveInstructionUp = plannerViewModel::moveInstructionUp,
            onMoveInstructionDown = plannerViewModel::moveInstructionDown,
            onRemoveInstruction = plannerViewModel::removeInstruction,
            onSave = plannerViewModel::saveUnit,
        )
    }

    val sessionActions = remember(plannerViewModel) {
        SessionEditorActions(
            onBeginNew = plannerViewModel::beginNewSession,
            onEdit = plannerViewModel::editSession,
            onDelete = plannerViewModel::deleteSession,
            onDuplicate = plannerViewModel::duplicateSession,
            onConsumeSavedId = plannerViewModel::consumeSavedSessionId,
            onClearDuplicatedId = plannerViewModel::clearDuplicatedSessionId,
            onUpdateName = plannerViewModel::updateSessionName,
            onUpdateNotes = plannerViewModel::updateSessionNotes,
            onAddItem = plannerViewModel::addSessionItem,
            onUpdateItemUnit = plannerViewModel::updateSessionItemUnit,
            onUpdateItemRepeatCount = plannerViewModel::updateSessionItemRepeatCount,
            onUpdateItemClubReference = plannerViewModel::updateSessionItemClubReference,
            onUpdateItemNotes = plannerViewModel::updateSessionItemNotes,
            onUpdateItemFocusCue = plannerViewModel::updateSessionItemFocusCue,
            onMoveItem = plannerViewModel::moveSessionItem,
            onRemoveItem = plannerViewModel::removeSessionItem,
            onSave = plannerViewModel::saveSession,
        )
    }

    val settingsActions = remember(settingsViewModel, authViewModel) {
        SettingsActions(
            onSignOut = authViewModel::signOut,
            onSetThemeMode = settingsViewModel::setThemeMode,
            onSelectDistanceUnit = settingsViewModel::selectDistanceUnit,
            onSelectSpeedUnit = settingsViewModel::selectSpeedUnit,
            onSetClubEnabled = settingsViewModel::setClubEnabled,
        )
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
                        unitActions = unitActions,
                        sessionActions = sessionActions,
                        settingsActions = settingsActions,
                        onRefreshPlanning = plannerViewModel::refreshPlanning,
                        onRefreshPlanningOnNavigation = plannerViewModel::refreshPlanningOnNavigation,
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
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.actionInProgress && uiState.environment.isAuthConfigured,
                onClick = onSignIn,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign in with Google")
            }
            if (uiState.actionInProgress || uiState.authState is AuthState.Restoring) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAppShell(
    authUiState: AuthUiState,
    plannerUiState: PracticePlannerUiState,
    settingsUiState: SettingsUiState,
    unitActions: UnitEditorActions,
    sessionActions: SessionEditorActions,
    settingsActions: SettingsActions,
    onRefreshPlanning: () -> Unit,
    onRefreshPlanningOnNavigation: () -> Unit,
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

    LaunchedEffect(plannerUiState.status) {
        val s = plannerUiState.status
        if (s?.showAsSnackbar == true && s is PlannerStatus.Notification) {
            snackbarHostState.showSnackbar(s.text)
        }
    }

    LaunchedEffect(plannerUiState.duplicatedSessionId) {
        plannerUiState.duplicatedSessionId?.let { sessionId ->
            sessionActions.onClearDuplicatedId()
            shellNavController.navigate(RangeworkRoutes.sessionEdit(sessionId))
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute.shouldRefreshPlanningOnEnter()) {
            onRefreshPlanningOnNavigation()
        }
    }

    val isOnEditorRoute = currentRoute.isEditorRoute()
    val isCurrentEditorDirty = when (currentRoute.editorType()) {
        EditorType.Unit -> plannerUiState.isUnitEditorDirty
        EditorType.Session -> plannerUiState.isSessionEditorDirty
        null -> false
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingNavigation by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun attemptLeave(navigate: () -> Unit) {
        if (isCurrentEditorDirty) {
            pendingNavigation = navigate
            showDiscardDialog = true
        } else {
            navigate()
        }
    }

    BackHandler(enabled = isOnEditorRoute && isCurrentEditorDirty) {
        pendingNavigation = { shellNavController.popBackStack() }
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
                pendingNavigation = null
            },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved changes will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        unitActions.onClearBaselines()
                        pendingNavigation?.invoke()
                        pendingNavigation = null
                    },
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    pendingNavigation = null
                }) {
                    Text("Keep editing")
                }
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            when (currentRoute) {
                RangeworkRoutes.Units -> FloatingActionButton(
                    onClick = {
                        unitActions.onBeginNew()
                        shellNavController.navigate(RangeworkRoutes.UnitCreate)
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New unit")
                }
                RangeworkRoutes.Sessions -> if (plannerUiState.units.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            sessionActions.onBeginNew()
                            shellNavController.navigate(RangeworkRoutes.SessionCreate)
                        },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New session")
                    }
                }
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = titleForRoute(currentRoute, plannerUiState))
                },
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = { attemptLeave { shellNavController.popBackStack() } }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    } else {
                        BrandWordmark(modifier = Modifier.padding(start = 12.dp))
                    }
                },
            )
        },
        bottomBar = {
            if (navigationType == RangeworkNavigationType.BottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = navBackStackEntry?.destination.isRouteSelected(destination.route),
                            onClick = {
                                attemptLeave {
                                    shellNavController.navigate(destination.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(shellNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
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
                                attemptLeave {
                                    shellNavController.navigate(destination.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(shellNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
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
                NavHost(
                    navController = shellNavController,
                    startDestination = RangeworkRoutes.Overview,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(RangeworkRoutes.Overview) {
                        OverviewScreen(
                            authUiState = authUiState,
                            plannerUiState = plannerUiState,
                            isExpandedLayout = navigationType == RangeworkNavigationType.NavigationRail,
                            onCreateUnit = {
                                unitActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.UnitCreate)
                            },
                            onCreateSession = {
                                sessionActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.SessionCreate)
                            },
                        )
                    }
                    composable(RangeworkRoutes.Units) {
                        UnitListScreen(
                            plannerUiState = plannerUiState,
                            onRefresh = onRefreshPlanning,
                            onCreateUnit = {
                                unitActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.UnitCreate)
                            },
                            onViewUnit = { unitId ->
                                shellNavController.navigate(RangeworkRoutes.unitDetail(unitId))
                            },
                            onEditUnit = { unitId ->
                                unitActions.onEdit(unitId)
                                shellNavController.navigate(RangeworkRoutes.unitEdit(unitId))
                            },
                            onDeleteUnit = unitActions.onDelete,
                        )
                    }
                    composable(RangeworkRoutes.UnitCreate) {
                        LaunchedEffect(Unit) {
                            unitActions.onBeginNew()
                        }
                        LaunchedEffect(plannerUiState.savedUnitId) {
                            plannerUiState.savedUnitId?.let { unitId ->
                                unitActions.onConsumeSavedId()
                                shellNavController.navigate(RangeworkRoutes.unitDetail(unitId)) {
                                    popUpTo(RangeworkRoutes.UnitCreate) { inclusive = true }
                                }
                            }
                        }
                        UnitEditorScreen(
                            plannerUiState = plannerUiState,
                            title = "New unit",
                            onSaveUnit = unitActions.onSave,
                            onUpdateTitle = unitActions.onUpdateTitle,
                            onUpdateNotes = unitActions.onUpdateNotes,
                            onUpdateFocus = unitActions.onUpdateFocus,
                            onUpdateDefaultClubReference = unitActions.onUpdateDefaultClubReference,
                            onAddInstruction = unitActions.onAddInstruction,
                            onUpdateInstructionText = unitActions.onUpdateInstructionText,
                            onUpdateInstructionBallCount = unitActions.onUpdateInstructionBallCount,
                            onMoveInstructionUp = unitActions.onMoveInstructionUp,
                            onMoveInstructionDown = unitActions.onMoveInstructionDown,
                            onRemoveInstruction = unitActions.onRemoveInstruction,
                        )
                    }
                    composable(RangeworkRoutes.UnitDetail) { backStackEntry ->
                        val unitId = backStackEntry.arguments?.getString(UnitIdArg).orEmpty()
                        UnitDetailScreen(
                            plannerUiState = plannerUiState,
                            unitId = unitId,
                            onCreateUnit = {
                                unitActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.UnitCreate)
                            },
                            onEditUnit = {
                                unitActions.onEdit(unitId)
                                shellNavController.navigate(RangeworkRoutes.unitEdit(unitId))
                            },
                            onDeleteUnit = {
                                unitActions.onDelete(unitId)
                                shellNavController.popBackStack()
                            },
                        )
                    }
                    composable(RangeworkRoutes.UnitEdit) { backStackEntry ->
                        val unitId = backStackEntry.arguments?.getString(UnitIdArg).orEmpty()
                        LaunchedEffect(unitId) {
                            unitActions.onEdit(unitId)
                        }
                        LaunchedEffect(plannerUiState.savedUnitId) {
                            plannerUiState.savedUnitId?.let { savedUnitId ->
                                unitActions.onConsumeSavedId()
                                shellNavController.navigate(RangeworkRoutes.unitDetail(savedUnitId)) {
                                    popUpTo(RangeworkRoutes.UnitEdit) { inclusive = true }
                                }
                            }
                        }
                        UnitEditorScreen(
                            plannerUiState = plannerUiState,
                            title = "Edit unit",
                            onSaveUnit = unitActions.onSave,
                            onUpdateTitle = unitActions.onUpdateTitle,
                            onUpdateNotes = unitActions.onUpdateNotes,
                            onUpdateFocus = unitActions.onUpdateFocus,
                            onUpdateDefaultClubReference = unitActions.onUpdateDefaultClubReference,
                            onAddInstruction = unitActions.onAddInstruction,
                            onUpdateInstructionText = unitActions.onUpdateInstructionText,
                            onUpdateInstructionBallCount = unitActions.onUpdateInstructionBallCount,
                            onMoveInstructionUp = unitActions.onMoveInstructionUp,
                            onMoveInstructionDown = unitActions.onMoveInstructionDown,
                            onRemoveInstruction = unitActions.onRemoveInstruction,
                        )
                    }
                    composable(RangeworkRoutes.Sessions) {
                        SessionListScreen(
                            plannerUiState = plannerUiState,
                            onRefresh = onRefreshPlanning,
                            onCreateSession = {
                                sessionActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.SessionCreate)
                            },
                            onViewSession = { sessionId ->
                                shellNavController.navigate(RangeworkRoutes.sessionDetail(sessionId))
                            },
                            onEditSession = { sessionId ->
                                sessionActions.onEdit(sessionId)
                                shellNavController.navigate(RangeworkRoutes.sessionEdit(sessionId))
                            },
                            onDeleteSession = sessionActions.onDelete,
                            onDuplicateSession = sessionActions.onDuplicate,
                        )
                    }
                    composable(RangeworkRoutes.SessionCreate) {
                        LaunchedEffect(Unit) {
                            sessionActions.onBeginNew()
                        }
                        LaunchedEffect(plannerUiState.savedSessionId) {
                            plannerUiState.savedSessionId?.let { sessionId ->
                                sessionActions.onConsumeSavedId()
                                shellNavController.navigate(RangeworkRoutes.sessionDetail(sessionId)) {
                                    popUpTo(RangeworkRoutes.SessionCreate) { inclusive = true }
                                }
                            }
                        }
                        SessionEditorScreen(
                            plannerUiState = plannerUiState,
                            title = "New session",
                            onSaveSession = sessionActions.onSave,
                            onUpdateSessionName = sessionActions.onUpdateName,
                            onUpdateSessionNotes = sessionActions.onUpdateNotes,
                            onAddSessionItem = sessionActions.onAddItem,
                            onUpdateSessionItemUnit = sessionActions.onUpdateItemUnit,
                            onUpdateSessionItemRepeatCount = sessionActions.onUpdateItemRepeatCount,
                            onUpdateSessionItemClubReference = sessionActions.onUpdateItemClubReference,
                            onUpdateSessionItemNotes = sessionActions.onUpdateItemNotes,
                            onUpdateSessionItemFocusCue = sessionActions.onUpdateItemFocusCue,
                            onMoveSessionItem = sessionActions.onMoveItem,
                            onRemoveSessionItem = sessionActions.onRemoveItem,
                        )
                    }
                    composable(RangeworkRoutes.SessionDetail) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getString(SessionIdArg).orEmpty()
                        SessionDetailScreen(
                            plannerUiState = plannerUiState,
                            sessionId = sessionId,
                            onCreateSession = {
                                sessionActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.SessionCreate)
                            },
                            onEditSession = {
                                sessionActions.onEdit(sessionId)
                                shellNavController.navigate(RangeworkRoutes.sessionEdit(sessionId))
                            },
                            onDeleteSession = {
                                sessionActions.onDelete(sessionId)
                                shellNavController.popBackStack()
                            },
                        )
                    }
                    composable(RangeworkRoutes.SessionEdit) { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getString(SessionIdArg).orEmpty()
                        LaunchedEffect(sessionId) {
                            sessionActions.onEdit(sessionId)
                        }
                        LaunchedEffect(plannerUiState.savedSessionId) {
                            plannerUiState.savedSessionId?.let { savedSessionId ->
                                sessionActions.onConsumeSavedId()
                                shellNavController.navigate(RangeworkRoutes.sessionDetail(savedSessionId)) {
                                    popUpTo(RangeworkRoutes.SessionEdit) { inclusive = true }
                                }
                            }
                        }
                        SessionEditorScreen(
                            plannerUiState = plannerUiState,
                            title = "Edit session",
                            onSaveSession = sessionActions.onSave,
                            onUpdateSessionName = sessionActions.onUpdateName,
                            onUpdateSessionNotes = sessionActions.onUpdateNotes,
                            onAddSessionItem = sessionActions.onAddItem,
                            onUpdateSessionItemUnit = sessionActions.onUpdateItemUnit,
                            onUpdateSessionItemRepeatCount = sessionActions.onUpdateItemRepeatCount,
                            onUpdateSessionItemClubReference = sessionActions.onUpdateItemClubReference,
                            onUpdateSessionItemNotes = sessionActions.onUpdateItemNotes,
                            onUpdateSessionItemFocusCue = sessionActions.onUpdateItemFocusCue,
                            onMoveSessionItem = sessionActions.onMoveItem,
                            onRemoveSessionItem = sessionActions.onRemoveItem,
                        )
                    }
                    composable(RangeworkRoutes.Settings) {
                        SettingsScreen(
                            authUiState = authUiState,
                            settingsUiState = settingsUiState,
                            onSignOut = settingsActions.onSignOut,
                            onSetThemeMode = settingsActions.onSetThemeMode,
                            onSelectDistanceUnit = settingsActions.onSelectDistanceUnit,
                            onSelectSpeedUnit = settingsActions.onSelectSpeedUnit,
                            onSetClubEnabled = settingsActions.onSetClubEnabled,
                        )
                    }
                }
            }
        }
    }
}

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

internal fun titleForRoute(route: String, plannerUiState: PracticePlannerUiState): String {
    if (route.startsWith("sessions/") && !route.endsWith("/edit")) {
        val id = route.removePrefix("sessions/")
        plannerUiState.sessions.firstOrNull { it.id == id }?.name?.let { return it }
    }
    if (route.startsWith("units/") && !route.endsWith("/edit")) {
        val id = route.removePrefix("units/")
        plannerUiState.units.firstOrNull { it.id == id }?.title?.let { return it }
    }
    return titleForRoute(route)
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

private fun NavDestination?.isRouteSelected(route: String): Boolean = this?.hierarchy?.any { destination ->
    val destinationRoute = destination.route ?: return@any false
    destinationRoute == route || destinationRoute.startsWith("$route/")
} == true
