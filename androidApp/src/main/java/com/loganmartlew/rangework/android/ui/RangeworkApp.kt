package com.loganmartlew.rangework.android.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import com.loganmartlew.rangework.android.auth.AndroidGoogleIdTokenProvider
import com.loganmartlew.rangework.android.config.baselineAndroidAppAuthConfig
import com.loganmartlew.rangework.android.ui.components.BrandMarkContainer
import com.loganmartlew.rangework.android.ui.components.BrandWordmark
import com.loganmartlew.rangework.android.ui.components.GoogleSignInButton
import com.loganmartlew.rangework.android.ui.components.RangeworkExtendedFab
import com.loganmartlew.rangework.android.ui.components.RangeworkFab
import com.loganmartlew.rangework.android.ui.components.DeleteConfirmationDialog
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.OverflowMenu
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.android.ui.components.showUndoSnackbar
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.android.ui.screens.OverviewScreen
import com.loganmartlew.rangework.android.ui.screens.SessionDetailScreen
import com.loganmartlew.rangework.android.ui.screens.SessionEditorScreen
import com.loganmartlew.rangework.android.ui.screens.SessionListScreen
import com.loganmartlew.rangework.android.ui.screens.ManageClubsScreen
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
    val dynamicColor = settingsUiState.dynamicColor

    val unitActions = remember(plannerViewModel) {
        UnitEditorActions(
            onBeginNew = plannerViewModel::beginNewUnit,
            onEdit = plannerViewModel::editUnit,
            onDelete = plannerViewModel::deleteUnit,
            onDuplicate = plannerViewModel::duplicateUnit,
            onClearDuplicatedId = plannerViewModel::clearDuplicatedUnitId,
            onClearBaselines = plannerViewModel::clearEditorBaselines,
            onConsumeSavedId = plannerViewModel::consumeSavedUnitId,
            onUpdateTitle = plannerViewModel::updateUnitTitle,
            onUpdateNotes = plannerViewModel::updateUnitNotes,
            onUpdateFocus = plannerViewModel::updateUnitFocus,
            onUpdateDefaultClubReference = plannerViewModel::updateUnitDefaultClubReference,
            onAddInstruction = plannerViewModel::addInstruction,
            onUpdateInstructionText = plannerViewModel::updateInstructionText,
            onUpdateInstructionBallCount = { i, v -> plannerViewModel.updateInstructionBallCount(i, v) },
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
            onUpdateItemRepeatCount = { i, v -> plannerViewModel.updateSessionItemRepeatCount(i, v) },
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
            onToggleDynamicColor = settingsViewModel::toggleDynamicColor,
            onSelectDistanceUnit = settingsViewModel::selectDistanceUnit,
            onSelectSpeedUnit = settingsViewModel::selectSpeedUnit,
            onSetClubEnabled = settingsViewModel::setClubEnabled,
            onEnableCommonBag = settingsViewModel::enableCommonBag,
            onDisableAllClubs = settingsViewModel::disableAllClubs,
        )
    }

    RangeworkTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
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
                        onRestoreUnit = plannerViewModel::restoreUnit,
                        onRestoreSession = plannerViewModel::restoreSession,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1.2f))
            BrandMarkContainer(size = 84.dp, markSize = 60.dp, twoColor = true)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = bootstrapMessage.headline,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = bootstrapMessage.detail,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1.6f))
            GoogleSignInButton(
                onClick = onSignIn,
                enabled = !uiState.actionInProgress && uiState.environment.isAuthConfigured,
            )
            if (uiState.actionInProgress || uiState.authState is AuthState.Restoring) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(16.dp))
            LegalLine()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LegalLine() {
    val linkColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = textColor)) {
            append("By continuing you agree to the ")
        }
        pushStringAnnotation("TERMS", "terms")
        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
            append("Terms")
        }
        pop()
        withStyle(SpanStyle(color = textColor)) {
            append(" & ")
        }
        pushStringAnnotation("PRIVACY", "privacy")
        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
            append("Privacy Policy")
        }
        pop()
    }
    ClickableText(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
        onClick = { /* Terms and Privacy URLs to be wired when policies are published */ },
    )
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
    onRestoreUnit: (PracticeUnit) -> Unit,
    onRestoreSession: (PracticeSession) -> Unit,
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

    // Delete dialog state for unit and session
    var showUnitDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteUnit by remember { mutableStateOf<PracticeUnit?>(null) }
    var showSessionDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteSession by remember { mutableStateOf<PracticeSession?>(null) }

    // Undo state: entity captured just before delete so it can be restored
    var justDeletedUnit by remember { mutableStateOf<PracticeUnit?>(null) }
    var justDeletedSession by remember { mutableStateOf<PracticeSession?>(null) }

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

    LaunchedEffect(plannerUiState.duplicatedUnitId) {
        plannerUiState.duplicatedUnitId?.let { unitId ->
            unitActions.onClearDuplicatedId()
            shellNavController.navigate(RangeworkRoutes.unitEdit(unitId))
        }
    }

    LaunchedEffect(justDeletedUnit) {
        val unit = justDeletedUnit ?: return@LaunchedEffect
        snackbarHostState.showUndoSnackbar(
            message = "Deleted \"${unit.title}\"",
            onRestore = { onRestoreUnit(unit) },
        )
        justDeletedUnit = null
    }

    LaunchedEffect(justDeletedSession) {
        val session = justDeletedSession ?: return@LaunchedEffect
        snackbarHostState.showUndoSnackbar(
            message = "Deleted \"${session.name}\"",
            onRestore = { onRestoreSession(session) },
        )
        justDeletedSession = null
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute.shouldRefreshPlanningOnEnter()) {
            onRefreshPlanningOnNavigation()
        }
    }

    // Derive the entity ID when on a detail route (not edit) for app-bar actions
    val currentUnitId: String? = if (currentRoute.startsWith("units/") && !currentRoute.endsWith("/edit")) {
        navBackStackEntry?.arguments?.getString(UnitIdArg)
    } else null
    val currentSessionId: String? = if (currentRoute.startsWith("sessions/") && !currentRoute.endsWith("/edit")) {
        navBackStackEntry?.arguments?.getString(SessionIdArg)
    } else null

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

    if (showUnitDeleteDialog) {
        DeleteConfirmationDialog(
            itemName = pendingDeleteUnit?.title ?: "unit",
            onConfirm = {
                showUnitDeleteDialog = false
                val unit = pendingDeleteUnit
                pendingDeleteUnit = null
                if (unit != null) {
                    unitActions.onDelete(unit.id)
                    shellNavController.popBackStack()
                    justDeletedUnit = unit
                }
            },
            onDismiss = {
                showUnitDeleteDialog = false
                pendingDeleteUnit = null
            },
        )
    }

    if (showSessionDeleteDialog) {
        DeleteConfirmationDialog(
            itemName = pendingDeleteSession?.name ?: "session",
            onConfirm = {
                showSessionDeleteDialog = false
                val session = pendingDeleteSession
                pendingDeleteSession = null
                if (session != null) {
                    sessionActions.onDelete(session.id)
                    shellNavController.popBackStack()
                    justDeletedSession = session
                }
            },
            onDismiss = {
                showSessionDeleteDialog = false
                pendingDeleteSession = null
            },
        )
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
                RangeworkRoutes.Units -> {
                    val unitsEmpty = plannerUiState.units.isEmpty()
                    if (unitsEmpty) {
                        RangeworkExtendedFab(
                            onClick = {
                                unitActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.UnitCreate)
                            },
                            text = "New unit",
                        )
                    } else {
                        RangeworkFab(
                            onClick = {
                                unitActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.UnitCreate)
                            },
                            contentDescription = "New unit",
                        )
                    }
                }
                RangeworkRoutes.Sessions -> if (plannerUiState.units.isNotEmpty()) {
                    val sessionsEmpty = plannerUiState.sessions.isEmpty()
                    if (sessionsEmpty) {
                        RangeworkExtendedFab(
                            onClick = {
                                sessionActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.SessionCreate)
                            },
                            text = "New session",
                        )
                    } else {
                        RangeworkFab(
                            onClick = {
                                sessionActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.SessionCreate)
                            },
                            contentDescription = "New session",
                        )
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
                actions = {
                    currentUnitId?.let { unitId ->
                        val unit = plannerUiState.units.firstOrNull { it.id == unitId }
                        IconButton(
                            onClick = {
                                unitActions.onEdit(unitId)
                                shellNavController.navigate(RangeworkRoutes.unitEdit(unitId))
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit ${unit?.title ?: "unit"}",
                            )
                        }
                        Box {
                            OverflowMenu(
                                contentDescription = "More options for ${unit?.title ?: "unit"}",
                                onDuplicate = { unitActions.onDuplicate(unitId) },
                                onDelete = {
                                    pendingDeleteUnit = unit
                                    showUnitDeleteDialog = true
                                },
                            )
                        }
                    }
                    currentSessionId?.let { sessionId ->
                        val session = plannerUiState.sessions.firstOrNull { it.id == sessionId }
                        IconButton(
                            onClick = {
                                sessionActions.onEdit(sessionId)
                                shellNavController.navigate(RangeworkRoutes.sessionEdit(sessionId))
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit ${session?.name ?: "session"}",
                            )
                        }
                        Box {
                            OverflowMenu(
                                contentDescription = "More options for ${session?.name ?: "session"}",
                                onDuplicate = { sessionActions.onDuplicate(sessionId) },
                                onDelete = {
                                    pendingDeleteSession = session
                                    showSessionDeleteDialog = true
                                },
                            )
                        }
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
                            onNavigateToUnits = {
                                shellNavController.navigate(RangeworkRoutes.Units) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(shellNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                }
                            },
                            onNavigateToSessions = {
                                shellNavController.navigate(RangeworkRoutes.Sessions) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(shellNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                }
                            },
                            onNavigateToUnitDetail = { unitId ->
                                shellNavController.navigate(RangeworkRoutes.unitDetail(unitId))
                            },
                            onNavigateToSessionDetail = { sessionId ->
                                shellNavController.navigate(RangeworkRoutes.sessionDetail(sessionId))
                            },
                            onCreateUnit = {
                                unitActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.UnitCreate)
                            },
                            onCreateSession = {
                                sessionActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.SessionCreate)
                            },
                            onEditUnit = { unitId ->
                                unitActions.onEdit(unitId)
                                shellNavController.navigate(RangeworkRoutes.unitEdit(unitId))
                            },
                            onEditSession = { sessionId ->
                                sessionActions.onEdit(sessionId)
                                shellNavController.navigate(RangeworkRoutes.sessionEdit(sessionId))
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
                            onDuplicateUnit = unitActions.onDuplicate,
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
                            onGoToUnits = {
                                shellNavController.navigate(RangeworkRoutes.Units) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(shellNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                }
                            },
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
                            onNavigateToCreateUnit = {
                                unitActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.UnitCreate)
                            },
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
                            onNavigateToCreateUnit = {
                                unitActions.onBeginNew()
                                shellNavController.navigate(RangeworkRoutes.UnitCreate)
                            },
                        )
                    }
                    composable(RangeworkRoutes.Settings) {
                        SettingsScreen(
                            authUiState = authUiState,
                            settingsUiState = settingsUiState,
                            onSignOut = settingsActions.onSignOut,
                            onSetThemeMode = settingsActions.onSetThemeMode,
                            onToggleDynamicColor = settingsActions.onToggleDynamicColor,
                            onSelectDistanceUnit = settingsActions.onSelectDistanceUnit,
                            onSelectSpeedUnit = settingsActions.onSelectSpeedUnit,
                            onNavigateToManageClubs = {
                                shellNavController.navigate(RangeworkRoutes.ManageClubs)
                            },
                        )
                    }
                    composable(RangeworkRoutes.ManageClubs) {
                        ManageClubsScreen(
                            settingsUiState = settingsUiState,
                            onSetClubEnabled = settingsActions.onSetClubEnabled,
                            onEnableCommonBag = settingsActions.onEnableCommonBag,
                            onDisableAllClubs = settingsActions.onDisableAllClubs,
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
    route == RangeworkRoutes.ManageClubs -> "Club bag"
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
