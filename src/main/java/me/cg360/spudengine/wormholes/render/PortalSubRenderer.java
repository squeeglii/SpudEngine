package me.cg360.spudengine.wormholes.render;

import me.cg360.spudengine.core.render.data.DataTypes;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.data.type.MatrixHelper;
import me.cg360.spudengine.core.render.data.type.VectorHelper;
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
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK11;
import org.tinylog.Logger;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class PortalSubRenderer implements SubRenderProcess {

    private final WormholeDemo game;

    private DescriptorSetLayout lPortalTransform;
    private UniformDescriptorSet[] dPortalTransforms;
    private GeneralBuffer[] uPortalTransforms;
    private MatrixHelper portalTransformType;

    private DescriptorSetLayout lPortalOrigins;
    private UniformDescriptorSet[] dPortalOrigins;
    private GeneralBuffer[] uPortalOrigins;
    private VectorHelper portalOriginType;

    private DescriptorSetLayout lRoomDepthTarget;
    private UniformDescriptorSet[] dRoomDepthTarget;
    private GeneralBuffer[] uRoomDepthTarget;

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

        this.lPortalOrigins = new UniformDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_GEOMETRY_BIT)
                                        .enablePerFrameWrites(builder.swapChain());
        this.portalOriginType = DataTypes.VEC4F.asList(2);

        this.lRoomDepthTarget = new UniformDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_GEOMETRY_BIT)
                                        .enablePerFrameWrites(builder.swapChain());

        builder.addGeometryUniform(this.lPortalTransform);
        builder.addGeometryUniform(this.lPortalOrigins);
        builder.addGeometryUniform(this.lRoomDepthTarget);
    }

    @Override
    public void createDescriptorSets(DescriptorPool pool) {
        this.dPortalTransforms = UniformDescriptorSet.create(pool, this.lPortalTransform, this.portalTransformType, 0);
        this.uPortalTransforms = ShaderIO.collectUniformBuffers(this.dPortalTransforms);

        this.dPortalOrigins = UniformDescriptorSet.create(pool, this.lPortalOrigins, this.portalOriginType, 0);
        this.uPortalOrigins = ShaderIO.collectUniformBuffers(this.dPortalOrigins);

        this.dRoomDepthTarget = UniformDescriptorSet.create(pool, this.lRoomDepthTarget, DataTypes.INT, 0);
        this.uRoomDepthTarget = ShaderIO.collectUniformBuffers(this.dRoomDepthTarget);
    }

    @Override
    public void renderPreMesh(ShaderIO shaderIO, int frameIndex) {
        PortalTracker pTrack = this.game.getPortalTracker();

        if (pTrack.hasPortalPair()) {
            PortalEntity bluePortal =  pTrack.getBluePortal();
            PortalEntity orangePortal =  pTrack.getOrangePortal();

            Matrix4f bluePortalTransform = bluePortal.calculateConnectionTransform(orangePortal);
            Matrix4f orangePortalTransform = orangePortal.calculateConnectionTransform(bluePortal);

            //TODO: Fix alignment of vec3s in shaders.
            //      Shader handled uniforms in minimum of 16 byte chunks on this device.
            //      Get the val from the device, and align based off of that.
            Vector4f blueOrigin = new Vector4f(bluePortal.getPosition(), 1);
            Vector4f orangeOrigin = new Vector4f(orangePortal.getPosition(), 1);

            this.portalTransformType.copyToBuffer(this.uPortalTransforms[frameIndex], bluePortalTransform, orangePortalTransform);
            this.portalOriginType.copy4fToBuffer(this.uPortalOrigins[frameIndex], blueOrigin, orangeOrigin);
        }

        shaderIO.setUniform(this.lPortalTransform, this.dPortalTransforms, frameIndex);
        shaderIO.setUniform(this.lPortalOrigins, this.dPortalOrigins, frameIndex);
    }

    @Override
    public void tmp_setPortalUniform(ShaderIO shaderIO, int targetRoomId, int frameIndex) {
        GeneralBuffer buffer = this.dRoomDepthTarget[frameIndex].getBuffer();
        buffer.map();

        long mappedMemory = buffer.map();
        ByteBuffer write = MemoryUtil.memByteBuffer(mappedMemory, (int) buffer.getRequestedSize());
        write.putInt(targetRoomId);
        buffer.unmap();

        shaderIO.setUniform(this.lRoomDepthTarget, this.dRoomDepthTarget, frameIndex);
    }

    @Override
    public void cleanup() {
        Arrays.stream(this.uPortalTransforms).forEach(GeneralBuffer::cleanup);
        Arrays.stream(this.uPortalOrigins).forEach(GeneralBuffer::cleanup);
        Arrays.stream(this.uRoomDepthTarget).forEach(GeneralBuffer::cleanup);
    }
}
