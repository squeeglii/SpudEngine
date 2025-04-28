package me.cg360.spudengine.wormholes;

import me.cg360.spudengine.core.render.impl.PicassoRenderer;
import me.cg360.spudengine.core.render.impl.RenderProcessInitialiser;
import me.cg360.spudengine.core.render.impl.forward.ForwardRenderer;
import me.cg360.spudengine.core.render.impl.naiveforward.NaiveForwardRenderer;

public enum SelectedRenderer {

    NAIVE_FORWARD(NaiveForwardRenderer::new),
    MULTI_PASS_FORWARD(ForwardRenderer::new),
    LAYERED_COMPOSE(PicassoRenderer::new);

    private final RenderProcessInitialiser initialiser;

    SelectedRenderer(RenderProcessInitialiser initialiser) {
        this.initialiser = initialiser;
    }

    public RenderProcessInitialiser initialiser() {
        return this.initialiser;
    }

}
