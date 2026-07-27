package com.example.drift

import android.os.Bundle
import android.content.Intent
import android.app.TimePickerDialog
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drift.ui.theme.DriftTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.drift.data.auth.AuthRepository
import com.example.drift.data.auth.DriftAuthState
import com.example.drift.data.auth.OAuthOrigin
import com.example.drift.data.auth.SignupOutcome
import com.example.drift.data.profile.OnboardingDraft
import com.example.drift.data.profile.ProfileRepository
import com.example.drift.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private val authDeepLinkError = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthIntent(intent)
        enableEdgeToEdge()

        setContent {
            val appearancePreferences = remember { getSharedPreferences("appearance", MODE_PRIVATE) }
            var darkMode by remember { mutableStateOf(appearancePreferences.getBoolean("dark_mode", false)) }
            DriftTheme(darkTheme = darkMode) {
                // The platform-backed ripple can crash on some Samsung devices when a
                // navigation click removes its button before the first ripple frame.
                RippleSafeContent {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                    var currentScreen by remember { mutableStateOf("auth_loading") }
                    var instagramBudget by remember { mutableStateOf(45) }
                    var youtubeBudget by remember { mutableStateOf(40) }
                    var browserBudget by remember { mutableStateOf(60) }
                    var lastFocusSeconds by remember { mutableStateOf(0) }
                    var lastBreakSeconds by remember { mutableStateOf(0) }
                    var focusRemainingSeconds by remember { mutableStateOf(40 * 60) }
                    var focusSessionStarted by remember { mutableStateOf(false) }
                    var pendingVerificationEmail by remember { mutableStateOf("") }
                    var signupVerificationRequired by remember { mutableStateOf(false) }
                    var onboardingDraft by remember { mutableStateOf(OnboardingDraft()) }
                    var onboardingEditReturn by remember { mutableStateOf<String?>(null) }
                    var profileName by remember { mutableStateOf("") }
                    var profileEmail by remember { mutableStateOf("") }
                    var profileLoadError by remember { mutableStateOf<String?>(null) }
                    var onboardingSaveError by remember { mutableStateOf<String?>(null) }
                    var onboardingSaving by remember { mutableStateOf(false) }
                    val authState by AuthRepository.authState.collectAsState()
                    val passwordRecoveryReady by
                        AuthRepository.passwordRecoveryReady.collectAsState()
                    val googleSignInDestination by
                        AuthRepository.googleSignInDestination.collectAsState()
                    val appScope = rememberCoroutineScope()

                    LaunchedEffect(authDeepLinkError.value) {
                        if (authDeepLinkError.value != null) {
                            currentScreen = "login"
                        }
                    }

                    LaunchedEffect(Unit) {
                        AuthRepository.initialize()
                    }

                    LaunchedEffect(authState) {
                        when (authState) {
                            DriftAuthState.Loading -> Unit
                            DriftAuthState.PasswordRecovery -> {
                                currentScreen = "reset_password"
                            }
                            DriftAuthState.Authenticated -> {
                                if (currentScreen in setOf(
                                        "auth_loading",
                                        "welcome"
                                    )
                                ) {
                                    currentScreen = "profile_loading"
                                }
                            }
                            DriftAuthState.SignedOut -> {
                                onboardingEditReturn = null
                                onboardingDraft = OnboardingDraft()
                                profileName = ""
                                profileEmail = ""
                                if (currentScreen == "auth_loading" ||
                                    currentScreen !in setOf(
                                        "welcome",
                                        "login",
                                        "signup",
                                        "verify"
                                    )
                                ) {
                                    currentScreen = "welcome"
                                }
                            }
                        }
                    }

                    LaunchedEffect(passwordRecoveryReady) {
                        if (passwordRecoveryReady) {
                            currentScreen = "reset_password"
                        }
                    }

                    LaunchedEffect(googleSignInDestination) {
                        if (googleSignInDestination != null) {
                            currentScreen = "profile_loading"
                            AuthRepository.consumeGoogleSignInDestination()
                        }
                    }

                    LaunchedEffect(currentScreen) {
                        if (currentScreen == "profile_loading") {
                            onboardingEditReturn = null
                            onboardingDraft = OnboardingDraft()
                            profileLoadError = null
                            profileEmail = AuthRepository.currentEmail()
                            ProfileRepository.loadCurrentProfile()
                                .onSuccess { profile ->
                                    profile?.let {
                                        profileName = it.fullName
                                        onboardingDraft = OnboardingDraft(
                                            ageRange = it.ageRange,
                                            gender = it.gender,
                                            role = it.role,
                                            otherRole = it.otherRole,
                                            managing = it.managing.toSet(),
                                            academicYear = it.academicYear,
                                            course = it.course,
                                            studyStart = it.studyStart.ifBlank { "7:00 PM" },
                                            studyEnd = it.studyEnd.ifBlank { "10:00 PM" },
                                            studyDays = it.studyDays.toSet().ifEmpty {
                                                setOf("Mon", "Wed", "Fri")
                                            },
                                            windDownStart = it.windDownStart.ifBlank {
                                                "10:30 PM"
                                            },
                                            windDownEnd = it.windDownEnd.ifBlank {
                                                "06:30 AM"
                                            },
                                            windDownEnabled = it.windDownEnabled
                                        )
                                    }
                                    currentScreen = if (profile?.onboardingCompleted == true) {
                                        "dashboard"
                                    } else {
                                        "onboarding_intro"
                                    }
                                }
                                .onFailure { error ->
                                    profileLoadError = profileErrorMessage(error)
                                    currentScreen = "profile_error"
                                }
                        }
                    }

                    when (currentScreen) {
                        "auth_loading" -> AuthLoadingScreen()
                        "profile_loading" -> ProfileLoadingScreen()
                        "profile_error" -> ProfileLoadErrorScreen(
                            message = profileLoadError
                                ?: "We couldn’t load your profile. Please try again.",
                            onRetry = { currentScreen = "profile_loading" },
                            onLogOut = {
                                appScope.launch {
                                    AuthRepository.signOut()
                                }
                            }
                        )

                        "reset_password" -> ResetPasswordScreen(
                            onUpdatePassword = AuthRepository::updateRecoveredPassword,
                            onPasswordUpdated = { currentScreen = "login" },
                            onCancel = {
                                appScope.launch {
                                    AuthRepository.cancelPasswordRecovery()
                                    currentScreen = "login"
                                }
                            }
                        )

                        "login" -> LoginScreen(
                            onBack = { currentScreen = "welcome" },
                            onSignUpClick = { currentScreen = "signup" },
                            onLoginClick = AuthRepository::signIn,
                            onForgotPasswordClick = AuthRepository::sendPasswordReset,
                            onGoogleClick = {
                                authDeepLinkError.value = null
                                AuthRepository.signInWithGoogle(OAuthOrigin.Login)
                            },
                            externalError = authDeepLinkError.value,
                            onLoginSuccess = { currentScreen = "profile_loading" }
                        )

                        "signup" -> SignupScreen(
                            onBack = { currentScreen = "welcome" },
                            onSignUpClick = AuthRepository::signUp,
                            onGoogleClick = {
                                authDeepLinkError.value = null
                                AuthRepository.signInWithGoogle(OAuthOrigin.Signup)
                            },
                            onSignUpSuccess = { email, outcome ->
                                onboardingEditReturn = null
                                onboardingDraft = OnboardingDraft()
                                profileName = ""
                                profileEmail = email
                                onboardingSaveError = null
                                onboardingSaving = false
                                pendingVerificationEmail = email
                                signupVerificationRequired =
                                    outcome == SignupOutcome.VerificationRequired
                                currentScreen = when (outcome) {
                                    SignupOutcome.VerificationRequired -> "verify"
                                    SignupOutcome.Authenticated -> "onboarding_intro"
                                }
                            },
                            onLoginClick = { currentScreen = "login" }
                        )

                        "verify" -> VerifyEmailScreen(
                            onBack = { currentScreen = "signup" },
                            email = pendingVerificationEmail,
                            onVerifyClick = AuthRepository::verifySignupEmail,
                            onVerifySuccess = {
                                onboardingEditReturn = null
                                onboardingDraft = OnboardingDraft()
                                currentScreen = "onboarding_intro"
                            },
                            onResendClick = AuthRepository::resendSignupCode
                        )

                        "onboarding_intro" -> OnboardingIntroScreen(
                            onBack = {
                                currentScreen =
                                    if (signupVerificationRequired) "verify" else "signup"
                            },
                            onContinue = { currentScreen = "about_you" }
                        )

                        "about_you" -> AboutYouScreen(
                            onBack = {
                                currentScreen = onboardingEditReturn ?: "onboarding_intro"
                            },
                            draft = onboardingDraft,
                            isEditing = onboardingEditReturn != null,
                            onContinue = { ageRange, gender ->
                                val updatedDraft = onboardingDraft.copy(
                                    ageRange = ageRange,
                                    gender = gender
                                )
                                onboardingDraft = updatedDraft
                                if (onboardingEditReturn != null) {
                                    appScope.launch {
                                        ProfileRepository.saveOnboarding(updatedDraft)
                                            .onSuccess {
                                                currentScreen = onboardingEditReturn ?: "profile"
                                                onboardingEditReturn = null
                                            }
                                    }
                                } else currentScreen = "role"
                            }
                        )

                        "role" -> RoleScreen(
                            onBack = { currentScreen = onboardingEditReturn ?: "about_you" },
                            draft = onboardingDraft,
                            isEditing = onboardingEditReturn != null,
                            onContinue = { role, otherRole ->
                                val updatedDraft = onboardingDraft.copy(
                                    role = role,
                                    otherRole = otherRole
                                )
                                onboardingDraft = updatedDraft
                                if (onboardingEditReturn != null) {
                                    appScope.launch {
                                        ProfileRepository.saveOnboarding(updatedDraft)
                                            .onSuccess {
                                                currentScreen = onboardingEditReturn ?: "profile"
                                                onboardingEditReturn = null
                                            }
                                    }
                                } else currentScreen = "managing"
                            }
                        )

                        "managing" -> ManagingScreen(
                            onBack = { currentScreen = onboardingEditReturn ?: "role" },
                            draft = onboardingDraft,
                            isEditing = onboardingEditReturn != null,
                            onContinue = { managing ->
                                val updatedDraft = onboardingDraft.copy(managing = managing)
                                onboardingDraft = updatedDraft
                                if (onboardingEditReturn != null) {
                                    appScope.launch {
                                        ProfileRepository.saveOnboarding(updatedDraft)
                                            .onSuccess {
                                                currentScreen = onboardingEditReturn ?: "profile"
                                                onboardingEditReturn = null
                                            }
                                    }
                                } else currentScreen = "study_schedule"
                            }
                        )

                        "study_schedule" -> StudyScheduleScreen(
                            onBack = { currentScreen = onboardingEditReturn ?: "managing" },
                            draft = onboardingDraft,
                            isEditing = onboardingEditReturn != null,
                            onContinue = { start, end, days ->
                                val updatedDraft = onboardingDraft.copy(
                                    studyStart = start,
                                    studyEnd = end,
                                    studyDays = days
                                )
                                onboardingDraft = updatedDraft
                                if (onboardingEditReturn != null) {
                                    appScope.launch {
                                        ProfileRepository.saveOnboarding(updatedDraft)
                                            .onSuccess {
                                                currentScreen = onboardingEditReturn ?: "profile"
                                                onboardingEditReturn = null
                                            }
                                    }
                                } else currentScreen = "wind_down"
                            }
                        )

                        "wind_down" -> WindDownScreen(
                            onBack = { currentScreen = onboardingEditReturn ?: "study_schedule" },
                            draft = onboardingDraft,
                            isEditing = onboardingEditReturn != null,
                            isSaving = onboardingSaving,
                            saveError = onboardingSaveError,
                            onContinue = { start, end, enabled ->
                                val updatedDraft = onboardingDraft.copy(
                                    windDownStart = start,
                                    windDownEnd = end,
                                    windDownEnabled = enabled
                                )
                                onboardingDraft = updatedDraft
                                onboardingSaveError = null
                                onboardingSaving = true
                                appScope.launch {
                                    ProfileRepository.saveOnboarding(updatedDraft)
                                        .onSuccess {
                                            onboardingSaving = false
                                            val returnScreen = onboardingEditReturn
                                            onboardingEditReturn = null
                                            currentScreen = returnScreen ?: "profile_loading"
                                        }
                                        .onFailure { error ->
                                            onboardingSaving = false
                                            onboardingSaveError = profileErrorMessage(error)
                                        }
                                }
                            }
                        )

                        "dashboard" -> DashboardScreen(
                            userName = profileName,
                            onFocusClick = {
                                focusRemainingSeconds = 40 * 60
                                focusSessionStarted = false
                                currentScreen = "focus_timer"
                            },
                            onFocusScoreClick = { currentScreen = "focus" },
                            onBudgetClick = { currentScreen = "budget" },
                            onTasksClick = { currentScreen = "tasks" },
                            onInsightsClick = { currentScreen = "insights" },
                            onProfileClick = { currentScreen = "profile" },
                            onSettingsClick = { currentScreen = "settings" }
                        )

                        "focus" -> FocusScoreScreen(
                            onBack = { currentScreen = "dashboard" },
                            onBudgetClick = { currentScreen = "budget" },
                            onFocusTimerClick = {
                                focusRemainingSeconds = 40 * 60
                                focusSessionStarted = false
                                currentScreen = "focus_timer"
                            },
                            onTasksClick = { currentScreen = "tasks" },
                            onInsightsClick = { currentScreen = "insights" }
                        )

                        "budget" -> UsageBudgetScreen(
                            onBack = { currentScreen = "dashboard" },
                            onEditClick = { currentScreen = "edit_budget" },
                            onHomeClick = { currentScreen = "dashboard" },
                            onFocusClick = {
                                focusRemainingSeconds = 40 * 60
                                focusSessionStarted = false
                                currentScreen = "focus_timer"
                            },
                            onTasksClick = { currentScreen = "tasks" },
                            onInsightsClick = { currentScreen = "insights" },
                            instagramLimit = instagramBudget,
                            youtubeLimit = youtubeBudget,
                            browserLimit = browserBudget
                        )

                        "edit_budget" -> EditBudgetScreen(
                            onBack = { currentScreen = "budget" },
                            initialInstagram = instagramBudget,
                            initialYoutube = youtubeBudget,
                            initialBrowser = browserBudget,
                            onSave = { instagram, youtube, browser ->
                                instagramBudget = instagram
                                youtubeBudget = youtube
                                browserBudget = browser
                                currentScreen = "budget"
                            }
                        )

                        "tasks" -> TasksScreen(
                            onHomeClick = { currentScreen = "dashboard" },
                            onBudgetClick = { currentScreen = "budget" },
                            onFocusClick = {
                                focusRemainingSeconds = 40 * 60
                                focusSessionStarted = false
                                currentScreen = "focus_timer"
                            },
                            onInsightsClick = { currentScreen = "insights" }
                        )

                        "insights" -> InsightsScreen(
                            onHomeClick = { currentScreen = "dashboard" },
                            onBudgetClick = { currentScreen = "budget" },
                            onFocusClick = {
                                focusRemainingSeconds = 40 * 60
                                focusSessionStarted = false
                                currentScreen = "focus_timer"
                            },
                            onTasksClick = { currentScreen = "tasks" },
                        )

                        "intervention" -> InterventionScreen(
                            onBack = { currentScreen = "dashboard" },
                            onStartFocus = {
                                focusRemainingSeconds = 40 * 60
                                focusSessionStarted = true
                                lastFocusSeconds = 0
                                lastBreakSeconds = 0
                                currentScreen = "focus_timer"
                            }
                        )

                        "focus_timer" -> FocusTimerScreen(
                            onBack = {
                                focusRemainingSeconds = 40 * 60
                                focusSessionStarted = false
                                currentScreen = "dashboard"
                            },
                            sessionStarted = focusSessionStarted,
                            onStartSession = {
                                focusSessionStarted = true
                                lastFocusSeconds = 0
                                lastBreakSeconds = 0
                            },
                            remainingSeconds = focusRemainingSeconds,
                            onRemainingSecondsChange = { focusRemainingSeconds = it },
                            onTakeBreak = { elapsedSeconds ->
                                lastFocusSeconds = elapsedSeconds
                                currentScreen = "break_timer"
                            },
                            onSessionComplete = {
                                lastFocusSeconds = 40 * 60
                                focusSessionStarted = false
                                currentScreen = "session_complete"
                            }
                        )

                        "break_timer" -> BreakTimerScreen(
                            onBack = { currentScreen = "focus_timer" },
                            focusedSeconds = lastFocusSeconds,
                            onComplete = { elapsedSeconds ->
                                lastBreakSeconds = elapsedSeconds
                                currentScreen = "focus_timer"
                            }
                        )

                        "session_complete" -> SessionCompleteScreen(
                            onDone = { currentScreen = "dashboard" },
                            onBack = { currentScreen = "focus_timer" },
                            focusedSeconds = lastFocusSeconds,
                            breakSeconds = lastBreakSeconds
                        )

                        "settings" -> SettingsScreen(
                            onBack = { currentScreen = "dashboard" },
                            onEditOnboarding = {
                                onboardingEditReturn = "settings"
                                currentScreen = "study_schedule"
                            },
                            darkMode = darkMode,
                            onDarkModeChange = {
                                darkMode = it
                                appearancePreferences.edit().putBoolean("dark_mode", it).apply()
                            }
                        )

                        "profile" -> ProfileScreen(
                            onBack = { currentScreen = "dashboard" },
                            onEditAbout = {
                                onboardingEditReturn = "profile"
                                currentScreen = "about_you"
                            },
                            onEditRole = {
                                onboardingEditReturn = "profile"
                                currentScreen = "role"
                            },
                            onEditManaging = {
                                onboardingEditReturn = "profile"
                                currentScreen = "managing"
                            },
                            onEditSchedule = {
                                onboardingEditReturn = "profile"
                                currentScreen = "study_schedule"
                            },
                            onEditWindDown = {
                                onboardingEditReturn = "profile"
                                currentScreen = "wind_down"
                            },
                            onLogout = {
                                appScope.launch { AuthRepository.signOut() }
                            },
                            name = profileName,
                            email = profileEmail,
                            onboarding = onboardingDraft
                        )

                        else -> WelcomeScreen(
                            onLoginClick = { currentScreen = "login" },
                            onSignUpClick = { currentScreen = "signup" }
                        )
                    }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent) {
        if (SupabaseProvider.configurationError != null ||
            intent.data?.scheme != "drift"
        ) {
            return
        }
        val isPasswordRecovery = intent.data?.path == "/recovery" ||
                intent.data?.getQueryParameter("type") == "recovery" ||
                intent.data?.fragment
                    ?.split("&")
                    ?.any { it == "type=recovery" } == true
        val callbackError = intent.data?.getQueryParameter("error_description")
            ?: intent.data?.fragment
                ?.split("&")
                ?.firstOrNull { it.startsWith("error_description=") }
                ?.substringAfter("=")
        if (callbackError != null) {
            AuthRepository.cancelPendingGoogleSignIn()
            authDeepLinkError.value =
                "Google sign-in was cancelled or couldn't be completed. Please try again."
            return
        }
        SupabaseProvider.client.handleDeeplinks(
            intent = intent,
            onSessionSuccess = { session ->
                if (isPasswordRecovery) {
                    AuthRepository.markPasswordRecoveryReady()
                } else {
                    AuthRepository.completeGoogleSignIn(session)
                }
            },
            onError = {
                authDeepLinkError.value = if (isPasswordRecovery) {
                    "This password-reset link is invalid or expired. Request a new email."
                } else {
                    "We couldn't complete authentication. Please try again."
                }
            }
        )
    }
}

@Composable
private fun AuthLoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Drift",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "Restoring your session…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileLoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Preparing your Drift profile…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileLoadErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onLogOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "We hit a small snag",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = CircleShape
        ) {
            Text("Try again")
        }
        TextButton(onClick = onLogOut) {
            Text("Log out")
        }
    }
}

