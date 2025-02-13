package me.cg360.spudengine.core.render.geometry.model;

import java.util.List;

public class Model {

    private String id;
    private List<Mesh> subMeshes;

    public Model(String id, Mesh... subMeshes) {
        this(id, List.of(subMeshes));
    }

    public Model(String id, List<Mesh> subMeshes) {
        this.id = id;
        this.subMeshes = subMeshes;
    }


    public String getId() {
        return this.id;
    }

    public List<Mesh> getSubMeshes() {
        return this.subMeshes;
    }

}
