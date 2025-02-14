package me.cg360.spudengine.core;

import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.Scene;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;

public abstract class GameHooks {

    private final String instanceNickname;
    private final SpudEngine engineInstance;

    public GameHooks(String nickname, SpudEngine engineInstance) {
        this.instanceNickname = nickname;
        this.engineInstance = engineInstance;
    }

    protected abstract void init(Window window, Scene scene, Renderer renderer);

    protected abstract void logicTick(Window window, Scene scene, long delta);

    protected abstract void input(Window window, Scene scene, long delta);

    protected void inputEvent(Window window, int key, int action, int modifiers) { }

    @Override
    public String toString() {
        return "Game Instance ['%s']".formatted(this.instanceNickname);
    }

    public final String getInstanceNickname() {
        return this.instanceNickname;
    }

    public final SpudEngine getEngine() {
        return this.engineInstance;
    }
}
