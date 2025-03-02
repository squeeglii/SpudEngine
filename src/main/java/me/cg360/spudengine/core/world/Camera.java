package me.cg360.spudengine.core.world;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Camera {

    private static final double MAX_PITCH = Math.toRadians(89.5f);

    private Vector3f position;
    private Vector2f rotation;
    private Vector3f right;
    private Vector3f up;
    private Vector3f forward;

    private Matrix4f viewMatrix;

    private boolean clampPitch;

    public Camera(boolean clampPitch) {
        this.forward = new Vector3f();
        this.right = new Vector3f();
        this.up = new Vector3f();
        this.position = new Vector3f();
        this.viewMatrix = new Matrix4f();
        this.rotation = new Vector2f();

        this.clampPitch = clampPitch;
    }


    public void addRotation(float pitch, float yaw) {
        this.rotation.add(pitch, yaw);
        this.recalculate();
    }

    /** Up-down movement works relative to the world. Horizontal movement works relative to the view direction. */
    public void move(float inpForward, float inpUp, float inpLeft, float step) {
        if(inpForward != 0.0f || inpLeft != 0.0f) {
            Vector2f movementDir = new Vector2f(inpLeft, inpForward).normalize().mul(step);

            this.viewMatrix.positiveX(this.right).mul(movementDir.x);
            this.viewMatrix.positiveZ(this.forward).mul(movementDir.y);
            this.position.sub(this.right).sub(this.forward);
        }

        this.position.add(0, inpUp * step, 0);
        this.recalculate();
    }

    public void moveBackwards(float inc) {
        this.viewMatrix.positiveZ(forward).negate().mul(inc);
        this.position.sub(forward);
        this.recalculate();
    }

    public void panDown(float inc) {
        this.viewMatrix.positiveY(up).mul(inc);
        this.position.sub(up);
        this.recalculate();
    }

    public void moveForward(float inc) {
        this.viewMatrix.positiveZ(forward).negate().mul(inc);
        this.position.add(forward);
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

    public void panUp(float inc) {
        this.viewMatrix.positiveY(up).mul(inc);
        this.position.add(up);
        this.recalculate();
    }

    private void recalculate() {
        this.rotation.x = this.rotation.x % (float) (2*Math.PI); // make pitch loop so it's somewhat sensible.

        if(this.clampPitch && Math.abs(this.rotation.x) > MAX_PITCH) {
            this.rotation.x = Math.signum(this.rotation.x) * (float) MAX_PITCH;
        }

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

    public void setPitchClamped(boolean clampPitch) {
        this.clampPitch = clampPitch;
    }


    public boolean isUpsideDown() {
        return Math.abs(this.rotation.x) >= (Math.PI / 2);
    }

    public Vector3f getPosition() {
        return this.position;
    }

    public Vector3f getFacingDirection() {
        return forward;
    }

    public Matrix4f getViewMatrix() {
        return this.viewMatrix;
    }
}
