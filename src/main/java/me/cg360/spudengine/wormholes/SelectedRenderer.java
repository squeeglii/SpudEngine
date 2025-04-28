package me.cg360.spudengine.wormholes;

import me.cg360.spudengine.core.render.impl.layered.LayeredRenderer;
import me.cg360.spudengine.core.render.impl.RenderProcessInitialiser;
import me.cg360.spudengine.core.render.impl.forward.multipass.MultiPassForwardRenderer;
import me.cg360.spudengine.core.render.impl.forward.naiveforward.NaiveForwardRenderer;

public enum SelectedRenderer {

    NAIVE_FORWARD(NaiveForwardRenderer::new),
    MULTI_PASS_FORWARD(MultiPassForwardRenderer::new),
    LAYERED_COMPOSE(LayeredRenderer::new);

    private final RenderProcessInitialiser initialiser;

    SelectedRenderer(RenderProcessInitialiser initialiser) {
        this.initialiser = initialiser;
    }

    public RenderProcessInitialiser initialiser() {
        return this.initialiser;
    }

}
