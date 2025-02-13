package me.cg360.spudengine.core;

import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.Scene;

public abstract class GameHooks {

    private final String instanceNickname;

    public GameHooks(String nickname) {
        this.instanceNickname = nickname;
    }

    protected abstract void init(Window window, Scene scene, Renderer renderer);

    protected abstract void logicTick(Window window, Scene scene, long delta);

    protected abstract void input(Window window, Scene scene, long delta);

    @Override
    public String toString() {
        return "Game Instance ['%s']".formatted(this.instanceNickname);
    }

    public final String getInstanceNickname() {
        return this.instanceNickname;
    }
}
