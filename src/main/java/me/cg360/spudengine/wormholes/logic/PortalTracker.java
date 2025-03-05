package me.cg360.spudengine.wormholes.logic;

import me.cg360.spudengine.core.GameComponent;
import me.cg360.spudengine.core.SpudEngine;
import me.cg360.spudengine.core.render.Window;
import me.cg360.spudengine.wormholes.world.entity.PortalEntity;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.tinylog.Logger;

public class PortalTracker extends GameComponent {

    private PortalEntity bluePortal = null;
    private PortalEntity orangePortal = null;

    public PortalTracker(GameComponent parent, SpudEngine engineInstance) {
        super(
                GameComponent.sub(parent, "portal_tracker"),
                engineInstance
        );
    }

    @Override
    protected void onInputEvent(Window window, int key, int action, int modifiers) {
        // todo: create portal with raycast.

        if(key == GLFW.GLFW_MOUSE_BUTTON_1 && action == GLFW.GLFW_RELEASE) {
            Vector2f mousePos = window.getMouseInput().getCurrentPos();
            //this.renderer().getWorldPosFrom((int) mousePos.x, (int) mousePos.y);
        }
    }

    public void usePortal(PortalEntity newPortal) {
        if(newPortal == null)
            throw new IllegalArgumentException("Portal must not be null!");

        PortalEntity portalToRemove = switch (newPortal.getPortalType()) {
            case BLUE -> this.bluePortal;
            case ORANGE -> this.orangePortal;
        };

        if(portalToRemove != null)
            this.scene().removeEntity(portalToRemove);

        switch (newPortal.getPortalType()) {
            case BLUE -> this.bluePortal = newPortal;
            case ORANGE -> this.orangePortal = newPortal;
        }
    }

    public void removePortal(PortalEntity oldPortal) {
        PortalEntity existingPortal = switch (oldPortal.getPortalType()) {
            case BLUE -> this.bluePortal;
            case ORANGE -> this.orangePortal;
        };

        if(existingPortal == null) {
            Logger.warn("Unregistered portal was removed.");
            return;
        }

        if(!existingPortal.getEntityId().equals(oldPortal.getEntityId())) {
            Logger.warn("Unregistered portal's id did not match the known id. Removing the current known {} portal.", oldPortal.getPortalType());
            this.scene().removeEntity(existingPortal);
            return;
        }

        switch (oldPortal.getPortalType()) {
            case BLUE -> this.bluePortal = null;
            case ORANGE -> this.orangePortal = null;
        }
    }

    public boolean hasBluePortal() {
        return this.bluePortal != null;
    }

    public boolean hasOrangePortal() {
        return this.orangePortal != null;
    }

    public boolean hasPortalPair() {
        return this.hasBluePortal() && this.hasOrangePortal();
    }

    public PortalEntity getBluePortal() {
        return this.bluePortal;
    }

    public PortalEntity getOrangePortal() {
        return this.orangePortal;
    }
}
