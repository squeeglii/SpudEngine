package me.cg360.spudengine.core.render.geometry.model;

import java.util.List;

public class BufferedModel {

    private final String id;
    private final List<BufferedMesh> subMeshes;

    public BufferedModel(String id, BufferedMesh... subMeshes) {
        this(id, List.of(subMeshes));
    }

    public BufferedModel(String id, List<BufferedMesh> subMeshes) {
        this.id = id;
        this.subMeshes = subMeshes;
    }


    public void cleanup() {
        this.subMeshes.forEach(BufferedMesh::cleanup);
    }

    public String getId() {
        return this.id;
    }

    public List<BufferedMesh> getSubMeshes() {
        return this.subMeshes;
    }
}
