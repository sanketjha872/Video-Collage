package com.jhainusa.video_collage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jhainusa.video_collage.presentation.ui.PickerScreen
import com.jhainusa.video_collage.presentation.ui.ProcessingScreen
import com.jhainusa.video_collage.presentation.ui.ResultScreen
import com.jhainusa.video_collage.ui.theme.VideoCollageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoCollageTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "picker") {
                        composable("picker") {
                            PickerScreen()
                        }
                        composable("processing") {
                            ProcessingScreen()
                        }
                        composable("result") {
                            ResultScreen()
                        }
                    }
                }
            }
        }
    }
}
