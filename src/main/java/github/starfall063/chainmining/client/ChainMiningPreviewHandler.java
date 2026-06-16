package github.starfall063.chainmining.client;

import com.github.bsideup.jabel.Desugar;
import github.starfall063.chainmining.BlockMatchMode;
import github.starfall063.chainmining.ChainMiningHooks;
import github.starfall063.chainmining.ChainMiningLang;
import github.starfall063.chainmining.ChainMiningStateManager;
import github.starfall063.chainmining.ChainShapeMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class ChainMiningPreviewHandler {
    private static final long PREVIEW_REFRESH_TICKS_FAST = 2L;
    private static final long PREVIEW_REFRESH_TICKS_LARGE = 6L;
    private static final long PREVIEW_REFRESH_TICKS_EXTREME = 10L;
    private PreviewSnapshot cachedPreview = PreviewSnapshot.EMPTY;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        PreviewSnapshot preview = getPreview(Minecraft.getMinecraft());
        if (!preview.active || preview.lines.isEmpty()) {
            return;
        }
        renderPreview(Minecraft.getMinecraft(), event.getPartialTicks(), preview.lines);
    }

    private static List<LineSegment> buildOutline(List<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return Collections.emptyList();
        }

        Set<BlockPos> blockSet = new HashSet<>(blocks);
        Map<LineSegment, EdgeAccumulator> edgeMap = new HashMap<>(blocks.size() * 12);
        for (BlockPos pos : blocks) {
            for (EnumFacing face : EnumFacing.values()) {
                if (blockSet.contains(pos.offset(face))) {
                    continue;
                }
                addFaceEdges(edgeMap, pos, face);
            }
        }

        List<LineSegment> result = new ArrayList<>(edgeMap.size());
        for (Map.Entry<LineSegment, EdgeAccumulator> entry : edgeMap.entrySet()) {
            if (entry.getValue().shouldRender()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private boolean shouldShowStatus(Minecraft minecraft) {
        EntityPlayerSP player = minecraft.player;
        return player != null
                && minecraft.world != null
                && minecraft.currentScreen == null
                && ChainMiningStateManager.isClientEnabled()
                && ChainMiningHooks.canUseAnyChainAction(player, player.getHeldItemMainhand());
    }

    private static void addFaceEdges(Map<LineSegment, EdgeAccumulator> edgeMap, BlockPos pos, EnumFacing face) {
        int minX = pos.getX();
        int minY = pos.getY();
        int minZ = pos.getZ();
        int maxX = minX + 1;
        int maxY = minY + 1;
        int maxZ = minZ + 1;
        switch (face) {
            case DOWN:
                addEdge(edgeMap, minX, minY, minZ, maxX, minY, minZ, face);
                addEdge(edgeMap, maxX, minY, minZ, maxX, minY, maxZ, face);
                addEdge(edgeMap, maxX, minY, maxZ, minX, minY, maxZ, face);
                addEdge(edgeMap, minX, minY, maxZ, minX, minY, minZ, face);
                return;
            case UP:
                addEdge(edgeMap, minX, maxY, minZ, minX, maxY, maxZ, face);
                addEdge(edgeMap, minX, maxY, maxZ, maxX, maxY, maxZ, face);
                addEdge(edgeMap, maxX, maxY, maxZ, maxX, maxY, minZ, face);
                addEdge(edgeMap, maxX, maxY, minZ, minX, maxY, minZ, face);
                return;
            case NORTH:
                addEdge(edgeMap, minX, minY, minZ, minX, maxY, minZ, face);
                addEdge(edgeMap, minX, maxY, minZ, maxX, maxY, minZ, face);
                addEdge(edgeMap, maxX, maxY, minZ, maxX, minY, minZ, face);
                addEdge(edgeMap, maxX, minY, minZ, minX, minY, minZ, face);
                return;
            case SOUTH:
                addEdge(edgeMap, minX, minY, maxZ, maxX, minY, maxZ, face);
                addEdge(edgeMap, maxX, minY, maxZ, maxX, maxY, maxZ, face);
                addEdge(edgeMap, maxX, maxY, maxZ, minX, maxY, maxZ, face);
                addEdge(edgeMap, minX, maxY, maxZ, minX, minY, maxZ, face);
                return;
            case WEST:
                addEdge(edgeMap, minX, minY, minZ, minX, minY, maxZ, face);
                addEdge(edgeMap, minX, minY, maxZ, minX, maxY, maxZ, face);
                addEdge(edgeMap, minX, maxY, maxZ, minX, maxY, minZ, face);
                addEdge(edgeMap, minX, maxY, minZ, minX, minY, minZ, face);
                return;
            case EAST:
            default:
                addEdge(edgeMap, maxX, minY, minZ, maxX, maxY, minZ, face);
                addEdge(edgeMap, maxX, maxY, minZ, maxX, maxY, maxZ, face);
                addEdge(edgeMap, maxX, maxY, maxZ, maxX, minY, maxZ, face);
                addEdge(edgeMap, maxX, minY, maxZ, maxX, minY, minZ, face);
        }
    }

    private static long getRefreshInterval(ChainShapeMode shapeMode, int totalCount, int previewRenderLimit) {
        if (totalCount >= 768) {
            return PREVIEW_REFRESH_TICKS_EXTREME;
        }
        if (totalCount >= previewRenderLimit || shapeMode == ChainShapeMode.SHAPELESS && totalCount >= 128) {
            return PREVIEW_REFRESH_TICKS_LARGE;
        }
        return PREVIEW_REFRESH_TICKS_FAST;
    }

    private void renderPreview(Minecraft minecraft, float partialTicks, List<LineSegment> lines) {
        Entity view = minecraft.getRenderViewEntity();
        if (view == null) {
            return;
        }

        double renderX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double renderY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double renderZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(2.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        int color = ChainMiningClientSettings.chainMiningPreviewColor;
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        for (LineSegment line : lines) {
            buffer.pos(line.x1 - renderX, line.y1 - renderY, line.z1 - renderZ).color(r, g, b, a).endVertex();
            buffer.pos(line.x2 - renderX, line.y2 - renderY, line.z2 - renderZ).color(r, g, b, a).endVertex();
        }
        tessellator.draw();

        GlStateManager.glLineWidth(1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void addEdge(Map<LineSegment, EdgeAccumulator> edgeMap, int startX, int startY, int startZ, int endX, int endY, int endZ, EnumFacing face) {
        LineSegment segment = new LineSegment(startX, startY, startZ, endX, endY, endZ);
        edgeMap.computeIfAbsent(segment, ignored -> new EdgeAccumulator()).record(face);
    }

    @SubscribeEvent
    public void onRenderOverlayText(RenderGameOverlayEvent.Text event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!shouldShowStatus(minecraft)) {
            return;
        }

        boolean onLeft = !"top_right".equals(ChainMiningClientSettings.chainMiningHudPosition)
                && !"bottom_right".equals(ChainMiningClientSettings.chainMiningHudPosition);
        List<String> target = onLeft ? event.getLeft() : event.getRight();

        int neighborRange = ChainMiningStateManager.getEffectiveNeighborRange();
        String matchModeName = ChainMiningLang.tr(ChainMiningStateManager.getClientMatchMode().getTranslationKey());
        target.add(ChainMiningLang.tr(
                "overlay.chainmining.info",
                matchModeName,
                neighborRange
        ));

        target.add(ChainMiningLang.tr(
                "overlay.chainmining.status",
                ChainMiningLang.tr(ChainMiningStateManager.getClientShape().getTranslationKey())
        ));

        PreviewSnapshot preview = getPreview(minecraft);
        if (preview.active) {
            target.add(ChainMiningLang.tr(
                    "overlay.chainmining.count",
                    preview.totalCount,
                    preview.renderedCount,
                    preview.hiddenCount
            ));
        }
    }

    private PreviewSnapshot getPreview(Minecraft minecraft) {
        EntityPlayerSP player = minecraft.player;
        if (player == null || minecraft.world == null || minecraft.currentScreen != null || !ChainMiningStateManager.isClientEnabled()) {
            this.cachedPreview = PreviewSnapshot.EMPTY;
            return this.cachedPreview;
        }

        ItemStack held = player.getHeldItemMainhand();
        if (!ChainMiningHooks.canUseAnyChainAction(player, held)) {
            this.cachedPreview = PreviewSnapshot.EMPTY;
            return this.cachedPreview;
        }

        RayTraceResult rayTrace = minecraft.objectMouseOver;
        if (rayTrace == null || rayTrace.typeOfHit != RayTraceResult.Type.BLOCK) {
            this.cachedPreview = PreviewSnapshot.EMPTY;
            return this.cachedPreview;
        }

        BlockPos origin = rayTrace.getBlockPos();
        EnumFacing face = rayTrace.sideHit == null ? player.getHorizontalFacing().getOpposite() : rayTrace.sideHit;
        EnumFacing tunnelDirection = ChainMiningHooks.resolveTunnelDirection(player);
        ChainShapeMode shapeMode = ChainMiningStateManager.getClientShape();
        BlockMatchMode matchMode = ChainMiningStateManager.getClientMatchMode();
        int neighborRange = ChainMiningStateManager.getEffectiveNeighborRange();
        Item heldItem = held.isEmpty() ? null : held.getItem();
        int heldMeta = held.isEmpty() ? 0 : held.getMetadata();
        long worldTime = minecraft.world.getTotalWorldTime();
        int dimension = minecraft.world.provider.getDimension();
        int originBlockHash = minecraft.world.getBlockState(origin).hashCode();
        if (this.cachedPreview.matches(worldTime, dimension, origin, face, tunnelDirection, shapeMode, matchMode, neighborRange, heldItem, heldMeta, held.isEmpty(), originBlockHash)) {
            return this.cachedPreview;
        }

        int maxBlocks = ChainMiningStateManager.getEffectiveMaxBlocks();
        int directionalRange = ChainMiningStateManager.getEffectiveDirectionalRange();
        int previewRenderLimit = ChainMiningStateManager.getEffectivePreviewRenderLimit();
        ChainMiningHooks.SelectionTarget target = ChainMiningHooks.resolvePreviewTarget(
                minecraft.world,
                player,
                origin,
                face,
                held,
                shapeMode,
                matchMode,
                maxBlocks,
                neighborRange,
                directionalRange,
                previewRenderLimit
        );
        if (target == null || target.getPositions().isEmpty()) {
            this.cachedPreview = PreviewSnapshot.EMPTY;
            return this.cachedPreview;
        }

        List<BlockPos> renderedBlocks = target.getRenderedPositions(previewRenderLimit);
        this.cachedPreview = new PreviewSnapshot(
                true,
                worldTime + getRefreshInterval(shapeMode, target.getTotalCount(), previewRenderLimit),
                dimension,
                origin,
                face,
                tunnelDirection,
                shapeMode,
                matchMode,
                neighborRange,
                heldItem,
                heldMeta,
                held.isEmpty(),
                target.isCountExact(),
                originBlockHash,
                target.getTotalCount(),
                target.getRenderedCount(previewRenderLimit),
                target.getHiddenCount(previewRenderLimit),
                buildOutline(renderedBlocks)
        );
        return this.cachedPreview;
    }

    private static final class EdgeAccumulator {
        private int count;
        private int faceMask;

        private void record(EnumFacing face) {
            this.count++;
            this.faceMask |= 1 << face.ordinal();
        }

        private boolean shouldRender() {
            return this.count == 1 || Integer.bitCount(this.faceMask) > 1;
        }
    }

    @Desugar
    private record LineSegment(int x1, int y1, int z1, int x2, int y2, int z2) {
            private LineSegment(int x1, int y1, int z1, int x2, int y2, int z2) {
                if (compare(x1, y1, z1, x2, y2, z2) <= 0) {
                    this.x1 = x1;
                    this.y1 = y1;
                    this.z1 = z1;
                    this.x2 = x2;
                    this.y2 = y2;
                    this.z2 = z2;
                } else {
                    this.x1 = x2;
                    this.y1 = y2;
                    this.z1 = z2;
                    this.x2 = x1;
                    this.y2 = y1;
                    this.z2 = z1;
                }
            }

            private static int compare(int ax, int ay, int az, int bx, int by, int bz) {
                if (ax != bx) {
                    return Integer.compare(ax, bx);
                }
                if (ay != by) {
                    return Integer.compare(ay, by);
                }
                if (az != bz) {
                    return Integer.compare(az, bz);
                }
                return 0;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (!(obj instanceof LineSegment other)) {
                    return false;
                }
                return this.x1 == other.x1 && this.y1 == other.y1 && this.z1 == other.z1
                        && this.x2 == other.x2 && this.y2 == other.y2 && this.z2 == other.z2;
            }

    }

    @Desugar
    private record PreviewSnapshot(boolean active, long nextRefreshTick, int dimension, BlockPos origin,
                                   EnumFacing face, EnumFacing tunnelDirection, ChainShapeMode shapeMode, BlockMatchMode matchMode, int neighborRange, Item heldItem,
                                   int heldMeta, boolean heldEmpty, boolean countExact, int originBlockHash, int totalCount, int renderedCount, int hiddenCount,
                                   List<LineSegment> lines) {
            private static final PreviewSnapshot EMPTY = new PreviewSnapshot(false, -1L, 0, BlockPos.ORIGIN, EnumFacing.UP, EnumFacing.NORTH, null, BlockMatchMode.META_ONLY, 1, null, 0, true, true, 0, 0, 0, 0, Collections.emptyList());

        private boolean matches(long worldTime, int dimension, BlockPos origin, EnumFacing face, EnumFacing tunnelDirection, ChainShapeMode shapeMode, BlockMatchMode matchMode, int neighborRange, Item heldItem, int heldMeta, boolean heldEmpty, int originBlockHash) {
                return this.active
                        && worldTime < this.nextRefreshTick
                        && this.dimension == dimension
                        && this.origin.equals(origin)
                        && this.face == face
                        && this.tunnelDirection == tunnelDirection
                        && this.heldItem == heldItem
                        && this.heldMeta == heldMeta
                        && this.heldEmpty == heldEmpty
                        && this.shapeMode == shapeMode;
            }
        }
}
