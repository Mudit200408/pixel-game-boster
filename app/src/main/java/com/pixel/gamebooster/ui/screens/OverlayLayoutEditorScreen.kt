package com.pixel.gamebooster.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pixel.gamebooster.R
import com.pixel.gamebooster.data.repository.SettingsRepository
import com.pixel.gamebooster.overlay.model.OverlayBlockConfig
import com.pixel.gamebooster.overlay.model.OverlayBlockType
import com.pixel.gamebooster.overlay.model.OverlayLayoutConfig
import com.pixel.gamebooster.overlay.model.OverlayTileCategory
import com.pixel.gamebooster.overlay.model.OverlayTileType
import com.pixel.gamebooster.overlay.model.TelemetryMetric
import com.pixel.gamebooster.overlay.ui.ExpandedPanelModeSwitcher
import com.pixel.gamebooster.overlay.ui.ExpandedPanelTelemetryPill
import com.pixel.gamebooster.overlay.ui.ExpressiveSystemSlider
import com.pixel.gamebooster.overlay.ui.M3EActionTile
import com.pixel.gamebooster.overlay.ui.iconVector
import com.pixel.gamebooster.overlay.ui.labelRes
import com.pixel.gamebooster.overlay.ui.titleRes
import com.pixel.gamebooster.ui.components.FloatingBackTitleToolbar
import com.pixel.gamebooster.ui.core.cards.IconToggleItem
import com.pixel.gamebooster.ui.core.containers.RoundedCardContainer
import com.pixel.gamebooster.ui.theme.GoogleSansFlexRounded
import com.pixel.gamebooster.ui.util.rememberAdaptiveLayoutInfo
import com.pixel.gamebooster.utils.HapticUtil
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class OverlayLayoutTab {
    PANEL,
    ACTION_BUTTONS,
}

private sealed interface SelectedOverlayItem {
    data class Block(val type: OverlayBlockType) : SelectedOverlayItem
    data class Tile(val tile: OverlayTileType) : SelectedOverlayItem
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OverlayLayoutEditorScreen(
    repo: SettingsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val layout by repo.overlayLayout.collectAsState()
    val adaptive = rememberAdaptiveLayoutInfo()
    val bottomPadding = if (adaptive.isCompactHeight) 80.dp else 108.dp

    var isEditMode by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<SelectedOverlayItem?>(null) }
    var selectedTab by remember { mutableStateOf(OverlayLayoutTab.PANEL) }

