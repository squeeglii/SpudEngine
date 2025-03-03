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
import me.cg360.spudengine.core.render.pipeline.descriptor.active.DescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.SamplerDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.UniformDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.SamplerDescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.UniformDescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.pass.SwapChainRenderPass;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderProgram;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.render.sync.SyncSemaphores;
import me.cg360.spudengine.core.exception.EngineLimitExceededException;
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

    private static final int DEPTH_ATTACHMENT_FORMAT = VK11.VK_FORMAT_D32_SFLOAT;

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
    private DescriptorSetLayout modelTransformLayout;
    private DescriptorSetLayout viewMatixLayout;
    private DescriptorSetLayout samplerDescriptorSetLayout;

    private TextureSampler uSampler;
    private Map<String, SamplerDescriptorSet> samplerDescriptors;

    private UniformDescriptorSet dProjectionMatrix;
    private GeneralBuffer uProjectionMatrix;

    private UniformDescriptorSet[] dViewMatrix;
    private GeneralBuffer[] uViewMatrix;

    private final ShaderIO shaderIO; // #reset(...) whenever new draw call


    public ForwardRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene) {
        this.swapChain = swapChain;
        this.pipelineCache = pipelineCache;
        this.device = swapChain.getDevice();

        this.scene = scene;

        this.shaderIO = new ShaderIO();

        int numImages = swapChain.getImageViews().length;
        this.createDepthImages();
        this.renderPass = new SwapChainRenderPass(swapChain, this.depthAttachments[0].getImage().getFormat());
        this.createFrameBuffers();

        this.shaderProgram = ShaderProgram.attemptCompile(
                this.device,
                EngineProperties.shaders
        );

        DescriptorSetLayout[] descriptorSetLayouts = this.createDescriptorSets();

        Logger.debug("Building Pipelines");
        Pipeline.Builder builder = Pipeline.builder(VertexFormats.POSITION_UV.getDefinition())
                .setDescriptorLayouts(descriptorSetLayouts)
                .setPushConstantLayout(  // @see ForwardRenderer#applyPushConstants(...) for how this is filled.
                        //DataTypes.MAT4X4F, // proj
                        DataTypes.MAT4X4F // transform
                )
                .setCullMode(VK11.VK_CULL_MODE_FRONT_BIT); // uhhhh, right. Sure. This works but

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
                    DEPTH_ATTACHMENT_FORMAT, VK11.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
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
        this.modelTransformLayout = new UniformDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_VERTEX_BIT);
        this.viewMatixLayout = new UniformDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_VERTEX_BIT)
                .enablePerFrameWrites(this.swapChain);

        this.samplerDescriptorSetLayout = new SamplerDescriptorSetLayout(this.device, 0, VK11.VK_SHADER_STAGE_FRAGMENT_BIT)
                .setCount(EngineProperties.MAX_TEXTURES);

        DescriptorSetLayout[] layout = new DescriptorSetLayout[] {
                this.modelTransformLayout,
                this.viewMatixLayout,
                this.samplerDescriptorSetLayout
        };
        this.descriptorPool = new DescriptorPool(this.device, layout);

        // Uniform - constant.
        this.dProjectionMatrix = UniformDescriptorSet.create(this.descriptorPool, this.modelTransformLayout, DataTypes.MAT4X4F, 0)[0];
        this.uProjectionMatrix = this.dProjectionMatrix.getBuffer();

        this.dViewMatrix = UniformDescriptorSet.create(this.descriptorPool, this.viewMatixLayout, DataTypes.MAT4X4F, 0);
        this.uViewMatrix = this.collectUniformBuffers(this.dViewMatrix);

        // Sampler
        this.samplerDescriptors = new HashMap<>();
        this.uSampler = new TextureSampler(this.device, 1, true);

        return layout;
    }

    private GeneralBuffer[] collectUniformBuffers(UniformDescriptorSet[] set) {
        GeneralBuffer[] buffers = new GeneralBuffer[set.length];
        for (int i = 0; i < set.length; i++)
            buffers[i] = set[i].getBuffer();

        return buffers;
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
            this.shaderIO.reset(stack, selectedPipeline, this.descriptorPool);

            Matrix4f view = this.scene.getMainCamera().getViewMatrix();
            DataTypes.MAT4X4F.copyToBuffer(this.uViewMatrix[idx], view);

            this.shaderIO.setUniform(0, this.dProjectionMatrix);
            this.shaderIO.setUniform(1, this.dViewMatrix, idx);
            // [ If pushing pushConsants that don't change per vertex, do it here. ]

            // Push updated view every frame.


            this.drawMeshes(cmd, renderer, selectedPipeline);

            this.shaderIO.free(); // free buffers from stack.
            VK11.vkCmdEndRenderPass(cmd); // ------------------------------------------------------
            commandBuffer.endRecording();
        }
    }

    private void drawMeshes(VkCommandBuffer cmd, Renderer renderer, Pipeline selectedPipeline) {
        for (String modelId: this.scene.getUsedModelIds()) {
            BufferedModel model = renderer.getModelManager().getModel(modelId);
            List<StaticModelEntity> entities = this.scene.getEntitiesWithModel(modelId);

            for(BundledMaterial material: model.getMaterials()) {
                if (material.meshes().isEmpty()) continue;

                // Swap texture descriptor out when the material changes.
                SamplerDescriptorSet samplerDescriptorSet = this.samplerDescriptors.get(material.texture().getResourceName());
                this.shaderIO.setUniform(2, samplerDescriptorSet);

                for(BufferedMesh mesh: material.meshes()) {
                    this.shaderIO.bindMesh(cmd, mesh);

                    for(RenderedEntity entity: entities) {
                        long layoutHandle = selectedPipeline.getPipelineLayoutHandle();
                        this.shaderIO.bindDescriptorSets(cmd, layoutHandle);
                        this.shaderIO.applyVertexPushConstants(cmd, layoutHandle, entity.getTransform());

                        VK11.vkCmdDrawIndexed(cmd, mesh.numIndices(), 1, 0, 0, 0);
                    }
                }
            }
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
        Arrays.stream(this.uViewMatrix).forEach(GeneralBuffer::cleanup);
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

    @Override
    public Attachment getDepthAttachment(int index) {
        return this.depthAttachments[index];
    }

    @Override
    public int getDepthFormat() {
        return DEPTH_ATTACHMENT_FORMAT;
    }

    public static class ShaderIO {
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

        public void setUniform(int setNumber, DescriptorSet[] set, int swapImageIndex) {
            this.descriptorSets.put(setNumber, set[swapImageIndex].getHandle());
        }

        public void bindMesh(VkCommandBuffer cmd, BufferedMesh mesh) {
            this.vertexBuffer.put(0, mesh.vertices().getHandle());
            VK11.vkCmdBindVertexBuffers(cmd, 0, this.vertexBuffer, this.vertexBufferOffset);
            VK11.vkCmdBindIndexBuffer(cmd, mesh.indices().getHandle(), 0, VK11.VK_INDEX_TYPE_UINT32);
        }

        public void bindDescriptorSets(VkCommandBuffer cmd, long pipelineLayout) {
            VK11.vkCmdBindDescriptorSets(cmd, VK11.VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, this.descriptorSets, null);
        }

        private void applyVertexPushConstants(VkCommandBuffer cmd, long currentPipelineLayout, Matrix4f transform) {
            //proj.get(buf);
            transform.get(this.pushConstantsBuffer); // copy transform to pushConstantsBuffer
            VK11.vkCmdPushConstants(cmd, currentPipelineLayout, VK11.VK_SHADER_STAGE_VERTEX_BIT, 0, this.pushConstantsBuffer);
        }

        protected void reset(MemoryStack stack, Pipeline pipeline, DescriptorPool pool) {
            this.descriptorSets = stack.mallocLong(pool.getSetPositionCount());
            this.vertexBuffer = stack.mallocLong(1);
            this.vertexBufferOffset = stack.mallocLong(1);
            this.vertexBufferOffset.put(0, 0L);

            this.pushConstantsBuffer = stack.malloc(pipeline.getPushConstantSize());
        }

        protected void free() {
            this.descriptorSets = null;
            this.vertexBuffer = null;
            this.vertexBufferOffset = null;
            this.pushConstantsBuffer = null;
        }

    }

}
