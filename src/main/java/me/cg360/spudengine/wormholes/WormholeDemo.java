package me.cg360.spudengine.wormholes;

import me.cg360.spudengine.core.EngineSetupContext;
import me.cg360.spudengine.core.GameComponent;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.component.CursorCapture;
import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.render.impl.RenderProcessInitialiser;
import me.cg360.spudengine.core.util.Bounds2D;
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
import org.tinylog.Logger;

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
    protected void onPreInit(EngineSetupContext engineSetupContext) {
        instance = this;

        engineSetupContext.overrideRenderProcess(GameProperties.RENDER_PROCESS.initialiser());
    }

    @Override
    protected void onInit(Window window, Scene scene, Renderer renderer) {
        // remember, new world geometry goes in AssetInitialisationStage

        this.levelGeometry = new EnvironmentGeometry("env/chamber_01_texupdate");
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

        this.handlePortalDebugKeys(key, action);

        if(key == GLFW.GLFW_KEY_F2 && action == GLFW.GLFW_RELEASE) {
            Logger.info("Calculating bounds...");

            if(this.getPortalTracker().hasBluePortal()) {
                Bounds2D bounds = this.getPortalTracker().getBluePortal().getScreenBounds(this.scene());
                Logger.info("Blue Portal Bounds: {} \n| intersects: {}\n| contains: {}",
                        bounds,
                        Bounds2D.SCREEN_BOUNDS.intersects(bounds),
                        Bounds2D.SCREEN_BOUNDS.contains(bounds)
                );
            }

            if(this.getPortalTracker().hasOrangePortal()) {
                Bounds2D bounds = this.getPortalTracker().getOrangePortal().getScreenBounds(this.scene());
                Logger.info("Orange Portal Bounds: {} \n| intersects: {}\n| contains: {}",
                        bounds,
                        Bounds2D.SCREEN_BOUNDS.intersects(bounds),
                        Bounds2D.SCREEN_BOUNDS.contains(bounds)
                );
            }
        }
    }

    private void handlePortalDebugKeys(int key, int action) {
        if(key == GLFW.GLFW_KEY_0 && action == GLFW.GLFW_RELEASE) {

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

        if(key == GLFW.GLFW_KEY_1 && action == GLFW.GLFW_RELEASE) {
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

        if(key == GLFW.GLFW_KEY_2 && action == GLFW.GLFW_RELEASE) {
            if(this.getPortalTracker().hasOrangePortal()) {
                this.getPortalTracker().removeOrangePortal();

            } else {
                PortalEntity orangePortal = new PortalEntity(
                        PortalType.ORANGE,
                        new Vector3f(-6f, 0f, -2.99f),
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
