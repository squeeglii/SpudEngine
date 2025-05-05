package me.cg360.spudengine.wormholes;

import me.cg360.spudengine.core.EngineSetupContext;
import me.cg360.spudengine.core.GameComponent;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.component.CursorCapture;
import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.util.Bounds3D;
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
    protected void onInit(Window window, Scene scene, RenderSystem renderSystem) {
        // remember, new world geometry goes in AssetInitialisationStage

        this.levelGeometry = new EnvironmentGeometry("env/chamber_01_texupdate");
        this.scene().addEntity(this.levelGeometry);

        this.playerEntity = new LocalPlayerEntity(GeneratedAssets.PLAYER_MODEL);
        this.playerEntity.setPosition(0, 2, 0);
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
        this.handleFunctionKeys(key, action);
        this.handlePortalDebugPlacementKeys(key, action);
        this.handleDebugViewKeys(key, action);
    }

    private void handleFunctionKeys(int key, int action) {
        if(key == GLFW.GLFW_KEY_F1 && action == GLFW.GLFW_RELEASE) {
            this.getEngine().getRenderer().useWireframe = !this.getEngine().getRenderer().useWireframe;
            Logger.info("Toggled wireframe mode.");
        }

        if(key == GLFW.GLFW_KEY_F2 && action == GLFW.GLFW_RELEASE) {
            Logger.info("Calculating bounds...");

            if(this.getPortalTracker().hasBluePortal()) {
                Bounds3D bounds = this.getPortalTracker().getBluePortal().getScreenBounds(this.scene());
                Logger.info("Blue Portal Bounds: {} \n| intersects: {}\n| contains: {}",
                        bounds,
                        Bounds3D.SCREEN_BOUNDS.intersects(bounds),
                        Bounds3D.SCREEN_BOUNDS.contains(bounds)
                );
            }

            if(this.getPortalTracker().hasOrangePortal()) {
                Bounds3D bounds = this.getPortalTracker().getOrangePortal().getScreenBounds(this.scene());
                Logger.info("Orange Portal Bounds: {} \n| intersects: {}\n| contains: {}",
                        bounds,
                        Bounds3D.SCREEN_BOUNDS.intersects(bounds),
                        Bounds3D.SCREEN_BOUNDS.contains(bounds)
                );
            }
        }

        if(key == GLFW.GLFW_KEY_F3 && action == GLFW.GLFW_RELEASE) {
            Logger.info("Camera State: \n| position: {}\n| rotation: {} \n| facing: {}",
                    this.scene().getMainCamera().getPosition(),
                    this.scene().getMainCamera().getRotation(),
                    this.scene().getMainCamera().getFacingDirection()
            );
        }

        if(key == GLFW.GLFW_KEY_F4 && action == GLFW.GLFW_RELEASE) {
            GameProperties.forceRenderSolidPortals = !GameProperties.forceRenderSolidPortals;
            Logger.info("Toggled forceRenderSolidPortals to {}", GameProperties.forceRenderSolidPortals);
        }

        if(key == GLFW.GLFW_KEY_F5 && action == GLFW.GLFW_RELEASE) {
            this.playerEntity.toggleRendering();
            Logger.info("Toggled player model rendering.");
        }
    }

    private void handleDebugViewKeys(int key, int action) {
        if(action != GLFW.GLFW_RELEASE) return;

        switch (key) {
            case GLFW.GLFW_KEY_I -> {
                this.scene().getMainCamera().setPosition(1000000, 0, 0);
                this.scene().getMainCamera().setRotation(0, 0);
            }

            case GLFW.GLFW_KEY_O -> {
                this.scene().getMainCamera().setPosition(0, 2, 0);
                this.scene().getMainCamera().setRotation(0, 0);
            }
        }
    }

    private void handlePortalDebugPlacementKeys(int key, int action) {
        if(action != GLFW.GLFW_RELEASE) return;

        switch (key) {
            case GLFW.GLFW_KEY_0 -> {
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

            case GLFW.GLFW_KEY_9 -> {
                if(this.getPortalTracker().hasBluePortal()) {
                    this.getPortalTracker().removeBluePortal();
                } else {
                    PortalEntity bluePortal = new PortalEntity(
                            PortalType.BLUE,
                            new Vector3f(-2f, 0f, 3.99f),
                            Vectors.toRadians(0, 0, 0)
                    );

                    this.scene().addEntity(bluePortal);
                }
            }

            case GLFW.GLFW_KEY_8 -> {
                if(this.getPortalTracker().hasBluePortal()) {
                    this.getPortalTracker().removeBluePortal();
                } else {
                    PortalEntity bluePortal = new PortalEntity(
                            PortalType.BLUE,
                            new Vector3f(-3.99f, 0f, 1),
                            Vectors.toRadians(0, -90, 0)
                    );

                    this.scene().addEntity(bluePortal);
                }
            }

            // Orange Portals --------------------------------------------------

            case GLFW.GLFW_KEY_1 -> {
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

            case GLFW.GLFW_KEY_2 -> {
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

            case GLFW.GLFW_KEY_3 -> {
                if(this.getPortalTracker().hasOrangePortal()) {
                    this.getPortalTracker().removeOrangePortal();

                } else {
                    PortalEntity orangePortal = new PortalEntity(
                            PortalType.ORANGE,
                            new Vector3f(-2f, 0f, -3.99f),
                            Vectors.toRadians(0, 180, 0)
                    );

                    this.scene().addEntity(orangePortal);
                }
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
