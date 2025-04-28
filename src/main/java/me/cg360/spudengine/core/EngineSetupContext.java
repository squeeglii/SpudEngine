package me.cg360.spudengine.core;

import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.render.impl.RenderProcessInitialiser;
import me.cg360.spudengine.core.render.impl.forward.multipass.MultiPassForwardRenderer;
import me.cg360.spudengine.core.world.Scene;

public class EngineSetupContext {

    private static final RenderProcessInitialiser DEFAULT_RENDERER = MultiPassForwardRenderer::new;

    private final Window window;
    private final Scene scene;

    private RenderProcessInitialiser renderProcessInitialiser;

    public EngineSetupContext(Window window, Scene scene) {
        this.window = window;
        this.scene = scene;

        this.renderProcessInitialiser = DEFAULT_RENDERER;
    }


    public EngineSetupContext overrideRenderProcess(RenderProcessInitialiser renderProcessInitialiser) {
        this.renderProcessInitialiser = renderProcessInitialiser != null
                ? renderProcessInitialiser
                : DEFAULT_RENDERER;
        return this;
    }

    public Window getWindow() {
        return this.window;
    }

    public Scene getScene() {
        return this.scene;
    }

    public RenderProcessInitialiser getRenderProcessInitialiser() {
        return this.renderProcessInitialiser;
    }
}
