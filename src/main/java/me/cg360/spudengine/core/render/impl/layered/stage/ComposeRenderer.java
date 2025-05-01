package me.cg360.spudengine.core.render.impl.layered.stage;

import me.cg360.spudengine.core.render.image.SwapChain;
import me.cg360.spudengine.core.render.impl.AbstractRenderer;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.world.Scene;

public abstract class ComposeRenderer extends AbstractRenderer {

    public ComposeRenderer(SwapChain swapChain, Scene scene, SubRenderProcess[] subRenderProcesses) {
        super(swapChain, scene, subRenderProcesses);
    }

}
