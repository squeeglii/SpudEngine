package me.cg360.spudengine.core.component;

import me.cg360.spudengine.core.GameComponent;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.input.MouseInput;
import me.cg360.spudengine.core.render.RenderSystem;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.core.world.Scene;
import org.lwjgl.glfw.GLFW;

public class AverageFramerateMonitor extends GameComponent {

    public AverageFramerateMonitor(GameComponent parent, SpudEngine engineInstance) {
        super(GameComponent.sub(parent, "cursor_capture"), engineInstance);
    }

}
