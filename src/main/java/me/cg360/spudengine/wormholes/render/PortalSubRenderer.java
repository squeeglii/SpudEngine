package me.cg360.spudengine.wormholes.render;

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
import me.cg360.spudengine.wormholes.GeneratedAssets;
import me.cg360.spudengine.wormholes.WormholeDemo;
import me.cg360.spudengine.wormholes.logic.PortalTracker;
import me.cg360.spudengine.wormholes.render.pass.PortalLayerColourRenderPass;
import me.cg360.spudengine.wormholes.world.entity.PortalEntity;
import org.joml.Matrix4f;
import org.lwjgl.vulkan.VK11;
import org.tinylog.Logger;

import java.util.Arrays;

public class PortalSubRenderer implements SubRenderProcess {

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
                .setCount(PortalLayerColourRenderPass.MAX_PORTAL_DEPTH); // fixed values that can be swapped out like textures.

        builder.addGeometryUniform(this.lBluePortal);
        builder.addGeometryUniform(this.lOrangePortal);
        builder.addGeometryUniform(this.lPortalLayer);
    }

    @Override
    public void createDescriptorSets(DescriptorPool pool) {
        this.dBluePortal = UniformDescriptorSet.create(pool, this.lBluePortal, PORTAL_TYPE, 0);
        this.uBluePortal = ShaderIO.collectUniformBuffers(this.dBluePortal);

        this.dOrangePortal = UniformDescriptorSet.create(pool, this.lOrangePortal, PORTAL_TYPE, 0);
        this.uOrangePortal = ShaderIO.collectUniformBuffers(this.dOrangePortal);

        this.dPortalLayer = UniformDescriptorSet.create(pool, this.lPortalLayer, DataTypes.INT, 0);
        this.uPortalLayer = ShaderIO.collectUniformBuffers(this.dPortalLayer);

        for(int i = 0; i < PortalLayerColourRenderPass.MAX_PORTAL_DEPTH; i++) {
            final int layerOrd = i;
            this.uPortalLayer[i].runMapped(buf -> buf.putInt(layerOrd));
        }
    }

    @Override
    public void renderPreMesh(ShaderIO shaderIO, StandardSamplers samplers, int frameIndex, int subPass) {
        PortalTracker pTrack = this.game.getPortalTracker();

        // last subpass == closest room copy. invert using max depth.
        //Logger.info("Rendering Portal PreMesh {}", subPass);
        int layer = PortalLayerColourRenderPass.MAX_PORTAL_DEPTH - 1 - subPass;

        if (pTrack.hasPortalPair()) {
            PortalEntity bluePortal =  pTrack.getBluePortal();
            PortalEntity orangePortal =  pTrack.getOrangePortal();

            Matrix4f bluePortalTransform = bluePortal.calculateConnectionTransform(orangePortal);
            Matrix4f orangePortalTransform = orangePortal.calculateConnectionTransform(bluePortal);

            //TODO: Fix alignment of vec3s in shaders.
            //      Shader handled uniforms in minimum of 16 byte chunks on this device.
            //      Get the val from the device, and align based off of that.

            PortalTransformHelper.copyToBuffer(this.uBluePortal[frameIndex], bluePortal, bluePortalTransform);
            PortalTransformHelper.copyToBuffer(this.uOrangePortal[frameIndex], orangePortal, orangePortalTransform);

            samplers.setOverlayMaterial(GeneratedAssets.PORTAL_CUTOUT);

        } else {
            boolean noPortals = true;

            if(pTrack.hasBluePortal()) {
                PortalEntity bluePortal = pTrack.getBluePortal();
                PortalTransformHelper.copyIncompleteToBuffer(this.uBluePortal[frameIndex], bluePortal);
                PortalTransformHelper.setAsMissingToBuffer(this.uOrangePortal[frameIndex]);
                noPortals = false;
            }

            if(pTrack.hasOrangePortal()) {
                PortalEntity orangePortal = pTrack.getOrangePortal();
                PortalTransformHelper.setAsMissingToBuffer(this.uBluePortal[frameIndex]);
                PortalTransformHelper.copyIncompleteToBuffer(this.uOrangePortal[frameIndex], orangePortal);
                noPortals = false;
            }

            if(noPortals) {
                PortalTransformHelper.setAsMissingToBuffer(this.uBluePortal[frameIndex]);
                PortalTransformHelper.setAsMissingToBuffer(this.uOrangePortal[frameIndex]);
            }

            samplers.setOverlayMaterial(GeneratedAssets.PORTAL_INCOMPLETE);
        }

        shaderIO.setUniform(this.lBluePortal, this.dBluePortal, frameIndex);
        shaderIO.setUniform(this.lOrangePortal, this.dOrangePortal, frameIndex);
        shaderIO.setUniform(this.lPortalLayer, this.dPortalLayer[layer]);
    }

    @Override
    public void cleanup() {
        Arrays.stream(this.uBluePortal).forEach(GeneralBuffer::cleanup);
        Arrays.stream(this.uOrangePortal).forEach(GeneralBuffer::cleanup);
        Arrays.stream(this.uPortalLayer).forEach(GeneralBuffer::cleanup);
    }
}
