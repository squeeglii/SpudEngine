package me.cg360.spudengine.core.render.geometry.model;

import me.cg360.spudengine.core.render.geometry.VertexFormatSummary;

import java.util.ArrayList;
import java.util.List;

public class BufferedModel {

    private final String id;
    private final List<BufferedMesh> subMeshes;

    private final VertexFormatSummary format;

    public BufferedModel(String id, VertexFormatSummary format) {
        this(id, format, new ArrayList<>());
    }

    public BufferedModel(String id, VertexFormatSummary format, List<BufferedMesh> subMeshes) {
        this.id = id.toLowerCase();
        this.format = format;
        this.subMeshes = subMeshes;
    }


    public void cleanup() {
        this.subMeshes.forEach(BufferedMesh::cleanup);
    }

    public String getId() {
        return this.id;
    }

    public VertexFormatSummary getFormat() {
        return this.format;
    }

    public List<BufferedMesh> getSubMeshes() {
        return this.subMeshes;
    }
}
