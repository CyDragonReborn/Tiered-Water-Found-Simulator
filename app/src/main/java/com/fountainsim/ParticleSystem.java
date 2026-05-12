package com.fountainsim;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import android.opengl.GLES20;

public class ParticleSystem {
    private static class Particle {
        float x, y, z;
        float vx, vy, vz;
        float life, maxLife;
        float size;
    }

    private ArrayList<Particle> particles = new ArrayList<>();
    private float[] vertData;
    private FloatBuffer vertexBuffer;
    private int maxParticles = 800;
    private float gravity = -2.5f;

    public ParticleSystem() {
        vertData = new float[maxParticles * 6 * 7];
        vertexBuffer = ByteBuffer.allocateDirect(vertData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public void emitSpray(float cx, float cy, float cz, float spread, float speed, int count) {
        for (int i = 0; i < count && particles.size() < maxParticles; i++) {
            Particle p = new Particle();
            p.x = cx + (float)(Math.random() - 0.5) * 0.05f;
            p.y = cy;
            p.z = cz + (float)(Math.random() - 0.5) * 0.05f;
            float angle = (float)(Math.random() * 2 * Math.PI);
            float spreadAmt = (float)(Math.random() * spread);
            p.vx = (float)Math.cos(angle) * spreadAmt;
            float vUp = speed * (0.7f + (float)Math.random() * 0.3f);
            p.vy = vUp;
            p.vz = (float)Math.sin(angle) * spreadAmt;
            float life = 1.5f + (float)Math.random() * 1.0f;
            p.life = life; p.maxLife = life;
            p.size = 0.06f + (float)Math.random() * 0.04f;
            particles.add(p);
        }
    }

    public void emitOverflow(float cx, float cz, float rimY, float spreadR, int count) {
        for (int i = 0; i < count && particles.size() < maxParticles; i++) {
            Particle p = new Particle();
            float angle = (float)(Math.random() * 2 * Math.PI);
            float r = spreadR * (0.9f + (float)Math.random() * 0.1f);
            p.x = cx + r * (float)Math.cos(angle);
            p.y = rimY;
            p.z = cz + r * (float)Math.sin(angle);
            p.vx = (float)(Math.random() - 0.5) * 0.1f;
            p.vy = -(0.3f + (float)Math.random() * 0.4f);
            p.vz = (float)(Math.random() - 0.5) * 0.1f;
            float life = 1.8f + (float)Math.random() * 1.2f;
            p.life = life; p.maxLife = life;
            p.size = 0.03f + (float)Math.random() * 0.03f;
            particles.add(p);
        }
    }

    public void update(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                particles.remove(i);
                continue;
            }
            p.vx *= 0.99f;
            p.vy += gravity * dt;
            p.vz *= 0.99f;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.z += p.vz * dt;
            if (p.y < -0.5f) {
                p.vy = Math.abs(p.vy) * 0.3f;
                p.y = -0.5f;
                if (Math.abs(p.vy) < 0.1f) p.life = 0;
            }
        }
    }

    public void render(int posHandle, int cornerHandle, int sizeHandle,
                       int alphaHandle, int mvpHandle, int colorUniform,
                       float[] vpMatrix, float[] right, float[] up,
                       float r, float g, float b, float a) {
        int count = particles.size();
        if (count == 0) return;

        int vi = 0;
        float hs;
        for (Particle p : particles) {
            float lifeRatio = p.life / p.maxLife;
            float alpha = lifeRatio * a;
            hs = p.size * 0.5f;

            float rx = right[0]*hs, ry = right[1]*hs, rz = right[2]*hs;
            float ux = up[0]*hs, uy = up[1]*hs, uz = up[2]*hs;

            float px = p.x, py = p.y, pz = p.z;

            // Triangle 1
            vertData[vi++] = px - rx - ux; vertData[vi++] = py - ry - uy; vertData[vi++] = pz - rz - uz;
            vertData[vi++] = -1; vertData[vi++] = -1; vertData[vi++] = p.size; vertData[vi++] = alpha;

            vertData[vi++] = px + rx - ux; vertData[vi++] = py + ry - uy; vertData[vi++] = pz + rz - uz;
            vertData[vi++] = 1; vertData[vi++] = -1; vertData[vi++] = p.size; vertData[vi++] = alpha;

            vertData[vi++] = px - rx + ux; vertData[vi++] = py - ry + uy; vertData[vi++] = pz - rz + uz;
            vertData[vi++] = -1; vertData[vi++] = 1; vertData[vi++] = p.size; vertData[vi++] = alpha;

            // Triangle 2
            vertData[vi++] = px + rx - ux; vertData[vi++] = py + ry - uy; vertData[vi++] = pz + rz - uz;
            vertData[vi++] = 1; vertData[vi++] = -1; vertData[vi++] = p.size; vertData[vi++] = alpha;

            vertData[vi++] = px - rx + ux; vertData[vi++] = py - ry + uy; vertData[vi++] = pz - rz + uz;
            vertData[vi++] = -1; vertData[vi++] = 1; vertData[vi++] = p.size; vertData[vi++] = alpha;

            vertData[vi++] = px + rx + ux; vertData[vi++] = py + ry + uy; vertData[vi++] = pz + rz + uz;
            vertData[vi++] = 1; vertData[vi++] = 1; vertData[vi++] = p.size; vertData[vi++] = alpha;
        }

        vertexBuffer.clear();
        vertexBuffer.put(vertData, 0, count * 7 * 6);
        vertexBuffer.position(0);

        GLES20.glUniform4f(colorUniform, r, g, b, a);

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 28, vertexBuffer);
        GLES20.glEnableVertexAttribArray(posHandle);

        vertexBuffer.position(3);
        GLES20.glVertexAttribPointer(cornerHandle, 2, GLES20.GL_FLOAT, false, 28, vertexBuffer);
        GLES20.glEnableVertexAttribArray(cornerHandle);

        vertexBuffer.position(5);
        GLES20.glVertexAttribPointer(sizeHandle, 1, GLES20.GL_FLOAT, false, 28, vertexBuffer);
        GLES20.glEnableVertexAttribArray(sizeHandle);

        vertexBuffer.position(6);
        GLES20.glVertexAttribPointer(alphaHandle, 1, GLES20.GL_FLOAT, false, 28, vertexBuffer);
        GLES20.glEnableVertexAttribArray(alphaHandle);

        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, vpMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count * 6);
    }

    public int getCount() { return particles.size(); }
    public void clear() { particles.clear(); }
}
