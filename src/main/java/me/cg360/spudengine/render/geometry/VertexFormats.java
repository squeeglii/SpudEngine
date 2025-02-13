package me.cg360.spudengine.render.geometry;

import java.util.function.Supplier;

public class VertexFormats {

    public static final int DEFAULT_BINDING = 0;

    public static final Supplier<VertexFormatDefinition> POINTS = () -> new VertexFormatDefinition(
            Attribute.VEC3F.withBinding(DEFAULT_BINDING)
    );

}
