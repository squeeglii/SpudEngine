package me.cg360.spudengine.core.render.geometry.model;

import java.util.List;

public class Model {

    public static final Material DEFAULT_MATERIAL = new Material("missing", Material.WHITE);

    private String id;
    private List<Material> materials;
    private List<Mesh> subMeshes;

    public Model(String id, Mesh... subMeshes) {
        this(id, List.of(DEFAULT_MATERIAL), List.of(subMeshes));
    }

    public Model(String id, Material material, Mesh... subMeshes) {
        this(id, List.of(material), List.of(subMeshes));
    }

    public Model(String id, List<Material> materials, List<Mesh> subMeshes) {
        this.id = id;
        this.materials = materials;
        this.subMeshes = subMeshes;
    }


    public String getId() {
        return this.id;
    }

    public List<Material> getMaterials() {
        return this.materials;
    }

    public List<Mesh> getSubMeshes() {
        return this.subMeshes;
    }

}
