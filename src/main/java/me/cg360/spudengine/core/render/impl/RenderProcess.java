package me.cg360.spudengine.core.render.impl;

import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.image.texture.Texture;

import java.util.List;

public abstract class RenderProcess {

    protected final SubRenderProcess[] subRenderProcesses;

    public RenderProcess(SubRenderProcess[] subRenderProcesses) {
        this.subRenderProcesses = subRenderProcesses;
    }

    public abstract void recordDraw(Renderer renderer);
    public abstract void submit(CommandQueue queue);

    public abstract void processModelBatch(List<BufferedModel> models);
    public abstract void processOverlays(CommandPool uploadPool, CommandQueue queue, List<Texture> overlayTextures);

    public abstract void waitTillFree(); // should this be generalized?

    public abstract void onResize(SwapChain newSwapChain);

    public abstract void cleanup();

    public abstract Attachment getDepthAttachment(int index);
    public abstract int getDepthFormat();
}
