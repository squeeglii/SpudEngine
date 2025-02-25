package me.cg360.spudengine.core.render.impl;

import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.command.CommandQueue;
import me.cg360.spudengine.core.render.geometry.model.BufferedModel;
import me.cg360.spudengine.core.render.image.SwapChain;

import java.util.List;

public interface RenderProcess {

    void recordDraw(Renderer renderer);
    void submit(CommandQueue queue);

    void processModelBatch(List<BufferedModel> models);

    void waitTillFree(); // should this be generalized?

    void onResize(SwapChain newSwapChain);

    void cleanup();
}
