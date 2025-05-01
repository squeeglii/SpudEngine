package me.cg360.spudengine.core.render.pipeline.shader;

import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.geometry.model.BufferedMesh;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.DescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.UniformDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.tinylog.Logger;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

public class ShaderIO {
    private LongBuffer descriptorSets;
    private LongBuffer vertexBuffer;
    private LongBuffer vertexBufferOffset;
    private ByteBuffer pushConstantsBuffer;

    public ShaderIO() {
        this.free();
    }

    public void setUniform(int setNumber, DescriptorSet set) {
        this.descriptorSets.put(setNumber, set.getHandle());
    }

    public void setUniform(DescriptorSetLayout layout, DescriptorSet set) {
        this.setUniform(layout.getSetPosition(), set);
    }

    public void setUniform(int setNumber, DescriptorSet[] set, int swapImageIndex) {
        this.setUniform(setNumber, set[swapImageIndex]);
    }

    public void setUniform(DescriptorSetLayout layout, DescriptorSet[] set, int swapImageIndex) {
        this.setUniform(layout.getSetPosition(), set[swapImageIndex]);
    }

    public void bindMesh(VkCommandBuffer cmd, BufferedMesh mesh) {
        this.vertexBuffer.put(0, mesh.vertices().getHandle());
        VK11.vkCmdBindVertexBuffers(cmd, 0, this.vertexBuffer, this.vertexBufferOffset);
        VK11.vkCmdBindIndexBuffer(cmd, mesh.indices().getHandle(), 0, VK11.VK_INDEX_TYPE_UINT32);
    }

    public void bindDescriptorSets(VkCommandBuffer cmd, long pipelineLayout) {
        VK11.vkCmdBindDescriptorSets(cmd, VK11.VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, this.descriptorSets, null);
    }

    public void applyVertexPushConstants(VkCommandBuffer cmd, long currentPipelineLayout, Matrix4f transform) {
        transform.get(this.pushConstantsBuffer); // copy transform to pushConstantsBuffer
        VK11.vkCmdPushConstants(cmd, currentPipelineLayout, VK11.VK_SHADER_STAGE_VERTEX_BIT, 0, this.pushConstantsBuffer);
    }

    public void applyGeometryPushConstants(VkCommandBuffer cmd, long currentPipelineLayout, Matrix4f transform) {
        transform.get(this.pushConstantsBuffer); // copy transform to pushConstantsBuffer
        VK11.vkCmdPushConstants(cmd, currentPipelineLayout, VK11.VK_SHADER_STAGE_GEOMETRY_BIT, 0, this.pushConstantsBuffer);
    }

    public void reset(MemoryStack stack, Pipeline pipeline, DescriptorPool pool) {
        this.descriptorSets = stack.mallocLong(pool.getSetPositionCount());
        this.vertexBuffer = stack.mallocLong(1);
        this.vertexBufferOffset = stack.mallocLong(1);
        this.vertexBufferOffset.put(0, 0L);

        this.pushConstantsBuffer = stack.malloc(pipeline.getPushConstantSize());
    }

    public void free() {
        this.descriptorSets = null;
        this.vertexBuffer = null;
        this.vertexBufferOffset = null;
        this.pushConstantsBuffer = null;
    }

    public static GeneralBuffer[] collectUniformBuffers(UniformDescriptorSet[] set) {
        GeneralBuffer[] buffers = new GeneralBuffer[set.length];
        for (int i = 0; i < set.length; i++)
            buffers[i] = set[i].getBuffer();

        return buffers;
    }

}
