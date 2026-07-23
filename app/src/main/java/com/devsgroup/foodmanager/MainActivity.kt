package com.devsgroup.foodmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.devsgroup.foodmanager.ui.FoodApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // FoodApp applies FoodManagerTheme itself, driven by the user's theme preference.
        setContent {
            FoodApp()
        }
    }
}
