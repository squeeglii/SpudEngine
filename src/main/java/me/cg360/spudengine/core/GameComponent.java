package me.cg360.spudengine.core;

import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.Scene;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class GameComponent {

    private final String instanceNickname;
    private final SpudEngine engineInstance;

    private final List<GameComponent> subListeners;

    public GameComponent(String nickname, SpudEngine engineInstance, GameComponent... subListeners) {
        this.instanceNickname = nickname;
        this.engineInstance = engineInstance;

        this.subListeners = new LinkedList<>();
        this.subListeners.addAll(Arrays.asList(subListeners));
    }

    /** Registers and returns the provided sub-listener. */
    public <T extends GameComponent> T addSubListener(T listener) {
        if(listener == null)
            throw new NullPointerException("Sub-Listener cannot be null");

        this.subListeners.add(listener);
        return listener;
    }

    protected final void passInit(Window window, Scene scene, Renderer renderer) {
        for(GameComponent subListener : this.subListeners)
            subListener.passInit(window, scene, renderer);

        this.onInit(window, scene, renderer);
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


    protected void onInit(Window window, Scene scene, Renderer renderer) { }

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

    protected final Renderer renderer() {
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

    public static String sub(GameComponent parent, String name) {
        return "%s.%s".formatted(parent.getInstanceNickname(), name);
    }
}
