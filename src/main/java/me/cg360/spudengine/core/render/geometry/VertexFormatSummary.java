package me.cg360.spudengine.core.render.geometry;

public class VertexFormatSummary {

    private Attribute[] attributes;

    private final int vertexSize;

    public VertexFormatSummary(Attribute... attributes) {
        this.attributes = attributes;

        int vSize = 0;
        for(Attribute attribute : attributes) {
            vSize += attribute.size();
        }

        this.vertexSize = vSize;
    }

    public VertexFormatDefinition getDefinition() {
        return new VertexFormatDefinition(this.attributes);
    }

    public int getVertexSize() {
        return this.vertexSize;
    }
}
