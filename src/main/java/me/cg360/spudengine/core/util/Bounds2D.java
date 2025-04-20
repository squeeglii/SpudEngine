package me.cg360.spudengine.core.util;

import me.cg360.spudengine.core.world.Camera;
import me.cg360.spudengine.core.world.Projection;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

import java.util.List;

public record Bounds2D(double minX, double minY, double maxX, double maxY) {

    public static final Bounds2D SCREEN_BOUNDS = new Bounds2D(0, 0, 1, 1);

    public boolean contains(Bounds2D other) {
        return other.minX >= this.minX &&
               other.minY >= this.minY &&
               other.maxX <= this.maxX &&
               other.maxY <= this.maxY;
    }

    public boolean intersects(Bounds2D other) {
        return other.minX < this.maxX &&
               other.minY < this.maxY &&
               other.maxX > this.minY &&
               other.maxY > this.minY;
    }


    @NotNull
    @Override
    public String toString() {
        return "[ Bounds2D (%.2f x %.2f) (%.2f, %.2f) -> (%.2f, %.2f) ]".formatted(
                maxX - minX, maxY - minY,
                minX, minY,
                maxX, maxY
        );
    }


    public static Bounds2D fromProjectedPoints(Camera view, Projection projection, Matrix4f modelTransform, List<Vector3f> points) {
        if(points.isEmpty())
            throw new IllegalArgumentException("Must provide at least one point");

        Matrix4f viewMatrix = view.getViewMatrix();
        Matrix4f projMatrix = projection.asMatrix();

        Matrix4f screenspaceMat = projMatrix.mul(viewMatrix, new Matrix4f()).mul(modelTransform);

        double minX = 0, minY = 0, maxX = 0, maxY = 0;
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
                firstPointSet = true;
                continue;
            }

            if(scaled.x < minX) minX = scaled.x;
            else if (scaled.x > maxX) maxX = scaled.x;

            if(scaled.y < minY) minY = scaled.y;
            else if (scaled.y > maxY) maxY = scaled.y;
        }

        double minXScaled = ((minX + 1d) / 2d);
        double maxXScaled = ((maxX + 1d) / 2d);
        double minYScaled = ((minY + 1d) / 2d);
        double maxYScaled = ((maxY + 1d) / 2d);

        return new Bounds2D(minXScaled, minYScaled, maxXScaled, maxYScaled);
    }



}
