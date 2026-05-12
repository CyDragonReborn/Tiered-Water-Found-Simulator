package com.fountainsim;

import android.opengl.Matrix;

public class Camera {
    public float theta = 0.5f;
    public float phi = 0.55f;
    public float radius = 9.0f;
    public float targetX = 0f, targetY = 2.5f, targetZ = 0f;

    private float[] viewMatrix = new float[16];
    private float[] projMatrix = new float[16];
    private float[] vpMatrix = new float[16];

    public void updateViewMatrix() {
        float x = targetX + radius * (float)(Math.cos(phi) * Math.sin(theta));
        float y = targetY + radius * (float)(Math.sin(phi));
        float z = targetZ + radius * (float)(Math.cos(phi) * Math.cos(theta));
        Matrix.setLookAtM(viewMatrix, 0, x, y, z, targetX, targetY, targetZ, 0, 1, 0);
    }

    public void setProjection(float fov, float aspect, float near, float far) {
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, near, far);
    }

    public float[] getViewMatrix() { return viewMatrix; }
    public float[] getProjMatrix() { return projMatrix; }

    public float[] getVPMatrix() {
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0);
        return vpMatrix;
    }

    public void rotate(float dx, float dy) {
        theta += dx * 0.005f;
        phi = Math.max(-1.0f, Math.min(1.2f, phi + dy * 0.005f));
    }

    public void zoom(float factor) {
        radius = Math.max(4f, Math.min(20f, radius * factor));
    }

    public void getRightUp(float[] right, float[] up) {
        float eyeX = targetX + radius * (float)(Math.cos(phi) * Math.sin(theta));
        float eyeY = targetY + radius * (float)(Math.sin(phi));
        float eyeZ = targetZ + radius * (float)(Math.cos(phi) * Math.cos(theta));

        float fx = targetX - eyeX, fy = targetY - eyeY, fz = targetZ - eyeZ;
        float flen = (float)Math.sqrt(fx*fx + fy*fy + fz*fz);
        fx /= flen; fy /= flen; fz /= flen;

        right[0] = fz; right[1] = 0; right[2] = -fx;
        float rlen = (float)Math.sqrt(right[0]*right[0] + right[2]*right[2]);
        if (rlen > 0.0001f) { right[0] /= rlen; right[2] /= rlen; }

        up[0] = right[1]*fz - right[2]*fy;
        up[1] = right[2]*fx - right[0]*fz;
        up[2] = right[0]*fy - right[1]*fx;
    }

    public void autoRotate(float dt) {
        theta += dt * 0.08f;
    }
}
