package me.cg360.spudengine.core.render.impl.layered;

import me.cg360.spudengine.core.exception.UnimplementedException;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.impl.RenderProcess;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.world.Scene;

import java.util.List;

/** Render each layer to  */
public class LayeredRenderer extends RenderProcess {

    public LayeredRenderer(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene, SubRenderProcess[] subRenderProcesses) {
        super(subRenderProcesses);
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

    }

    @Override
    public void processOverlays(CommandPool uploadPool, CommandQueue queue, List<Texture> overlayTextures) {

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
