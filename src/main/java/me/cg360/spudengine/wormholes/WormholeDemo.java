package me.cg360.spudengine.wormholes;

import me.cg360.spudengine.core.GameInstance;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.wormholes.logic.AssetInitialisationStage;
import me.cg360.spudengine.wormholes.logic.PortalTracker;

public class WormholeDemo extends GameInstance {

    private static WormholeDemo instance = null;

    private PortalTracker portalTracker;

    public WormholeDemo(SpudEngine engineInstance) {
        super("Wormhole Demo", engineInstance);

        if(instance != null)
            throw new IllegalStateException("Wormhole Game Instance already running");

        this.addSubListener(new AssetInitialisationStage(engineInstance));
        this.portalTracker = this.addSubListener(new PortalTracker(this, engineInstance));
    }

    @Override
    protected void onInit(Window window, Scene scene, Renderer renderer) {
        instance = this;
    }

    @Override
    protected void onLogicTick(Window window, Scene scene, long delta) {

    }

    @Override
    protected void onInputTick(Window window, Scene scene, long delta) {

    }

    public PortalTracker getPortalTracker() {
        return this.portalTracker;
    }

    public static WormholeDemo get() {
        return instance;
    }
}
