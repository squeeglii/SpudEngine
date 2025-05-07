package me.cg360.spudengine.core;

import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.world.Scene;
import org.tinylog.Logger;

import java.util.LinkedList;
import java.util.Queue;

public class SpudEngine {

    private static SpudEngine engineInstance;

    private final Window window;
    private final Scene scene;
    private RenderSystem renderSystem;

    private GameComponent gameInstance;

    private final Queue<Long> frameDeltas;
    private long lastFrameCaptureInstant = 0;
    private long ticksAlive = 0;

    private boolean isRunning = false;

    public SpudEngine() {
        this.window = new Window(EngineProperties.WINDOW_TITLE, this::inputEvent);
        this.scene = new Scene(this.window);

        this.renderSystem = null;
        this.gameInstance = null;

        if(EngineProperties.ENABLE_INTERNAL_FPS_MONITOR) {
            this.frameDeltas = new LinkedList<>();
            this.lastFrameCaptureInstant = 0;

            if(EngineProperties.FRAME_CAPTURE_INTERVAL_MS < 0)
                throw new IllegalStateException("Negative frame capture interval! This is bad for the maths!");
        }

        this.preInit(this.window, this.scene);
    }

    // Engine Controls:
    public void start() {
        this.gameInstance = EngineProperties.GAME.create(this);

        EngineSetupContext engineSetup = new EngineSetupContext(this.window, this.scene);
        this.gameInstance.passPreInit(engineSetup);

        this.renderSystem = new RenderSystem(
                this.window, this.scene,
                engineSetup.getRenderProcessInitialiser(),
                this.gameInstance.getRendererAddons().toArray(SubRenderProcess[]::new)
        );

        this.init(this.window, this.scene, this.renderSystem);

        this.isRunning = true;
        this.ticksAlive = 0;
        this.enterMainLoop();
    }

    public void stop() {
        Logger.info("Stop triggered...");
        this.isRunning = false;
    }

    public SpudEngine setAsInstance() {
        engineInstance = this;
        return this;
    }

    private void enterMainLoop() {
        long prevInstant = System.currentTimeMillis();
        float updateTargetPeriod = 1000.0f / EngineProperties.UPDATES_PER_SECOND;
        double updateDelta = 0.0f;
        long updateInstant = prevInstant;

        while (this.isRunning && !this.window.shouldClose()) {
            this.window.pollEvents();
            long now = System.currentTimeMillis();

            // debugging! :)
            if(EngineProperties.ENABLE_INTERNAL_FPS_MONITOR) {
                this.frameDeltas.add(now - prevInstant);
                if (this.lastFrameCaptureInstant + EngineProperties.FRAME_CAPTURE_INTERVAL_MS < now) {
                    this.evaluateFramerate();
                    this.frameDeltas.clear();

                    this.lastFrameCaptureInstant = now;
                }
            }

            updateDelta += (now - prevInstant) / updateTargetPeriod;

            this.inputTick(this.window, this.scene, now - prevInstant);

            if(updateDelta >= 1) {
                long delta = now - updateInstant;
                this.logicTick(this.window, this.scene, delta);
                updateInstant = now;
                updateDelta--;
            }

            this.renderSystem.render(this.window, this.scene, (float) (updateDelta + this.ticksAlive));
            prevInstant = now;
        }

        // Wrap up fully.
        this.isRunning = false;

        Logger.info("Cleaning up program.");
        this.cleanup();
    }

    // Core Logic:
    private void preInit(Window window, Scene scene) {
        Logger.info("Initializing engine... (pre-init)");

        Logger.info("Completed engine initialisation (pre-init)");
    }

    private void init(Window window, Scene scene, RenderSystem renderSystem) {
        Logger.info("Initialising logic... (init)");

        renderSystem.getModelManager().createMissingModel();

        this.gameInstance.passInit(window, scene, renderSystem);

        Logger.info("Completed logic initialisation (init)");
    }

    private void logicTick(Window window, Scene scene, long delta) {
        this.gameInstance.passLogicTick(window, scene, delta);
        this.ticksAlive++;
    }

    private void inputTick(Window window, Scene scene, long delta) {
        // Game Logic Tick
        this.gameInstance.passInputTick(window, scene, delta);

        // Entity Tick
        this.scene.passInputTick(window, scene, delta);
    }

    private void inputEvent(long windowHandle, int key, int scanCode, int action, int modifiers) {
        this.gameInstance.passInputEvent(window, key, action, modifiers);
    }

    private void evaluateFramerate() {
        if(!EngineProperties.ENABLE_INTERNAL_FPS_MONITOR)
            throw new IllegalStateException("Tried to evaluate framerate while internal fps monitoring was disabled");

        long total = 0;
        long longestDelta = -1;
        long shortestDelta = Long.MAX_VALUE;

        for(long millis : this.frameDeltas) {
            total += millis;
            if(millis > longestDelta) longestDelta = millis;
            if(millis < shortestDelta) shortestDelta = millis;
        }

        double avgFrametimeMillis = (double) total / this.frameDeltas.size();
        double avgFrametimeSeconds = (double) total / (1000 * this.frameDeltas.size());

        double framerate = 1 / avgFrametimeSeconds;

        Logger.info("= Framerate Report ({0.00} seconds): \n| FPS: {0.0} \n| Frame Times: {0.000}ms avg, {}ms min, {}ms max",
                EngineProperties.FRAME_CAPTURE_INTERVAL_MS / 1000.0d,
                framerate,
                avgFrametimeMillis, shortestDelta, longestDelta);
    }


    private void cleanup() {
        // Cleanup logic
        this.renderSystem.cleanup();
        this.window.cleanup();
    }

    // Engine Components:


    public RenderSystem getRenderer() {
        return this.renderSystem;
    }

    public Window getWindow() {
        return this.window;
    }

    public Scene getScene() {
        return this.scene;
    }

    public GameComponent getGameInstance() {
        return this.gameInstance;
    }

    // Engine State:
    public boolean isRunning() {
        return this.isRunning;
    }

    public long getTicksAlive() {
        return this.ticksAlive;
    }

    public static SpudEngine getMainInstance() {
        return engineInstance;
    }
}
