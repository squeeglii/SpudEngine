package me.cg360.spudengine.wormholes.render;

import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.data.DataTypes;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.UniformDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.UniformDescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.shader.DescriptorSetLayoutBundle;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderIO;
import me.cg360.spudengine.core.render.pipeline.shader.StandardSamplers;
import me.cg360.spudengine.core.util.Bounds3D;
import me.cg360.spudengine.core.util.VulkanUtil;
import me.cg360.spudengine.wormholes.GameProperties;
import me.cg360.spudengine.wormholes.GeneratedAssets;
import me.cg360.spudengine.wormholes.WormholeDemo;
import me.cg360.spudengine.wormholes.logic.PortalTracker;
import me.cg360.spudengine.wormholes.world.entity.PortalEntity;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.tinylog.Logger;

public class PortalSubRenderer implements SubRenderProcess {

    private static final int DISABLE_CHECKS = 0;
    private static final int SKIP_LAYER = 1;

    private static final int UNASSIGNED_PORTAL_TYPE = 1;
    private static final int BLUE_PORTAL_TYPE = 2;
    private static final int ORANGE_PORTAL_TYPE = 3;

    private final WormholeDemo game;

    private static final PortalTransformHelper PORTAL_TYPE = new PortalTransformHelper();

    private DescriptorSetLayout lBluePortal;
    private UniformDescriptorSet[] dBluePortal;
    private GeneralBuffer[] uBluePortal;

    private DescriptorSetLayout lOrangePortal;
    private UniformDescriptorSet[] dOrangePortal;
    private GeneralBuffer[] uOrangePortal;

    private DescriptorSetLayout lPortalLayer;
    private UniformDescriptorSet[] dPortalLayer;
    private GeneralBuffer[] uPortalLayer;

    private DescriptorSetLayout lPortalTypeMask;
    private UniformDescriptorSet[] dPortalTypeMask;
    private GeneralBuffer[] uPortalTypeMask;

    public PortalSubRenderer(WormholeDemo game) {
        this.game = game;

        Logger.info("Using Portal Sub-Renderer");
    }

