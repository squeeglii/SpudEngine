package me.cg360.spudengine.wormholes;

import me.cg360.spudengine.core.render.impl.layered.stage.LayerRenderer;
import me.cg360.spudengine.core.render.impl.RenderProcessInitialiser;
import me.cg360.spudengine.core.render.impl.forward.multipass.MultiPassForwardRenderer;
import me.cg360.spudengine.core.render.impl.forward.naiveforward.NaiveForwardRenderer;

public enum SelectedRenderer {

    NAIVE_FORWARD(NaiveForwardRenderer::new),
    MULTI_PASS_FORWARD(
            (swapchain, commandPool, pipelineCache, scene, subRenderers) ->
            new MultiPassForwardRenderer(swapchain, commandPool, pipelineCache, scene, GameProperties.MAX_PORTAL_DEPTH, subRenderers)),
    LAYERED_COMPOSE(
            (swapchain, commandPool, pipelineCache, scene, subRenderers) ->
            new LayerRenderer(swapchain, commandPool, pipelineCache, scene, GameProperties.MAX_PORTAL_DEPTH, subRenderers)),;

    // TODO: Replace Layer Renderer with LAYER*ED* renderer.

    private final RenderProcessInitialiser initialiser;

    SelectedRenderer(RenderProcessInitialiser initialiser) {
        this.initialiser = initialiser;
    }

    public RenderProcessInitialiser initialiser() {
        return this.initialiser;
    }

}
