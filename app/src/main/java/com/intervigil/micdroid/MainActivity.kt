package com.intervigil.micdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.intervigil.micdroid.ui.VocalTuneApp
import com.intervigil.micdroid.ui.theme.VocalTuneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VocalTuneTheme {
                VocalTuneApp()
            }
        }
    }
}
