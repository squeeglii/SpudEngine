package me.cg360.spudengine.core;

import me.cg360.spudengine.core.render.Renderer;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.Scene;
import org.tinylog.Logger;

@FunctionalInterface
public interface Entrypoint {

    GameHooks create(SpudEngine engine);

}
