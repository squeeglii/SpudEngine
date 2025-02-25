package me.cg360.spudengine.core.render.geometry.model;

import me.cg360.spudengine.core.render.geometry.ModelValidationException;

public record Mesh(float[] positions, float[] textureCoordinates, int[] indices, int materialIdx) {

    public void validate() {
        if(this.positions == null)
            throw new ModelValidationException("Mesh property 'positions' cannot be null");

        if(this.textureCoordinates == null)
            throw new ModelValidationException("Mesh property 'textureCoordinates' cannot be null");

        if(this.indices == null)
            throw new ModelValidationException("Mesh property 'indices' cannot be null");

        int expectedTexCoordCount = this.vertexCount() * 2;
        if(this.textureCoordinates.length != expectedTexCoordCount)
            throw new ModelValidationException("Position & Texture coordinate mismatch - [%s positions; expected %s texcoords, got %s]".formatted(
                    this.positions.length, expectedTexCoordCount, this.textureCoordinates.length
            ));
    }

    public int vertexCount() {
        return this.positions.length / 3;
    }

    public static Mesh withoutProvidedUVs(float[] positions, int[] indices) {
        return Mesh.withoutProvidedUVs(positions, indices, 0);
    }

    public static Mesh withoutProvidedUVs(float[] positions, int[] indices, int materialIndex) {
        int texCoordCount = (positions.length / 3) * 2;
        return new Mesh(positions, new float[texCoordCount], indices, materialIndex);
    }

}
