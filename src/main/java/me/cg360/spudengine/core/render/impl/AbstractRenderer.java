package me.cg360.spudengine.core.render.impl;

import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.data.DataTypes;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.geometry.model.BufferedMesh;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.geometry.model.BundledMaterial;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.UniformDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.UniformDescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.shader.DescriptorSetLayoutBundle;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderIO;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderProgram;
import me.cg360.spudengine.core.render.pipeline.shader.StandardSamplers;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.core.world.entity.RenderedEntity;
import me.cg360.spudengine.core.world.entity.StaticModelEntity;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.tinylog.Logger;

import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractRenderer extends RenderProcess {

    public static final int DEPTH_ATTACHMENT_FORMAT = VK11.VK_FORMAT_D32_SFLOAT_S8_UINT;

    protected final LogicalDevice device;
    protected final Scene scene;

    protected SwapChain swapChain;

    protected DescriptorPool descriptorPool;

    protected DescriptorSetLayout lProjectionMatrix;
    protected UniformDescriptorSet[] dProjectionMatrix;
    protected GeneralBuffer[] uProjectionMatrix;

    protected DescriptorSetLayout lViewMatrix;
    protected UniformDescriptorSet[] dViewMatrix;
    protected GeneralBuffer[] uViewMatrix;

    protected StandardSamplers standardSamplers;

    protected final ShaderIO shaderIO; // #reset(...) whenever new draw call

    public AbstractRenderer(SwapChain swapChain, Scene scene, SubRenderProcess[] subRenderProcesses) {
        super(subRenderProcesses);

        this.swapChain = swapChain;
        this.device = swapChain.getDevice();
        this.scene = scene;

        this.shaderIO = new ShaderIO();
    }

    protected abstract void buildPipelines(DescriptorSetLayout[] descriptorSetLayouts);
    protected abstract ShaderProgram buildShaderProgram();

    protected void doRenderPass(VkCommandBuffer cmd, VkRenderPassBeginInfo renderPassBeginInfo, Pipeline pipeline, int frameIndex, int passNum, MemoryStack stack, Consumer<RenderContext> passActions) {
        VK11.vkCmdBeginRenderPass(cmd, renderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE);
        this.shaderIO.reset(stack, pipeline.bind(cmd), this.descriptorPool);
        passActions.accept(this.getCurrentContext());
        VK11.vkCmdEndRenderPass(cmd);
    }

    protected final DescriptorSetLayout[] initDescriptorSets() {
        Logger.info("Initializing Descriptor Sets...");

        DescriptorSetLayoutBundle bundle = new DescriptorSetLayoutBundle(this.device, this.swapChain);
        DescriptorSetLayout[] layout = this.buildUniformLayout(bundle).merge();
        Logger.info("Collected {} uniform(s) from {}", layout.length, bundle);

        this.descriptorPool = new DescriptorPool(this.device, layout);
        this.buildDescriptorSets();

        Logger.info("Initialized Descriptor Sets!");
        return layout;
    }

    protected DescriptorSetLayoutBundle buildUniformLayout(DescriptorSetLayoutBundle bundle) {
        this.lProjectionMatrix = new UniformDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_GEOMETRY_BIT)
                .enablePerFrameWrites(this.swapChain);
        this.lViewMatrix = new UniformDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_GEOMETRY_BIT)
                .enablePerFrameWrites(this.swapChain);

        bundle.addGeometryUniforms(this.lProjectionMatrix, this.lViewMatrix);

        this.standardSamplers = new StandardSamplers(this.shaderIO, bundle);

        for(SubRenderProcess process: this.subRenderProcesses)
            process.buildUniformLayout(bundle);

        return bundle;
    }

    protected void buildDescriptorSets() {
        this.dProjectionMatrix = UniformDescriptorSet.create(this.descriptorPool, this.lProjectionMatrix, DataTypes.MAT4X4F, 0);
        this.uProjectionMatrix = ShaderIO.collectUniformBuffers(this.dProjectionMatrix);

        this.dViewMatrix = UniformDescriptorSet.create(this.descriptorPool, this.lViewMatrix, DataTypes.MAT4X4F, 0);
        this.uViewMatrix = ShaderIO.collectUniformBuffers(this.dViewMatrix);

        this.standardSamplers.buildSets(this.descriptorPool);

        for(SubRenderProcess process: this.subRenderProcesses)
            process.createDescriptorSets(this.descriptorPool);
    }


    protected void startRenderPass(VkCommandBuffer cmd, VkRenderPassBeginInfo renderPassBeginInfo, Pipeline pipeline, MemoryStack stack) {
        VK11.vkCmdBeginRenderPass(cmd, renderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE);
        this.shaderIO.reset(stack, pipeline.bind(cmd), this.descriptorPool);
    }

    protected void nextSubPass(VkCommandBuffer cmd, Pipeline pipeline, MemoryStack stack) {
        VK11.vkCmdNextSubpass(cmd, VK11.VK_SUBPASS_CONTENTS_INLINE);
        this.shaderIO.reset(stack, pipeline.bind(cmd), this.descriptorPool);
    }

    protected void drawAllSceneModels(VkCommandBuffer cmd, RenderSystem renderSystem, Pipeline selectedPipeline, int frameIndex) {
        for (String modelId: this.scene.getUsedModelIds()) {
            this.drawModel(cmd, renderSystem, selectedPipeline, modelId, frameIndex);
        }
    }

    protected void drawModel(VkCommandBuffer cmd, RenderSystem renderSystem, Pipeline selectedPipeline, String modelId, int frameIndex) {
        BufferedModel model = renderSystem.getModelManager().getModel(modelId);
        List<StaticModelEntity> entities = this.scene.getEntitiesWithModel(modelId);

        for(SubRenderProcess process: this.subRenderProcesses)
            process.renderModel(this.getCurrentContext(), this.shaderIO, this.standardSamplers, model);

        for(BundledMaterial material: model.getMaterials()) {
            if (material.meshes().isEmpty())
                continue;

            this.standardSamplers.setMaterial(material);

            for(BufferedMesh mesh: material.meshes()) {
                this.shaderIO.bindMesh(cmd, mesh);

                for(RenderedEntity entity: entities) {
                    if(!entity.shouldDraw(this.getCurrentContext())) continue;

                    long layoutHandle = selectedPipeline.getPipelineLayoutHandle();
                    this.shaderIO.bindDescriptorSets(cmd, layoutHandle);
                    this.shaderIO.applyGeometryPushConstants(cmd, layoutHandle, entity.getTransform());

                    VK11.vkCmdDrawIndexed(cmd, mesh.numIndices(), 1, 0, 0, 0);
                }
            }
        }
    }

    @Override
    public void processModelBatch(List<BufferedModel> models) {
        this.device.waitIdle();
        Logger.debug("Processing {} models", models.size());

        for (BufferedModel vulkanModel : models) {
            for (BundledMaterial vulkanMaterial : vulkanModel.getMaterials()) {
                if (vulkanMaterial.meshes().isEmpty())
                    continue;

                this.standardSamplers.registerTexture(vulkanMaterial.texture());
            }
        }
    }

    @Override
    public void processOverlays(CommandPool uploadPool, CommandQueue queue, List<Texture> overlayTextures) {
        this.device.waitIdle();
        Logger.debug("Processing {} overlay textures", overlayTextures.size());

        CommandBuffer cmd = new CommandBuffer(uploadPool, true, true);

        cmd.record(() -> {
            for(Texture texture: overlayTextures) {
                texture.upload(cmd);
            }
        }).submitAndWait(queue);

        // all textures transformed, use!
        for(Texture texture : overlayTextures) {
            this.standardSamplers.registerOverlay(texture);
        }
    }

}
