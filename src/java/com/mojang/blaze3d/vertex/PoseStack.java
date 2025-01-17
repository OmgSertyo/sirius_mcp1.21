package com.mojang.blaze3d.vertex;

import com.google.common.collect.Queues;
import com.mojang.math.Axis;
import com.mojang.math.MatrixUtil;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.Util;
import net.minecraftforge.client.extensions.IForgePoseStack;
import net.optifine.util.MathUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PoseStack implements IForgePoseStack {
    Deque<PoseStack.Pose> freeEntries = new ArrayDeque<>();
    private final Deque<PoseStack.Pose> poseStack = Util.make(Queues.newArrayDeque(), dequeIn -> {
        Matrix4f matrix4f = new Matrix4f();
        Matrix3f matrix3f = new Matrix3f();
        dequeIn.add(new PoseStack.Pose(matrix4f, matrix3f));
    });

    public void translate(double pX, double pY, double pZ) {
        this.translate((float)pX, (float)pY, (float)pZ);
    }

    public void translate(float pX, float pY, float pZ) {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.translate(pX, pY, pZ);
    }

    public void scale(float pX, float pY, float pZ) {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.scale(pX, pY, pZ);
        if (Math.abs(pX) != Math.abs(pY) || Math.abs(pY) != Math.abs(pZ)) {
            posestack$pose.normal.scale(1.0F / pX, 1.0F / pY, 1.0F / pZ);
            posestack$pose.trustedNormals = false;
        } else if (pX < 0.0F || pY < 0.0F || pZ < 0.0F) {
            posestack$pose.normal.scale(Math.signum(pX), Math.signum(pY), Math.signum(pZ));
        }
    }

    public void mulPose(Quaternionf pQuaternion) {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.rotate(pQuaternion);
        posestack$pose.normal.rotate(pQuaternion);
    }

    public void rotateAround(Quaternionf pQuaternion, float pX, float pY, float pZ) {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.rotateAround(pQuaternion, pX, pY, pZ);
        posestack$pose.normal.rotate(pQuaternion);
    }

    public void pushPose() {
        PoseStack.Pose posestack$pose = this.freeEntries.pollLast();
        if (posestack$pose != null) {
            PoseStack.Pose posestack$pose1 = this.poseStack.getLast();
            posestack$pose.pose.set(posestack$pose1.pose);
            posestack$pose.normal.set(posestack$pose1.normal);
            posestack$pose.trustedNormals = posestack$pose1.trustedNormals;
            this.poseStack.addLast(posestack$pose);
        } else {
            this.poseStack.addLast(new PoseStack.Pose(this.poseStack.getLast()));
        }
    }

    public void popPose() {
        PoseStack.Pose posestack$pose = this.poseStack.removeLast();
        if (posestack$pose != null) {
            this.freeEntries.add(posestack$pose);
        }
    }

    public PoseStack.Pose last() {
        return this.poseStack.getLast();
    }

    public boolean clear() {
        return this.poseStack.size() == 1;
    }

    public void rotateDegXp(float angle) {
        this.mulPose(Axis.XP.rotationDegrees(angle));
    }

    public void rotateDegXn(float angle) {
        this.mulPose(Axis.XN.rotationDegrees(angle));
    }

    public void rotateDegYp(float angle) {
        this.mulPose(Axis.YP.rotationDegrees(angle));
    }

    public void rotateDegYn(float angle) {
        this.mulPose(Axis.YN.rotationDegrees(angle));
    }

    public void rotateDegZp(float angle) {
        this.mulPose(Axis.ZP.rotationDegrees(angle));
    }

    public void rotateDegZn(float angle) {
        this.mulPose(Axis.ZN.rotationDegrees(angle));
    }

    public void rotateDeg(float angle, float x, float y, float z) {
        Vector3f vector3f = new Vector3f(x, y, z);
        Quaternionf quaternionf = MathUtils.rotationDegrees(vector3f, angle);
        this.mulPose(quaternionf);
    }

    public int size() {
        return this.poseStack.size();
    }

    @Override
    public String toString() {
        return this.last().toString() + "Depth: " + this.poseStack.size();
    }

    public void setIdentity() {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.identity();
        posestack$pose.normal.identity();
        posestack$pose.trustedNormals = true;
    }

    public void mulPose(Matrix4f pPose) {
        PoseStack.Pose posestack$pose = this.poseStack.getLast();
        posestack$pose.pose.mul(pPose);
        if (!MatrixUtil.isPureTranslation(pPose)) {
            if (MatrixUtil.isOrthonormal(pPose)) {
                posestack$pose.normal.mul(new Matrix3f(pPose));
            } else {
                posestack$pose.computeNormalMatrix();
            }
        }
    }

    public static final class Pose {
        final Matrix4f pose;
        final Matrix3f normal;
        boolean trustedNormals = true;

        Pose(Matrix4f pPose, Matrix3f pNormal) {
            this.pose = pPose;
            this.normal = pNormal;
        }

        Pose(PoseStack.Pose pPose) {
            this.pose = new Matrix4f(pPose.pose);
            this.normal = new Matrix3f(pPose.normal);
            this.trustedNormals = pPose.trustedNormals;
        }

        void computeNormalMatrix() {
            this.normal.set(this.pose).invert().transpose();
            this.trustedNormals = false;
        }

        public Matrix4f pose() {
            return this.pose;
        }

        public Matrix3f normal() {
            return this.normal;
        }

        public Vector3f transformNormal(Vector3f pVector, Vector3f pDestination) {
            return this.transformNormal(pVector.x, pVector.y, pVector.z, pDestination);
        }

        public Vector3f transformNormal(float pX, float pY, float pZ, Vector3f pDestination) {
            Vector3f vector3f = this.normal.transform(pX, pY, pZ, pDestination);
            return this.trustedNormals ? vector3f : vector3f.normalize();
        }

        public PoseStack.Pose copy() {
            return new PoseStack.Pose(this);
        }

        @Override
        public String toString() {
            return this.pose.toString() + this.normal.toString();
        }
    }
}