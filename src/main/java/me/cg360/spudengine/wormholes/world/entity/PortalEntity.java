package me.cg360.spudengine.wormholes.world.entity;

import me.cg360.spudengine.core.world.Scene;
import me.cg360.spudengine.core.world.entity.SimpleEntity;
import me.cg360.spudengine.wormholes.WormholeDemo;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

public class PortalEntity extends SimpleEntity {

    private final PortalType portalType;

    // this loosely depends on this.rotation.
    // If portals can be moved (which is a colossal task)
    // make sure this gets updated.
    private Vector3f up;
    private Vector3f normal;

    public PortalEntity(PortalType type, Vector3f position, Vector3f rotation) {
        super(UUID.randomUUID(), type.modelId());

        this.portalType = type;
        this.position = position.add(0, 1, 0); // Spawn portal at bottom.
        this.up = new Vector3f(0, 1, 0);
        this.normal = new Vector3f(0, 1, 0);

        this.rotation.rotateXYZ(rotation.x(), rotation.y(), rotation.z());
        this.up.rotate(this.rotation);

        this.updateTransform();
    }


    @Override
    public void onAdd(Scene scene) {
        WormholeDemo.get().getPortalTracker().usePortal(this);
    }

    @Override
    public void onRemove(Scene scene) {
        WormholeDemo.get().getPortalTracker().removePortal(this);
    }

    public PortalType getPortalType() {
        return this.portalType;
    }

    /** Calculates the transform that would:
     *  - treat this portal as the "current room" portal
     *     - "current room" cannot be translated, rotated, scaled, etc. Anchor,
     *  - treat the otherPortal as the "copied room" portal
     *     - "copied room" can be freely translated, rotated, and scaled.
     *  - stitch this portal back-to-back to the otherPortal.
     */
    public Matrix4f calculateConnectionTransform(PortalEntity otherPortal) {
        Matrix4f mat = new Matrix4f();

        Vector3f subOtherPos = new Vector3f(otherPortal.position).mul(-1);
        mat.translate(subOtherPos);

        // THIS WORKS!
        // I can't lie, this rotation magic is half planned, and
        // half tinkering around with values till it worked.
        // BUT IT WORKS.
        // So I'm not touching it.
        Quaternionf thisRot = new Quaternionf();
        Vector3f viewThis = new Vector3f(0, 0, -1);
        viewThis.rotate(this.rotation);
        thisRot.lookAlong(viewThis, this.up);

        Quaternionf otherRot = new Quaternionf();
        Vector3f invertedViewOther = new Vector3f(0, 0, 1); // invert the view dir, so the back side is used to align rotations.
        invertedViewOther.rotate(otherPortal.rotation);
        otherRot.lookAlong(invertedViewOther, otherPortal.up);

        // Rotate around origin, the difference between the two portals rotations.
        Quaternionf rotDiff = new Quaternionf();
        otherRot.mul(thisRot.invert(), rotDiff);
        mat.rotateLocal(rotDiff);

        mat.translateLocal(this.position);

        return mat;
    }

    public Vector3f getUp() {
        return this.up;
    }

    @Override
    public boolean shouldDraw() {
        // Entity has a model for debugging, but it blocks the portal effect.
        // The portal effect passes this entity's properties to a shader, which
        // uses overlay textures for the effect, instead of the model.
        return false;
    }
}
