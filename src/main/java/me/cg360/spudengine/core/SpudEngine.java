package me.cg360.spudengine.core;

import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.Scene;
import org.tinylog.Logger;

public class SpudEngine {

    private static SpudEngine engineInstance;

    private Window window;
    private Scene scene;
    private Renderer renderer;

    private GameHooks gameInstance;

    private long ticksAlive = 0;

    private boolean isRunning = false;

    public SpudEngine() {
        this.window = new Window(EngineProperties.WINDOW_TITLE);
        this.scene = new Scene(this.window);
        this.renderer = new Renderer(this.window, this.scene);
        this.gameInstance = null;

        this.preinit(this.window, this.scene, this.renderer);
    }

    // Engine Controls:
    public void start() {
        this.gameInstance = EngineProperties.GAME.create(this);
        this.init(this.window, this.scene, this.renderer);

        this.isRunning = true;
        this.enterMainLoop();
    }

    public void stop() {
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
            updateDelta += (now - prevInstant) / updateTargetPeriod;

            this.input(this.window, this.scene, now - prevInstant);

            if(updateDelta >= 1) {
                long delta = now - updateInstant;
                this.logicTick(this.window, this.scene, delta);
                updateInstant = now;
                updateDelta--;
            }

            this.renderer.render(this.window, this.scene);
            prevInstant = now;
        }

        Logger.info("Cleaning up program.");
        this.cleanup();
    }

    // Core Logic:
    private void preinit(Window window, Scene scene, Renderer renderer) {
        Logger.info("Initializing engine... (pre-init)");

        // logic!

        Logger.info("Completed engine initialisation (init)");
    }

    private void init(Window window, Scene scene, Renderer renderer) {
        Logger.info("Initialising logic... (init)");

        this.gameInstance.init(window, scene, renderer);

        Logger.info("Completed logic initialisation (init)");
    }

    private void logicTick(Window window, Scene scene, long delta) {
        this.gameInstance.logicTick(window, scene, delta);
        this.ticksAlive++;
    }

    private void input(Window window, Scene scene, long delta) {
        this.gameInstance.input(window, scene, delta);
    }


    private void cleanup() {
        // Cleanup logic

        this.renderer.cleanup();
        this.window.cleanup();
    }

    // Engine State:
    public boolean isRunning() {
        return this.isRunning;
    }

    public static SpudEngine getMainInstance() {
        return engineInstance;
    }
}
