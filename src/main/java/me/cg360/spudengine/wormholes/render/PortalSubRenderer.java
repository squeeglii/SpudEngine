package me.cg360.spudengine.wormholes.render;

import me.cg360.spudengine.core.render.data.DataTypes;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.data.type.MatrixHelper;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.UniformDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.UniformDescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.shader.DescriptorSetLayoutBundle;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderIO;
import me.cg360.spudengine.wormholes.WormholeDemo;
import me.cg360.spudengine.wormholes.logic.PortalTracker;
import me.cg360.spudengine.wormholes.world.entity.PortalEntity;
import org.joml.Matrix4f;
import org.lwjgl.vulkan.VK11;
import org.tinylog.Logger;

import java.util.Arrays;

public class PortalSubRenderer implements SubRenderProcess {

    private final WormholeDemo game;

    private DescriptorSetLayout lPortalTransform;
    private UniformDescriptorSet[] dPortalTransforms;
    private GeneralBuffer[] uPortalTransforms;
    private MatrixHelper portalTransformType;

    public PortalSubRenderer(WormholeDemo game) {
        this.game = game;

        Logger.info("Using Portal Sub-Renderer");
    }

    @Override
    public void buildUniformLayout(DescriptorSetLayoutBundle builder) {
        Logger.info("Building Portal Uniform Layout");
        this.lPortalTransform = new UniformDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_GEOMETRY_BIT)
                                         .enablePerFrameWrites(builder.swapChain());
        this.portalTransformType = DataTypes.MAT4X4F.asList(2);


        builder.addGeometryUniform(this.lPortalTransform);
    }

    @Override
    public void createDescriptorSets(DescriptorPool pool) {
        this.dPortalTransforms = UniformDescriptorSet.create(pool, this.lPortalTransform, this.portalTransformType, 0);
        this.uPortalTransforms = ShaderIO.collectUniformBuffers(this.dPortalTransforms);
    }

    @Override
    public void renderPreMesh(ShaderIO shaderIO, int frameIndex) {
        PortalTracker pTrack = this.game.getPortalTracker();

        if (pTrack.hasPortalPair()) {
            PortalEntity bluePortal =  pTrack.getBluePortal();
            PortalEntity orangePortal =  pTrack.getOrangePortal();

            Matrix4f bluePortalTransform = bluePortal.calculateConnectionTransform(orangePortal);
            Matrix4f orangePortalTransform = orangePortal.calculateConnectionTransform(bluePortal);

            this.portalTransformType.copyToBuffer(this.uPortalTransforms[frameIndex], bluePortalTransform, orangePortalTransform);

        }

        shaderIO.setUniform(this.lPortalTransform, this.dPortalTransforms, frameIndex);
    }

    @Override
    public void cleanup() {
        Arrays.stream(this.uPortalTransforms).forEach(GeneralBuffer::cleanup);
    }
}
