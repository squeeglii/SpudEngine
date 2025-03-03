package me.cg360.spudengine.wormholes;

import me.cg360.spudengine.core.GameComponent;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.component.FlyCameraController;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.core.world.entity.impl.EnvironmentGeometry;
import me.cg360.spudengine.wormholes.logic.AssetInitialisationStage;
import me.cg360.spudengine.wormholes.logic.PortalTracker;

public class WormholeDemo extends GameComponent {

    private static WormholeDemo instance = null;

    private PortalTracker portalTracker;

    private EnvironmentGeometry levelGeometry;

    public WormholeDemo(SpudEngine engineInstance) {
        super("Wormhole Demo", engineInstance);

        if(instance != null)
            throw new IllegalStateException("Wormhole Game Instance already running");

        this.addSubListener(new AssetInitialisationStage(this, engineInstance));
        this.addSubListener(new FlyCameraController(this, engineInstance));
        this.portalTracker = this.addSubListener(new PortalTracker(this, engineInstance));
    }

    @Override
    protected void onInit(Window window, Scene scene, Renderer renderer) {
        instance = this;

        this.levelGeometry = new EnvironmentGeometry("env/chamber01");
        this.scene().addEntity(this.levelGeometry);
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
