package com.miir.astralscience.client.render;

import com.miir.astralscience.AstralClient;
import com.miir.astralscience.AstralScience;
import com.miir.astralscience.Config;
import com.miir.astralscience.block.AstralBlocks;
import com.miir.astralscience.util.Text;
import com.miir.astralscience.world.dimension.AstralDimensions;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.List;
import java.util.Random;

public class Render {

    public static void register() {
//        BlockRenderLayerMap.INSTANCE.putBlock(AstralBlocks.STARSHIP_CONSTRUCTION_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(AstralBlocks.GHOST_VINES_PLANT, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(AstralBlocks.GHOST_VINES, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(AstralBlocks.FIRECAP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(AstralBlocks.FROSTFUR, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(AstralBlocks.BLUEMOSS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(AstralBlocks.ANGLER_KELP, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(AstralBlocks.ANGLER_KELP_PLANT, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(AstralBlocks.NEPHRUM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(AstralBlocks.SPEAR_FERN, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(AstralBlocks.BRAMBLEWOOD_DOOR, RenderLayer.getCutout());

        WorldRenderEvents.START.register(context -> {
            Camera camera = context.camera();
            double cullHeight = Config.ATMOSPHERIC_CULL_HEIGHT;
            MatrixStack matrices = context.matrixStack();
            double cameraHeight = camera.getPos().y;
            float tickDelta = context.tickDelta();
            World world = context.world();


            if (AstralDimensions.isOrbit(world)) {
                RenderSystem.enableTexture();
                float angle = world.getSkyAngle(tickDelta);
                float lightLevel = ((Math.abs(angle - .5F)) % .5F) * 2;
                if (world.getTimeOfDay() % 24000 == 6000) {
                    lightLevel = 1;
                }
                drawFloorBody(matrices, lightLevel, cameraHeight + Config.ORBIT_DROP_HEIGHT + 64, 1.0F, tickDelta);
            } else if (
                    cameraHeight >= Config.ATMOSPHERIC_FOG_HEIGHT && ((AstralDimensions.isAstralDimension(world)) ||
                            world.getRegistryKey().equals(World.OVERWORLD))) {
                RenderSystem.enableTexture();
                world.getTimeOfDay();
                float opacity = 1.0F;
                if (cullHeight > cameraHeight && cameraHeight > Config.ATMOSPHERIC_FOG_HEIGHT) {
                    opacity = (float) ((cameraHeight - Config.ATMOSPHERIC_FOG_HEIGHT) / (cullHeight - Config.ATMOSPHERIC_FOG_HEIGHT));
                }
                opacity = MathHelper.clamp(opacity, 0, 1);
                if (opacity > 0) {
                    Render.drawStarfield(matrices, opacity);
                }
            }
        });
        WorldRenderEvents.AFTER_SETUP.register(context -> {
            Camera camera = context.camera();
            double cullHeight = Config.ATMOSPHERIC_CULL_HEIGHT;
            double fogHeight = Config.ATMOSPHERIC_FOG_HEIGHT;
            MatrixStack matrices = context.matrixStack();
            double cameraHeight = camera.getPos().y;
            float tickDelta = context.tickDelta();
            World world = context.world();


            if (AstralDimensions.isOrbit(world)) {
                RenderSystem.enableTexture();
                float angle = world.getSkyAngle(tickDelta);
                float lightLevel = ((Math.abs(angle - .5F)) % .5F) * 2;
                if (world.getTimeOfDay() % 24000 == 6000) {
                    lightLevel = 1;
                }
                drawFloorBody(matrices, lightLevel, cameraHeight + Config.ORBIT_DROP_HEIGHT + 64, 1.0F, tickDelta);
            } else if (
                    cameraHeight >= fogHeight/2 && ((AstralDimensions.isAstralDimension(world)) ||
                            world.getRegistryKey().equals(World.OVERWORLD))) {
                RenderSystem.enableTexture();
                float angle = world.getSkyAngle(tickDelta);
                float lightLevel = ((Math.abs(angle - .5F)) % .5F) * 2;
                if (world.getTimeOfDay() % 24000 == 6000) {
                    lightLevel = 1;
                }
                float opacity = 1.0F;
                if (fogHeight > cameraHeight) opacity = 0;
                if (cullHeight > cameraHeight && cameraHeight > Config.ATMOSPHERIC_FOG_HEIGHT) {
                    opacity = (float) ((cameraHeight - Config.ATMOSPHERIC_FOG_HEIGHT) / (cullHeight - Config.ATMOSPHERIC_FOG_HEIGHT));
                }
                opacity = MathHelper.clamp(opacity, 0, 1);
                if (opacity >= 0) {
                    drawFloorBody(matrices, lightLevel, cameraHeight, opacity, tickDelta);
                }
            }
        });
    }

