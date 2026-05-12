package com.fountainsim;

import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import android.opengl.GLES20;
import android.opengl.Matrix;

public class FountainScene {
    private ArrayList<Bowl> bowls = new ArrayList<>();
    private ParticleSystem sprayParticles;
    private ParticleSystem overflowParticles;
    private Camera camera;

    private float time = 0f;
    private float[] modelMatrix = new float[16];
    private float[] mvMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    private float[] lightDir = {0.3f, 1.0f, 0.5f};

    private float[] right = new float[3];
    private float[] up = new float[3];

    // Pillar geometry
    private FloatBuffer pillarVertBuf, pillarNormBuf;
    private ShortBuffer pillarIdxBuf;
    private int pillarIdxCount;

    // Water surface geometry
    private FloatBuffer waterVertexBuffer;
    private ShortBuffer waterIndexBuffer;
    private int waterIndices;

    public FountainScene(Camera camera) {
        this.camera = camera;
        sprayParticles = new ParticleSystem();
        overflowParticles = new ParticleSystem();
        buildFountain();
        buildPillar();
        buildWaterSurface();
    }

    private void buildFountain() {
        Bowl b1 = new Bowl(4.8f, 0.7f, 0.25f, 0.06f);
        b1.setColor(0.75f, 0.75f, 0.78f);
        bowls.add(b1);

        Bowl b2 = new Bowl(3.3f, 1.1f, 0.30f, 0.08f);
        b2.setColor(0.73f, 0.73f, 0.76f);
        bowls.add(b2);

        Bowl b3 = new Bowl(1.8f, 1.6f, 0.35f, 0.10f);
        b3.setColor(0.70f, 0.70f, 0.73f);
        bowls.add(b3);

        Bowl b4 = new Bowl(0.3f, 2.2f, 0.35f, 0.12f);
        b4.setColor(0.68f, 0.68f, 0.71f);
        bowls.add(b4);

        for (Bowl b : bowls) b.waterLevel = b.maxWaterLevel * 0.3f;
    }

    private void buildPillar() {
        int seg = 20, vpr = seg + 1;
        int vc = vpr * 2;
        float[] verts = new float[vc * 3];
        float[] norms = new float[vc * 3];
        short[] idx = new short[seg * 6];
        int vi = 0, ni = 0, ii = 0;

        float topY = 7.0f, botY = 0.0f, topR = 0.12f, botR = 0.25f;

        for (int p = 0; p < 2; p++) {
            float r = (p == 0) ? topR : botR;
            float y = (p == 0) ? topY : botY;
            for (int s = 0; s < vpr; s++) {
                float a = (float)s / seg * 2f * (float)Math.PI;
                verts[vi++] = r * (float)Math.cos(a);
                verts[vi++] = y;
                verts[vi++] = r * (float)Math.sin(a);
                float nx = (float)Math.cos(a);
                float nz = (float)Math.sin(a);
                float len = (float)Math.sqrt(nx*nx + nz*nz);
                norms[ni++] = nx/len; norms[ni++] = 0; norms[ni++] = nz/len;
            }
        }
        for (int s = 0; s < seg; s++) {
            int a = s, b = s + 1, c = vpr + s, d = vpr + s + 1;
            idx[ii++] = (short)a; idx[ii++] = (short)c; idx[ii++] = (short)b;
            idx[ii++] = (short)b; idx[ii++] = (short)c; idx[ii++] = (short)d;
        }
        pillarIdxCount = ii;

        pillarVertBuf = ByteBuffer.allocateDirect(verts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        pillarVertBuf.put(verts).position(0);
        pillarNormBuf = ByteBuffer.allocateDirect(norms.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        pillarNormBuf.put(norms).position(0);
        pillarIdxBuf = ByteBuffer.allocateDirect(idx.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        pillarIdxBuf.put(idx).position(0);
    }

    private void buildWaterSurface() {
        int seg = 24, vpr = seg + 1;
        float[] verts = new float[vpr * vpr * 3];
        short[] idx = new short[seg * seg * 6];
        int vi = 0, ii = 0;

        for (int row = 0; row < vpr; row++) {
            float t = (float)row / seg;
            for (int col = 0; col < vpr; col++) {
                float u = (float)col / seg;
                float angle = u * 2f * (float)Math.PI;
                float r = t;
                verts[vi++] = r * (float)Math.cos(angle);
                verts[vi++] = 0f;
                verts[vi++] = r * (float)Math.sin(angle);
            }
        }
        for (int row = 0; row < seg; row++) {
            for (int col = 0; col < seg; col++) {
                int a = row * vpr + col, b = row * vpr + col + 1;
                int c = (row + 1) * vpr + col, d = (row + 1) * vpr + col + 1;
                idx[ii++] = (short)a; idx[ii++] = (short)c; idx[ii++] = (short)b;
                idx[ii++] = (short)b; idx[ii++] = (short)c; idx[ii++] = (short)d;
            }
        }
        waterIndices = ii;

        waterVertexBuffer = ByteBuffer.allocateDirect(verts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        waterVertexBuffer.put(verts).position(0);
        waterIndexBuffer = ByteBuffer.allocateDirect(idx.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        waterIndexBuffer.put(idx).position(0);
    }

    public void update(float dt) {
        time += dt;

        float[] cr = new float[3], cu = new float[3];
        camera.getRightUp(cr, cu);
        right = cr; up = cu;

        sprayParticles.update(dt);
        overflowParticles.update(dt);

        float nozzleY = bowls.get(0).getRimY() + 0.3f;

        if (time > 0.5f) {
            sprayParticles.emitSpray(0f, nozzleY, 0f, 0.25f, 3.5f, 3);
        }

        for (int i = 0; i < bowls.size(); i++) {
            Bowl b = bowls.get(i);
            float fillRate = 0.02f * dt * 60f;
            if (i == 0) {
                b.waterLevel = Math.min(b.maxWaterLevel, b.waterLevel + fillRate * 2f);
            }
            if (b.waterLevel > b.maxWaterLevel * 0.9f && i < bowls.size() - 1) {
                float overflow = (b.waterLevel - b.maxWaterLevel * 0.9f) * 0.02f;
                b.waterLevel -= overflow;
                bowls.get(i + 1).waterLevel += overflow * 0.8f;
                overflowParticles.emitOverflow(0, 0, b.getInnerRimY(), b.topRadius * 0.85f, 2);
            }
            b.waterLevel = Math.max(0, Math.min(b.maxWaterLevel, b.waterLevel));
        }

        float drainRate = 0.002f * dt * 60f;
        bowls.get(bowls.size() - 1).waterLevel = Math.max(0, bowls.get(bowls.size() - 1).waterLevel - drainRate);

        if (Math.random() < 0.02) {
            sprayParticles.emitSpray(0f, nozzleY, 0f, 0.15f, 4.0f, 5);
        }
    }

    public void renderBowls(int program,
                            int posHandle, int normHandle, int colorUniform,
                            int mvpHandle, int mvHandle, int lightUniform) {
        GLES20.glUseProgram(program);
        float[] vpMatrix = camera.getVPMatrix();
        float[] viewMatrix = camera.getViewMatrix();
        GLES20.glUniform3fv(lightUniform, 1, lightDir, 0);

        for (Bowl b : bowls) {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);
            b.render(posHandle, normHandle, colorUniform, mvpMatrix, mvMatrix, mvpHandle, mvHandle);
        }

        // Pillar
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, vpMatrix, 0);
        GLES20.glUniformMatrix4fv(mvHandle, 1, false, viewMatrix, 0);
        GLES20.glUniform4f(colorUniform, 0.7f, 0.7f, 0.73f, 1.0f);

        pillarVertBuf.position(0);
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, pillarVertBuf);
        GLES20.glEnableVertexAttribArray(posHandle);
        pillarNormBuf.position(0);
        GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, 0, pillarNormBuf);
        GLES20.glEnableVertexAttribArray(normHandle);
        pillarIdxBuf.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, pillarIdxCount, GLES20.GL_UNSIGNED_SHORT, pillarIdxBuf);
    }

