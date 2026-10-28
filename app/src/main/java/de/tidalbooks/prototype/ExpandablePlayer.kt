package de.tidalbooks.prototype

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlin.math.roundToInt

/** Ein dauerhaftes Player-Panel: nur seine Grenzen ändern sich, nie die Ansicht dahinter. */
@Composable
internal fun ExpandablePlayer(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    miniBounds: Rect,
    miniContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val contentState = rememberSaveableStateHolder()
    var progress by remember { mutableFloatStateOf(if (expanded) 1f else 0f) }
    var settleRequest by remember { mutableIntStateOf(0) }
    var velocity by remember { mutableFloatStateOf(0f) }
    var motion by remember { mutableStateOf<Job?>(null) }
    var isAnimating by remember { mutableStateOf(false) }
    val currentExpandedChange by rememberUpdatedState(onExpandedChange)
    val inset = with(density) { 12.dp.toPx() }
    val verticalInset = with(density) { 6.dp.toPx() }
    val startTop = miniBounds.top + verticalInset
    val travel = startTop.coerceAtLeast(1f)
    val flingThreshold = with(density) { 400.dp.toPx() }
    val dismissDistance = with(density) { 96.dp.toPx() }
    var gestureStart by remember { mutableFloatStateOf(progress) }
    var nestedDragging by remember { mutableStateOf(false) }

    fun beginDrag() {
        motion?.cancel()
        gestureStart = progress
    }
    fun settle(pixelsPerSecond: Float) {
        velocity = -pixelsPerSecond / travel
        val open = when {
            pixelsPerSecond < -flingThreshold -> true
            pixelsPerSecond > flingThreshold -> false
            gestureStart >= .99f -> (1f - progress) * travel < dismissDistance
            else -> progress >= .5f
        }
        currentExpandedChange(open)
        nestedDragging = false
        settleRequest++
    }
    fun dragBy(delta: Float): Float {
        val previous = progress
        progress = (progress - delta / travel).coerceIn(0f, 1f)
        return (previous - progress) * travel
    }

    // Scrollen hat Vorrang, bis die Inhaltsliste oben angekommen ist. Ab dort
    // übernimmt das Panel den überschüssigen Weg derselben Geste.
    val nestedScroll = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || progress >= 1f) return Offset.Zero
            if (!nestedDragging) { beginDrag(); nestedDragging = true }
            return Offset(0f, dragBy(available.y))
        }
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
            if (!nestedDragging) { beginDrag(); nestedDragging = true }
            return Offset(0f, dragBy(available.y))
        }
        override suspend fun onPreFling(available: Velocity): Velocity {
            if (!nestedDragging) return Velocity.Zero
            settle(available.y)
            return available
        }
    }

    LaunchedEffect(expanded, settleRequest) {
        motion = currentCoroutineContext().job
        isAnimating = true
        try {
            animate(progress, if (expanded) 1f else 0f, initialVelocity = velocity,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow)) { value, _ ->
                progress = value.coerceIn(0f, 1f)
            }
        } finally {
            isAnimating = false
        }
        velocity = 0f
    }
    DisposableEffect(Unit) { onDispose { motion?.cancel() } }
    BackHandler(progress > 0f) { currentExpandedChange(false); settleRequest++ }

    val dragState = rememberDraggableState { delta ->
        dragBy(delta)
    }
    val dragModifier = Modifier.draggable(
        state = dragState,
        orientation = Orientation.Vertical,
        startDragImmediately = isAnimating,
        onDragStarted = { beginDrag() },
        onDragStopped = { settle(it) },
    )

    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val fullWidth = maxWidth
        val fullHeight = maxHeight
        val widthPx = with(density) { fullWidth.toPx() }
        val heightPx = with(density) { fullHeight.toPx() }
        val p = progress
        val left = (miniBounds.left + inset) * (1f - p)
        val top = startTop * (1f - p)
        val width = (miniBounds.width - 2f * inset) * (1f - p) + widthPx * p
        val height = (miniBounds.height - 2f * verticalInset) * (1f - p) + heightPx * p

        // Keine Abdunklung, kein Austausch der Bibliothek. Nur Eingaben hinter dem
        // teilweise geöffneten Panel abfangen, damit dort nichts versehentlich wechselt.
        if (p > 0f) Box(Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent().changes.forEach { it.consume() }
            }
        })
        Surface(
            modifier = Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(with(density) { width.toDp() }, with(density) { height.toDp() })
                .testTag("expandable_player")
                .nestedScroll(nestedScroll)
                .then(dragModifier),
            shape = RoundedCornerShape(24.dp * (1f - p)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box {
                // Vollansicht stets in ihrer endgültigen Größe messen: kein Umbruch
                // und kein springender Scrollbereich während des Ziehens.
                if (p > 0f) Box(Modifier
                    .wrapContentSize(unbounded = true, align = androidx.compose.ui.Alignment.TopStart)
                    .requiredSize(fullWidth, fullHeight)
                    .graphicsLayer { alpha = ((p - .1f) / .55f).coerceIn(0f, 1f) }
                    .then(if (p < .65f) Modifier.clearAndSetSemantics {} else Modifier)) {
                    contentState.SaveableStateProvider("player") { content() }
                }
                if (p < .65f) Box(Modifier.fillMaxWidth()
                    .graphicsLayer { alpha = (1f - p / .35f).coerceIn(0f, 1f) }) {
                    miniContent()
                }
            }
        }
    }
}
