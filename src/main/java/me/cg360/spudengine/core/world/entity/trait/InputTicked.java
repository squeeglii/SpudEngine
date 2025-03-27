package me.cg360.spudengine.core.world.entity.trait;

import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.Scene;

public interface InputTicked {

    void consumeInputTick(Window window, Scene scene, long delta);

}
