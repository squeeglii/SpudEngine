package me.cg360.spudengine.core.render.geometry.model;

import me.cg360.spudengine.core.render.image.texture.Texture;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;

import java.util.List;

public record BundledMaterial(Vector4f diffuse, @NotNull Texture texture, List<BufferedMesh> meshes) {

    public void cleanup() {
        this.meshes.forEach(BufferedMesh::cleanup);
    }

}