    val canRemove = isEditMode && selectedItem != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState(), enabled = !isEditMode)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Live Preview Header + Top-Right Remove Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.overlay_layout_preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                if (isEditMode) {
                    Button(
                        onClick = {
                            val current = selectedItem ?: return@Button
                            HapticUtil.performVirtualKeyHaptic(view)
                            when (current) {
                                is SelectedOverlayItem.Block -> {
                                    repo.setOverlayLayout(layout.setBlockEnabled(current.type, false))
                                }
                                is SelectedOverlayItem.Tile -> {
                                    repo.setOverlayLayout(layout.removeTile(current.tile))
                                }
                            }
                            selectedItem = null
                        },
                        enabled = canRemove,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.overlay_layout_remove),
                            style = TextStyle(
                                fontFamily = GoogleSansFlexRounded,
                                fontWeight = if (canRemove) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                            ),
                        )
                    }
                }
            }

            // 2. Exact-Size Live Preview + Edit/Reset Controls Row
            OverlayLayoutLivePreview(
                layout = layout,
                isEditMode = isEditMode,
                selectedItem = selectedItem,
                onSelectItem = { item ->
                    HapticUtil.performToggle(view)
                    selectedItem = if (selectedItem == item) null else item
                },
                onClearSelection = {
                    selectedItem = null
                },
                onToggleEditMode = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    isEditMode = !isEditMode
                    if (!isEditMode) selectedItem = null
                },
                onReset = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    repo.resetOverlayLayout()
                },
                onMoveBlock = { fromIndex, toIndex ->
                    HapticUtil.performUIHaptic(view)
                    repo.setOverlayLayout(repo.overlayLayout.value.moveBlock(fromIndex, toIndex))
                },
                onMoveTile = { fromIndex, toIndex ->
                    HapticUtil.performUIHaptic(view)
                    repo.setOverlayLayout(repo.overlayLayout.value.moveTile(fromIndex, toIndex))
                },
            )

            // 3. Below Sections (hidden while editing the Live Preview)
            AnimatedVisibility(
                visible = !isEditMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Big Connected Toggle Button: Panel <-> Action Buttons
                    val tabs = listOf(
                        OverlayLayoutTab.PANEL to stringResource(R.string.overlay_layout_tab_panel),
                        OverlayLayoutTab.ACTION_BUTTONS to stringResource(R.string.overlay_layout_tab_action_buttons),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        tabs.forEachIndexed { idx, (tab, label) ->
                            val isSelected = selectedTab == tab
                            ToggleButton(
                                checked = isSelected,
                                onCheckedChange = {
                                    HapticUtil.performToggle(view)
                                    selectedTab = tab
                                },
                                shapes = when (idx) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                },
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .semantics { role = Role.RadioButton },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "layout_tab_content",
                    ) { tab ->
                        when (tab) {
                            OverlayLayoutTab.PANEL -> {
                                PanelBlocksSection(
                                    layout = layout,
                                    onToggleBlock = { blockType, enabled ->
                                        HapticUtil.performToggle(view)
                                        repo.setOverlayLayout(layout.setBlockEnabled(blockType, enabled))
                                    },
                                    onToggleMetric = { metric, enabled ->
                                        HapticUtil.performToggle(view)
                                        repo.setOverlayLayout(layout.setTelemetryMetricEnabled(metric, enabled))
                                    },
                                )
                            }
                            OverlayLayoutTab.ACTION_BUTTONS -> {
                                ActionButtonsSwitchesSection(
                                    layout = layout,
                                    onToggleTile = { tile, enabled ->
                                        HapticUtil.performToggle(view)
                                        repo.setOverlayLayout(
                                            if (enabled) layout.addTile(tile) else layout.removeTile(tile),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingBackTitleToolbar(
            title = stringResource(R.string.overlay_layout_screen_title),
            onBack = {
                if (isEditMode) {
                    isEditMode = false
                    selectedItem = null
                } else {
                    onBack()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PanelBlocksSection(
    layout: OverlayLayoutConfig,
    onToggleBlock: (OverlayBlockType, Boolean) -> Unit,
    onToggleMetric: (TelemetryMetric, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.overlay_layout_section_blocks),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
            Text(
                text = stringResource(R.string.overlay_layout_section_blocks_desc),
                style = TextStyle(
                    fontFamily = GoogleSansFlexRounded,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        RoundedCardContainer {
            layout.blocks.forEachIndexed { index, block ->
                val subtitle = when (block.type) {
                    OverlayBlockType.TELEMETRY_PILL -> stringResource(R.string.overlay_layout_block_telemetry_desc)
                    OverlayBlockType.MODE_SWITCHER -> stringResource(R.string.overlay_layout_block_mode_desc)
                    OverlayBlockType.ACTION_TILES -> stringResource(R.string.overlay_layout_block_tiles_desc, layout.tiles.size)
                    OverlayBlockType.BRIGHTNESS_SLIDER -> stringResource(R.string.overlay_layout_block_brightness_desc)
                    OverlayBlockType.VOLUME_SLIDER -> stringResource(R.string.overlay_layout_block_volume_desc)
                }
                val blockIcon: ImageVector = when (block.type) {
                    OverlayBlockType.TELEMETRY_PILL -> Icons.Rounded.MonitorHeart
                    OverlayBlockType.MODE_SWITCHER -> Icons.Rounded.Speed
                    OverlayBlockType.ACTION_TILES -> Icons.Rounded.GridView
                    OverlayBlockType.BRIGHTNESS_SLIDER -> Icons.Rounded.LightMode
                    OverlayBlockType.VOLUME_SLIDER -> Icons.AutoMirrored.Rounded.VolumeUp
                }

                IconToggleItem(
                    title = stringResource(block.type.titleRes()),
                    subtitle = subtitle,
                    icon = blockIcon,
                    checked = block.enabled,
                    onCheckedChange = { enabled -> onToggleBlock(block.type, enabled) },
                    index = index,
                    count = layout.blocks.size,
                )

                AnimatedVisibility(
                    visible = block.type == OverlayBlockType.TELEMETRY_PILL && block.enabled,
                ) {
                    val metrics = TelemetryMetric.entries
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        metrics.forEachIndexed { idx, metric ->
                            val selected = layout.telemetryMetrics.contains(metric)
                            ToggleButton(
                                checked = selected,
                                onCheckedChange = { onToggleMetric(metric, !selected) },
                                shapes = when (idx) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    metrics.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                                colors = ToggleButtonDefaults.toggleButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.Checkbox },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    text = stringResource(metric.labelRes()),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = GoogleSansFlexRounded,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsSwitchesSection(
    layout: OverlayLayoutConfig,
    onToggleTile: (OverlayTileType, Boolean) -> Unit,
) {
    val tilesByCategory = OverlayTileType.entries.groupBy { it.category }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.overlay_layout_section_tiles_desc),
            style = TextStyle(
                fontFamily = GoogleSansFlexRounded,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        OverlayTileCategory.entries.forEach { category ->
            val categoryTiles = tilesByCategory[category].orEmpty()
            if (categoryTiles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(category.labelRes()),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    )

                    RoundedCardContainer {
                        categoryTiles.forEachIndexed { index, tile ->
                            val isEnabled = layout.tiles.contains(tile)
                            IconToggleItem(
                                title = stringResource(tile.labelRes()),
                                icon = tile.iconVector(),
                                checked = isEnabled,
                                onCheckedChange = { enabled -> onToggleTile(tile, enabled) },
                                index = index,
                                count = categoryTiles.size,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun computeBlockSlotTops(
    blocks: List<OverlayBlockConfig>,
    blockHeights: Map<OverlayBlockType, Int>,
    spacingPx: Float,
): Map<OverlayBlockType, Float> {
    val map = mutableMapOf<OverlayBlockType, Float>()
    var y = 0f
    blocks.forEach { block ->
        map[block.type] = y
        y += (blockHeights[block.type] ?: 0).toFloat() + spacingPx
    }
    return map
}

private fun computeBlockTargetIdx(
    activeBlocks: List<OverlayBlockConfig>,
    draggedType: OverlayBlockType,
    blockHeights: Map<OverlayBlockType, Int>,
    baseSlotTops: Map<OverlayBlockType, Float>,
    dragOffsetY: Float,
): Int {
    val draggedH = (blockHeights[draggedType] ?: 0).toFloat()
    val draggedCenterY = (baseSlotTops[draggedType] ?: 0f) + draggedH / 2f + dragOffsetY
    var idx = 0
    activeBlocks.forEach { other ->
        if (other.type != draggedType) {
            val otherH = (blockHeights[other.type] ?: 0).toFloat()
            val otherCenterY = (baseSlotTops[other.type] ?: 0f) + otherH / 2f
            if (draggedCenterY > otherCenterY) idx++
        }
    }
    return idx
}

private fun computeTileTargetIdx(
    tileCount: Int,
    fromIdx: Int,
    colWidthPx: Float,
    tileHeightPx: Float,
    hSpacingPx: Float,
    vSpacingPx: Float,
    dragOffsetX: Float,
    dragOffsetY: Float,
): Int {
    if (fromIdx < 0 || colWidthPx <= 0f || tileHeightPx <= 0f) return -1
    fun slotX(i: Int) = (i % 3) * (colWidthPx + hSpacingPx)
    fun slotY(i: Int) = (i / 3) * (tileHeightPx + vSpacingPx)
    val cx = slotX(fromIdx) + colWidthPx / 2f + dragOffsetX
    val cy = slotY(fromIdx) + tileHeightPx / 2f + dragOffsetY
    return (0 until tileCount).minByOrNull { idx ->
        val dx = (slotX(idx) + colWidthPx / 2f) - cx
        val dy = (slotY(idx) + tileHeightPx / 2f) - cy
        dx * dx + dy * dy
    } ?: fromIdx
}

@Composable
private fun OverlayLayoutLivePreview(
    layout: OverlayLayoutConfig,
    isEditMode: Boolean,
    selectedItem: SelectedOverlayItem?,
    onSelectItem: (SelectedOverlayItem) -> Unit,
    onClearSelection: () -> Unit,
    onToggleEditMode: () -> Unit,
    onReset: () -> Unit,
    onMoveBlock: (Int, Int) -> Unit,
    onMoveTile: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeBlocks = layout.blocks.filter { block ->
        block.enabled && when (block.type) {
            OverlayBlockType.TELEMETRY_PILL -> layout.telemetryMetrics.isNotEmpty()
            OverlayBlockType.ACTION_TILES -> layout.tiles.isNotEmpty()
            else -> true
        }
    }

    val density = LocalDensity.current
    val spacingPx = with(density) { 10.dp.toPx() }
    val scope = rememberCoroutineScope()
    val blockHeights = remember { mutableStateMapOf<OverlayBlockType, Int>() }

    var draggingBlock by remember { mutableStateOf<OverlayBlockType?>(null) }
    var dragSnapshotBlocks by remember { mutableStateOf<List<OverlayBlockConfig>?>(null) }
    var isDropping by remember { mutableStateOf(false) }
    var isTileDragging by remember { mutableStateOf(false) }
    var lockedTargetIdx by remember { mutableIntStateOf(-1) }
    val dragOffsetY = remember { Animatable(0f) }

    val bouncySwapSpec = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
    }
    val dropSettleSpec = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    }

    val currentLayout by rememberUpdatedState(layout)
    val currentActiveBlocks by rememberUpdatedState(activeBlocks)
    val currentOnSelectItem by rememberUpdatedState(onSelectItem)
    val currentOnClearSelection by rememberUpdatedState(onClearSelection)
    val currentOnMoveBlock by rememberUpdatedState(onMoveBlock)

    // Freeze the rendered order while dragging/settling so committing the repo update
    // never causes other blocks to re-animate before the drop handoff completes.
    val renderedBlocks = dragSnapshotBlocks ?: activeBlocks

    val baseSlotTops by remember(renderedBlocks, spacingPx) {
        derivedStateOf {
            computeBlockSlotTops(renderedBlocks, blockHeights, spacingPx)
        }
    }

    val draggedType = draggingBlock
    val dragFromIdx = if (draggedType != null) renderedBlocks.indexOfFirst { it.type == draggedType } else -1
    val dragTargetIdx by remember(renderedBlocks, draggedType, dragFromIdx, isDropping, lockedTargetIdx) {
        derivedStateOf {
            if (draggedType != null && dragFromIdx >= 0) {
                if (isDropping && lockedTargetIdx >= 0) {
                    lockedTargetIdx
                } else {
                    computeBlockTargetIdx(renderedBlocks, draggedType, blockHeights, baseSlotTops, dragOffsetY.value)
                }
            } else {
                -1
            }
        }
    }

    val previewSlotTops by remember(renderedBlocks, draggedType, dragFromIdx, spacingPx) {
        derivedStateOf {
            if (draggedType == null || dragFromIdx < 0 || dragTargetIdx < 0) {
                baseSlotTops
            } else {
                val reordered = renderedBlocks.toMutableList()
                val moved = reordered.removeAt(dragFromIdx)
                reordered.add(dragTargetIdx.coerceIn(0, reordered.size), moved)
                computeBlockSlotTops(reordered, blockHeights, spacingPx)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(348.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f))
                .then(
                    if (isEditMode) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(28.dp),
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            if (renderedBlocks.isEmpty()) {
                Text(
                    text = stringResource(R.string.overlay_layout_preview_empty),
                    style = TextStyle(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (draggedType != null) {
                        val dropTop = previewSlotTops[draggedType] ?: 0f
                        val dropHeightDp = with(density) { (blockHeights[draggedType] ?: 0).toDp() }
                        val padDp = 6.dp
                        val padPx = with(density) { padDp.toPx() }
                        val slotShape = blockHighlightShape(draggedType)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset { IntOffset(0, (dropTop - padPx).roundToInt()) }
                                .requiredWidth(308.dp + padDp * 2)
                                .height(dropHeightDp + padDp * 2)
                                .clip(slotShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = slotShape,
                                ),
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        renderedBlocks.forEach { block ->
                            key(block.type) {
                                val isDraggingThisBlock = draggedType == block.type
                                val showSelectionBorder = isEditMode &&
                                    selectedItem == SelectedOverlayItem.Block(block.type) &&
                                    draggedType == null &&
                                    !isDropping

                                val targetShiftY = if (draggedType != null && !isDraggingThisBlock) {
                                    (previewSlotTops[block.type] ?: 0f) - (baseSlotTops[block.type] ?: 0f)
                                } else {
                                    0f
                                }
                                val animatedShiftY by animateFloatAsState(
                                    targetValue = targetShiftY,
                                    animationSpec = if (draggedType != null) bouncySwapSpec else snap(),
                                    label = "block_rearrange_shift",
                                )

                                val scale by animateFloatAsState(
                                    targetValue = if (isDraggingThisBlock && !isDropping) 1.03f else 1f,
                                    animationSpec = if (draggedType != null) {
                                        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                                    } else {
                                        snap()
                                    },
                                    label = "block_drag_scale",
                                )
                                val elevation by animateFloatAsState(
                                    targetValue = if (isDraggingThisBlock && !isDropping) 16f else 0f,
                                    label = "block_drag_elevation",
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(if (isDraggingThisBlock) 10f else 0f)
                                        .onSizeChanged { size -> blockHeights[block.type] = size.height }
                                        .graphicsLayer {
                                            translationY = when {
                                                draggedType == null -> 0f
                                                isDraggingThisBlock -> dragOffsetY.value
                                                else -> animatedShiftY
                                            }
                                            scaleX = if (draggedType == null) 1f else scale
                                            scaleY = if (draggedType == null) 1f else scale
                                            shadowElevation = if (draggedType == null) 0f else elevation
                                        }
                                        .then(
                                            if (showSelectionBorder) {
                                                val selectionShape = blockHighlightShape(block.type)
                                                Modifier
                                                    .clip(selectionShape)
                                                    .border(
                                                        width = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = selectionShape,
                                                    )
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                ) {
                                    fun Modifier.attachBlockGestures(blockType: OverlayBlockType): Modifier = this
                                        .pointerInput(blockType) {
                                            detectTapGestures(
                                                onTap = {
                                                    if (!isDropping && draggingBlock == null && !isTileDragging) {
                                                        currentOnSelectItem(SelectedOverlayItem.Block(blockType))
                                                    }
                                                },
                                            )
                                        }
                                        .pointerInput(blockType) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    if (isDropping || isTileDragging) return@detectDragGestures
                                                    currentOnClearSelection()
                                                    dragSnapshotBlocks = currentActiveBlocks
                                                    draggingBlock = blockType
                                                    lockedTargetIdx = -1
                                                    scope.launch { dragOffsetY.snapTo(0f) }
                                                },
                                                onDragEnd = {
                                                    if (draggingBlock != blockType || isDropping) return@detectDragGestures
                                                    val snapshot = dragSnapshotBlocks ?: currentActiveBlocks
                                                    val liveLayout = currentLayout
                                                    val fromIdx = snapshot.indexOfFirst { it.type == blockType }
                                                    val liveBaseTops = computeBlockSlotTops(snapshot, blockHeights, spacingPx)
                                                    val targetIdx = if (fromIdx >= 0) {
                                                        computeBlockTargetIdx(snapshot, blockType, blockHeights, liveBaseTops, dragOffsetY.value)
                                                    } else {
                                                        -1
                                                    }
                                                    isDropping = true
                                                    lockedTargetIdx = targetIdx

                                                    val dropOffset = if (fromIdx >= 0 && targetIdx >= 0) {
                                                        val reordered = snapshot.toMutableList()
                                                        val moved = reordered.removeAt(fromIdx)
                                                        reordered.add(targetIdx.coerceIn(0, reordered.size), moved)
                                                        val finalTops = computeBlockSlotTops(reordered, blockHeights, spacingPx)
                                                        (finalTops[blockType] ?: 0f) - (liveBaseTops[blockType] ?: 0f)
                                                    } else {
                                                        0f
                                                    }

                                                    // Commit immediately for instant haptic & state update while
                                                    // dragSnapshotBlocks keeps the UI visually frozen during settle.
                                                    if (fromIdx >= 0 && targetIdx >= 0 && targetIdx != fromIdx) {
                                                        val origFrom = liveLayout.blocks.indexOfFirst { it.type == snapshot[fromIdx].type }
                                                        val origTo = liveLayout.blocks.indexOfFirst { it.type == snapshot[targetIdx].type }
                                                        if (origFrom >= 0 && origTo >= 0 && origFrom != origTo) {
                                                            currentOnMoveBlock(origFrom, origTo)
                                                        }
                                                    }

                                                    scope.launch {
                                                        dragOffsetY.animateTo(dropOffset, dropSettleSpec)
                                                        dragOffsetY.snapTo(0f)
                                                        draggingBlock = null
                                                        dragSnapshotBlocks = null
                                                        isDropping = false
                                                        lockedTargetIdx = -1
                                                    }
                                                },
                                                onDragCancel = {
                                                    if (draggingBlock != blockType || isDropping) return@detectDragGestures
                                                    val snapshot = dragSnapshotBlocks ?: currentActiveBlocks
                                                    isDropping = true
                                                    lockedTargetIdx = snapshot.indexOfFirst { it.type == blockType }
                                                    scope.launch {
                                                        dragOffsetY.animateTo(0f, dropSettleSpec)
                                                        dragOffsetY.snapTo(0f)
                                                        draggingBlock = null
                                                        dragSnapshotBlocks = null
                                                        isDropping = false
                                                        lockedTargetIdx = -1
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    if (draggingBlock != blockType || isDropping) return@detectDragGestures
                                                    change.consume()
                                                    scope.launch { dragOffsetY.snapTo(dragOffsetY.value + dragAmount.y) }
                                                },
                                            )
                                        }

                                    when (block.type) {
                                        OverlayBlockType.TELEMETRY_PILL -> {
                                            ExpandedPanelTelemetryPill(
                                                metrics = layout.telemetryMetrics,
                                                currentFps = 120,
                                                temperatureC = 37,
                                                wattageW = 4.1f,
                                                batteryPercent = 84,
                                            )
                                        }

                                        OverlayBlockType.MODE_SWITCHER -> {
                                            ExpandedPanelModeSwitcher(
                                                selectedMode = 2,
                                                onModeSelected = {},
                                            )
                                        }

                                        OverlayBlockType.ACTION_TILES -> {
                                            // 2-Layer Action Buttons Component:
                                            // Layer 1 (Outer): Edit-mode container + top handle bar to select/remove or drag the entire section.
                                            // Layer 2 (Inner): DraggablePreviewTileGrid to select/remove or rearrange individual buttons inside.
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(
                                                        if (isEditMode) {
                                                            Modifier
                                                                .clip(RoundedCornerShape(18.dp))
                                                                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f))
                                                                .padding(horizontal = 6.dp, vertical = 6.dp)
                                                        } else {
                                                            Modifier
                                                        },
                                                    ),
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                if (isEditMode) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .attachBlockGestures(OverlayBlockType.ACTION_TILES)
                                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                                        contentAlignment = Alignment.CenterEnd,
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(12.dp)
                                                                    .height(3.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                                                            )
                                                        }
                                                    }
                                                }

                                                DraggablePreviewTileGrid(
                                                    tiles = layout.tiles,
                                                    isEditMode = isEditMode && draggingBlock == null && !isDropping,
                                                    selectedTile = (selectedItem as? SelectedOverlayItem.Tile)?.tile,
                                                    onSelectTile = { tile -> currentOnSelectItem(SelectedOverlayItem.Tile(tile)) },
                                                    onClearSelection = currentOnClearSelection,
                                                    onMoveTile = onMoveTile,
                                                    onTileDragActiveChange = { active -> isTileDragging = active },
                                                )
                                            }
                                        }

                                        OverlayBlockType.BRIGHTNESS_SLIDER -> {
                                            ExpressiveSystemSlider(
                                                value = 0.72f,
                                                onValueChange = {},
                                                icon = Icons.Rounded.LightMode,
                                                contentDescription = stringResource(R.string.overlay_brightness_title),
                                                interactive = false,
                                            )
                                        }

                                        OverlayBlockType.VOLUME_SLIDER -> {
                                            ExpressiveSystemSlider(
                                                value = 0.54f,
                                                onValueChange = {},
                                                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                                                contentDescription = stringResource(R.string.overlay_volume_title),
                                                interactive = false,
                                            )
                                        }
                                    }

                                    if (isEditMode && block.type != OverlayBlockType.ACTION_TILES) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .attachBlockGestures(block.type),
                                        )
                                    } else if (!isEditMode) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .pointerInput(Unit) {
                                                    detectTapGestures { /* Non-interactive preview */ }
                                                },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Edit / Reset Bar right below the Live Preview card
        Row(
            modifier = Modifier.width(348.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isEditMode) {
                Text(
                    text = stringResource(R.string.overlay_layout_edit_hint),
                    style = TextStyle(
                        fontFamily = GoogleSansFlexRounded,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onToggleEditMode,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.overlay_layout_done),
                        style = TextStyle(
                            fontFamily = GoogleSansFlexRounded,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        ),
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onReset,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.overlay_layout_reset),
                        style = TextStyle(
                            fontFamily = GoogleSansFlexRounded,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                        ),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                FilledTonalButton(
                    onClick = onToggleEditMode,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.overlay_layout_edit),
                        style = TextStyle(
                            fontFamily = GoogleSansFlexRounded,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggablePreviewTileGrid(
    tiles: List<OverlayTileType>,
    isEditMode: Boolean,
    selectedTile: OverlayTileType?,
    onSelectTile: (OverlayTileType) -> Unit,
    onClearSelection: () -> Unit,
    onMoveTile: (Int, Int) -> Unit,
    onTileDragActiveChange: (Boolean) -> Unit = {},
) {
    val density = LocalDensity.current
    val hSpacingPx = with(density) { 4.dp.toPx() }
    val vSpacingPx = with(density) { 8.dp.toPx() }
    val scope = rememberCoroutineScope()

    var gridWidthPx by remember { mutableIntStateOf(0) }
    val tileSizes = remember { mutableStateMapOf<OverlayTileType, IntSize>() }

    var draggingTile by remember { mutableStateOf<OverlayTileType?>(null) }
    var dragSnapshotTiles by remember { mutableStateOf<List<OverlayTileType>?>(null) }
    var isDropping by remember { mutableStateOf(false) }
    var lockedTargetIdx by remember { mutableIntStateOf(-1) }
    val dragOffsetX = remember { Animatable(0f) }
    val dragOffsetY = remember { Animatable(0f) }

    val bouncySwapSpec = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
    }
    val dropSettleSpec = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    }

    val currentTiles by rememberUpdatedState(tiles)
    val currentOnSelectTile by rememberUpdatedState(onSelectTile)
    val currentOnClearSelection by rememberUpdatedState(onClearSelection)
    val currentOnMoveTile by rememberUpdatedState(onMoveTile)
    val currentOnTileDragActiveChange by rememberUpdatedState(onTileDragActiveChange)

    val renderedTiles = dragSnapshotTiles ?: tiles

    val colWidthPx = if (gridWidthPx > 0) (gridWidthPx - 2f * hSpacingPx) / 3f else 0f
    val tileHeightPx = (tileSizes.values.firstOrNull()?.height ?: 0).toFloat()

    fun slotX(index: Int): Float = (index % 3) * (colWidthPx + hSpacingPx)
    fun slotY(index: Int): Float = (index / 3) * (tileHeightPx + vSpacingPx)

    val draggedTile = draggingTile
    val dragFromIdx = if (draggedTile != null) renderedTiles.indexOf(draggedTile) else -1
    val dragTargetIdx by remember(
        renderedTiles,
        draggedTile,
        dragFromIdx,
        isDropping,
        lockedTargetIdx,
        colWidthPx,
        tileHeightPx,
        hSpacingPx,
        vSpacingPx,
    ) {
        derivedStateOf {
            if (draggedTile != null && dragFromIdx >= 0) {
                if (isDropping && lockedTargetIdx >= 0) {
                    lockedTargetIdx
                } else {
                    computeTileTargetIdx(
                        tileCount = renderedTiles.size,
                        fromIdx = dragFromIdx,
                        colWidthPx = colWidthPx,
                        tileHeightPx = tileHeightPx,
                        hSpacingPx = hSpacingPx,
                        vSpacingPx = vSpacingPx,
                        dragOffsetX = dragOffsetX.value,
                        dragOffsetY = dragOffsetY.value,
                    )
                }
            } else {
                -1
            }
        }
    }

    val previewOrder = remember(renderedTiles, dragFromIdx, dragTargetIdx) {
        if (dragFromIdx >= 0 && dragTargetIdx >= 0 && dragFromIdx != dragTargetIdx) {
            renderedTiles.toMutableList().apply {
                val moved = removeAt(dragFromIdx)
                add(dragTargetIdx.coerceIn(0, size), moved)
            }
        } else {
            renderedTiles
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { gridWidthPx = it.width },
    ) {
        if (draggedTile != null && dragTargetIdx >= 0 && colWidthPx > 0f && tileHeightPx > 0f) {
            val padDp = 5.dp
            val padPx = with(density) { padDp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (slotX(dragTargetIdx) - padPx).roundToInt(),
                            (slotY(dragTargetIdx) - padPx).roundToInt(),
                        )
                    }
                    .width(with(density) { colWidthPx.toDp() } + padDp * 2)
                    .height(with(density) { tileHeightPx.toDp() } + padDp * 2)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(18.dp),
                    ),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            renderedTiles.chunked(3).forEachIndexed { rowIdx, rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    rowTiles.forEachIndexed { colIdx, tile ->
                        key(tile) {
                            val tileIndex = rowIdx * 3 + colIdx
                            val isDraggingThis = draggedTile == tile
                            val showSelectionBorder = isEditMode &&
                                selectedTile == tile &&
                                draggedTile == null &&
                                !isDropping

                            val previewIdx = if (draggedTile != null && !isDraggingThis) {
                                previewOrder.indexOf(tile).takeIf { it >= 0 } ?: tileIndex
                            } else {
                                tileIndex
                            }
                            val targetShiftX = if (draggedTile != null && !isDraggingThis) {
                                slotX(previewIdx) - slotX(tileIndex)
                            } else {
                                0f
                            }
                            val targetShiftY = if (draggedTile != null && !isDraggingThis) {
                                slotY(previewIdx) - slotY(tileIndex)
                            } else {
                                0f
                            }

                            val animatedShiftX by animateFloatAsState(
                                targetValue = targetShiftX,
                                animationSpec = if (draggedTile != null) bouncySwapSpec else snap(),
                                label = "tile_rearrange_x",
                            )
                            val animatedShiftY by animateFloatAsState(
                                targetValue = targetShiftY,
                                animationSpec = if (draggedTile != null) bouncySwapSpec else snap(),
                                label = "tile_rearrange_y",
                            )

                            val scale by animateFloatAsState(
                                targetValue = if (isDraggingThis && !isDropping) 1.08f else 1f,
                                animationSpec = if (draggedTile != null) {
                                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                                } else {
                                    snap()
                                },
                                label = "tile_drag_scale",
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .zIndex(if (isDraggingThis) 20f else 0f)
                                    .onSizeChanged { size -> tileSizes[tile] = size }
                                    .graphicsLayer {
                                        translationX = when {
                                            draggedTile == null -> 0f
                                            isDraggingThis -> dragOffsetX.value
                                            else -> animatedShiftX
                                        }
                                        translationY = when {
                                            draggedTile == null -> 0f
                                            isDraggingThis -> dragOffsetY.value
                                            else -> animatedShiftY
                                        }
                                        scaleX = if (draggedTile == null) 1f else scale
                                        scaleY = if (draggedTile == null) 1f else scale
                                    }
                                    .clip(RoundedCornerShape(18.dp))
                                    .then(
                                        if (showSelectionBorder) {
                                            Modifier
                                                .border(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(18.dp),
                                                )
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                .padding(horizontal = 5.dp, vertical = 5.dp)
                                        } else {
                                            Modifier.background(Color.Transparent)
                                        },
                                    ),
                            ) {
                                M3EActionTile(
                                    icon = tile.iconVector(),
                                    label = stringResource(tile.labelRes()),
                                    isActive = false,
                                    onClick = {},
                                    interactive = false,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                if (isEditMode) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .pointerInput(tile) {
                                                detectTapGestures(
                                                    onTap = {
                                                        if (!isDropping && draggingTile == null) {
                                                            currentOnSelectTile(tile)
                                                        }
                                                    },
                                                )
                                            }
                                            .pointerInput(tile) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        if (isDropping) return@detectDragGestures
                                                        currentOnClearSelection()
                                                        currentOnTileDragActiveChange(true)
                                                        dragSnapshotTiles = currentTiles
                                                        draggingTile = tile
                                                        lockedTargetIdx = -1
                                                        scope.launch {
                                                            dragOffsetX.snapTo(0f)
                                                            dragOffsetY.snapTo(0f)
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        if (draggingTile != tile || isDropping) return@detectDragGestures
                                                        val snapshot = dragSnapshotTiles ?: currentTiles
                                                        val liveColW = if (gridWidthPx > 0) (gridWidthPx - 2f * hSpacingPx) / 3f else 0f
                                                        val liveTileH = (tileSizes.values.firstOrNull()?.height ?: 0).toFloat()
                                                        val fromIdx = snapshot.indexOf(tile)
                                                        val targetIdx = computeTileTargetIdx(
                                                            tileCount = snapshot.size,
                                                            fromIdx = fromIdx,
                                                            colWidthPx = liveColW,
                                                            tileHeightPx = liveTileH,
                                                            hSpacingPx = hSpacingPx,
                                                            vSpacingPx = vSpacingPx,
                                                            dragOffsetX = dragOffsetX.value,
                                                            dragOffsetY = dragOffsetY.value,
                                                        )
                                                        isDropping = true
                                                        lockedTargetIdx = targetIdx

                                                        val targetOffsetX = if (fromIdx >= 0 && targetIdx >= 0) {
                                                            ((targetIdx % 3) - (fromIdx % 3)) * (liveColW + hSpacingPx)
                                                        } else {
                                                            0f
                                                        }
                                                        val targetOffsetY = if (fromIdx >= 0 && targetIdx >= 0) {
                                                            ((targetIdx / 3) - (fromIdx / 3)) * (liveTileH + vSpacingPx)
                                                        } else {
                                                            0f
                                                        }

                                                        if (fromIdx >= 0 && targetIdx >= 0 && targetIdx != fromIdx) {
                                                            currentOnMoveTile(fromIdx, targetIdx)
                                                        }

                                                        scope.launch {
                                                            val jx = launch { dragOffsetX.animateTo(targetOffsetX, dropSettleSpec) }
                                                            val jy = launch { dragOffsetY.animateTo(targetOffsetY, dropSettleSpec) }
                                                            jx.join()
                                                            jy.join()
                                                            dragOffsetX.snapTo(0f)
                                                            dragOffsetY.snapTo(0f)
                                                            draggingTile = null
                                                            dragSnapshotTiles = null
                                                            isDropping = false
                                                            lockedTargetIdx = -1
                                                            currentOnTileDragActiveChange(false)
                                                        }
                                                    },
                                                    onDragCancel = {
                                                        if (draggingTile != tile || isDropping) return@detectDragGestures
                                                        val snapshot = dragSnapshotTiles ?: currentTiles
                                                        isDropping = true
                                                        lockedTargetIdx = snapshot.indexOf(tile)
                                                        scope.launch {
                                                            val jx = launch { dragOffsetX.animateTo(0f, dropSettleSpec) }
                                                            val jy = launch { dragOffsetY.animateTo(0f, dropSettleSpec) }
                                                            jx.join()
                                                            jy.join()
                                                            dragOffsetX.snapTo(0f)
                                                            dragOffsetY.snapTo(0f)
                                                            draggingTile = null
                                                            dragSnapshotTiles = null
                                                            isDropping = false
                                                            lockedTargetIdx = -1
                                                            currentOnTileDragActiveChange(false)
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        if (draggingTile != tile || isDropping) return@detectDragGestures
                                                        change.consume()
                                                        scope.launch {
                                                            dragOffsetX.snapTo(dragOffsetX.value + dragAmount.x)
                                                            dragOffsetY.snapTo(dragOffsetY.value + dragAmount.y)
                                                        }
                                                    },
                                                )
                                            },
                                    )
                                }
                            }
                        }
                    }
                    repeat(3 - rowTiles.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun blockHighlightShape(type: OverlayBlockType): RoundedCornerShape = when (type) {
    OverlayBlockType.TELEMETRY_PILL -> RoundedCornerShape(43.dp)
    OverlayBlockType.MODE_SWITCHER -> RoundedCornerShape(26.dp)
    OverlayBlockType.ACTION_TILES -> RoundedCornerShape(22.dp)
    OverlayBlockType.BRIGHTNESS_SLIDER,
    OverlayBlockType.VOLUME_SLIDER -> RoundedCornerShape(18.dp)
}
