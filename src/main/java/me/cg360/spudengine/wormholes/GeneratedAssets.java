package me.cg360.spudengine.wormholes;

import me.cg360.spudengine.core.render.geometry.model.Material;
import me.cg360.spudengine.core.render.geometry.model.Mesh;
import me.cg360.spudengine.core.render.geometry.model.Model;
import me.cg360.spudengine.core.render.image.texture.TextureManager;

import java.awt.*;
import java.util.List;

public class GeneratedAssets {

    private static final float[] PORTAL_POINTS = new float[] {
            -0.5f,  1.0f, 0.0f,    // left top
             0.5f,  1.0f, 0.0f,    // right top
             0.5f, -1.0f, 0.0f,    // right bottom
            -0.5f, -1.0f, 0.0f,    // left bottom
             0.0f,  0.0f, 0.0f,    // center
    };
    private static final float[] PORTAL_UVs = new float[] {
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
            0.5f, 0.5f
    };
    private static final int[] PORTAL_INDICES = new int[] {
            4, 0, 1,
            4, 1, 2,
            4, 2, 3,
            4, 3, 0
    };


    public static final Mesh PORTAL_MESH = new Mesh(PORTAL_POINTS, PORTAL_UVs, PORTAL_INDICES, 0);

    public static final Material BLUE_PORTAL_MATERIAL = new Material("generated/portal/blue", Material.WHITE);
    public static final Model BLUE_PORTAL_MODEL = new Model("generated/portal/blue", BLUE_PORTAL_MATERIAL, PORTAL_MESH);

    public static final Material ORANGE_PORTAL_MATERIAL = new Material("generated/portal/orange", Material.WHITE);
    public static final Model ORANGE_PORTAL_MODEL = new Model("generated/portal/orange", ORANGE_PORTAL_MATERIAL, PORTAL_MESH);

    public static List<Model> getAllModels() {
        return List.of(BLUE_PORTAL_MODEL, ORANGE_PORTAL_MODEL);
    }

    public static void registerTextures(TextureManager textureManager) {
        Color nonMask = new Color(255, 255, 255, 0);

        textureManager.newCircleTexture(BLUE_PORTAL_MATERIAL.texture(), 512, 512, 1, 256, new Color(50, 100, 255, 255), nonMask);
        textureManager.newCircleTexture(ORANGE_PORTAL_MATERIAL.texture(), 512, 512, 1, 256, new Color(255, 100, 0, 255), nonMask);
    }
}
