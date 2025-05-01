package me.cg360.spudengine.core.render.impl.layered;

import me.cg360.spudengine.core.exception.UnimplementedException;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.command.CommandBuffer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.geometry.model.BundledMaterial;
import me.cg360.spudengine.core.render.hardware.LogicalDevice;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.FrameBuffer;
import me.cg360.spudengine.core.render.image.RenderTargetAttachmentSet;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.impl.RenderProcess;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.render.pipeline.descriptor.DescriptorPool;
import me.cg360.spudengine.core.render.pipeline.descriptor.active.UniformDescriptorSet;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderIO;
import me.cg360.spudengine.core.render.pipeline.shader.ShaderProgram;
import me.cg360.spudengine.core.render.pipeline.shader.StandardSamplers;
import me.cg360.spudengine.core.render.sync.Fence;
import me.cg360.spudengine.core.world.Scene;
import org.lwjgl.vulkan.VK11;
import org.tinylog.Logger;

import java.util.List;

/** Render each layer to  */
public class LayeredRenderer extends RenderProcess {

    public static final int DEPTH_ATTACHMENT_FORMAT = VK11.VK_FORMAT_D32_SFLOAT_S8_UINT;

    protected final LogicalDevice device;

    protected CommandBuffer[] commandBuffers;
    protected Fence[] fences;

    protected RenderTargetAttachmentSet renderTargets;
    protected SwapChain swapChain;
    protected FrameBuffer[] frameBuffers;

    protected PipelineCache pipelineCache;

    protected ShaderProgram shaderProgram;

    protected final Scene scene;

    protected DescriptorPool descriptorPool;

    protected DescriptorSetLayout lProjectionMatrix;
    protected UniformDescriptorSet dProjectionMatrix;
    protected GeneralBuffer uProjectionMatrix;

    protected DescriptorSetLayout lViewMatrix;
    protected UniformDescriptorSet[] dViewMatrix;
    protected GeneralBuffer[] uViewMatrix;

    protected StandardSamplers standardSamplers;

    protected final ShaderIO shaderIO; // #reset(...) whenever new draw call

    public LayeredRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene, SubRenderProcess[] subRenderProcesses) {
        super(subRenderProcesses);

        this.device = swapChain.getDevice();
        this.scene = scene;
        this.pipelineCache = pipelineCache;

        // shaders, passes, and render targets.

        this.shaderIO = new ShaderIO();

        // build pipelines & command buffers
    }

    @Override
    public void recordDraw(Renderer renderer) {
        throw new UnimplementedException("Layered Renderer is not implemented yet.");
    }

    @Override
    public void submit(CommandQueue queue) {

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

    @Override
    public void waitTillFree() {

    }

    @Override
    public void onResize(SwapChain newSwapChain) {

    }

    @Override
    public void cleanup() {

    }

    @Override
    public Attachment getDepthAttachment(int index) {
        return null;
    }

    @Override
    public int getDepthFormat() {
        return 0;
    }

    @Override
    public RenderContext getCurrentContext() {
        return null;
    }
}
