package me.cg360.spudengine.wormholes;

import me.cg360.spudengine.core.GameComponent;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.component.CursorCapture;
import me.cg360.spudengine.core.component.FlyCameraController;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.util.Vectors;
import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.core.world.entity.impl.EnvironmentGeometry;
import me.cg360.spudengine.core.world.entity.impl.LocalPlayerEntity;
import me.cg360.spudengine.wormholes.logic.AssetInitialisationStage;
import me.cg360.spudengine.wormholes.logic.PortalTracker;
import me.cg360.spudengine.wormholes.render.PortalSubRenderer;
import me.cg360.spudengine.wormholes.world.entity.PortalEntity;
import me.cg360.spudengine.wormholes.world.entity.PortalType;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class WormholeDemo extends GameComponent {

    private static WormholeDemo instance = null;

    private final PortalTracker portalTracker;

    private EnvironmentGeometry levelGeometry;
    private LocalPlayerEntity playerEntity;

    public WormholeDemo(SpudEngine engineInstance) {
        super("Wormhole Demo", engineInstance);

        if(instance != null)
            throw new IllegalStateException("Wormhole Game Instance already running");

        this.addSubListener(new AssetInitialisationStage(this, engineInstance));
        this.addSubListener(new CursorCapture(this, engineInstance));
        this.portalTracker = this.addSubListener(new PortalTracker(this, engineInstance));

        this.addRenderProcess(new PortalSubRenderer(this));
    }


    @Override
    protected void onInit(Window window, Scene scene, Renderer renderer) {
        instance = this;

        this.levelGeometry = new EnvironmentGeometry("env/chamber01");
        this.scene().addEntity(this.levelGeometry);

        this.playerEntity = new LocalPlayerEntity(GeneratedAssets.PLAYER_MODEL);
        this.scene().addEntity(this.playerEntity);

        //this.testTranslation();
    }

    @Override
    protected void onLogicTick(Window window, Scene scene, long delta) {

    }

    @Override
    protected void onInputTick(Window window, Scene scene, long delta) {

    }

    @Override
    protected void onInputEvent(Window window, int key, int action, int modifiers) {
        if(key == GLFW.GLFW_KEY_F1 && action == GLFW.GLFW_RELEASE)
            this.getEngine().getRenderer().useWireframe = !this.getEngine().getRenderer().useWireframe;

        if(key == GLFW.GLFW_KEY_9 && action == GLFW.GLFW_RELEASE) {

            if(this.getPortalTracker().hasBluePortal()) {
                this.getPortalTracker().removeBluePortal();
            } else {
                PortalEntity bluePortal = new PortalEntity(
                        PortalType.BLUE,
                        new Vector3f(-6f, 0f, -1.01f),
                        Vectors.toRadians(0, 0, 0)
                );

                this.scene().addEntity(bluePortal);
            }
        }

        if(key == GLFW.GLFW_KEY_0 && action == GLFW.GLFW_RELEASE) {
            if(this.getPortalTracker().hasOrangePortal()) {
                this.getPortalTracker().removeOrangePortal();

            } else {
                PortalEntity orangePortal = new PortalEntity(
                        PortalType.ORANGE,
                        new Vector3f(0, 2.0f, -3.99f),
                        Vectors.toRadians(0, 180, 0)
                );

                this.scene().addEntity(orangePortal);
            }
        }
    }

    public PortalTracker getPortalTracker() {
        return this.portalTracker;
    }

    public static WormholeDemo get() {
        return instance;
    }
}