    public void renderWater(int program,
                            int posHandle, int colorUniform, int mvpHandle) {
        GLES20.glUseProgram(program);
        float[] vpMatrix = camera.getVPMatrix();
        float[] viewMatrix = camera.getViewMatrix();

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthMask(false);

        for (Bowl b : bowls) {
            if (b.waterLevel <= 0.01f) continue;

            float waterY = b.yBase + b.waterLevel;
            float waterR = b.topRadius * (float)Math.sqrt(b.waterLevel / b.maxWaterLevel);

            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.translateM(modelMatrix, 0, 0, waterY, 0);
            Matrix.scaleM(modelMatrix, 0, waterR, 1, waterR);
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0);
            GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);

            float wave = (float)Math.sin(time * 0.5f + b.yBase) * 0.01f;
            float alpha = 0.35f + 0.1f * (b.waterLevel / b.maxWaterLevel);
            GLES20.glUniform4f(colorUniform, 0.3f + wave, 0.55f + wave, 0.8f, alpha);

            waterVertexBuffer.position(0);
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, waterVertexBuffer);
            GLES20.glEnableVertexAttribArray(posHandle);
            waterIndexBuffer.position(0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, waterIndices, GLES20.GL_UNSIGNED_SHORT, waterIndexBuffer);
        }

        GLES20.glDepthMask(true);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    public void renderParticles(int program,
                                int posHandle, int cornerHandle,
                                int sizeHandle, int alphaHandle,
                                int mvpHandle, int colorUniform,
                                int camRightHandle, int camUpHandle) {
        GLES20.glUseProgram(program);
        float[] vpMatrix = camera.getVPMatrix();

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthMask(false);

        GLES20.glUniform3fv(camRightHandle, 1, right, 0);
        GLES20.glUniform3fv(camUpHandle, 1, up, 0);

        sprayParticles.render(posHandle, cornerHandle, sizeHandle, alphaHandle,
                              mvpHandle, colorUniform, vpMatrix, right, up,
                              0.6f, 0.8f, 1.0f, 0.6f);

        overflowParticles.render(posHandle, cornerHandle, sizeHandle, alphaHandle,
                                 mvpHandle, colorUniform, vpMatrix, right, up,
                                 0.5f, 0.7f, 1.0f, 0.5f);

        GLES20.glDepthMask(true);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    public Camera getCamera() { return camera; }
}