    public static void drawStarfield(MatrixStack matrices, float opacity) {
        try {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableTexture();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
//            RenderSystem.setShaderTexture(0, AstralClient.renderBody("cyri"));
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();

            for (int i = 0; i < 6; ++i) {
                matrices.push();
                if (i == 0) {
                    RenderSystem.setShaderTexture(0, AstralClient.SKYBOX_LEFT);
                    matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
                }

                if (i == 1) {
                    RenderSystem.setShaderTexture(0, AstralClient.SKYBOX_RIGHT);
                    matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-90.0F));
                }

                if (i == 2) {
                    RenderSystem.setShaderTexture(0, AstralClient.SKYBOX_UP);
                    matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180.0F));
                }

                if (i == 3) {
                    RenderSystem.setShaderTexture(0, AstralClient.SKYBOX_FRONT);
                    matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(90.0F));
                }

                if (i == 4) {
                    RenderSystem.setShaderTexture(0, AstralClient.SKYBOX_BACK);
                    matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-90.0F));
                }
                if (i == 5) {
                    RenderSystem.setShaderTexture(0, AstralClient.SKYBOX_DOWN);
                }
                Matrix4f matrix4f = matrices.peek().getPositionMatrix();
                int alpha = (int)(0xFF * (opacity*.9+.1));
                bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                bufferBuilder.vertex(matrix4f, -150.0F, -150.0F, -150.0F).texture(0.0F, 0.0F).color(255, 255, 255, alpha).next();
                bufferBuilder.vertex(matrix4f, -150.0F, -150.0F, 150.0F).texture(0.0F, 1.0F).color(255, 255, 255, alpha).next();
                bufferBuilder.vertex(matrix4f, 150.0F, -150.0F, 150.0F).texture(1.0F, 1.0F).color(255, 255, 255, alpha).next();
                bufferBuilder.vertex(matrix4f, 150.0F, -150.0F, -150.0F).texture(1.0F, 0.0F).color(255, 255, 255, alpha).next();
                tessellator.draw();
                matrices.pop();
            }
//            RenderSystem.enableTexture();
            RenderSystem.enableDepthTest();
        } catch (NullPointerException e) {
            AstralScience.LOGGER.warn("Error rendering cosmic background!");
            e.printStackTrace();
        }
