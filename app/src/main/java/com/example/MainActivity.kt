package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.model.UserRole
import com.example.data.remote.FirebaseService
import com.example.data.repository.SkinToolRepository
import com.example.domain.ApplyEngine
import com.example.shizuku.ShizukuManager
import com.example.ui.components.LoopingVideoBackground
import com.example.ui.screens.admin.*
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.auth.SplashScreen
import com.example.ui.screens.common.SettingsScreen
import com.example.ui.screens.user.*
import com.example.ui.theme.SHSkinToolTheme

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var firebaseService: FirebaseService
    private lateinit var repository: SkinToolRepository
    private lateinit var shizukuManager: ShizukuManager
    private lateinit var applyEngine: ApplyEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getDatabase(applicationContext)
        firebaseService = FirebaseService(applicationContext)
        repository = SkinToolRepository(applicationContext, database, firebaseService)
        shizukuManager = ShizukuManager(applicationContext)
        applyEngine = ApplyEngine(applicationContext, repository, shizukuManager)

        setContent {
            SHSkinToolTheme {
                val navController = rememberNavController()
                val appSettings by repository.appSettings.collectAsState()
                val currentUser by repository.currentUser.collectAsState()

                // Global session invalidation observer across ALL devices and screens
                LaunchedEffect(appSettings.userPassword, appSettings.authVersion, currentUser) {
                    if (currentUser?.role == UserRole.USER) {
                        if (com.example.security.SessionManager.isUserSessionExpired(appSettings.userPassword, appSettings.authVersion)) {
                            repository.logout()
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }

                // Universal Looping HD Video Wallpaper across the ENTIRE APP
                Box(modifier = Modifier.fillMaxSize()) {
                    LoopingVideoBackground()

                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Cyberpunk Pro Splash / Loading Animation Screen
                        composable("splash") {
                            SplashScreen(
                                repository = repository,
                                onSplashComplete = {
                                    navController.navigate("auth") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Password-Only Login Screen
                        composable("auth") {
                            AuthScreen(
                                repository = repository,
                                onLoginSuccess = { role ->
                                    if (role == UserRole.ADMIN) {
                                        navController.navigate("admin_dashboard") {
                                            popUpTo("auth") { inclusive = true }
                                        }
                                    } else {
                                        // Direct redirect to User Home screen without forced gatekeeper block
                                        navController.navigate("user_home") {
                                            popUpTo("auth") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                    // Shizuku Gatekeeper Screen (Checks & Grants Shizuku before unlocking User Panel)
                    composable("shizuku_gatekeeper") {
                        UserShizukuGatekeeperScreen(
                            shizukuManager = shizukuManager,
                            onShizukuGranted = {
                                navController.navigate("user_home") {
                                    popUpTo("shizuku_gatekeeper") { inclusive = true }
                                }
                            },
                            onLogout = {
                                repository.logout()
                                navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // User Panel Home (Unlocked only after Shizuku is authorized)
                    composable("user_home") {
                        UserHomeScreen(
                            repository = repository,
                            shizukuManager = shizukuManager,
                            applyEngine = applyEngine,
                            onNavigateToDynamicScreen = { screenId ->
                                navController.navigate("dynamic_screen/$screenId")
                            },
                            onNavigateToPackageDetails = { packageId ->
                                navController.navigate("package_details/$packageId")
                            },
                            onNavigateToFolder = { folderId ->
                                navController.navigate("folder/$folderId")
                            },
                            onNavigateToFavorites = {
                                navController.navigate("favorites")
                            },
                            onNavigateToHistory = {
                                navController.navigate("history")
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            },
                            onNavigateToSupport = {
                                navController.navigate("user_support")
                            },
                            onNavigateToMailbox = {
                                navController.navigate("user_mailbox")
                            },
                            onLogout = {
                                repository.logout()
                                navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // Dynamic Screen View (Rendered dynamically from Cloud data)
                    composable(
                        route = "dynamic_screen/{screenId}",
                        arguments = listOf(navArgument("screenId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val screenId = backStackEntry.arguments?.getString("screenId") ?: ""
                        DynamicScreenView(
                            screenId = screenId,
                            repository = repository,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToPackageDetails = { packageId ->
                                navController.navigate("package_details/$packageId")
                            }
                        )
                    }

                    // Folder View
                    composable(
                        route = "folder/{folderId}",
                        arguments = listOf(navArgument("folderId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
                        FolderView(
                            folderId = folderId,
                            repository = repository,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToPackageDetails = { packageId ->
                                navController.navigate("package_details/$packageId")
                            }
                        )
                    }

                    // Package Details & Apply Now Screen
                    composable(
                        route = "package_details/{packageId}",
                        arguments = listOf(navArgument("packageId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val packageId = backStackEntry.arguments?.getString("packageId") ?: ""
                        PackageDetailsScreen(
                            packageId = packageId,
                            repository = repository,
                            applyEngine = applyEngine,
                            shizukuManager = shizukuManager,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // User Favorites
                    composable("favorites") {
                        FavoritesScreen(
                            repository = repository,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToPackageDetails = { packageId ->
                                navController.navigate("package_details/$packageId")
                            }
                        )
                    }

                    // Apply History
                    composable("history") {
                        HistoryScreen(
                            repository = repository,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // Settings Screen
                    composable("settings") {
                        SettingsScreen(
                            repository = repository,
                            shizukuManager = shizukuManager,
                            onNavigateBack = { navController.popBackStack() },
                            onLogout = {
                                navController.navigate("auth") {
                                    popUpTo("user_home") { inclusive = true }
                                }
                            }
                        )
                    }

                    // Admin Panel Dashboard (Guarded with Admin Authorization)
                    composable("admin_dashboard") {
                        AdminRouteGuard(repository, onUnauthorized = { navController.navigate("auth") { popUpTo(0) { inclusive = true } } }) {
                            AdminDashboardScreen(
                                repository = repository,
                                onNavigateToCategories = { navController.navigate("admin_categories") },
                                onNavigateToPackages = { navController.navigate("admin_packages") },
                                onNavigateToSecurity = { navController.navigate("admin_security") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToSupport = { navController.navigate("admin_support") },
                                onSwitchToUserPanel = {
                                    navController.navigate("shizuku_gatekeeper")
                                },
                                onLogout = {
                                    repository.logout()
                                    navController.navigate("auth") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }

                    // Admin Password & Get Key Link Manager
                    composable("admin_security") {
                        AdminRouteGuard(repository, onUnauthorized = { navController.navigate("auth") { popUpTo(0) { inclusive = true } } }) {
                            AdminSecuritySettingsScreen(
                                repository = repository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Admin Screen Builder
                    composable("admin_screens") {
                        AdminRouteGuard(repository, onUnauthorized = { navController.navigate("auth") { popUpTo(0) { inclusive = true } } }) {
                            AdminScreensManager(
                                repository = repository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Admin Package Manager
                    composable("admin_packages") {
                        AdminRouteGuard(repository, onUnauthorized = { navController.navigate("auth") { popUpTo(0) { inclusive = true } } }) {
                            AdminPackageManager(
                                repository = repository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Admin File Uploader
                    composable("admin_files") {
                        AdminRouteGuard(repository, onUnauthorized = { navController.navigate("auth") { popUpTo(0) { inclusive = true } } }) {
                            AdminFilesManager(
                                repository = repository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Admin Categories & Folders
                    composable("admin_categories") {
                        AdminRouteGuard(repository, onUnauthorized = { navController.navigate("auth") { popUpTo(0) { inclusive = true } } }) {
                            AdminCategoryFolderManager(
                                repository = repository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Admin User Directory
                    composable("admin_users") {
                        AdminRouteGuard(repository, onUnauthorized = { navController.navigate("auth") { popUpTo(0) { inclusive = true } } }) {
                            AdminUsersManager(
                                repository = repository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Admin Publishing Hub
                    composable("admin_publishing") {
                        AdminRouteGuard(repository, onUnauthorized = { navController.navigate("auth") { popUpTo(0) { inclusive = true } } }) {
                            AdminPublishingHub(
                                repository = repository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // User Customer Service Support
                    composable("user_support") {
                        CustomerSupportScreen(
                            repository = repository,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToMailbox = { navController.navigate("user_mailbox") }
                        )
                    }

                    // User Mailbox Screen
                    composable("user_mailbox") {
                        UserMailboxScreen(
                            repository = repository,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToSupport = { navController.navigate("user_support") }
                        )
                    }

                    // Admin Customer Service & User Mailbox
                    composable("admin_support") {
                        AdminRouteGuard(repository, onUnauthorized = { navController.navigate("auth") { popUpTo(0) { inclusive = true } } }) {
                            AdminCustomerServiceScreen(
                                repository = repository,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

    override fun onResume() {
        super.onResume()
        if (::shizukuManager.isInitialized) {
            shizukuManager.checkStatus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::shizukuManager.isInitialized) {
            shizukuManager.destroy()
        }
    }
}

@Composable
fun AdminRouteGuard(
    repository: SkinToolRepository,
    onUnauthorized: () -> Unit,
    content: @Composable () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val isAuthorized = currentUser?.role == UserRole.ADMIN && com.example.security.SessionManager.isAdminSessionValid()

    if (!isAuthorized) {
        LaunchedEffect(Unit) {
            onUnauthorized()
        }
    } else {
        content()
    }
}