    @Override
    public void buildUniformLayout(DescriptorSetLayoutBundle builder) {
        Logger.info("Building Portal Uniform Layout");
        this.lBluePortal = new UniformDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_GEOMETRY_BIT)
                .enablePerFrameWrites(builder.swapChain());
        this.lOrangePortal = new UniformDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_GEOMETRY_BIT)
                .enablePerFrameWrites(builder.swapChain());
        this.lPortalLayer = new UniformDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_GEOMETRY_BIT)
                .setCount(GameProperties.MAX_PORTAL_DEPTH + 2); // fixed values that can be swapped out like textures.
        this.lPortalTypeMask = new UniformDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_GEOMETRY_BIT)
                .setCount(4); // -1 disabled,   0 unassigned to portal,   1 blue portal, 2 orange portal.


        builder.addGeometryUniform(this.lBluePortal);
        builder.addGeometryUniform(this.lOrangePortal);
        builder.addGeometryUniform(this.lPortalLayer);
        builder.addGeometryUniform(this.lPortalTypeMask);
    }

    @Override
    public void createDescriptorSets(DescriptorPool pool) {
        this.dBluePortal = UniformDescriptorSet.create(pool, this.lBluePortal, PORTAL_TYPE, 0);
        this.uBluePortal = ShaderIO.collectUniformBuffers(this.dBluePortal);

        this.dOrangePortal = UniformDescriptorSet.create(pool, this.lOrangePortal, PORTAL_TYPE, 0);
        this.uOrangePortal = ShaderIO.collectUniformBuffers(this.dOrangePortal);

        this.dPortalLayer = UniformDescriptorSet.create(pool, this.lPortalLayer, DataTypes.INT, 0);
        this.uPortalLayer = ShaderIO.collectUniformBuffers(this.dPortalLayer);

        // create values -1  -->  max-depth - 1
        // [0] = -1 - disables layer checking (naive renderer)
        // [1] = 100000 - aka skip rendering entirely.
        // any other value locks on to a specific layer.
        this.uPortalLayer[DISABLE_CHECKS].runMapped(buf -> buf.putInt(-1));
        this.uPortalLayer[SKIP_LAYER].runMapped(buf -> buf.putInt(100000));

        for(int i = 0; i < GameProperties.MAX_PORTAL_DEPTH; i++) {
            final int layerOrd = i;
            this.uPortalLayer[i + 2].runMapped(buf -> buf.putInt(layerOrd));
            Logger.info("Layer: {}", layerOrd);
        }


        this.dPortalTypeMask = UniformDescriptorSet.create(pool, this.lPortalTypeMask, DataTypes.INT, 0);
        this.uPortalTypeMask = ShaderIO.collectUniformBuffers(this.dPortalTypeMask);

        this.uPortalTypeMask[DISABLE_CHECKS].runMapped(buf -> buf.putInt(-1));
        this.uPortalTypeMask[UNASSIGNED_PORTAL_TYPE].runMapped(buf -> buf.putInt(0));
        this.uPortalTypeMask[BLUE_PORTAL_TYPE].runMapped(buf -> buf.putInt(1));
        this.uPortalTypeMask[ORANGE_PORTAL_TYPE].runMapped(buf -> buf.putInt(2));
    }

    @Override
    public void renderPreMesh(RenderContext renderContext, ShaderIO shaderIO, StandardSamplers samplers, VkCommandBuffer cmd) {
        PortalTracker pTrack = this.game.getPortalTracker();

        // last subpass == closest room copy. invert using max depth.
        //Logger.info("Rendering Portal PreMesh {}", subPass);
        int layerOnPass = GameProperties.MAX_PORTAL_DEPTH - 1 - renderContext.pass();
        int layerOnSubpass = GameProperties.MAX_PORTAL_DEPTH - 1 - renderContext.subpass();

        if (pTrack.hasPortalPair()) {
            PortalEntity bluePortal =  pTrack.getBluePortal();
            PortalEntity orangePortal =  pTrack.getOrangePortal();

            Matrix4f bluePortalTransform = bluePortal.calculateConnectionTransform(orangePortal);
            Matrix4f orangePortalTransform = orangePortal.calculateConnectionTransform(bluePortal);

            //TODO: Fix alignment of vec3s in shaders.
            //      Shader handled uniforms in minimum of 16 byte chunks on this device.
            //      Get the val from the device, and align based off of that.

            PortalTransformHelper.copyToBuffer(this.uBluePortal[renderContext.frameIndex()], bluePortal, bluePortalTransform);
            PortalTransformHelper.copyToBuffer(this.uOrangePortal[renderContext.frameIndex()], orangePortal, orangePortalTransform);

            samplers.setOverlayMaterial(GeneratedAssets.PORTAL_CUTOUT);

        } else {
            boolean noPortals = true;

            if(pTrack.hasBluePortal()) {
                PortalEntity bluePortal = pTrack.getBluePortal();
                PortalTransformHelper.copyIncompleteToBuffer(this.uBluePortal[renderContext.frameIndex()], bluePortal);
                PortalTransformHelper.setAsMissingToBuffer(this.uOrangePortal[renderContext.frameIndex()]);
                noPortals = false;
            }

            if(pTrack.hasOrangePortal()) {
                PortalEntity orangePortal = pTrack.getOrangePortal();
                PortalTransformHelper.setAsMissingToBuffer(this.uBluePortal[renderContext.frameIndex()]);
                PortalTransformHelper.copyIncompleteToBuffer(this.uOrangePortal[renderContext.frameIndex()], orangePortal);
                noPortals = false;
            }

            if(noPortals) {
                PortalTransformHelper.setAsMissingToBuffer(this.uBluePortal[renderContext.frameIndex()]);
                PortalTransformHelper.setAsMissingToBuffer(this.uOrangePortal[renderContext.frameIndex()]);
            }

            samplers.setOverlayMaterial(GeneratedAssets.PORTAL_INCOMPLETE);
        }

        shaderIO.setUniform(this.lBluePortal, this.dBluePortal, renderContext.frameIndex());
        shaderIO.setUniform(this.lOrangePortal, this.dOrangePortal, renderContext.frameIndex());

        switch (GameProperties.RENDER_PROCESS) {
            case NAIVE_FORWARD -> {
                // sends a -1.
                shaderIO.setUniform(this.lPortalLayer, this.dPortalLayer[DISABLE_CHECKS]);
                shaderIO.setUniform(this.lPortalTypeMask, this.dPortalTypeMask[DISABLE_CHECKS]);
            }

            case MULTI_PASS_FORWARD -> {
                this.limitPortalLayerToPass(shaderIO, layerOnPass);
                shaderIO.setUniform(this.lPortalTypeMask, this.dPortalTypeMask[DISABLE_CHECKS]);
            }

            case LAYERED_COMPOSE -> {
                this.limitPortalLayerToPass(shaderIO, layerOnSubpass);
                int redrawStep = renderContext.redrawIteration();

                if(layerOnSubpass == 0) {
                    shaderIO.setUniform(this.lPortalTypeMask, this.dPortalTypeMask[UNASSIGNED_PORTAL_TYPE]);

                    try(MemoryStack stack = MemoryStack.stackPush()) {
                        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                                .extent(it -> it
                                        .width(10000)
                                        .height(10000))
                                .offset(it -> it
                                        .x(0)
                                        .y(0));
                        VK11.vkCmdSetScissor(cmd, 0, scissor);
                    }


                // Redraw - draws 2 cycles in the same subpass.
                } else {
                    if(redrawStep == 0) {
                        shaderIO.setUniform(this.lPortalTypeMask, this.dPortalTypeMask[BLUE_PORTAL_TYPE]);
                        handleBlueLayer(cmd, pTrack);
                        renderContext.requestRedraw();

                        //TODO: bounds2d tests don't seem to be working so it isn;t culling. Fix those
                        // and then add the more specific checks.

                    } else {
                        shaderIO.setUniform(this.lPortalTypeMask, this.dPortalTypeMask[ORANGE_PORTAL_TYPE]);
                        handleOrangeLayer(cmd, pTrack);
                    }
                }
            }
        }

    }

    private static void handleBlueLayer(VkCommandBuffer cmd, PortalTracker pTrack) {
        if(!pTrack.hasBluePortal()) return;

        Bounds3D portalBounds = pTrack.getBluePortal().getScreenBounds(pTrack.getEngine().getScene());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            boolean shouldDraw = Bounds3D.SCREEN_BOUNDS.intersects(portalBounds);

            if(!shouldDraw) {
                cullDraw(cmd, stack);
            } else {
                noCull(cmd, stack);
            }
        }
    }

    private static void handleOrangeLayer(VkCommandBuffer cmd, PortalTracker pTrack) {
        if(!pTrack.hasOrangePortal()) return;

        Bounds3D portalBounds = pTrack.getOrangePortal().getScreenBounds(pTrack.getEngine().getScene());

        try (MemoryStack stack = MemoryStack.stackPush()) {

            boolean shouldDraw = Bounds3D.SCREEN_BOUNDS.intersects(portalBounds);

            if(!shouldDraw) {
                cullDraw(cmd, stack);
            } else {
                noCull(cmd, stack);
            }
        }
    }

    private static void cullDraw(VkCommandBuffer cmd, MemoryStack stack) {
        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                .extent(it -> it
                        .width(0)
                        .height(0))
                .offset(it -> it
                        .x(0)
                        .y(0));
        VK11.vkCmdSetScissor(cmd, 0, scissor);
    }

    private static void noCull(VkCommandBuffer cmd, MemoryStack stack) {
        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                .extent(it -> it
                        .width(100000)
                        .height(100000))
                .offset(it -> it
                        .x(0)
                        .y(0));
        VK11.vkCmdSetScissor(cmd, 0, scissor);
    }

    private void limitPortalLayerToPass(ShaderIO shaderIO, int layer) {
        // sends a 0 --> max-depth - 1

        UniformDescriptorSet currentLayer = this.dPortalLayer[layer + 2];

        if (GameProperties.ONLY_SHOW_RECURSION_LEVEL >= 0) {
            UniformDescriptorSet selectedSet = layer == GameProperties.ONLY_SHOW_RECURSION_LEVEL
                    ? currentLayer
                    : this.dPortalLayer[SKIP_LAYER];

            shaderIO.setUniform(this.lPortalLayer, selectedSet);

        } else {
            shaderIO.setUniform(this.lPortalLayer, currentLayer);
        }

        // Guarantee that origin room is shown, even if skipped.
        if(layer == 0 && GameProperties.SHOW_ORIGIN_ROOM) {
            shaderIO.setUniform(this.lPortalLayer, currentLayer);
        }
    }

    @Override
    public void cleanup() {
        VulkanUtil.cleanupAll(this.uBluePortal);
        VulkanUtil.cleanupAll(this.uOrangePortal);
        VulkanUtil.cleanupAll(this.uPortalLayer);
        VulkanUtil.cleanupAll(this.uPortalTypeMask);
    }
}
