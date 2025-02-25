package me.cg360.spudengine.core.render.geometry.model;

import me.cg360.spudengine.core.render.data.buffer.GeneralBuffer;

public record BufferedMesh(GeneralBuffer vertices, GeneralBuffer indices, int numIndices) {

    public void cleanup() {
        this.vertices.cleanup();
        this.indices.cleanup();
    }

}
