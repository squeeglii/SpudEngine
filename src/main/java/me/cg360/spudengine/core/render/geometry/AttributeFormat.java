package me.cg360.spudengine.core.render.geometry;

@FunctionalInterface
public interface AttributeFormat {

    Attribute withBinding(int binding);

    // Use for push constants or pre-calculating size.
    default Attribute reference() {
        return withBinding(-1);
    }

}
