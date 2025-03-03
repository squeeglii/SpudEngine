package me.cg360.spudengine.core.render.geometry.model;

import org.joml.Vector4f;

import java.awt.*;

public record Material(String texture, Vector4f diffuse){

    public static final Vector4f WHITE = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

    public static Vector4f useColour(Color color) {
        return new Vector4f(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

}
