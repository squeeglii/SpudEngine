package me.cg360.spudengine.core.render.geometry;

import java.util.function.Supplier;

public class VertexFormats {

    public static final int DEFAULT_BINDING = 0;

    public static final Supplier<VertexFormatDefinition> POSITION = () -> new VertexFormatDefinition(
            Attribute.VEC3F.withBinding(DEFAULT_BINDING)
    );

}
