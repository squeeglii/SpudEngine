package me.cg360.spudengine.core.render.geometry;

import java.util.function.Supplier;

public class VertexFormats {

    public static final int DEFAULT_BINDING = 0;

    public static final VertexFormatSummary POSITION = new VertexFormatSummary(
            Attribute.VEC3F.withBinding(DEFAULT_BINDING)
    );

    public static final VertexFormatSummary POSITION_UV = new VertexFormatSummary(
            Attribute.VEC3F.withBinding(DEFAULT_BINDING),
            Attribute.VEC2F.withBinding(DEFAULT_BINDING)
    );

}