//        RenderSystem.depthMask(true);
//        RenderSystem.disableBlend();
    }

    public static void drawFloorBody(MatrixStack matrices, float light, double playerHeight, float opacity, double tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        Entity cameraEntity = client.getCameraEntity();
        int alpha = (int) (0xFF * (opacity*.9+.1));
        int planetSize = Config.PLANET_SIZE;
        double cullHeight = Config.ATMOSPHERIC_CULL_HEIGHT;
        double fogHeight = Config.ATMOSPHERIC_FOG_HEIGHT;
        if (playerHeight < fogHeight) {
            alpha = 0;
        }
        float h = (float) playerHeight / Config.ORBIT_DROP_HEIGHT;
        assert world != null;
        assert cameraEntity != null;
        int lightmapCoords = WorldRenderer.getLightmapCoordinates(world, cameraEntity.getBlockPos());
        int l;
        if (AstralClient.isLuminescent(world)) {
            l = 240;
        } else {
            l = (int) (200 * light);
        }
        float height = 50;
        String renderPath = Text.deorbitify(world.getRegistryKey().getValue().getPath());
        Identifier planet = AstralClient.renderBody(renderPath);
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.enableTexture();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (world.getRegistryKey().equals(World.OVERWORLD) && (cameraEntity.getPos().y > fogHeight/2 && cameraEntity.getPos().y < cullHeight)) {
//            mask to hide the transition between real terrain and the rendered earth image
//                BlockPos location = new BlockPos(cameraEntity.getBlockPos().getX(), 128, cameraEntity.getBlockPos().getZ());
//            float[] color = world.getDimensionEffects().getFogColorOverride(world.getSkyAngle((float) tickDelta), (float) tickDelta);
//            if (color == null) {
//                color = new float[] {0, 0, 0};
//            }
            Vec3i clr = new Vec3i(RenderSystem.getShaderFogColor()[0] * 255, RenderSystem.getShaderFogColor()[1] * 255, RenderSystem.getShaderFogColor()[2] * 255);
            int r = clr.getX();//(int)color[0]*255;
            int g = clr.getY();//(int)color[1]*255;
            int b = clr.getZ();//(int)color[2]*255;
            RenderSystem.setShaderTexture(0, AstralClient.renderAtmos(world.getRegistryKey().getValue().getPath()));
            RenderSystem.setShader(GameRenderer::getPositionTexLightmapColorShader);
            Matrix4f positionMatrix1 = matrices.peek().getPositionMatrix();
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_LIGHT_COLOR);
            float hMask = -height * h-1;
            bufferBuilder.vertex(positionMatrix1, -planetSize, hMask, -planetSize).texture(0.0F, 0.0F).light(lightmapCoords).color(r, g, b, 255).next();
            bufferBuilder.vertex(positionMatrix1, -planetSize, hMask, planetSize).texture(0.0F, 1.0F).light(lightmapCoords).color(r, g, b, 255).next();
            bufferBuilder.vertex(positionMatrix1, planetSize, hMask, planetSize).texture(1.0F, 1.0F).light(lightmapCoords).color(r, g, b, 255).next();
            bufferBuilder.vertex(positionMatrix1, planetSize, hMask, -planetSize).texture(1.0F, 0.0F).light(lightmapCoords).color(r, g, b, 255).next();
            BufferRenderer.drawWithShader(bufferBuilder.end());
        }

        // draw body
        RenderSystem.setShader(GameRenderer::getPositionTexLightmapColorShader);
        RenderSystem.setShaderTexture(0, planet);
        Matrix4f positionMatrix = matrices.peek().getPositionMatrix();

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_LIGHT_COLOR);
        bufferBuilder.vertex(positionMatrix, -planetSize, -height * h, -planetSize).texture(0.0F, 0.0F).light(lightmapCoords).color(l + 10, l + 10, l + 10, alpha).next();
        bufferBuilder.vertex(positionMatrix, -planetSize, -height * h, planetSize).texture(0.0F, 1.0F).light(lightmapCoords).color(l + 10, l + 10, l + 10, alpha).next();
        bufferBuilder.vertex(positionMatrix, planetSize, -height * h, planetSize).texture(1.0F, 1.0F).light(lightmapCoords).color(l + 10, l + 10, l + 10, alpha).next();
        bufferBuilder.vertex(positionMatrix, planetSize, -height * h, -planetSize).texture(1.0F, 0.0F).light(lightmapCoords).color(l + 10, l + 10, l + 10, alpha).next();
        BufferRenderer.drawWithShader(bufferBuilder.end());

        // draw clouds
        if (AstralClient.hasClouds(world)) {
            Identifier clouds = AstralClient.renderClouds(Text.deorbitify(world.getRegistryKey().getValue().getPath()));
            float cloudHeight = (float) (-height * (h - (fogHeight / ((float) Config.ORBIT_DROP_HEIGHT))));
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, clouds);
            Matrix4f matrix4f2 = matrices.peek().getPositionMatrix();
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bufferBuilder.vertex(matrix4f2, -planetSize, cloudHeight, -planetSize).texture(0.0F, 0.0F).color((int) (200 * light) + 50, (int) (200 * light) + 50, (int) (200 * light) + 50, (int) (128 * opacity)).next();
            bufferBuilder.vertex(matrix4f2, -planetSize, cloudHeight, planetSize).texture(0.0F, 1.0F).color((int) (200 * light) + 50, (int) (200 * light) + 50, (int) (200 * light) + 50, (int) (128 * opacity)).next();
            bufferBuilder.vertex(matrix4f2, planetSize, cloudHeight, planetSize).texture(1.0F, 1.0F).color((int) (200 * light) + 50, (int) (200 * light) + 50, (int) (200 * light) + 50, (int) (128 * opacity)).next();
            bufferBuilder.vertex(matrix4f2, planetSize, cloudHeight, -planetSize).texture(1.0F, 0.0F).color((int) (200 * light) + 50, (int) (200 * light) + 50, (int) (200 * light) + 50, (int) (128 * opacity)).next();
            BufferRenderer.drawWithShader(bufferBuilder.end());
        }
        if (AstralDimensions.hasAtmosphere(world, true)) {
            double r = 1;
            double g = 1;
            double b = 1;
            Identifier atmos = AstralClient.renderAtmos(Text.deorbitify(world.getRegistryKey().getValue().getPath()));
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, atmos);
            Matrix4f matrix4f2 = matrices.peek().getPositionMatrix();
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float atmosphereHeight = (float) (-height * (h - (fogHeight / ((float) Config.ORBIT_DROP_HEIGHT)))) - 1;
            bufferBuilder.vertex(matrix4f2, -planetSize, atmosphereHeight, -planetSize).texture(0.0F, 0.0F).color((int) (180 * light * r) + 20, (int) (200 * light * g) + 20, (int) (220 * light * b) + 20, (int) (64 * opacity)).next();
            bufferBuilder.vertex(matrix4f2, -planetSize, atmosphereHeight, planetSize).texture(0.0F, 1.0F).color((int) (180 * light * r) + 20, (int) (200 * light * g) + 20, (int) (220 * light * b) + 20, (int) (64 * opacity)).next();
            bufferBuilder.vertex(matrix4f2, planetSize, atmosphereHeight, planetSize).texture(1.0F, 1.0F).color((int) (180 * light * r) + 20, (int) (200 * light * g) + 20, (int) (220 * light * b) + 20, (int) (64 * opacity)).next();
            bufferBuilder.vertex(matrix4f2, planetSize, atmosphereHeight, -planetSize).texture(1.0F, 0.0F).color((int) (180 * light * r) + 20, (int) (200 * light * g) + 20, (int) (220 * light * b) + 20, (int) (64 * opacity)).next();
            BufferRenderer.drawWithShader(bufferBuilder.end());
        }
        RenderSystem.depthMask(true);
    }

    public static void renderOrbitalSky(WorldRenderContext context) {
        WorldRenderer renderer = context.worldRenderer();
        Camera camera = context.camera();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = context.world();
        float tickDelta = context.tickDelta();
        MatrixStack matrices = context.matrixStack();
        Matrix4f projectionMatrix = context.projectionMatrix();
        float q;
        float p;
        float o;
        int m;
        float k;
        float i;
        CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
        if (cameraSubmersionType == CameraSubmersionType.POWDER_SNOW || cameraSubmersionType == CameraSubmersionType.LAVA) {
            return;
        }
        RenderSystem.disableTexture();
        Vec3d vec3d = Vec3d.ZERO; // world.getSkyColor(client.gameRenderer.getCamera().getPos(), tickDelta);
        float f = (float) vec3d.x;
        float g = (float) vec3d.y;
        float h = (float) vec3d.z;
        BackgroundRenderer.setFogBlack();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(f, g, h, 1.0f);
        Shader shader = RenderSystem.getShader();
//        this.lightSkyBuffer.bind();
//        this.lightSkyBuffer.draw(matrices.peek().getPositionMatrix(), projectionMatrix, shader);
        VertexBuffer.unbind();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float[] fs = world.getDimensionEffects().getFogColorOverride(world.getSkyAngle(tickDelta), tickDelta);
        if (fs != null) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.disableTexture();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            matrices.push();
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0f));
            i = MathHelper.sin(world.getSkyAngleRadians(tickDelta)) < 0.0f ? 180.0f : 0.0f;
            matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(i));
            matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(90.0f));
            float j = fs[0];
            k = fs[1];
            float l = fs[2];
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            bufferBuilder.vertex(matrix4f, 0.0f, 100.0f, 0.0f).color(j, k, l, fs[3]).next();
            for (int n = 0; n <= 16; ++n) {
                o = (float) n * ((float) Math.PI * 2) / 16.0f;
                p = MathHelper.sin(o);
                q = MathHelper.cos(o);
                bufferBuilder.vertex(matrix4f, p * 120.0f, q * 120.0f, -q * 40.0f * fs[3]).color(fs[0], fs[1], fs[2], 0.0f).next();
            }
            BufferRenderer.drawWithShader(bufferBuilder.end());
            matrices.pop();
        }
        RenderSystem.enableTexture();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        matrices.push();
        i = 1.0f - world.getRainGradient(tickDelta);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, i);
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-90.0f));
        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(world.getSkyAngle(tickDelta) * 360.0f));
        Matrix4f matrix4f2 = matrices.peek().getPositionMatrix();
        k = 30.0f;
        drawStarfield(matrices, 1);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, AstralClient.SUN_ORBIT);
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix4f2, -k, 100.0f, -k).texture(0.0f, 0.0f).next();
        bufferBuilder.vertex(matrix4f2, k, 100.0f, -k).texture(1.0f, 0.0f).next();
        bufferBuilder.vertex(matrix4f2, k, 100.0f, k).texture(1.0f, 1.0f).next();
        bufferBuilder.vertex(matrix4f2, -k, 100.0f, k).texture(0.0f, 1.0f).next();
        BufferRenderer.drawWithShader(bufferBuilder.end());
        k = 20.0f;
        RenderSystem.setShaderTexture(0, AstralClient.REENTRY_MOON);
        int r = world.getMoonPhase();
        int s = r % 4;
        m = r / 4 % 2;
        float t = (float) (s) / 4.0f;
        o = (float) (m) / 2.0f;
        p = (float) (s + 1) / 4.0f;
        q = (float) (m + 1) / 2.0f;
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix4f2, -k, -100.0f, k).texture(p, q).next();
        bufferBuilder.vertex(matrix4f2, k, -100.0f, k).texture(t, q).next();
        bufferBuilder.vertex(matrix4f2, k, -100.0f, -k).texture(t, o).next();
        bufferBuilder.vertex(matrix4f2, -k, -100.0f, -k).texture(p, o).next();
        BufferRenderer.drawWithShader(bufferBuilder.end());
        RenderSystem.disableTexture();
