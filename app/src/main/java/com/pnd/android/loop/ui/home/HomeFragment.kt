package com.pnd.android.loop.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.pnd.android.loop.ui.AppNavHost
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.util.LocalBackPressedDispatcher
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val loopViewModel by activityViewModels<LoopViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)

        setContent {
            CompositionLocalProvider(
                LocalBackPressedDispatcher provides requireActivity().onBackPressedDispatcher,
            ) {
                AppNavHost(
                    loopViewModel = loopViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loopViewModel.loadWiseSaying()
        loopViewModel.syncLoops()
    }
}