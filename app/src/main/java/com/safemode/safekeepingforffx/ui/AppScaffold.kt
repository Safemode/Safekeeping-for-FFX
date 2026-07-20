package com.safemode.safekeepingforffx.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.safemode.safekeepingforffx.ui.navigation.AppDrawerContent
import com.safemode.safekeepingforffx.ui.navigation.FfxNavHost
import com.safemode.safekeepingforffx.ui.navigation.destinationForRoute
import com.safemode.safekeepingforffx.ui.navigation.navigateToDestination
import kotlinx.coroutines.launch

/** How long the second back press has to arrive before the confirmation lapses. */
private const val EXIT_CONFIRM_WINDOW_MS = 2_000L

/**
 * Root of the app: owns the drawer and nav state so individual screens never have to know the
 * drawer exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = destinationForRoute(currentRoute)

    val context = LocalContext.current
    val activity = LocalActivity.current
    var lastBackPress by remember { mutableLongStateOf(0L) }

    // A screen can claim the back press while it has transient state worth dismissing first - an
    // active search, on Home or on any category. This lives here rather than in a BackHandler
    // inside the screen because our handler is registered after the NavHost's and so would swallow
    // the press before the screen ever saw it.
    //
    // Keyed by screen rather than a single slot: mid-transition both the outgoing and incoming
    // screens are composed, and the outgoing one's cleanup would otherwise wipe the registration
    // the incoming one just made.
    val screenBackHandlers = remember { mutableStateMapOf<String, () -> Unit>() }
    val screenBackHandler = screenBackHandlers[currentDestination.route]

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentRoute = currentRoute,
                onDestinationClick = { destination ->
                    scope.launch { drawerState.close() }
                    navController.navigateToDestination(destination.route)
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(currentDestination.label) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open navigation menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            FfxNavHost(
                navController = navController,
                onScreenBackHandlerChange = { key, handler ->
                    if (handler == null) {
                        screenBackHandlers.remove(key)
                    } else {
                        screenBackHandlers[key] = handler
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )

            // Declared after the NavHost so this callback is registered later and therefore wins
            // over the NavHost's own back handling. That keeps the three cases in one place and in
            // a guaranteed order, instead of racing the drawer's and the NavHost's handlers.
            BackHandler {
                when {
                    drawerState.isOpen -> scope.launch { drawerState.close() }

                    // Backing out of a search returns you to the list underneath it, which is
                    // neither a reason to leave the screen nor to be asked about quitting.
                    screenBackHandler != null -> screenBackHandler()

                    navController.previousBackStackEntry != null -> navController.popBackStack()

                    // Only reachable from Home with the drawer shut, so quitting always leaves the
                    // app on "Your progress" for the next launch.
                    else -> {
                        val now = System.currentTimeMillis()
                        if (now - lastBackPress < EXIT_CONFIRM_WINDOW_MS) {
                            activity?.finish()
                        } else {
                            lastBackPress = now
                            Toast.makeText(
                                context,
                                "Press back again to exit",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}