//        float u = world.method_23787(tickDelta) * i;
//        if (u > 0.0f) {
//            RenderSystem.setShaderColor(u, u, u, u);
//            BackgroundRenderer.clearFog();
//            renderer.starsBuffer.bind();
//            renderer.starsBuffer.draw(matrices.peek().getPositionMatrix(), projectionMatrix, GameRenderer.getPositionShader());
//            VertexBuffer.unbind();
//        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        matrices.pop();
        RenderSystem.disableTexture();
        RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 1.0f);
//        double d = client.player.getCameraPosVec(tickDelta).y - world.getLevelProperties().getSkyDarknessHeight(world);
//        if (d < 0.0) {
//            matrices.push();
//            matrices.translate(0.0, 12.0, 0.0);
//            renderer.darkSkyBuffer.bind();
//            renderer.darkSkyBuffer.draw(matrices.peek().getPositionMatrix(), projectionMatrix, shader);
//            VertexBuffer.unbind();
//            matrices.pop();
//        }
        if (world.getDimensionEffects().isAlternateSkyColor()) {
            RenderSystem.setShaderColor(f * 0.2f + 0.04f, g * 0.2f + 0.04f, h * 0.6f + 0.1f, 1.0f);
        } else {
            RenderSystem.setShaderColor(f, g, h, 1.0f);
        }
        RenderSystem.enableTexture();
        RenderSystem.depthMask(true);
    }
}