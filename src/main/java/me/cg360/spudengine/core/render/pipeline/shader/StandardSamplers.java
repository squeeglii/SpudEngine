package me.cg360.spudengine.core.render.pipeline.shader;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.exception.EngineLimitExceededException;
import me.cg360.spudengine.core.render.geometry.model.BundledMaterial;
import me.cg360.spudengine.core.render.geometry.model.Material;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.image.texture.TextureSampler;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.SamplerDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.SamplerDescriptorSetLayout;
import org.lwjgl.vulkan.VK11;

import java.util.HashMap;
import java.util.Map;

public class StandardSamplers {

    private final ShaderIO shaderIO;

    private DescriptorPool hostPool;

    private final DescriptorSetLayout lSampler;
    private Map<String, SamplerDescriptorSet> dSampler;
    private TextureSampler uSampler;

    private final DescriptorSetLayout lOverlaySampler;
    private Map<String, SamplerDescriptorSet> dOverlaySampler;
    private TextureSampler uOverlaySampler;

    public StandardSamplers(ShaderIO shaderIO, DescriptorSetLayoutBundle builder) {
        this.shaderIO = shaderIO;

        this.lSampler = new SamplerDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_FRAGMENT_BIT)
                .setCount(EngineProperties.MAX_TEXTURES);
        this.lOverlaySampler = new SamplerDescriptorSetLayout(builder.device(), 0, VK11.VK_SHADER_STAGE_FRAGMENT_BIT)
                .setCount(EngineProperties.MAX_OVERLAY_TEXTURES);

        builder.addFragmentUniforms(this.lSampler, this.lOverlaySampler);
    }

    public void buildSets(DescriptorPool pool) {
        this.hostPool = pool;

        this.dSampler = new HashMap<>();
        this.uSampler = new TextureSampler(this.hostPool.getDevice(), 1, true);

        this.dOverlaySampler = new HashMap<>();
        this.uOverlaySampler = new TextureSampler(this.hostPool.getDevice(), 1, true, VK11.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
    }

    public void registerTexture(Texture texture) {
        String textureFileName = texture.getResourceName();
        SamplerDescriptorSet samplerDescriptorSet = this.dSampler.get(textureFileName);

        if (samplerDescriptorSet == null) {
            if(this.dSampler.size() >= EngineProperties.MAX_TEXTURES)
                throw new EngineLimitExceededException("MAX_TEXTURES", EngineProperties.MAX_TEXTURES, this.dSampler.size()+1);

            samplerDescriptorSet = new SamplerDescriptorSet(this.hostPool, this.lSampler, 0, texture, this.uSampler);
            this.dSampler.put(textureFileName, samplerDescriptorSet);
        }
    }

    public void registerOverlay(Texture texture) {
        String textureFileName = texture.getResourceName();
        SamplerDescriptorSet overlaySamplerDescriptorSet = this.dOverlaySampler.get(textureFileName);

        if (overlaySamplerDescriptorSet == null) {
            if(this.dOverlaySampler.size() >= EngineProperties.MAX_OVERLAY_TEXTURES)
                throw new EngineLimitExceededException("MAX_OVERLAY_TEXTURES", EngineProperties.MAX_OVERLAY_TEXTURES, this.dOverlaySampler.size()+1);

            overlaySamplerDescriptorSet = new SamplerDescriptorSet(this.hostPool, this.lOverlaySampler, 0, texture, this.uOverlaySampler);
            this.dOverlaySampler.put(textureFileName, overlaySamplerDescriptorSet);
        }
    }


    public void setMaterial(BundledMaterial material) {
        SamplerDescriptorSet samplerDescriptorSet = this.dSampler.get(material.texture().getResourceName());
        this.shaderIO.setUniform(this.lSampler, samplerDescriptorSet);
    }

    // todo: get some checks on this - make sure there's always a texture available.
    public void setOverlayMaterial(Material material) {
        SamplerDescriptorSet samplerDescriptorSet = this.dOverlaySampler.get(material.texture());
        this.shaderIO.setUniform(this.lOverlaySampler, samplerDescriptorSet);
    }


    public void cleanup() {
        this.uSampler.cleanup();
        this.uOverlaySampler.cleanup();
    }

}
