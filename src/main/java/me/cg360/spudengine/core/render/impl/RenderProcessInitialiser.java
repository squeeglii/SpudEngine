package me.cg360.spudengine.core.render.impl;

import me.cg360.spudengine.core.render.command.CommandPool;
import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.pipeline.PipelineCache;
import me.cg360.spudengine.core.world.Scene;

@FunctionalInterface
public interface RenderProcessInitialiser {

    RenderProcess create(SwapChain swapChain, CommandPool commandPool, PipelineCache pipelineCache, Scene scene, SubRenderProcess[] subRenderProcesses);

}
