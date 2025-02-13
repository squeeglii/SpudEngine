package me.cg360.spudengine.core.render.geometry.model;

import me.cg360.spudengine.core.render.buffer.GeneralBuffer;

public record BufferedMesh(GeneralBuffer positions, GeneralBuffer indices, int numIndices) {

    public void cleanup() {
        this.positions.cleanup();
        this.indices.cleanup();
    }

}
