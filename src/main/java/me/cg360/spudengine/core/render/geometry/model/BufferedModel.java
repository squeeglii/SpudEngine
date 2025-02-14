package me.cg360.spudengine.core.render.geometry.model;

import java.util.ArrayList;
import java.util.List;

public class BufferedModel {

    private final String id;
    private final List<BufferedMesh> subMeshes;

    public BufferedModel(String id) {
        this(id, new ArrayList<>());
    }

    public BufferedModel(String id, List<BufferedMesh> subMeshes) {
        this.id = id.toLowerCase();
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
