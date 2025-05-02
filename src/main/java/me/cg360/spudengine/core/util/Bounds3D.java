package me.cg360.spudengine.core.util;

import me.cg360.spudengine.core.world.Camera;
import me.cg360.spudengine.core.world.Projection;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.tinylog.Logger;

import java.util.List;

public record Bounds3D(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

    public static final Bounds3D SCREEN_BOUNDS = new Bounds3D(0, 0, 0, 1, 1, 1);

    public boolean contains(Bounds3D other) {
        return other.minX >= this.minX &&
               other.minY >= this.minY &&
               other.minZ >= this.minZ &&
               other.maxX <= this.maxX &&
               other.maxY <= this.maxY &&
               other.maxZ <= this.maxZ;
    }

    public boolean intersects(Bounds3D other) {
        return other.minX < this.maxX &&
               other.minY < this.maxY &&
               other.minZ < this.maxZ &&
               other.maxX > this.minY &&
               other.maxY > this.minY &&
               other.maxZ > this.minZ;
    }

    public double width() {
        return this.maxX - this.minX;
    }

    public double height() {
        return this.maxY - this.minY;
    }

    public double depth() {
        return this.minZ - this.maxZ;
    }

    @NotNull
    @Override
    public String toString() {
        return "[ Bounds3D (%.2f x %.2f x %.2f) (%.2f, %.2f, %.2f) -> (%.2f, %.2f, %.2f) ]".formatted(
                maxX - minX, maxY - minY, maxZ - minZ,
                minX, minY, minZ,
                maxX, maxY, maxZ
        );
    }


    public static Bounds3D fromProjectedPoints(Camera view, Projection projection, Matrix4f modelTransform, List<Vector3f> points) {
        if(points.isEmpty())
            throw new IllegalArgumentException("Must provide at least one point");

        Matrix4f viewMatrix = view.getViewMatrix();
        Matrix4f projMatrix = projection.asMatrix();

        Matrix4f screenspaceMat = projMatrix.mul(viewMatrix, new Matrix4f()).mul(modelTransform);

        double minX = 0, minY = 0, maxX = 0, maxY = 0, minZ = 0, maxZ = 0;
        boolean firstPointSet = false;

        for (Vector3f point : points) {
            Vector4d projected = new Vector4d(point, 1).mul(screenspaceMat);
            Vector4d scaled = projected.div(projected.w, new Vector4d());

            //Logger.info("Point: {} -> {}", point, scaled);

            if(!firstPointSet) {
                minX = scaled.x;
                maxX = scaled.x;
                minY = scaled.y;
                maxY = scaled.y;
                minZ = scaled.z;
                maxZ = scaled.z;
                firstPointSet = true;
                continue;
            }

            if(scaled.x < minX) minX = scaled.x;
            else if (scaled.x > maxX) maxX = scaled.x;

            if(scaled.y < minY) minY = scaled.y;
            else if (scaled.y > maxY) maxY = scaled.y;

            if(scaled.z < minZ) minZ = scaled.z;
            else if (scaled.z > maxZ) maxZ = scaled.z;
        }

        double minXScaled = ((minX + 1d) / 2d);
        double maxXScaled = ((maxX + 1d) / 2d);
        double minYScaled = ((minY + 1d) / 2d);
        double maxYScaled = ((maxY + 1d) / 2d);

        return new Bounds3D(minXScaled, minYScaled, minZ, maxXScaled, maxYScaled, maxZ);
    }



}
