package com.jhainusa.video_collage

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jhainusa.video_collage.presentation.ui.PickerScreen
import com.jhainusa.video_collage.presentation.ui.ProcessingScreen
import com.jhainusa.video_collage.presentation.ui.ResultScreen
import com.jhainusa.video_collage.presentation.viewmodel.ProcessingViewModel
import com.jhainusa.video_collage.presentation.viewmodel.ViewModelFactory
import com.jhainusa.video_collage.ui.theme.VideoCollageTheme
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private val viewModel: ProcessingViewModel by viewModels {
        ViewModelFactory((application as VideoCollageApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoCollageTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "picker") {
                        composable("picker") {
                            PickerScreen(onVideoSelected = { uri ->
                                val encodedUri = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString())
                                navController.navigate("processing/$encodedUri")
                            })
                        }
                        composable(
                            "processing/{videoUri}",
                            arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val videoUri = Uri.parse(backStackEntry.arguments?.getString("videoUri"))
                            ProcessingScreen(
                                videoUri = videoUri,
                                viewModel = viewModel,
                                onProcessingComplete = {
                                    navController.navigate("result") {
                                        popUpTo("processing/{videoUri}") { inclusive = true }
                                    }
                                },
                                onErrorRetry = {
                                    viewModel.reset()
                                    navController.navigate("picker") {
                                        popUpTo("picker") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("result") {
                            ResultScreen(
                                viewModel = viewModel,
                                onRestart = {
                                    viewModel.reset()
                                    navController.navigate("picker") {
                                        popUpTo("picker") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
