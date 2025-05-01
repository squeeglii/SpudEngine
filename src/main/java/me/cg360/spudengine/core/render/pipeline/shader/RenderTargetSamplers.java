package me.cg360.spudengine.core.render.pipeline.shader;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.exception.EngineLimitExceededException;
import me.cg360.spudengine.core.render.geometry.model.BundledMaterial;
import me.cg360.spudengine.core.render.geometry.model.Material;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.image.texture.TextureSampler;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.SamplerDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.SamplerDescriptorSetLayout;
import org.lwjgl.vulkan.VK11;

public class RenderTargetSamplers {

    private final ShaderIO shaderIO;

    private DescriptorPool hostPool;

    private final int renderTextureCount;

    private final DescriptorSetLayout lColourSampler;
    private SamplerDescriptorSet[] dColourSampler;
    private TextureSampler uColourSampler;

    private final DescriptorSetLayout lDepthSampler;
    private SamplerDescriptorSet[] dDepthSampler;
    private TextureSampler uDepthSampler;

    public RenderTargetSamplers(ShaderIO shaderIO, DescriptorSetLayoutBundle builder, int renderTextureCount) {
        this.shaderIO = shaderIO;

        this.renderTextureCount = renderTextureCount;

        this.lColourSampler = new SamplerDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_FRAGMENT_BIT)
                .setCount(renderTextureCount);
        this.lDepthSampler = new SamplerDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_FRAGMENT_BIT)
                .setCount(renderTextureCount);

        builder.addFragmentUniforms(this.lColourSampler, this.lDepthSampler);
    }

    public void buildSets(DescriptorPool pool) {
        this.hostPool = pool;

        this.dColourSampler = new SamplerDescriptorSet[this.renderTextureCount];
        this.uColourSampler = new TextureSampler(this.hostPool.getDevice(), 1, false, VK11.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);

        this.dDepthSampler = new SamplerDescriptorSet[this.renderTextureCount];
        this.uDepthSampler = new TextureSampler(this.hostPool.getDevice(), 1, false, VK11.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
    }

    public void registerRenderTarget(Attachment colourAttachment, Attachment depthAttachment, int renderTargetId) {
        this.dColourSampler[renderTargetId] = new SamplerDescriptorSet(this.hostPool, this.lColourSampler, 0, colourAttachment, this.uColourSampler);
        this.dDepthSampler[renderTargetId] = new SamplerDescriptorSet(this.hostPool, this.lDepthSampler, 0, depthAttachment, this.uDepthSampler);
    }

    public void selectRenderTarget(int renderTargetId) {
        this.shaderIO.setUniform(this.lColourSampler, this.dColourSampler[renderTargetId]);
        this.shaderIO.setUniform(this.lDepthSampler, this.dDepthSampler[renderTargetId]);
    }


    public void cleanup() {
        this.uColourSampler.cleanup();
        this.uDepthSampler.cleanup();
    }

}
