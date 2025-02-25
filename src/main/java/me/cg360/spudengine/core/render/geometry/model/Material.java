package me.cg360.spudengine.core.render.geometry.model;

import org.joml.Vector4f;

public record Material(String texture, Vector4f diffuse){

    public static final Vector4f WHITE = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

}
