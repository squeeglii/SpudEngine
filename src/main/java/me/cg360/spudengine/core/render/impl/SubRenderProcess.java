package me.cg360.spudengine.core.render.impl;

import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.shader.DescriptorSetLayoutBundle;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderIO;
import me.cg360.spudengine.core.render.pipeline.shader.StandardSamplers;
import org.lwjgl.vulkan.VkCommandBuffer;

public interface SubRenderProcess {

    default void buildUniformLayout(DescriptorSetLayoutBundle layout) {}

    default void createDescriptorSets(DescriptorPool pool) {}

    default void renderPreMesh(RenderContext renderContext, ShaderIO shaderIO, StandardSamplers samplers, VkCommandBuffer cmd) {}
    default void renderModel(RenderContext renderContext, ShaderIO shaderIO, StandardSamplers samplers, BufferedModel model) {}

    void cleanup();

}