@Composable
private fun ResetPasswordScreen(
    onUpdatePassword: suspend (String) -> Result<Unit>,
    onPasswordUpdated: () -> Unit,
    onCancel: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUpdating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val passwordIsValid = password.length >= 8
    val passwordsMatch = password == confirmPassword

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        DriftBackButton(
            onClick = { if (!isUpdating) onCancel() },
            contentDescription = "Cancel password update"
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Create a new password",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Choose a secure password you haven't used for Drift before.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(34.dp))

        Text(
            text = "New password",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            singleLine = true,
            placeholder = { Text("At least 8 characters") },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "Hide" else "Show")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Confirm new password",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = null
            },
            singleLine = true,
            placeholder = { Text("Enter it again") },
            visualTransformation = if (confirmPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(
                    onClick = { confirmPasswordVisible = !confirmPasswordVisible }
                ) {
                    Text(if (confirmPasswordVisible) "Hide" else "Show")
                }
            },
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            supportingText = {
                when {
                    password.isNotEmpty() && !passwordIsValid ->
                        Text("Use at least 8 characters.")
                    confirmPassword.isNotEmpty() && !passwordsMatch ->
                        Text("Passwords don't match.")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        Button(
            onClick = {
                errorMessage = null
                coroutineScope.launch {
                    isUpdating = true
                    onUpdatePassword(password)
                        .onSuccess { onPasswordUpdated() }
                        .onFailure { error ->
                            errorMessage = error.message
                                ?: "We couldn't update your password."
                        }
                    isUpdating = false
                }
            },
            enabled = passwordIsValid && passwordsMatch &&
                    confirmPassword.isNotEmpty() &&
                    !isUpdating,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Update password",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RippleSafeContent(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides null,
        content = content
    )
}

private fun isValidEmail(value: String): Boolean {
    val email = value.sanitizeEmailInput()
    return email == value &&
            email.isNotEmpty() &&
            !email.any { it.isWhitespace() } &&
            Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private fun String.sanitizeEmailInput(): String = filterNot { character ->
    character.isWhitespace() || Character.isSpaceChar(character)
}.trim()

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(70.dp))

        Image(
            painter = painterResource(R.drawable.drift_welcome_logo),
            contentDescription = "Drift logo",
            modifier = Modifier.size(116.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "DRIFT",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Focus better. Do more.\nLive in balance.",
            fontSize = 18.sp,
            lineHeight = 27.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "〰  Your journey to better focus starts here  〰",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Log In",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSignUpClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape
        ) {
            Text(
                text = "Sign Up",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(54.dp))

        Text(
            text = "By continuing, you agree to our",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Terms & Privacy Policy",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSignUpClick: () -> Unit = {},
    onLoginClick: suspend (String, String) -> Result<Unit> = { _, _ -> Result.success(Unit) },
    onForgotPasswordClick: suspend (String) -> Result<Unit> = { Result.success(Unit) },
    onGoogleClick: suspend () -> Result<Unit> = { Result.success(Unit) },
    externalError: String? = null,
    onLoginSuccess: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordResetMessage by remember { mutableStateOf<String?>(null) }
    var isSendingReset by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var isLaunchingGoogle by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(externalError) {
        if (externalError != null) {
            loginError = externalError
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        DriftBackButton(onClick = onBack)

        Spacer(modifier = Modifier.height(50.dp))

        Text(
            text = "Log In",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Welcome back! Let's get you focused.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(38.dp))

        Text(
            text = "Email",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { value ->
                email = value.sanitizeEmailInput()
                loginError = null
                passwordResetMessage = null
            },
            placeholder = { Text("Enter your email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Password",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                loginError = null
            },
            placeholder = { Text("Enter your password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(
                    onClick = { passwordVisible = !passwordVisible }
                ) {
                    Text(if (passwordVisible) "Hide" else "Show")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        TextButton(
            onClick = {
                loginError = null
                passwordResetMessage = null
                if (!isValidEmail(email)) {
                    loginError = "Enter your email address first."
                } else if (!isSendingReset) {
                    coroutineScope.launch {
                        isSendingReset = true
                        onForgotPasswordClick(email)
                            .onSuccess {
                                passwordResetMessage = "Password reset email sent."
                            }
                            .onFailure { error ->
                                loginError = error.message
                                    ?: "We couldn't send the reset email."
                            }
                        isSendingReset = false
                    }
                }
            },
            enabled = !isSendingReset,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text = if (isSendingReset) "Sending..." else "Forgot password?",
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                loginError = null
                if (!isValidEmail(email)) {
                    loginError = "Enter a valid email address."
                } else if (password.isNotBlank() && !isLoggingIn) {
                    coroutineScope.launch {
                        isLoggingIn = true
                        onLoginClick(email, password)
                            .onSuccess { onLoginSuccess() }
                            .onFailure { error ->
                                loginError = error.message
                                    ?: "Invalid email or password."
                            }
                        isLoggingIn = false
                    }
                }
            },
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoggingIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isLoggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Log In",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        loginError?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        passwordResetMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        if (email.isBlank() || password.isBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your email and password to continue",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))

            Text(
                text = "  or continue with  ",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                loginError = null
                if (!isLaunchingGoogle) {
                    coroutineScope.launch {
                        isLaunchingGoogle = true
                        onGoogleClick().onFailure { error ->
                            loginError = error.message ?: "Google sign-in is unavailable."
                        }
                        isLaunchingGoogle = false
                    }
                }
            },
            enabled = !isLaunchingGoogle && !isLoggingIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = CircleShape
        ) {
            if (isLaunchingGoogle) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "G   Continue with Google",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Don't have an account? ")

            Text(
                text = "Sign Up",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSignUpClick() }
            )
        }
    }
}

@Composable
fun SignupScreen(
    onBack: () -> Unit,
    onSignUpClick: suspend (String, String, String) -> Result<SignupOutcome> = { _, _, _ ->
        Result.success(SignupOutcome.VerificationRequired)
    },
    onSignUpSuccess: (String, SignupOutcome) -> Unit = { _, _ -> },
    onGoogleClick: suspend () -> Result<Unit> = { Result.success(Unit) },
    onLoginClick: () -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var showSignupValidation by remember { mutableStateOf(false) }
    var signupError by remember { mutableStateOf<String?>(null) }
    var isSigningUp by remember { mutableStateOf(false) }
    var isLaunchingGoogle by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val signupValid = fullName.isNotBlank() &&
            isValidEmail(email) &&
            password.length >= 8 &&
            termsAccepted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        DriftBackButton(onClick = onBack)

        Spacer(modifier = Modifier.height(50.dp))

        Text(
            text = "Create Account",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start your journey to a balanced digital life.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(38.dp))

        Text(
            text = "Full Name",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = { Text("Enter your full name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Email",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { value ->
                email = value.sanitizeEmailInput()
                signupError = null
            },
            placeholder = { Text("Enter your email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Password",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Create a password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(
                    onClick = { passwordVisible = !passwordVisible }
                ) {
                    Text(if (passwordVisible) "Hide" else "Show")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "At least 8 characters",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = { termsAccepted = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )

            Text(
                text = "I agree to the Terms & Privacy Policy",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showSignupValidation && !signupValid) {
            Text(
                text = when {
                    fullName.isBlank() -> "Enter your full name."
                    !isValidEmail(email) -> "Enter a valid email address without spaces."
                    password.length < 8 -> "Use at least 8 characters for your password."
                    else -> "Accept the Terms & Privacy Policy to continue."
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }

        signupError?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                showSignupValidation = true
                signupError = null
                if (signupValid && !isSigningUp) {
                    coroutineScope.launch {
                        isSigningUp = true
                        onSignUpClick(fullName, email, password)
                            .onSuccess { outcome ->
                                onSignUpSuccess(email.trim(), outcome)
                            }
                            .onFailure { error ->
                                signupError = error.message
                                    ?: "We couldn't create your account. Please try again."
                            }
                        isSigningUp = false
                    }
                }
            },
            enabled = !isSigningUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isSigningUp) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Sign Up",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))

            Text(
                text = "  or continue with  ",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                signupError = null
                if (!isLaunchingGoogle) {
                    coroutineScope.launch {
                        isLaunchingGoogle = true
                        onGoogleClick().onFailure { error ->
                            signupError = error.message ?: "Google sign-in is unavailable."
                        }
                        isLaunchingGoogle = false
                    }
                }
            },
            enabled = !isLaunchingGoogle && !isSigningUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = CircleShape
        ) {
            if (isLaunchingGoogle) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "G   Continue with Google",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Already have an account? ")

            Text(
                text = "Log In",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onLoginClick() }
            )
        }
    }
}

@Composable
fun VerifyEmailScreen(
    onBack: () -> Unit,
    email: String,
    onVerifyClick: suspend (String, String) -> Result<Unit>,
    onVerifySuccess: () -> Unit = {},
    onResendClick: suspend (String) -> Result<Unit>
) {
    var code by remember { mutableStateOf(List(6) { "" }) }
    var codeRemainingSeconds by remember(email) { mutableStateOf(5 * 60) }
    var verificationError by remember { mutableStateOf<String?>(null) }
    var resendMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val codeFocusRequesters = remember { List(6) { FocusRequester() } }

    LaunchedEffect(Unit) {
        delay(150)
        codeFocusRequesters.first().requestFocus()
    }

    LaunchedEffect(codeRemainingSeconds) {
        if (codeRemainingSeconds > 0) {
            delay(1000)
            codeRemainingSeconds--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            DriftBackButton(onClick = onBack)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .height(88.dp)
                .width(88.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✉",
                fontSize = 36.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Verify Your Email",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "We've sent a 6-digit code to\n$email",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            code.forEachIndexed { index, digit ->
                OutlinedTextField(
                    value = digit,
                    onValueChange = { newValue ->
                        verificationError = null
                        resendMessage = null
                        val enteredDigits = newValue.filter(Char::isDigit)
                        if (enteredDigits.isEmpty()) {
                            code = code.toMutableList().also { it[index] = "" }.toList()
                        } else {
                            val updatedCode = code.toMutableList()
                            enteredDigits.take(6 - index).forEachIndexed { offset, value ->
                                updatedCode[index + offset] = value.toString()
                            }
                            code = updatedCode.toList()
                            val nextIndex = (index + enteredDigits.length).coerceAtMost(5)
                            codeFocusRequesters[nextIndex].requestFocus()
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = if (index == 5) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            codeFocusRequesters[(index + 1).coerceAtMost(5)].requestFocus()
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(codeFocusRequesters[index])
                        .height(56.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (codeRemainingSeconds > 0) {
                "Code expires in %02d:%02d".format(
                    codeRemainingSeconds / 60,
                    codeRemainingSeconds % 60
                )
            } else {
                "Code expired — request a new one"
            },
            fontSize = 14.sp,
            color = if (codeRemainingSeconds > 0) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        verificationError?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        Button(
            onClick = {
                verificationError = null
                resendMessage = null
                coroutineScope.launch {
                    isVerifying = true
                    onVerifyClick(email, code.joinToString(""))
                        .onSuccess { onVerifySuccess() }
                        .onFailure { error ->
                            verificationError = error.message
                                ?: "The code is invalid or expired. Please try again."
                        }
                    isVerifying = false
                }
            },
            enabled = code.all { it.isNotBlank() } &&
                    codeRemainingSeconds > 0 &&
                    !isVerifying,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isVerifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Verify Email",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Didn't receive the code?",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isResending) "Sending..." else "Resend Code",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable(enabled = !isResending) {
                verificationError = null
                resendMessage = null
                coroutineScope.launch {
                    isResending = true
                    onResendClick(email)
                        .onSuccess {
                            code = List(6) { "" }
                            codeRemainingSeconds = 5 * 60
                            resendMessage = "A new code was sent."
                            codeFocusRequesters.first().requestFocus()
                        }
                        .onFailure { error ->
                            verificationError = error.message
                                ?: "We couldn't resend the code. Please try again."
                        }
                    isResending = false
                }
            }
        )

        resendMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun OnboardingProgressDots(activeStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val completed = index < activeStep
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = if (completed || index == activeStep) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = CircleShape
                    )
                    .border(
                        1.dp,
                        if (completed || index == activeStep) {
                            MaterialTheme.colorScheme.primary
                        } else MaterialTheme.colorScheme.outline,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (completed) {
                    Icon(
                        painter = painterResource(R.drawable.ic_completed_check),
                        contentDescription = "Completed",
                        tint = Color.Unspecified,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (index == activeStep) {
                            MaterialTheme.colorScheme.onPrimary
                        } else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (index < 4) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(2.dp)
                        .background(
                            if (index < activeStep) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}

@Composable
fun DriftPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun OnboardingStepButtons(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    nextText: String = "Next"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onPrevious,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = CircleShape
        ) {
            Text("Previous", fontWeight = FontWeight.SemiBold)
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(nextText, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun OnboardingIntroScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        DriftBackButton(onClick = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(88.dp)
                    .width(88.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⛰",
                    fontSize = 36.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Let's Get Started",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "DRIFT learns your habits to deliver personalized focus insights, timely nudges, and a healthier balance between study and screen time.",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            listOf(
                "Smarter insights",
                "Timely interventions",
                "Better balance"
            ).forEach { feature ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = feature,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        DriftPrimaryButton(
            text = "Continue",
            onClick = onContinue
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutYouScreen(
    onBack: () -> Unit,
    draft: OnboardingDraft = OnboardingDraft(),
    isEditing: Boolean = false,
    onContinue: (String, String) -> Unit = { _, _ -> }
) {
    var ageRange by remember(draft.ageRange) { mutableStateOf(draft.ageRange) }
    var gender by remember(draft.gender) { mutableStateOf(draft.gender) }
    var ageExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    val ageOptions = listOf("Under 18", "18–24", "25–34", "35–44", "45+")
    val genderOptions = listOf("Woman", "Man", "Non-binary", "Prefer not to say")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        DriftBackButton(onClick = onBack)
        if (!isEditing) {
            Spacer(Modifier.height(16.dp))
            OnboardingProgressDots(activeStep = 0)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "About You",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Optional — this helps us understand who Drift supports. You can skip it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text("Age range", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = ageExpanded,
                onExpandedChange = { ageExpanded = it }
            ) {
                OutlinedTextField(
                    value = ageRange,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Select age range") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(ageExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = CircleShape
                )
                DropdownMenu(
                    expanded = ageExpanded,
                    onDismissRequest = { ageExpanded = false }
                ) {
                    ageOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                ageRange = option
                                ageExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Gender", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = it }
            ) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Select gender") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(genderExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = CircleShape
                )
                DropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                gender = option
                                genderExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
        if (isEditing) {
            DriftPrimaryButton("Save Changes", { onContinue(ageRange, gender) })
        } else {
            OutlinedButton(
                onClick = { onContinue("", "") },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = CircleShape
            ) {
                Text("Skip for now", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OnboardingStepButtons(
                onPrevious = onBack,
                onNext = { onContinue(ageRange, gender) }
            )
        }
    }
}

@Composable
fun RoleScreen(
    onBack: () -> Unit,
    draft: OnboardingDraft = OnboardingDraft(),
    isEditing: Boolean = false,
    onContinue: (String, String) -> Unit = { _, _ -> }
) {
    val roles = listOf(
        "High school student", "Undergraduate", "Postgraduate",
        "Working professional", "Job seeker", "Other"
    )
    var role by remember(draft.role) { mutableStateOf(draft.role) }
    var otherRole by remember(draft.otherRole) { mutableStateOf(draft.otherRole) }
    var validationAttempted by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        DriftBackButton(onClick = onBack)
        if (!isEditing) {
            Spacer(Modifier.height(16.dp))
            OnboardingProgressDots(activeStep = 1)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(32.dp))
            Text(
                "What describes you?",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Choose the option that fits you best right now.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(22.dp))
            roles.forEach { option ->
                SelectionRow(option, role == option) { role = option }
            }
            if (role == "Other") {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = otherRole,
                    onValueChange = { otherRole = it },
                    label = { Text("Describe your role") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape
                )
            }
        }
        if (validationAttempted && role.isBlank()) {
            Text(
                "Select one option to continue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        } else if (validationAttempted && role == "Other" && otherRole.isBlank()) {
            Text(
                "Describe your role to continue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(20.dp))
        if (isEditing) {
            DriftPrimaryButton("Save Changes", {
                validationAttempted = true
                if (role.isNotBlank() && (role != "Other" || otherRole.isNotBlank())) {
                    onContinue(role, if (role == "Other") otherRole else "")
                }
            })
        } else {
            OnboardingStepButtons(
                onPrevious = onBack,
                onNext = {
                    validationAttempted = true
                    if (role.isNotBlank() && (role != "Other" || otherRole.isNotBlank())) {
                        onContinue(role, if (role == "Other") otherRole else "")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManagingScreen(
    onBack: () -> Unit,
    draft: OnboardingDraft = OnboardingDraft(),
    isEditing: Boolean = false,
    onContinue: (Set<String>) -> Unit = {}
) {
    val options = listOf(
        "Studies", "Exams", "Assignments", "Work projects",
        "Meetings", "Interview preparation", "Courses", "Personal responsibilities"
    )
    var selected by remember(draft.managing) { mutableStateOf(draft.managing) }
    var validationAttempted by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        DriftBackButton(onClick = onBack)
        if (!isEditing) {
            Spacer(Modifier.height(16.dp))
            OnboardingProgressDots(activeStep = 2)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(32.dp))
            Text(
                "What are you managing?",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Select everything you want Drift to help you balance.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(22.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEach { option ->
                    FilterChip(
                        selected = option in selected,
                        onClick = {
                            selected = if (option in selected) selected - option
                            else selected + option
                        },
                        label = { Text(option) },
                        shape = CircleShape
                    )
                }
            }
        }
        if (validationAttempted && selected.isEmpty()) {
            Text(
                "Select at least one item to continue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(20.dp))
        if (isEditing) {
            DriftPrimaryButton("Save Changes", {
                validationAttempted = true
                if (selected.isNotEmpty()) onContinue(selected)
            })
        } else {
            OnboardingStepButtons(
                onPrevious = onBack,
                onNext = {
                    validationAttempted = true
                    if (selected.isNotEmpty()) onContinue(selected)
                }
            )
        }
    }
}

@Composable
private fun SelectionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun TimePickerButton(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val match = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""")
                .find(value.uppercase())
            val displayHour = match?.groupValues?.get(1)?.toIntOrNull() ?: 7
            val minute = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val period = match?.groupValues?.get(3) ?: "PM"
            val hour24 = when {
                period == "AM" && displayHour == 12 -> 0
                period == "PM" && displayHour != 12 -> displayHour + 12
                else -> displayHour
            }
            TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute ->
                    val selectedPeriod = if (selectedHour >= 12) "PM" else "AM"
                    val selectedDisplayHour = when (val hour = selectedHour % 12) {
                        0 -> 12
                        else -> hour
                    }
                    onValueChange(
                        "$selectedDisplayHour:${selectedMinute.toString().padStart(2, '0')} $selectedPeriod"
                    )
                },
                hour24,
                minute,
                false
            ).show()
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = CircleShape
    ) {
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StudyScheduleScreen(
    onBack: () -> Unit,
    draft: OnboardingDraft = OnboardingDraft(),
    isEditing: Boolean = false,
    onContinue: (String, String, Set<String>) -> Unit = { _, _, _ -> }
) {
    var startTime by remember(draft.studyStart) {
        mutableStateOf(draft.studyStart)
    }
    var endTime by remember(draft.studyEnd) { mutableStateOf(draft.studyEnd) }
    var selectedDays by remember(draft.studyDays) {
        mutableStateOf(draft.studyDays)
    }
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        DriftBackButton(onClick = onBack)
        if (!isEditing) {
            Spacer(Modifier.height(16.dp))
            OnboardingProgressDots(activeStep = 3)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Work / Study Schedule",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "When do you usually focus?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Set your typical work or study window so Drift can support your focus time.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Start Time",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            TimePickerButton(
                value = startTime,
                onValueChange = { startTime = it }
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "End Time",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            TimePickerButton(
                value = endTime,
                onValueChange = { endTime = it }
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Work / Study Days",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { day ->
                    StudyDayChip(
                        day = day,
                        selected = day in selectedDays,
                        onClick = {
                            selectedDays = if (day in selectedDays) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (isEditing) {
            DriftPrimaryButton(
                "Save Changes",
                { onContinue(startTime, endTime, selectedDays) }
            )
        } else {
            OnboardingStepButtons(
                onPrevious = onBack,
                onNext = { onContinue(startTime, endTime, selectedDays) }
            )
        }
    }
}

@Composable
private fun StudyDayChip(
    day: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clickable { onClick() }
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.first().toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WindDownScreen(
    onBack: () -> Unit,
    draft: OnboardingDraft = OnboardingDraft(),
    isEditing: Boolean = false,
    isSaving: Boolean = false,
    saveError: String? = null,
    onContinue: (String, String, Boolean) -> Unit = { _, _, _ -> }
) {
    var windDownStart by remember(draft.windDownStart) {
        mutableStateOf(draft.windDownStart)
    }
    var windDownEnd by remember(draft.windDownEnd) {
        mutableStateOf(draft.windDownEnd)
    }
    var windDownEnabled by remember(draft.windDownEnabled) {
        mutableStateOf(draft.windDownEnabled)
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        DriftBackButton(onClick = onBack)
        if (!isEditing) {
            Spacer(Modifier.height(16.dp))
            OnboardingProgressDots(activeStep = 4)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Sleep & Wind-Down",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "What are your sleep hours?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "DRIFT will help you disconnect and prepare for restful sleep.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Wind-Down Mode",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Block distractions and receive reminders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = windDownEnabled,
                    onCheckedChange = { windDownEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Wind-Down Start",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            TimePickerButton(
                value = windDownStart,
                onValueChange = { windDownStart = it },
                enabled = windDownEnabled
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Wind-Down End",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            TimePickerButton(
                value = windDownEnd,
                onValueChange = { windDownEnd = it },
                enabled = windDownEnabled
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(18.dp)
            ) {
                val windDownDetails = if (windDownEnabled) {
                    listOf(
                        "Distracting apps will be blocked",
                        "Only selected apps can be used",
                        "Wind-down reminders"
                    )
                } else {
                    listOf("Wind-down protections and reminders will remain off")
                }
                windDownDetails.forEach { item ->
                    Text(
                        text = "•  $item",
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (isEditing) {
            DriftPrimaryButton(
                if (isSaving) "Saving…" else "Save Changes",
                {
                    if (!isSaving) {
                        onContinue(windDownStart, windDownEnd, windDownEnabled)
                    }
                }
            )
        } else {
            OnboardingStepButtons(
                onPrevious = onBack,
                onNext = {
                    if (!isSaving) {
                        onContinue(windDownStart, windDownEnd, windDownEnabled)
                    }
                },
                nextText = if (isSaving) "Saving…" else "Get Started"
            )
        }
        saveError?.let { message ->
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun OnboardingSummaryScreen(
    onBack: () -> Unit,
    draft: OnboardingDraft = OnboardingDraft(),
    isEditing: Boolean = false,
    onGetStarted: suspend () -> Result<Unit> = { Result.success(Unit) },
    onSaved: () -> Unit = {},
    onEditAcademic: () -> Unit = {},
    onEditSchedule: () -> Unit = {},
    onEditWindDown: () -> Unit = {},
    onStudyDaysChange: (Set<String>) -> Unit = {},
    onWindDownEnabledChange: (Boolean) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        DriftBackButton(onClick = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isEditing) "Review Changes" else "Almost Done!",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Tap a detail to edit it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                ReviewDetailRow(
                    badge = "A",
                    label = "Academic year",
                    value = draft.academicYear.ifBlank { "Not specified" },
                    onClick = onEditAcademic
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                ReviewDetailRow(
                    badge = "C",
                    label = "Course",
                    value = draft.course.ifBlank { "Not specified" },
                    onClick = onEditAcademic
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                ReviewDetailRow(
                    badge = "T",
                    label = "Study time",
                    value = "${draft.studyStart} – ${draft.studyEnd}",
                    onClick = onEditSchedule
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                ReviewDaysRow(
                    selectedDays = draft.studyDays,
                    onDaysChange = onStudyDaysChange
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                ReviewDetailRow(
                    badge = "S",
                    label = "Sleep hours",
                    value = if (draft.windDownEnabled) {
                        "${draft.windDownStart} – ${draft.windDownEnd}"
                    } else {
                        "Not active"
                    },
                    onClick = onEditWindDown
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                ReviewToggleRow(
                    label = "Wind-Down Mode",
                    enabled = draft.windDownEnabled,
                    onEnabledChange = onWindDownEnabledChange
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        DriftPrimaryButton(
            text = when {
                isSaving -> "Saving…"
                isEditing -> "Save Changes"
                else -> "Get Started"
            },
            onClick = {
                if (!isSaving) {
                    saveError = null
                    coroutineScope.launch {
                        isSaving = true
                        onGetStarted()
                            .onSuccess { onSaved() }
                            .onFailure { error ->
                                saveError = profileErrorMessage(error)
                            }
                        isSaving = false
                    }
                }
            }
        )

        saveError?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

    }
}

@Composable
private fun ReviewDetailRow(
    badge: String,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    CircleShape
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        DriftNextIcon(
            modifier = Modifier.size(18.dp),
            contentDescription = "Edit $label"
        )
    }
}

@Composable
private fun ReviewDaysRow(
    selectedDays: Set<String>,
    onDaysChange: (Set<String>) -> Unit
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Study days",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { day ->
                val selected = day in selectedDays
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            onDaysChange(
                                if (selected) selectedDays - day
                                else selectedDays + day
                            )
                        }
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                            },
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.first().toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewToggleRow(
    label: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (enabled) "Enabled" else "Disabled",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
    }
}

private fun profileErrorMessage(error: Throwable): String {
    val message = error.message.orEmpty().lowercase()
    return when {
        "session" in message ->
            "Your session expired. Please log in again."
        "42703" in message || "pgrst204" in message ||
            "age_range" in message || "wind_down_enabled" in message ||
            "column" in message && "does not exist" in message ->
            "Profile setup needs the latest database update. Apply the pending Supabase migrations and try again."
        "network" in message || "unable to resolve" in message ||
            "failed to connect" in message ->
            "We couldn’t reach Drift. Check your connection and try again."
        "profiles" in message && ("schema cache" in message || "pgrst205" in message) ->
            "Profile setup is temporarily unavailable. Please try again shortly."
        else -> "We couldn’t access your profile. Please try again."
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WelcomeScreenPreview() {
    DriftTheme {
        WelcomeScreen(onLoginClick = {})
    }
}
