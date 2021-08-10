package com.pnd.android.loop

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import androidx.viewbinding.ViewBindings
import com.pnd.android.loop.databinding.ContentMainBinding
import com.pnd.android.loop.ui.theme.AppTheme
import com.pnd.android.loop.util.LocalBackPressedDispatcher
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turn off the decor fitting system windows, which allows us to handle insets,
        // including IME animations
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CompositionLocalProvider(
                LocalBackPressedDispatcher provides this.onBackPressedDispatcher
            ) {
                val scaffoldState = rememberScaffoldState()

                AppTheme {
                    Scaffold(
                        scaffoldState = scaffoldState
                    ) {
                        AndroidViewBinding(factory = ContentMainBinding::inflate)
                    }
                }
            }
        }
    }
}

