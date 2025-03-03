package me.cg360.spudengine.wormholes.world.entity;

import me.cg360.spudengine.wormholes.GeneratedAssets;

public enum PortalType {

    BLUE(GeneratedAssets.BLUE_PORTAL_MODEL.getId()),
    ORANGE(GeneratedAssets.ORANGE_PORTAL_MODEL.getId()),;

    private final String modelId;

    PortalType(String modelId) {
        this.modelId = modelId;
    }

    public final String modelId() {
        return this.modelId;
    }

}
