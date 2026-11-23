package io.github.nicolasgoyon.beaconhold.client.dome;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.network.AreaSyncPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Shows the energy domes of the active beacons: a faint veil from far, and a ring of light with particles near the
 * edge. {@link DomeGeometry} gives the shape and the fade. The client option {@code dome.enabled} turns the domes off.
 */
public final class DomeRenderer {
    /** The color of the dome: a light cyan, near the color of the beacon beam. */
    private static final float RED = 0.62F;

    private static final float GREEN = 0.91F;
    private static final float BLUE = 1.0F;

    private DomeRenderer() {
    }

    /**
     * Draws the veil and the rings of the domes after the particles.
     *
     * @param event the render event of the level
     */
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !BeaconholdConfig.CLIENT.domeEnabled()) {
            return;
        }
        List<AreaSyncPayload.Area> areas = DomeAreas.get();
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (areas.isEmpty() || level == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f pose = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        double feetY = minecraft.player != null ? minecraft.player.getY() : camera.y;
        for (AreaSyncPayload.Area area : areas) {
            addDome(buffer, pose, level, area, camera, feetY);
        }
        MeshData mesh = buffer.build();
        if (mesh != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferUploader.drawWithShader(mesh);
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
        poseStack.popPose();
    }

    private static void addDome(
            BufferBuilder buffer, Matrix4f pose, ClientLevel level, AreaSyncPayload.Area area, Vec3 camera, double feetY) {
        double centerX = area.center().getX() + 0.5;
        double centerZ = area.center().getZ() + 0.5;
        double edge = DomeGeometry.edgeRadius(area.radius());
        if (!DomeGeometry.isInRange(camera.x, camera.z, centerX, centerZ, edge)) {
            return;
        }
        int count = DomeGeometry.segmentCount(area.radius());
        float middle = (float) camera.y;
        float bottom = (float) (camera.y - DomeGeometry.VEIL_HALF_HEIGHT);
        float top = (float) (camera.y + DomeGeometry.VEIL_HALF_HEIGHT);
        for (int i = 0; i < count; i++) {
            float x0 = (float) DomeGeometry.pointX(centerX, edge, i, count);
            float z0 = (float) DomeGeometry.pointZ(centerZ, edge, i, count);
            float x1 = (float) DomeGeometry.pointX(centerX, edge, i + 1, count);
            float z1 = (float) DomeGeometry.pointZ(centerZ, edge, i + 1, count);
            double d0 = DomeGeometry.horizontalDistance(camera.x, camera.z, x0, z0);
            double d1 = DomeGeometry.horizontalDistance(camera.x, camera.z, x1, z1);
            float veil0 = (float) DomeGeometry.veilAlpha(d0);
            float veil1 = (float) DomeGeometry.veilAlpha(d1);
            if (veil0 > 0.0F || veil1 > 0.0F) {
                quad(buffer, pose, x0, z0, x1, z1, bottom, bottom, 0.0F, 0.0F, middle, middle, veil0, veil1);
                quad(buffer, pose, x0, z0, x1, z1, middle, middle, veil0, veil1, top, top, 0.0F, 0.0F);
            }
            float ring0 = (float) DomeGeometry.ringAlpha(d0);
            float ring1 = (float) DomeGeometry.ringAlpha(d1);
            if (ring0 > 0.0F || ring1 > 0.0F) {
                float y0 = (float) DomeGeometry.ringY(groundY(level, x0, z0), feetY);
                float y1 = (float) DomeGeometry.ringY(groundY(level, x1, z1), feetY);
                float h = (float) DomeGeometry.RING_HEIGHT;
                quad(buffer, pose, x0, z0, x1, z1, y0, y1, ring0, ring1, y0 + h, y1 + h, 0.0F, 0.0F);
            }
        }
    }

    /** Adds a vertical quad between two points, with an opacity for each corner. */
    private static void quad(
            BufferBuilder buffer,
            Matrix4f pose,
            float x0,
            float z0,
            float x1,
            float z1,
            float low0,
            float low1,
            float lowAlpha0,
            float lowAlpha1,
            float high0,
            float high1,
            float highAlpha0,
            float highAlpha1) {
        buffer.addVertex(pose, x0, low0, z0).setColor(RED, GREEN, BLUE, lowAlpha0);
        buffer.addVertex(pose, x1, low1, z1).setColor(RED, GREEN, BLUE, lowAlpha1);
        buffer.addVertex(pose, x1, high1, z1).setColor(RED, GREEN, BLUE, highAlpha1);
        buffer.addVertex(pose, x0, high0, z0).setColor(RED, GREEN, BLUE, highAlpha0);
    }

    private static int groundY(ClientLevel level, double x, double z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
    }

    /**
     * Makes the particles of the rings near the player.
     *
     * @param event the tick event of the client
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused() || !BeaconholdConfig.CLIENT.domeEnabled()) {
            return;
        }
        RandomSource random = level.getRandom();
        for (AreaSyncPayload.Area area : DomeAreas.get()) {
            double centerX = area.center().getX() + 0.5;
            double centerZ = area.center().getZ() + 0.5;
            double edge = DomeGeometry.edgeRadius(area.radius());
            double toCenter = DomeGeometry.horizontalDistance(player.getX(), player.getZ(), centerX, centerZ);
            if (Math.abs(toCenter - edge) >= DomeGeometry.RING_DISTANCE) {
                continue;
            }
            int count = DomeGeometry.segmentCount(area.radius());
            for (int i = 0; i < count; i++) {
                double x = DomeGeometry.pointX(centerX, edge, i, count);
                double z = DomeGeometry.pointZ(centerZ, edge, i, count);
                double distance = DomeGeometry.horizontalDistance(player.getX(), player.getZ(), x, z);
                if (random.nextDouble() < DomeGeometry.particleChance(distance)) {
                    double t = random.nextDouble();
                    double px = Mth.lerp(t, x, DomeGeometry.pointX(centerX, edge, i + 1, count));
                    double pz = Mth.lerp(t, z, DomeGeometry.pointZ(centerZ, edge, i + 1, count));
                    double py = DomeGeometry.ringY(groundY(level, px, pz), player.getY()) + 0.1;
                    level.addParticle(ParticleTypes.END_ROD, px, py, pz, 0.0, 0.03, 0.0);
                }
            }
        }
    }
}
