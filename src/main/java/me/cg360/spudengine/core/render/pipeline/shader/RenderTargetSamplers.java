package me.cg360.spudengine.core.render.pipeline.shader;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.ImageView;
import me.cg360.spudengine.core.render.image.texture.TextureSampler;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.SamplerDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.SamplerDescriptorSetLayout;
import me.cg360.spudengine.core.util.VulkanUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;

import java.nio.LongBuffer;

public class RenderTargetSamplers {

    private final ShaderIO shaderIO;

    private DescriptorPool hostPool;

    private final int renderTargetCount;
    private final ImageView[] depthOnlyViews;

    private final DescriptorSetLayout lColourSampler;
    private SamplerDescriptorSet[] dColourSampler;
    private TextureSampler uColourSampler;

    private final DescriptorSetLayout lDepthSampler;
    private SamplerDescriptorSet[] dDepthSampler;
    private TextureSampler uDepthSampler;

    public RenderTargetSamplers(ShaderIO shaderIO, DescriptorSetLayoutBundle builder, int renderTargetCount) {
        this.shaderIO = shaderIO;

        this.renderTargetCount = renderTargetCount;
        this.depthOnlyViews = new ImageView[renderTargetCount];

        this.lColourSampler = new SamplerDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_FRAGMENT_BIT)
                .setCount(renderTargetCount);
        this.lDepthSampler = new SamplerDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_FRAGMENT_BIT)
                .setCount(renderTargetCount);

        builder.addFragmentUniforms(this.lColourSampler, this.lDepthSampler);
    }

    public void buildSets(DescriptorPool pool) {
        this.hostPool = pool;

        this.dColourSampler = new SamplerDescriptorSet[this.renderTargetCount];
        this.uColourSampler = new TextureSampler(this.hostPool.getDevice(), 1, false, VK11.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);

        this.dDepthSampler = new SamplerDescriptorSet[this.renderTargetCount];
        this.uDepthSampler = new TextureSampler(this.hostPool.getDevice(), 1, false, VK11.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
    }

    public void registerRenderTarget(LogicalDevice device, Attachment colourAttachment, Attachment depthAttachment, int renderTargetId) {
        if(this.dColourSampler[renderTargetId] != null || this.dDepthSampler[renderTargetId] != null)
            throw new IllegalStateException("Render Target Id is already registered to a sampler - see #freeRenderTarget(...)");

        this.dColourSampler[renderTargetId] = new SamplerDescriptorSet(this.hostPool, this.lColourSampler, 0, VK11.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, colourAttachment, this.uColourSampler);

        this.depthOnlyViews[renderTargetId] = ImageView.builder()
                .format(depthAttachment.getImage().getFormat())
                .aspectMask(VK11.VK_IMAGE_ASPECT_DEPTH_BIT)
                .build(device, depthAttachment.getImage().getHandle());

        this.dDepthSampler[renderTargetId] = new SamplerDescriptorSet(this.hostPool, this.lDepthSampler, 0, VK11.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, this.depthOnlyViews[renderTargetId], this.uDepthSampler);
    }

    public void freeRenderTargets() {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer renderTargetSets = stack.mallocLong(this.renderTargetCount*2);

            for(int i = 0; i < this.renderTargetCount; i++) {
                long colour = this.dColourSampler[i].getHandle();
                long depth = this.dDepthSampler[i].getHandle();

                this.depthOnlyViews[i].cleanup();
                renderTargetSets.put(colour);
                renderTargetSets.put(depth);

                this.dColourSampler[i] = null;
                this.dDepthSampler[i] = null;
                this.depthOnlyViews[i] = null; // Depth-Only view gets cleaned up automatically.
            }

            renderTargetSets.flip();

            VK11.vkFreeDescriptorSets(this.hostPool.getDevice().asVk(), this.hostPool.getHandle(), renderTargetSets);
        }
    }

    public void freeRenderTarget(int renderTargetId) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer renderTargetSets = stack.mallocLong(2);

            long colour = this.dColourSampler[renderTargetId].getHandle();
            long depth = this.dDepthSampler[renderTargetId].getHandle();

            renderTargetSets.put(0, colour);
            renderTargetSets.put(1, depth);

            VK11.vkFreeDescriptorSets(this.hostPool.getDevice().asVk(), this.hostPool.getHandle(), renderTargetSets);

            this.dColourSampler[renderTargetId] = null;
            this.dDepthSampler[renderTargetId] = null;
            this.depthOnlyViews[renderTargetId] = null;
        }
    }

    public void selectRenderTarget(int renderTargetId) {
        this.shaderIO.setUniform(this.lColourSampler, this.dColourSampler[renderTargetId]);
        this.shaderIO.setUniform(this.lDepthSampler, this.dDepthSampler[renderTargetId]);
    }

    public void cleanup() {
        VulkanUtil.cleanupAll(this.depthOnlyViews);
        this.uColourSampler.cleanup();
        this.uDepthSampler.cleanup();
    }

}
