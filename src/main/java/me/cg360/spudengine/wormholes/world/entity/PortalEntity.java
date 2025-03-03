package me.cg360.spudengine.wormholes.world.entity;

import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.core.world.entity.SimpleEntity;
import me.cg360.spudengine.wormholes.WormholeDemo;

import java.util.UUID;

public class PortalEntity extends SimpleEntity {

    private final PortalType portalType;

    public PortalEntity(PortalType type) {
        super(UUID.randomUUID(), type.modelId());

        this.portalType = type;
    }


    @Override
    public void onAdd(Scene scene) {
        WormholeDemo.get().getPortalTracker().usePortal(this);
    }

    @Override
    public void onRemove(Scene scene) {
        WormholeDemo.get().getPortalTracker().removePortal(this);
    }

    public PortalType getPortalType() {
        return this.portalType;
    }
}
