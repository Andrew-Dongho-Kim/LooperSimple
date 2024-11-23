package com.pnd.android.loop.ui.home.input

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.home.BlurState
import com.pnd.android.loop.ui.home.input.SharedElementsOfUserInput.KEY_BUTTON_IMAGE
import com.pnd.android.loop.ui.home.input.SharedElementsOfUserInput.TRANSITION_DURATION
import com.pnd.android.loop.ui.home.input.selector.Selectors
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.BackPressHandler
import com.pnd.android.loop.util.rememberImeOpenState
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UserInput(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    inputState: UserInputState,
    snackBarHostState: SnackbarHostState,
    lazyListState: LazyListState,
    onEnsureLoop: suspend (LoopBase) -> Boolean,
    onLoopSubmitted: (LoopBase) -> Unit,
) {
    SharedTransitionLayout(modifier = modifier) {

        AnimatedContent(
            targetState = inputState.isVisible,
            label = "shared_transition",
        ) { isExpanded ->
            if (isExpanded) {
                UserInput(
                    blurState = blurState,
                    inputState = inputState,
                    snackBarHostState = snackBarHostState,
                    lazyListState = lazyListState,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@AnimatedContent,
                    onEnsureLoop = onEnsureLoop,
                    onLoopSubmitted = onLoopSubmitted
                )
            } else {
                UserInputExpandButton(
                    inputState = inputState,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent
                )
            }
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UserInput(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    inputState: UserInputState,
    snackBarHostState: SnackbarHostState,
    lazyListState: LazyListState,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedVisibilityScope,
    onEnsureLoop: suspend (LoopBase) -> Boolean,
    onLoopSubmitted: (LoopBase) -> Unit,
) {
    val keyboardShown by rememberImeOpenState()
    val focusRequester = FocusRequester()

    val coroutineScope = rememberCoroutineScope()
    SideEffect {
        if (!keyboardShown) focusRequester.requestFocus()

        if (inputState.mode == UserInputState.Mode.New) {
            coroutineScope.launch { lazyListState.animateScrollToItem(0) }
        }
    }

    OverrideBackPress(inputState = inputState)

    Column(
        modifier
            .background(color = if (inputState.isVisible) AppColor.surface else Color.Transparent)
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = AppColor.onSurface.copy(alpha = 0.3f)
        )
        UserInputText(
            textField = inputState.textFieldValue,
            hasFocus = keyboardShown,
            onTextChanged = { textFiledValue -> inputState.update(title = textFiledValue) },
            onTextFieldFocused = { focused ->
                inputState.setTextFieldFocused(focused)
                if (focused) {
                    inputState.setSelector(InputSelector.NONE)
                }
            }
        )

        UserInputButtons(
            modifier = with(sharedTransitionScope) { Modifier.skipToLookaheadSize() },
            inputState = inputState,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedContentScope,
            onSubmitted = {
                coroutineScope.launch {
                    val loop = inputState.value
                    if (onEnsureLoop(loop)) {
                        onLoopSubmitted(loop)
                        inputState.reset()
                    }
                }
            },
        )

        Selectors(
            inputState = inputState,
            blurState = blurState,
            snackBarHostState = snackBarHostState,
            focusRequester = focusRequester,
        )
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun UserInputExpandButton(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    with(sharedTransitionScope) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(
                    bottom = 32.dp,
                    end = 32.dp
                )
        ) {

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(56.dp)
                    .shadow(2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .clickable {
                        inputState.toggleOpen(context)
                    }
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(
                            key = SharedElementsOfUserInput.KEY_BUTTON_BACKGROUND
                        ),
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                shape = CircleShape,
                border = BorderStroke(
                    width = 0.5.dp,
                    color = AppColor.onSurface.copy(alpha = 0.3f),
                )
            ) {

                val imageRotation by animatedVisibilityScope.transition.animateFloat(
                    transitionSpec = {
                        tween(
                            delayMillis = 250,
                            durationMillis = TRANSITION_DURATION,
                        )
                    },
                    label = "imageRotation"
                ) { state ->
                    when (state) {
                        EnterExitState.PreEnter -> 180f
                        EnterExitState.PostExit, EnterExitState.Visible -> 0f
                    }
                }

                Icon(
                    modifier = Modifier
                        .wrapContentWidth()
                        .wrapContentHeight()
                        .sharedElement(
                            state = rememberSharedContentState(
                                key = KEY_BUTTON_IMAGE
                            ),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                tween(
                                    durationMillis = TRANSITION_DURATION,
                                )
                            }
                        )
                        .graphicsLayer {
                            rotationZ = imageRotation
                        },
                    imageVector = Icons.Outlined.Add,
                    tint = AppColor.primary,
                    contentDescription = ""
                )
            }
        }
    }
}


@Composable
private fun OverrideBackPress(
    inputState: UserInputState,
) {
    if (inputState.currSelector != InputSelector.NONE) {
        BackPressHandler {
            inputState.setSelector(InputSelector.NONE)
        }
    } else if (inputState.mode != UserInputState.Mode.None) {
        BackPressHandler {
            inputState.reset()
        }
    }
}
