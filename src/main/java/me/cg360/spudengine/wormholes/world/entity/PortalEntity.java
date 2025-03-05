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

    public PortalEntity(PortalType type, Vector3f position, Vector3f rotation) {
        super(UUID.randomUUID(), type.modelId());

        this.portalType = type;
        this.position = position.add(0, 1, 0); // Spawn portal at bottom.
        this.rotation.rotateXYZ(rotation.x(), rotation.y(), rotation.z());
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

        Vector3f eularThis = new Vector3f();
        Vector3f eularOther = new Vector3f();

        this.rotation.getEulerAnglesXYZ(eularThis);
        otherPortal.rotation.getEulerAnglesXYZ(eularOther);

        eularThis.sub(eularOther);
        Quaternionf quaternion = new Quaternionf();
        quaternion.rotateXYZ(eularThis.x, eularThis.y, eularThis.z);

        mat.rotateLocal(quaternion);
        mat.translateLocal(this.position);

        return mat;
    }
}
