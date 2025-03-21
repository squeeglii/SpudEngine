package me.cg360.spudengine.wormholes.render;

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

        builder.addGeometryUniform(this.lBluePortal);
        builder.addGeometryUniform(this.lOrangePortal);
    }

    @Override
    public void createDescriptorSets(DescriptorPool pool) {
        this.dBluePortal = UniformDescriptorSet.create(pool, this.lBluePortal, PORTAL_TYPE, 0);
        this.uBluePortal = ShaderIO.collectUniformBuffers(this.dBluePortal);

        this.dOrangePortal = UniformDescriptorSet.create(pool, this.lOrangePortal, PORTAL_TYPE, 0);
        this.uOrangePortal = ShaderIO.collectUniformBuffers(this.dOrangePortal);
    }

    @Override
    public void renderPreMesh(ShaderIO shaderIO, StandardSamplers samplers, int frameIndex) {
        PortalTracker pTrack = this.game.getPortalTracker();

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
    }

    @Override
    public void cleanup() {
        Arrays.stream(this.uBluePortal).forEach(GeneralBuffer::cleanup);
        Arrays.stream(this.uOrangePortal).forEach(GeneralBuffer::cleanup);
    }
}
