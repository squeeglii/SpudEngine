package me.cg360.spudengine.core.render.impl;

import me.cg360.spudengine.core.EngineProperties;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.data.DataTypes;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.geometry.VertexFormats;
import me.cg360.spudengine.core.render.geometry.model.BufferedMesh;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.geometry.model.BundledMaterial;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.FrameBuffer;
import me.cg360.spudengine.core.render.image.ImageView;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.image.texture.TextureSampler;
import me.cg360.spudengine.core.render.pipeline.Pipeline;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.SamplerDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.UniformDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.SamplerDescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.UniformDescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.pass.SwapChainRenderPass;
import me.cg360.spudengine.core.render.pipeline.shader.BinaryShaderFile;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderCompiler;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderProgram;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderType;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.render.sync.SyncSemaphores;
import me.cg360.spudengine.core.exception.EngineLimitExceededException;
import me.cg360.spudengine.core.world.Projection;
import me.cg360.spudengine.core.world.entity.RenderedEntity;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.core.world.entity.StaticModelEntity;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForwardRenderer implements RenderProcess {

    private LogicalDevice device;

    private final CommandBuffer[] commandBuffers;
    private final Fence[] fences;
    private final SwapChainRenderPass renderPass;

    private SwapChain swapChain;
    private FrameBuffer[] frameBuffers;

    private final PipelineCache pipelineCache;
    private final Pipeline pipeline;
    private final Pipeline wireframePipeline;
    private final ShaderProgram shaderProgram;

    private final Scene scene;

    private Attachment[] depthAttachments;

    private DescriptorPool descriptorPool;
    private DescriptorSetLayout uniformDescriptorSetLayout;
    private DescriptorSetLayout samplerDescriptorSetLayout;
    private TextureSampler uSampler;
    private Map<String, SamplerDescriptorSet> samplerDescriptors;

    private UniformDescriptorSet dProjectionMatrix;
    private GeneralBuffer uProjectionMatrix;


    public ForwardRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene) {
        this.swapChain = swapChain;
        this.pipelineCache = pipelineCache;
        this.device = swapChain.getDevice();

        this.scene = scene;

        int numImages = swapChain.getImageViews().length;
        this.createDepthImages();
        this.renderPass = new SwapChainRenderPass(swapChain, this.depthAttachments[0].getImage().getFormat());
        this.createFrameBuffers();

        List<BinaryShaderFile> shaders = List.of(
                new BinaryShaderFile(ShaderType.VERTEX, "shaders/vertex"),
                new BinaryShaderFile(ShaderType.FRAGMENT, "shaders/fragment")
        );

        if (EngineProperties.SHOULD_RECOMPILE_SHADERS) {
            long recompiles = shaders.stream().filter(ShaderCompiler::compileShaderIfChanged).count();
            Logger.info("Recompiled {} shader(s).", recompiles);
        }

        this.shaderProgram = new ShaderProgram(this.device, shaders);

        DescriptorSetLayout[] descriptorSetLayouts = this.createDescriptorSets();

        Logger.debug("Building Pipelines");
        Pipeline.Builder builder = Pipeline.builder(VertexFormats.POSITION_UV.getDefinition())
                .setDescriptorLayouts(descriptorSetLayouts)
                .setPushConstantLayout(  // @see ForwardRenderer#applyPushConstants(...) for how this is filled.
                        //DataTypes.MAT4X4F, // proj
                        DataTypes.MAT4X4F // transform
                );
        this.pipeline = builder.build(pipelineCache, this.renderPass.getHandle(), this.shaderProgram, 1);
        this.wireframePipeline = builder.setUsingWireframe(true)
                                        .build(pipelineCache, this.renderPass.getHandle(), this.shaderProgram, 1);

        builder.cleanup();
        Logger.debug("Built pipelines.");

        this.commandBuffers = new CommandBuffer[numImages];
        this.fences = new Fence[numImages];
        for (int i = 0; i < numImages; i++) {
            this.commandBuffers[i] = new CommandBuffer(commandPool, true, false);
            this.fences[i] = new Fence(device, true);
        }

        // Initialize uniforms.
        DataTypes.MAT4X4F.copyToBuffer(this.uProjectionMatrix, this.scene.getProjection().asMatrix());
    }

    private void createDepthImages() {
        int numImages = this.swapChain.getImageViews().length;
        VkExtent2D swapChainExtent = this.swapChain.getSwapChainExtent();

        this.depthAttachments = new Attachment[numImages];
        for (int i = 0; i < numImages; i++) {
            this.depthAttachments[i] = new Attachment(this.device, swapChainExtent.width(), swapChainExtent.height(),
                    VK11.VK_FORMAT_D32_SFLOAT, VK11.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
        }
    }

    private void createFrameBuffers() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = this.swapChain.getSwapChainExtent();
            ImageView[] imageViews = this.swapChain.getImageViews();
            int numImages = imageViews.length;

            LongBuffer pAttachments = stack.mallocLong(2);
            this.frameBuffers = new FrameBuffer[numImages];

            for (int i = 0; i < numImages; i++) {
                ImageView depthView = this.depthAttachments[i].getImageView();
                pAttachments.put(0, imageViews[i].getHandle());
                pAttachments.put(1, depthView.getHandle());

                this.frameBuffers[i] = new FrameBuffer(this.device, swapChainExtent.width(), swapChainExtent.height(),
                                        pAttachments, this.renderPass.getHandle());
            }
        }
    }

    private DescriptorSetLayout[] createDescriptorSets() {
        Logger.info("Building Descriptor Sets");
        this.uniformDescriptorSetLayout = new UniformDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_VERTEX_BIT);
        this.samplerDescriptorSetLayout = new SamplerDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_FRAGMENT_BIT).setCount(EngineProperties.MAX_TEXTURES);

        DescriptorSetLayout[] layout = new DescriptorSetLayout[] {
                this.uniformDescriptorSetLayout,
                this.samplerDescriptorSetLayout
        };
        this.descriptorPool = new DescriptorPool(this.device, layout);

        // Uniform - constant.
        this.dProjectionMatrix = UniformDescriptorSet.create(this.descriptorPool, this.uniformDescriptorSetLayout, DataTypes.MAT4X4F, 0);
        this.uProjectionMatrix = this.dProjectionMatrix.getBuffer();

        // Sampler
        this.samplerDescriptors = new HashMap<>();
        this.uSampler = new TextureSampler(this.device, 1, true);

        return layout;
    }

    private void applyPushConstants(VkCommandBuffer cmd, long currentPipelineLayout, ByteBuffer buf, Matrix4f transform) {
        //proj.get(buf);
        transform.get(buf);
        VK11.vkCmdPushConstants(cmd, currentPipelineLayout, VK11.VK_SHADER_STAGE_VERTEX_BIT, 0, buf);
    }

    private void updateTextureDescriptorSet(Texture texture) {
        String textureFileName = texture.getResourceName();
        SamplerDescriptorSet samplerDescriptorSet = this.samplerDescriptors.get(textureFileName);

        if (samplerDescriptorSet == null) {
            if(this.samplerDescriptors.size() >= EngineProperties.MAX_TEXTURES)
                throw new EngineLimitExceededException("MAX_TEXTURES", EngineProperties.MAX_TEXTURES, this.samplerDescriptors.size()+1);

            samplerDescriptorSet = new SamplerDescriptorSet(descriptorPool, this.samplerDescriptorSetLayout, 0, texture, this.uSampler);
            this.samplerDescriptors.put(textureFileName, samplerDescriptorSet);
        }
    }

    @Override
    public void waitTillFree() {
        int idx = this.swapChain.getCurrentFrame();
        Fence currentFence = this.fences[idx];
        currentFence.fenceWait();
    }

    @Override
    public void recordDraw(Renderer renderer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = this.swapChain.getSwapChainExtent();
            int width = swapChainExtent.width();
            int height = swapChainExtent.height();
            int idx = this.swapChain.getCurrentFrame();

            //Fence fence = this.fences[idx];
            CommandBuffer commandBuffer = this.commandBuffers[idx];
            FrameBuffer frameBuffer = this.frameBuffers[idx];

            //fence.fenceWait();
            //fence.reset();
            commandBuffer.reset();

            VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
            Color c = EngineProperties.CLEAR_COLOUR;
            float[] components = new float[4];
            c.getComponents(components);
            clearValues.apply(0, v -> v.color()
                    .float32(0, components[0])
                    .float32(1, components[1])
                    .float32(2, components[2])
                    .float32(3, components[3])
            );
            clearValues.apply(1, v -> v.depthStencil().depth(1.0f));

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(this.renderPass.getHandle())
                    .pClearValues(clearValues)
                    .renderArea(a -> a.extent().set(width, height))
                    .framebuffer(frameBuffer.getHandle());

            commandBuffer.beginRecording();
            VkCommandBuffer cmd = commandBuffer.asVk();
            VK11.vkCmdBeginRenderPass(cmd, renderPassBeginInfo, VK11.VK_SUBPASS_CONTENTS_INLINE); // ----

            Pipeline selectedPipeline = renderer.useWireframe ? this.wireframePipeline : this.pipeline;
            VK11.vkCmdBindPipeline(cmd, VK11.VK_PIPELINE_BIND_POINT_GRAPHICS, selectedPipeline.getHandle());

            // Setup view
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                    .x(0)
                    .y(height)
                    .height(-height)         // flip viewport - opengl's coordinate system is nicer.
                    .width(width)
                    .minDepth(0.0f)
                    .maxDepth(1.0f);
            VK11.vkCmdSetViewport(cmd, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                    .extent(it -> it
                            .width(width)
                            .height(height))
                    .offset(it -> it
                            .x(0)
                            .y(0));
            VK11.vkCmdSetScissor(cmd, 0, scissor);

            // Render Models
            LongBuffer offsets = stack.mallocLong(1);
            offsets.put(0, 0L);
            LongBuffer vertexBufferHandle = stack.mallocLong(1);

            ByteBuffer pushConstants = stack.malloc(this.pipeline.getPushConstantSize());

            // Push Constants & Rendering!
            boolean perVertexConstants = selectedPipeline.isUsingPerVertexPushConstants();
            //if(!perVertexConstants)
            //    this.applyPushConstants(cmd, pushConstants);
            // todo: ^ figure how to make this easier to adapt.

            Projection proj = this.scene.getProjection();
            LongBuffer descriptorSets = stack.mallocLong(2);
            descriptorSets.put(0, this.dProjectionMatrix.getHandle()); // put proj matrix.

            for (String modelId: this.scene.getUsedModelIds()) {
                BufferedModel model = renderer.getModelManager().getModel(modelId);
                List<StaticModelEntity> entities = this.scene.getEntitiesWithModel(modelId);

                for(BundledMaterial material: model.getMaterials()) {
                    if (material.meshes().isEmpty()) continue;

                    SamplerDescriptorSet samplerDescriptorSet = this.samplerDescriptors.get(material.texture().getResourceName());
                    descriptorSets.put(1, samplerDescriptorSet.getHandle());

                    for(BufferedMesh mesh: material.meshes()) {
                        vertexBufferHandle.put(0, mesh.vertices().getHandle());
                        VK11.vkCmdBindVertexBuffers(cmd, 0, vertexBufferHandle, offsets);
                        VK11.vkCmdBindIndexBuffer(cmd, mesh.indices().getHandle(), 0, VK11.VK_INDEX_TYPE_UINT32);

                        for(RenderedEntity entity: entities) {
                            VK11.vkCmdBindDescriptorSets(cmd, VK11.VK_PIPELINE_BIND_POINT_GRAPHICS, selectedPipeline.getPipelineLayoutHandle(), 0, descriptorSets, null);

                            if (perVertexConstants)
                                this.applyPushConstants(cmd, selectedPipeline.getPipelineLayoutHandle(), pushConstants, entity.getTransform());

                            VK11.vkCmdDrawIndexed(cmd, mesh.numIndices(), 1, 0, 0, 0);
                        }
                    }
                }
            }

            VK11.vkCmdEndRenderPass(cmd); // ------------------------------------------------------
            commandBuffer.endRecording();
        }
    }

    @Override
    public void submit(CommandQueue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int idx = this.swapChain.getCurrentFrame();

            CommandBuffer commandBuffer = this.commandBuffers[idx];
            Fence currentFence = this.fences[idx];
            currentFence.reset();

            SyncSemaphores syncSemaphores = this.swapChain.getSyncSemaphores()[idx];

            queue.submit(stack.pointers(commandBuffer.asVk()),
                         stack.longs(syncSemaphores.imgAcquisitionSemaphore().getHandle()),
                         stack.ints(VK11.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                         stack.longs(syncSemaphores.renderCompleteSemaphore().getHandle()),
                         currentFence);
        }
    }

    @Override
    public void processModelBatch(List<BufferedModel> models) {
        this.device.waitIdle();

        for (BufferedModel vulkanModel : models) {
            for (BundledMaterial vulkanMaterial : vulkanModel.getMaterials()) {
                if (vulkanMaterial.meshes().isEmpty())
                    continue;

                this.updateTextureDescriptorSet(vulkanMaterial.texture());
            }
        }
    }

    @Override
    public void onResize(SwapChain newSwapChain) {
        DataTypes.MAT4X4F.copyToBuffer(this.uProjectionMatrix, this.scene.getProjection().asMatrix());

        this.swapChain = newSwapChain;
        Arrays.asList(this.frameBuffers).forEach(FrameBuffer::cleanup);
        Arrays.asList(this.depthAttachments).forEach(Attachment::cleanup);
        this.createDepthImages();
        this.createFrameBuffers();
    }

    @Override
    public void cleanup() {
        this.uProjectionMatrix.cleanup();
        this.uSampler.cleanup();
        this.descriptorPool.cleanup(); // descriptor sets cleaned up here.

        this.pipeline.cleanup();
        this.wireframePipeline.cleanup();

        this.shaderProgram.cleanup();

        Arrays.asList(this.frameBuffers).forEach(FrameBuffer::cleanup);
        Arrays.asList(this.depthAttachments).forEach(Attachment::cleanup);
        this.renderPass.cleanup();

        Arrays.asList(this.commandBuffers).forEach(CommandBuffer::cleanup);
        Arrays.asList(this.fences).forEach(Fence::cleanup);
    }

}
