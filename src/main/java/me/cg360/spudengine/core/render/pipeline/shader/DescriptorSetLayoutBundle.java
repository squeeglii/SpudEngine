package me.cg360.spudengine.core.render.pipeline.shader;

import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public final class DescriptorSetLayoutBundle {

    private final LogicalDevice device;
    private final SwapChain swapChain;

    private final List<DescriptorSetLayout> vertexUniforms;
    private final List<DescriptorSetLayout> geometryUniforms;
    private final List<DescriptorSetLayout> fragmentUniforms;

    public DescriptorSetLayoutBundle(LogicalDevice device, SwapChain swapChain) {
        this.device = device;
        this.swapChain = swapChain;

        this.vertexUniforms = new LinkedList<>();
        this.geometryUniforms = new LinkedList<>();
        this.fragmentUniforms = new LinkedList<>();
    }

    public void addVertexUniform(DescriptorSetLayout vertexUniform) {
        this.vertexUniforms.add(vertexUniform);
    }

    public void addVertexUniforms(DescriptorSetLayout... vertexUniforms) {
        this.vertexUniforms.addAll(Arrays.asList(vertexUniforms));
    }

    public void addGeometryUniform(DescriptorSetLayout geometryUniform) {
        this.geometryUniforms.add(geometryUniform);
    }

    public void addGeometryUniforms(DescriptorSetLayout... geometryUniforms) {
        this.geometryUniforms.addAll(Arrays.asList(geometryUniforms));
    }

    public void addFragmentUniform(DescriptorSetLayout fragmentUniform) {
        this.fragmentUniforms.add(fragmentUniform);
    }

    public void addFragmentUniforms(DescriptorSetLayout... fragmentUniforms) {
        this.fragmentUniforms.addAll(Arrays.asList(fragmentUniforms));
    }

    public LogicalDevice device() {
        return this.device;
    }

    public SwapChain swapChain() {
        return this.swapChain;
    }

    public DescriptorSetLayout[] merge() {
        List<DescriptorSetLayout> combinedSet = new LinkedList<>();
        combinedSet.addAll(this.vertexUniforms);
        combinedSet.addAll(this.geometryUniforms);
        combinedSet.addAll(this.fragmentUniforms);

        return combinedSet.toArray(DescriptorSetLayout[]::new);
    }

    @Override
    public String toString() {
        return "[ DescriptorSetLayout Bundle ]: { %s vertex, %s geometry, %s fragment}".formatted(
                this.vertexUniforms.size(), this.geometryUniforms.size(), this.fragmentUniforms.size()
        );
    }
}
