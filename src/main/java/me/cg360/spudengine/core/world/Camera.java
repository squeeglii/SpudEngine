package me.cg360.spudengine.core.world;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Camera {

    private Vector3f direction;
    private Vector3f position;
    private Vector3f right;
    private Vector2f rotation;
    private Vector3f up;

    private Matrix4f viewMatrix;

    public Camera() {
        this.direction = new Vector3f();
        this.right = new Vector3f();
        this.up = new Vector3f();
        this.position = new Vector3f();
        this.viewMatrix = new Matrix4f();
        this.rotation = new Vector2f();
    }

    public void addRotation(float x, float y) {
        this.rotation.add(x, y);
        this.recalculate();
    }

    public Vector3f getPosition() {
        return this.position;
    }

    public Matrix4f getViewMatrix() {
        return this.viewMatrix;
    }

    public void moveBackwards(float inc) {
        this.viewMatrix.positiveZ(direction).negate().mul(inc);
        this.position.sub(direction);
        this.recalculate();
    }

    public void moveDown(float inc) {
        this.viewMatrix.positiveY(up).mul(inc);
        this.position.sub(up);
        this.recalculate();
    }

    public void moveForward(float inc) {
        this.viewMatrix.positiveZ(direction).negate().mul(inc);
        this.position.add(direction);
        this.recalculate();
    }

    public void moveLeft(float inc) {
        this.viewMatrix.positiveX(this.right).mul(inc);
        this.position.sub(this.right);
        this.recalculate();
    }

    public void moveRight(float inc) {
        this.viewMatrix.positiveX(right).mul(inc);
        this.position.add(right);
        this.recalculate();
    }

    public void moveUp(float inc) {
        this.viewMatrix.positiveY(up).mul(inc);
        this.position.add(up);
        this.recalculate();
    }

    private void recalculate() {
        this.viewMatrix.identity()
                .rotateX(this.rotation.x)
                .rotateY(this.rotation.y)
                .translate(-this.position.x, -this.position.y, -this.position.z);
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        this.recalculate();
    }

    public void setRotation(float x, float y) {
        this.rotation.set(x, y);
        this.recalculate();
    }

}
