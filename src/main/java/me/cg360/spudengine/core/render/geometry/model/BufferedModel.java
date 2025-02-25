package me.cg360.spudengine.core.render.geometry.model;

import me.cg360.spudengine.core.render.geometry.VertexFormatSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class BufferedModel {

    private final String id;
    private final List<BundledMaterial> materials;

    private final VertexFormatSummary format;

    public BufferedModel(String id, VertexFormatSummary format) {
        this(id, format, new ArrayList<>());
    }

    public BufferedModel(String id, VertexFormatSummary format, List<BundledMaterial> materials) {
        this.id = id.toLowerCase();
        this.format = format;
        this.materials = materials;
    }


    public void cleanup() {
        this.materials.forEach(BundledMaterial::cleanup);
    }

    public String getId() {
        return this.id;
    }

    public VertexFormatSummary getFormat() {
        return this.format;
    }

    public List<BundledMaterial> getMaterials() {
        return this.materials;
    }

    /** Read-Only. */
    public List<BufferedMesh> collectMaterialMeshes() {
        List<BufferedMesh> collectedMeshes = new LinkedList<>();
        this.materials.forEach(mat -> collectedMeshes.addAll(mat.meshes()));
        return Collections.unmodifiableList(collectedMeshes);
    }
}
