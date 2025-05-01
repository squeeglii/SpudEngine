package me.cg360.spudengine.core.render.impl.layered;

import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.impl.RenderProcess;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.impl.layered.stage.ComposeRenderer;
import me.cg360.spudengine.core.render.impl.layered.stage.LayerRenderer;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.world.Scene;
import org.tinylog.Logger;

import java.util.List;

public class LayeredRenderer extends RenderProcess {

    private final LayerRenderer layerStage;     // smart renderer, assume everything wants to access this. (i.e, context, assets)
    private final ComposeRenderer composeStage; // dumb renderer, only use for anything final-output related.

    public LayeredRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene, int layerCount, SubRenderProcess[] subRenderProcesses) {
        super(new SubRenderProcess[0]);

        Logger.debug("Creating Sub-Renderer 'Layer Renderer':");
        this.layerStage = new LayerRenderer(swapChain, commandPool, pipelineCache, scene, layerCount, subRenderProcesses);

        Logger.debug("Creating Sub-Renderer 'Compose Renderer':");
        this.composeStage = new ComposeRenderer(swapChain, commandPool, pipelineCache, this.layerStage.getRenderTargetAttachments(), new SubRenderProcess[0]);
    }

    @Override
    protected void draw(RenderSystem renderSystem) {
        throw new IllegalStateException("LayeredRenderer should not be directly called with draw. Use recordAndSubmit");
    }

    @Override
    protected void submit(CommandQueue queue) {
        throw new IllegalStateException("LayeredRenderer should not be directly called with submit. Use recordAndSubmit");
    }

    @Override
    public void recordAndSubmit(RenderSystem renderSystem, CommandQueue commandQueue) {
        this.layerStage.recordAndSubmit(renderSystem, commandQueue);
        this.composeStage.recordAndSubmit(renderSystem, commandQueue);
    }

    @Override
    public void processModelBatch(List<BufferedModel> models) {
        this.layerStage.processModelBatch(models);
    }

    @Override
    public void processOverlays(CommandPool uploadPool, CommandQueue queue, List<Texture> overlayTextures) {
        this.layerStage.processOverlays(uploadPool, queue, overlayTextures);
    }

    @Override
    public void waitTillFree() {
        this.layerStage.waitTillFree();
    }

    @Override
    public void onResize(SwapChain newSwapChain) {
        this.layerStage.onResize(newSwapChain);
        this.composeStage.onResize(newSwapChain, this.layerStage.getRenderTargetAttachments());
    }

    @Override
    public void cleanup() {
        this.layerStage.cleanup();
        this.composeStage.cleanup();
    }

    @Override
    public Attachment getDepthAttachment(int index) {
        return this.composeStage.getDepthAttachment(index);
    }

    @Override
    public int getDepthFormat() {
        return this.composeStage.getDepthFormat();
    }

    @Override
    public RenderContext getCurrentContext() {
        return this.layerStage.getCurrentContext();
    }
}
