package com.example.drift

import android.os.Bundle
import android.content.Intent
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
                            profileLoadError = null
                            profileEmail = AuthRepository.currentEmail()
                            ProfileRepository.loadCurrentProfile()
                                .onSuccess { profile ->
                                    profile?.let {
                                        profileName = it.fullName
                                        onboardingDraft = OnboardingDraft(
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
                                            }
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
                            onVerifySuccess = { currentScreen = "onboarding_intro" },
                            onResendClick = AuthRepository::resendSignupCode
                        )

                        "onboarding_intro" -> OnboardingIntroScreen(
                            onBack = {
                                currentScreen =
                                    if (signupVerificationRequired) "verify" else "signup"
                            },
                            onSkip = {
                                appScope.launch {
                                    ProfileRepository.saveOnboarding(onboardingDraft)
                                        .onSuccess { currentScreen = "dashboard" }
                                }
                            },
                            onContinue = { currentScreen = "academic_info" }
                        )

                        "academic_info" -> AcademicInfoScreen(
                            onBack = {
                                val returnScreen = onboardingEditReturn
                                if (returnScreen != null) {
                                    onboardingEditReturn = null
                                    currentScreen = returnScreen
                                } else {
                                    currentScreen = "onboarding_intro"
                                }
                            },
                            draft = onboardingDraft,
                            onContinue = { academicYear, course ->
                                onboardingDraft = onboardingDraft.copy(
                                    academicYear = academicYear,
                                    course = course
                                )
                                currentScreen = "study_schedule"
                            }
                        )

                        "study_schedule" -> StudyScheduleScreen(
                            onBack = { currentScreen = "academic_info" },
                            draft = onboardingDraft,
                            onContinue = { start, end, days ->
                                onboardingDraft = onboardingDraft.copy(
                                    studyStart = start,
                                    studyEnd = end,
                                    studyDays = days
                                )
                                currentScreen = "wind_down"
                            }
                        )

                        "wind_down" -> WindDownScreen(
                            onBack = { currentScreen = "study_schedule" },
                            draft = onboardingDraft,
                            onContinue = { start, end ->
                                onboardingDraft = onboardingDraft.copy(
                                    windDownStart = start,
                                    windDownEnd = end
                                )
                                currentScreen = "onboarding_summary"
                            }
                        )

                        "onboarding_summary" -> OnboardingSummaryScreen(
                            onBack = { currentScreen = "wind_down" },
                            draft = onboardingDraft,
                            isEditing = onboardingEditReturn != null,
                            onGetStarted = {
                                ProfileRepository.saveOnboarding(onboardingDraft)
                            },
                            onSaved = {
                                currentScreen = onboardingEditReturn ?: "dashboard"
                                onboardingEditReturn = null
                            },
                            onEditSettings = { currentScreen = "academic_info" }
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
                            onBack = { currentScreen = "focus" },
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
                                currentScreen = "academic_info"
                            },
                            onLogout = {
                                appScope.launch { AuthRepository.signOut() }
                            },
                            darkMode = darkMode,
                            onDarkModeChange = {
                                darkMode = it
                                appearancePreferences.edit().putBoolean("dark_mode", it).apply()
                            }
                        )

                        "profile" -> ProfileScreen(
                            onBack = { currentScreen = "dashboard" },
                            onOpenSettings = { currentScreen = "settings" },
                            onEditProfile = {
                                onboardingEditReturn = "profile"
                                currentScreen = "academic_info"
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
            shape = RoundedCornerShape(10.dp)
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
        Text(
            text = "←",
            fontSize = 30.sp,
            modifier = Modifier.clickable(enabled = !isUpdating, onClick = onCancel)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Create a new password",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
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
            shape = RoundedCornerShape(10.dp),
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
            shape = RoundedCornerShape(10.dp),
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
            shape = RoundedCornerShape(10.dp)
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
        Text(
            text = "←",
            fontSize = 30.sp,
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(modifier = Modifier.height(50.dp))

        Text(
            text = "Log In",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
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
            shape = RoundedCornerShape(10.dp),
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
            shape = RoundedCornerShape(10.dp)
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
        Text(
            text = "←",
            fontSize = 30.sp,
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(modifier = Modifier.height(50.dp))

        Text(
            text = "Create Account",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
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
            shape = RoundedCornerShape(10.dp),
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
            shape = RoundedCornerShape(10.dp)
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
            Text(
                text = "←",
                fontSize = 30.sp,
                modifier = Modifier.clickable { onBack() }
            )
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
            color = MaterialTheme.colorScheme.onBackground,
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
            shape = RoundedCornerShape(10.dp),
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
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(8.dp)
                    .width(if (index == activeStep) 24.dp else 8.dp)
                    .background(
                        color = if (index == activeStep) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(50.dp)
                    )
            )
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
        shape = RoundedCornerShape(10.dp),
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
fun OnboardingIntroScreen(
    onBack: () -> Unit,
    onSkip: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 30.sp,
                modifier = Modifier.clickable { onBack() }
            )

            Text(
                text = "Skip",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onSkip() }
            )
        }

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
                color = MaterialTheme.colorScheme.onBackground,
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

        OnboardingProgressDots(activeStep = 0)

        Spacer(modifier = Modifier.height(20.dp))

        DriftPrimaryButton(
            text = "Continue",
            onClick = onContinue
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicInfoScreen(
    onBack: () -> Unit,
    draft: OnboardingDraft = OnboardingDraft(),
    onContinue: (String, String) -> Unit = { _, _ -> }
) {
    var academicYear by remember(draft.academicYear) {
        mutableStateOf(draft.academicYear)
    }
    var course by remember(draft.course) { mutableStateOf(draft.course) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val yearOptions = listOf("Year 1", "Year 2", "Year 3", "Year 4", "Postgraduate")
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        Text(
            text = "←",
            fontSize = 30.sp,
            modifier = Modifier.clickable { onBack() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Academic Info",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "What's your academic level?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This helps DRIFT tailor study recommendations and focus goals to your schedule.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Academic Year / Grade",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = academicYear,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Select academic year") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    yearOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                academicYear = option
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Course / Major",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = course,
                onValueChange = { course = it },
                placeholder = { Text("e.g. BSc Computer Science") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        OnboardingProgressDots(activeStep = 1)

        Spacer(modifier = Modifier.height(20.dp))

        DriftPrimaryButton(
            text = "Continue",
            onClick = { onContinue(academicYear, course) }
        )
    }
}

@Composable
fun StudyScheduleScreen(
    onBack: () -> Unit,
    draft: OnboardingDraft = OnboardingDraft(),
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
        Text(
            text = "←",
            fontSize = 30.sp,
            modifier = Modifier.clickable { onBack() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Study Schedule",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "When do you usually study?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Set your typical study window so DRIFT can protect your focus hours.",
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

            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "End Time",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = endTime,
                onValueChange = { endTime = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Study Days",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.take(4).forEach { day ->
                    StudyDayChip(
                        day = day,
                        selected = day in selectedDays,
                        onClick = {
                            selectedDays = if (day in selectedDays) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.drop(4).forEach { day ->
                    StudyDayChip(
                        day = day,
                        selected = day in selectedDays,
                        onClick = {
                            selectedDays = if (day in selectedDays) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        OnboardingProgressDots(activeStep = 2)

        Spacer(modifier = Modifier.height(20.dp))

        DriftPrimaryButton(
            text = "Continue",
            onClick = { onContinue(startTime, endTime, selectedDays) }
        )
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
            .height(44.dp)
            .clickable { onClick() }
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day,
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
    onContinue: (String, String) -> Unit = { _, _ -> }
) {
    var windDownStart by remember(draft.windDownStart) {
        mutableStateOf(draft.windDownStart)
    }
    var windDownEnd by remember(draft.windDownEnd) {
        mutableStateOf(draft.windDownEnd)
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        Text(
            text = "←",
            fontSize = 30.sp,
            modifier = Modifier.clickable { onBack() }
        )

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
                color = MaterialTheme.colorScheme.onBackground
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

            Text(
                text = "Wind-Down Start",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = windDownStart,
                onValueChange = { windDownStart = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Wind-Down End",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = windDownEnd,
                onValueChange = { windDownEnd = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
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
                listOf(
                    "Distracting apps will be blocked",
                    "Only selected apps can be used",
                    "Wind-down reminders"
                ).forEach { item ->
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

        OnboardingProgressDots(activeStep = 3)

        Spacer(modifier = Modifier.height(20.dp))

        DriftPrimaryButton(
            text = "Continue",
            onClick = { onContinue(windDownStart, windDownEnd) }
        )
    }
}

@Composable
fun OnboardingSummaryScreen(
    onBack: () -> Unit,
    draft: OnboardingDraft = OnboardingDraft(),
    isEditing: Boolean = false,
    onGetStarted: suspend () -> Result<Unit> = { Result.success(Unit) },
    onSaved: () -> Unit = {},
    onEditSettings: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val summaryItems = listOf(
        "Academic Year" to draft.academicYear.ifBlank { "Not specified" },
        "Course" to draft.course.ifBlank { "Not specified" },
        "Study Time" to "${draft.studyStart} - ${draft.studyEnd}",
        "Study Days" to draft.studyDays.joinToString(", "),
        "Sleep Hours" to "${draft.windDownStart} - ${draft.windDownEnd}",
        "Wind-Down Mode" to "Enabled"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 48.dp)
    ) {
        Text(
            text = "←",
            fontSize = 30.sp,
            modifier = Modifier.clickable { onBack() }
        )

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
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                    text = "✓",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isEditing) "Ready to save" else "All Set!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
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
                summaryItems.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$label:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = value,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End
                        )
                    }
                }
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

        TextButton(
            onClick = onEditSettings,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Text(
                text = "Edit Settings",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun profileErrorMessage(error: Throwable): String {
    val message = error.message.orEmpty().lowercase()
    return when {
        "session" in message ->
            "Your session expired. Please log in again."
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
