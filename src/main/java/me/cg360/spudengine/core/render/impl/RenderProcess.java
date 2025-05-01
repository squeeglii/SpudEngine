package me.cg360.spudengine.core.render.impl;

import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.context.RenderContext;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.image.Attachment;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.image.texture.Texture;
import me.cg360.spudengine.core.render.pipeline.descriptor.layout.DescriptorSetLayout;

import java.util.List;

public abstract class RenderProcess {

    protected final SubRenderProcess[] subRenderProcesses;

    public RenderProcess(SubRenderProcess[] subRenderProcesses) {
        this.subRenderProcesses = subRenderProcesses;
    }

    protected abstract void draw(RenderSystem renderSystem);
    protected abstract void submit(CommandQueue queue);

    public void recordAndSubmit(RenderSystem renderSystem, CommandQueue commandQueue) {
        this.draw(renderSystem);
        this.submit(commandQueue);
    }

    public abstract void processModelBatch(List<BufferedModel> models);
    public abstract void processOverlays(CommandPool uploadPool, CommandQueue queue, List<Texture> overlayTextures);

    public abstract void waitTillFree(); // should this be generalized?

    public abstract void onResize(SwapChain newSwapChain);

    public abstract void cleanup();

    public abstract Attachment getDepthAttachment(int index);
    public abstract int getDepthFormat();

    public abstract RenderContext getCurrentContext();
}
