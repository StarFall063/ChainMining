package github.starfall063.chainmining.util.handler;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.util.ChainMiningStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ChainMiningPreviewHandler {

    private static final OutlineCache OUTLINE_CACHE = new OutlineCache();

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        List<BlockPos> blocks = ChainMiningStateManager.getPreviewBlocks();
        if (blocks == null || blocks.isEmpty() || !ChainMiningStateManager.isClientEnabled()) return;

        if (!ChainMiningConfig.CLIENT.chainMiningEnablePreview) return;

        int limit = Math.min(blocks.size(), ChainMiningConfig.CLIENT.chainMiningPreviewRenderLimit);
        ChainMiningStateManager.setPreviewRendered(limit);
        ChainMiningStateManager.setPreviewHidden(Math.max(0, blocks.size() - limit));
        List<BlockPos> visibleBlocks = new ArrayList<>(blocks.subList(0, limit));
        List<LineSegment> lines = OUTLINE_CACHE.getOrBuild(visibleBlocks);
        if (lines.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        double rx = view.lastTickPosX + (view.posX - view.lastTickPosX) * event.getPartialTicks();
        double ry = view.lastTickPosY + (view.posY - view.lastTickPosY) * event.getPartialTicks();
        double rz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * event.getPartialTicks();

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(2.0F);

        int color;
        String colorCfg = ChainMiningConfig.CLIENT.chainMiningPreviewColor.trim();
        if ("rainbow".equalsIgnoreCase(colorCfg)) {
            long tick = Minecraft.getSystemTime();
            float hue = (tick % 3000L) / 3000F;
            color = Color.HSBtoRGB(hue, 1.0F, 1.0F);
        } else {
            try {
                color = Integer.parseUnsignedInt(ChainMiningConfig.CLIENT.chainMiningPreviewColor.trim(), 16);
            } catch (NumberFormatException e) {
                color = 0xFFE65CEB;
            }
        }

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        for (LineSegment line : lines) {
            buffer.pos(line.x1 - rx, line.y1 - ry, line.z1 - rz).color(r, g, b, a).endVertex();
            buffer.pos(line.x2 - rx, line.y2 - ry, line.z2 - rz).color(r, g, b, a).endVertex();
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

    private static List<LineSegment> buildOutline(List<BlockPos> blocks) {
        Set<BlockPos> blockSet = new HashSet<>(blocks);
        Map<LineSegment, EdgeAccumulator> edgeMap = new HashMap<>(blocks.size() * 12);

        for (BlockPos pos : blocks) {
            for (EnumFacing face : EnumFacing.values()) {
                if (blockSet.contains(pos.offset(face))) continue;

                int minX = pos.getX(), minY = pos.getY(), minZ = pos.getZ();
                int maxX = minX + 1, maxY = minY + 1, maxZ = minZ + 1;

                switch (face) {
                    case DOWN:
                        addEdge(edgeMap, minX, minY, minZ, maxX, minY, minZ, face);
                        addEdge(edgeMap, maxX, minY, minZ, maxX, minY, maxZ, face);
                        addEdge(edgeMap, maxX, minY, maxZ, minX, minY, maxZ, face);
                        addEdge(edgeMap, minX, minY, maxZ, minX, minY, minZ, face);
                        break;
                    case UP:
                        addEdge(edgeMap, minX, maxY, minZ, maxX, maxY, minZ, face);
                        addEdge(edgeMap, maxX, maxY, minZ, maxX, maxY, maxZ, face);
                        addEdge(edgeMap, maxX, maxY, maxZ, minX, maxY, maxZ, face);
                        addEdge(edgeMap, minX, maxY, maxZ, minX, maxY, minZ, face);
                        break;
                    case NORTH:
                        addEdge(edgeMap, minX, minY, minZ, minX, maxY, minZ, face);
                        addEdge(edgeMap, minX, maxY, minZ, maxX, maxY, minZ, face);
                        addEdge(edgeMap, maxX, maxY, minZ, maxX, minY, minZ, face);
                        addEdge(edgeMap, maxX, minY, minZ, minX, minY, minZ, face);
                        break;
                    case SOUTH:
                        addEdge(edgeMap, minX, minY, maxZ, maxX, minY, maxZ, face);
                        addEdge(edgeMap, maxX, minY, maxZ, maxX, maxY, maxZ, face);
                        addEdge(edgeMap, maxX, maxY, maxZ, minX, maxY, maxZ, face);
                        addEdge(edgeMap, minX, maxY, maxZ, minX, minY, maxZ, face);
                        break;
                    case WEST:
                        addEdge(edgeMap, minX, minY, minZ, minX, minY, maxZ, face);
                        addEdge(edgeMap, minX, minY, maxZ, minX, maxY, maxZ, face);
                        addEdge(edgeMap, minX, maxY, maxZ, minX, maxY, minZ, face);
                        addEdge(edgeMap, minX, maxY, minZ, minX, minY, minZ, face);
                        break;
                    case EAST:
                        addEdge(edgeMap, maxX, minY, minZ, maxX, maxY, minZ, face);
                        addEdge(edgeMap, maxX, maxY, minZ, maxX, maxY, maxZ, face);
                        addEdge(edgeMap, maxX, maxY, maxZ, maxX, minY, maxZ, face);
                        addEdge(edgeMap, maxX, minY, maxZ, maxX, minY, minZ, face);
                        break;
                }
            }
        }

        List<LineSegment> result = new ArrayList<>();
        for (Map.Entry<LineSegment, EdgeAccumulator> entry : edgeMap.entrySet()) {
            if (entry.getValue().shouldRender()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static void addEdge(Map<LineSegment, EdgeAccumulator> edgeMap,
                                 int x1, int y1, int z1,
                                 int x2, int y2, int z2,
                                 EnumFacing face) {
        edgeMap.computeIfAbsent(new LineSegment(x1, y1, z1, x2, y2, z2),
                k -> new EdgeAccumulator()).record(face);
    }

    private static final class OutlineCache {
        private List<BlockPos> lastBlocks = Collections.emptyList();
        private List<LineSegment> lastLines = Collections.emptyList();

        List<LineSegment> getOrBuild(List<BlockPos> blocks) {
            if (blocks.equals(this.lastBlocks)) {
                return this.lastLines;
            }
            this.lastLines = buildOutline(blocks);
            this.lastBlocks = new ArrayList<>(blocks);
            return this.lastLines;
        }
    }

    private static final class LineSegment {
        final int x1, y1, z1, x2, y2, z2;

        LineSegment(int a, int b, int c, int d, int e, int f) {
            if (compare(a, b, c, d, e, f) <= 0) {
                this.x1 = a; this.y1 = b; this.z1 = c;
                this.x2 = d; this.y2 = e; this.z2 = f;
            } else {
                this.x1 = d; this.y1 = e; this.z1 = f;
                this.x2 = a; this.y2 = b; this.z2 = c;
            }
        }

        private static int compare(int ax, int ay, int az, int bx, int by, int bz) {
            if (ax != bx) return Integer.compare(ax, bx);
            if (ay != by) return Integer.compare(ay, by);
            return Integer.compare(az, bz);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof LineSegment o)) return false;
            return x1 == o.x1 && y1 == o.y1 && z1 == o.z1
                    && x2 == o.x2 && y2 == o.y2 && z2 == o.z2;
        }

        @Override
        public int hashCode() {
            int h = x1;
            h = 31 * h + y1;
            h = 31 * h + z1;
            h = 31 * h + x2;
            h = 31 * h + y2;
            h = 31 * h + z2;
            return h;
        }
    }

    private static final class EdgeAccumulator {
        private int count;
        private int faceMask;

        void record(EnumFacing face) {
            count++;
            faceMask |= 1 << face.ordinal();
        }

        boolean shouldRender() {
            return count == 1 || Integer.bitCount(faceMask) > 1;
        }
    }
}
