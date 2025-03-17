package me.cg360.spudengine.core.render.impl;

import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.shader.DescriptorSetLayoutBundle;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderIO;

public interface SubRenderProcess {

    default void buildUniformLayout(DescriptorSetLayoutBundle layout) {}

    default void createDescriptorSets(DescriptorPool pool) {}

    default void renderPreMesh(ShaderIO shaderIO, int frameIndex) {}
    default void renderModel(ShaderIO shaderIO, BufferedModel model, int frameIndex) {}

    void cleanup();

}
