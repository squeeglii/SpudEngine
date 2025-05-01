package me.cg360.spudengine.core;

import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.render.impl.SubRenderProcess;
import me.cg360.spudengine.core.world.Scene;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class GameComponent {

    private final String instanceNickname;
    private final SpudEngine engineInstance;

    private final List<GameComponent> subListeners;
    private final List<SubRenderProcess> renderProcesses;

    public GameComponent(String nickname, SpudEngine engineInstance) {
        this.instanceNickname = nickname;
        this.engineInstance = engineInstance;

        this.subListeners = new LinkedList<>();
        this.renderProcesses = new LinkedList<>();
    }

    /** Registers and returns the provided sub-listener. */
    public <T extends GameComponent> T addSubListener(T listener) {
        if(listener == null)
            throw new NullPointerException("Sub-Listener cannot be null");

        this.subListeners.add(listener);
        this.renderProcesses.addAll(listener.getRendererAddons());
        return listener;
    }

    public <T extends SubRenderProcess> T addRenderProcess(T subRenderProcess) {
        if(subRenderProcess == null)
            throw new NullPointerException("SubRenderProcess cannot be null");

        this.renderProcesses.add(subRenderProcess);
        return subRenderProcess;
    }

    protected final void passPreInit(EngineSetupContext engineSetupContext) {
        for(GameComponent subListener : this.subListeners)
            subListener.passPreInit(engineSetupContext);

        this.onPreInit(engineSetupContext);
    }

    protected final void passInit(Window window, Scene scene, RenderSystem renderSystem) {
        for(GameComponent subListener : this.subListeners)
            subListener.passInit(window, scene, renderSystem);

        this.onInit(window, scene, renderSystem);
    }

    protected final void passLogicTick(Window window, Scene scene, long delta) {
        for(GameComponent subListener : this.subListeners)
            subListener.passLogicTick(window, scene, delta);

        this.onLogicTick(window, scene, delta);
    }

    protected final void passInputTick(Window window, Scene scene, long delta) {
        for(GameComponent subListener : this.subListeners)
            subListener.passInputTick(window, scene, delta);

        this.onInputTick(window, scene, delta);
    }

    protected final void passInputEvent(Window window, int key, int action, int modifiers) {
        for(GameComponent subListener : this.subListeners)
            subListener.passInputEvent(window, key, action, modifiers);

        this.onInputEvent(window, key, action, modifiers);
    }


    protected void onPreInit(EngineSetupContext engineSetupContext) { }

    protected void onInit(Window window, Scene scene, RenderSystem renderSystem) { }

    protected void onLogicTick(Window window, Scene scene, long delta) { }

    protected void onInputTick(Window window, Scene scene, long delta) { }

    protected void onInputEvent(Window window, int key, int action, int modifiers) { }


    @Override
    public String toString() {
        return "Game Instance ['%s']".formatted(this.instanceNickname);
    }

    // shortcuts
    protected final Scene scene() {
        return this.getEngine().getScene();
    }

    protected final RenderSystem renderer() {
        return this.getEngine().getRenderer();
    }

    protected final Window window() {
        return this.getEngine().getWindow();
    }

    public final String getInstanceNickname() {
        return this.instanceNickname;
    }

    public final SpudEngine getEngine() {
        return this.engineInstance;
    }

    public List<SubRenderProcess> getRendererAddons() {
        return Collections.unmodifiableList(this.renderProcesses);
    }

    public static String sub(GameComponent parent, String name) {
        return "%s.%s".formatted(parent.getInstanceNickname(), name);
    }
}
