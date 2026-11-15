package com.dragonminez.client.render;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.animation.state.BoneSnapshot;

/**
 * Copies the pose of a GeckoLib model onto a vanilla {@link PlayerModel} so the two stay in sync.
 *
 * <h2>26.2 / GeckoLib 5 migration</h2>
 * Two separate changes broke the 1.21 version:
 * <ul>
 *   <li>{@code PlayerModel} is no longer generic — it used to be {@code PlayerModel<T extends Player>},
 *       now it is {@code PlayerModel extends HumanoidModel<AvatarRenderState>};</li>
 *   <li>{@link GeoBone} became immutable. It exposes only {@code pivotX/Y/Z()} and
 *       {@code baseRotX/Y/Z()} — the old mutable {@code getRotX()/getPosX()} accessors are gone.
 *       Every per-frame value now lives on {@link BoneSnapshot} ({@code GeoBone#frameSnapshot}),
 *       which {@code AnimationProcessor#createBoneSnapshots} rebuilds before each render pass.</li>
 * </ul>
 *
 * <p>So the pose is read from the bone snapshots, not from the bones. {@code sync} therefore takes a
 * callback that resolves a bone name to its current snapshot; callers run it from inside a render
 * pass, where snapshots exist.
 */
public final class VanillaModelSync {
    private VanillaModelSync() {
    }

    /** Resolves a bone name to its current per-frame snapshot (null when the model has no such bone). */
    @FunctionalInterface
    public interface SnapshotSource {
        BoneSnapshot get(String boneName);
    }

    public static void sync(BakedGeoModel geoModel, PlayerModel vanillaModel, SnapshotSource snapshots) {
        BoneSnapshot waist = snapshots.get("waist");

        syncBone(geoModel, snapshots, "head", vanillaModel.head, waist);
        syncBone(geoModel, snapshots, "head", vanillaModel.hat, waist);
        syncBone(geoModel, snapshots, "body", vanillaModel.body, waist);
        syncBone(geoModel, snapshots, "right_arm", vanillaModel.rightArm, waist);
        syncBone(geoModel, snapshots, "left_arm", vanillaModel.leftArm, waist);

        syncBone(geoModel, snapshots, "right_leg", vanillaModel.rightLeg, null);
        syncBone(geoModel, snapshots, "left_leg", vanillaModel.leftLeg, null);

        // PlayerModel no longer exposes a crouching flag; callers animate the pose themselves.
        
    }

    private static void syncBone(BakedGeoModel geoModel, SnapshotSource snapshots, String boneName,
                                 ModelPart part, BoneSnapshot parent) {
        GeoBone bone = geoModel.getBone(boneName).orElse(null);
        BoneSnapshot self = snapshots.get(boneName);
        if (bone == null || self == null) return;

        float parentRotX = 0, parentRotY = 0, parentRotZ = 0;
        float parentPosX = 0, parentPosY = 0, parentPosZ = 0;
        float parentPivX = 0, parentPivY = 0, parentPivZ = 0;
        if (parent != null) {
            parentRotX = parent.getRotX();
            parentRotY = parent.getRotY();
            parentRotZ = parent.getRotZ();
            parentPosX = parent.getTranslateX();
            parentPosY = parent.getTranslateY();
            parentPosZ = parent.getTranslateZ();
            GeoBone parentBone = parent.getBone();
            if (parentBone != null) {
                parentPivX = parentBone.pivotX();
                parentPivY = parentBone.pivotY();
                parentPivZ = parentBone.pivotZ();
            }
        }

        part.xRot = -(parentRotX + self.getRotX());
        part.yRot = -(parentRotY + self.getRotY());
        part.zRot = (parentRotZ + self.getRotZ());

        float relX = (bone.pivotX() + self.getTranslateX()) - parentPivX;
        float relY = (bone.pivotY() + self.getTranslateY()) - parentPivY;
        float relZ = (bone.pivotZ() + self.getTranslateZ()) - parentPivZ;

        float[] rotated = rotateZYX(relX, relY, relZ, parentRotX, parentRotY, parentRotZ);

        float deltaX = (parentPivX + parentPosX + rotated[0]) - bone.pivotX();
        float deltaY = (parentPivY + parentPosY + rotated[1]) - bone.pivotY();
        float deltaZ = (parentPivZ + parentPosZ + rotated[2]) - bone.pivotZ();

        PartPose init = part.getInitialPose();
        part.x = init.x() - deltaX;
        part.y = init.y() - deltaY;
        part.z = init.z() + deltaZ;
    }

    private static float[] rotateZYX(float x, float y, float z, float rotX, float rotY, float rotZ) {
        float cx = Mth.cos(rotX), sx = Mth.sin(rotX);
        float y1 = y * cx - z * sx;
        float z1 = y * sx + z * cx;
        float x1 = x;

        float cy = Mth.cos(rotY), sy = Mth.sin(rotY);
        float x2 = x1 * cy + z1 * sy;
        float z2 = -x1 * sy + z1 * cy;
        float y2 = y1;

        float cz = Mth.cos(rotZ), sz = Mth.sin(rotZ);
        float x3 = x2 * cz - y2 * sz;
        float y3 = x2 * sz + y2 * cz;
        float z3 = z2;
        return new float[]{x3, y3, z3};
    }
}
