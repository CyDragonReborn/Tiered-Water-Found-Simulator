package com.fountainsim;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import android.opengl.GLES20;

public class Bowl {
    private FloatBuffer vertexBuffer, normalBuffer;
    private ShortBuffer indexBuffer;
    private int numIndices;

    public float yBase, topRadius, depth, rimHeight, wallThickness;
    public float waterLevel = 0.0f;
    public float maxWaterLevel;
    private float[] color = {0.72f, 0.72f, 0.75f, 1.0f};

    private static final int SEG = 36;

    public Bowl(float yBase, float topRadius, float depth, float rimHeight) {
        this.yBase = yBase;
        this.topRadius = topRadius;
        this.depth = depth;
        this.rimHeight = rimHeight;
        this.wallThickness = topRadius * 0.07f;
        this.maxWaterLevel = depth * 0.85f;
        buildMesh();
    }

    public void setColor(float r, float g, float b) {
        color[0] = r; color[1] = g; color[2] = b;
    }

    public float getRimY() { return yBase + depth + rimHeight; }
    public float getInnerRimY() { return yBase + depth; }

    private void buildMesh() {
        int vpr = SEG + 1;
        int innerRings = 8;

        int iVerts = (innerRings + 1) * vpr;
        int oVerts = vpr * 2;
        int sVerts = vpr * 2 * 2;
        int rVerts = vpr * 2;

        int totalVerts = iVerts + oVerts + sVerts + rVerts;
        int iIdx = innerRings * SEG * 6;
        int oIdx = SEG * 6;
        int sIdx = SEG * 6 * 2;
        int rIdx = SEG * 6;
        numIndices = iIdx + oIdx + sIdx + rIdx;

        float[] verts = new float[totalVerts * 3];
        float[] norms = new float[totalVerts * 3];
        short[] idx = new short[numIndices];
        int vo = 0, no = 0, io = 0;

        float obR = topRadius * 0.65f;
        float obY = yBase - 0.05f;
        float rimY = yBase + depth;
        float rw = wallThickness;

        // --- Inner basin (parabolic dish) ---
        for (int ring = 0; ring <= innerRings; ring++) {
            float t = (float)ring / innerRings;
            float r = topRadius * (0.12f + t * 0.88f);
            float y = yBase + depth * t * t;
            for (int seg = 0; seg < vpr; seg++) {
                float a = (float)seg / SEG * 2f * (float)Math.PI;
                float x = r * (float)Math.cos(a);
                float z = r * (float)Math.sin(a);
                verts[vo++] = x; verts[vo++] = y; verts[vo++] = z;
                float tR2 = topRadius * topRadius;
                float dx = 2f * depth * x / tR2;
                float dz = 2f * depth * z / tR2;
                float len = (float)Math.sqrt(dx*dx + 1 + dz*dz);
                norms[no++] = -dx/len; norms[no++] = 1f/len; norms[no++] = -dz/len;
            }
        }
        for (int ring = 0; ring < innerRings; ring++) {
            for (int seg = 0; seg < SEG; seg++) {
                int a = ring * vpr + seg, b = ring * vpr + seg + 1;
                int c = (ring+1) * vpr + seg, d = (ring+1) * vpr + seg + 1;
                idx[io++] = (short)a; idx[io++] = (short)c; idx[io++] = (short)b;
                idx[io++] = (short)b; idx[io++] = (short)c; idx[io++] = (short)d;
            }
        }

        // --- Outer wall ---
        int outerStart = iVerts;
        for (int pass = 0; pass < 2; pass++) {
            float r = (pass == 0) ? topRadius : obR;
            float y = (pass == 0) ? rimY : obY;
            for (int seg = 0; seg < vpr; seg++) {
                float a = (float)seg / SEG * 2f * (float)Math.PI;
                verts[vo++] = r * (float)Math.cos(a);
                verts[vo++] = y;
                verts[vo++] = r * (float)Math.sin(a);
                float nx = (float)Math.cos(a);
                float nz = (float)Math.sin(a);
                float ny = -0.25f;
                float len = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
                norms[no++] = nx/len; norms[no++] = ny/len; norms[no++] = nz/len;
            }
        }
        for (int seg = 0; seg < SEG; seg++) {
            int a = outerStart + seg, b = outerStart + seg + 1;
            int c = outerStart + vpr + seg, d = outerStart + vpr + seg + 1;
            idx[io++] = (short)a; idx[io++] = (short)c; idx[io++] = (short)b;
            idx[io++] = (short)b; idx[io++] = (short)c; idx[io++] = (short)d;
        }

        // --- Rim sides (vertical bands connecting bowl to rim top) ---
        int sideStart = iVerts + oVerts;
        float rimTopY = rimY + rimHeight;
        for (int side = 0; side < 2; side++) {
            float r = (side == 0) ? topRadius : (topRadius + rw);
            float nx = (side == 0) ? -1f : 1f;
            for (int pass = 0; pass < 2; pass++) {
                float y = (pass == 0) ? rimY : rimTopY;
                for (int seg = 0; seg < vpr; seg++) {
                    float a = (float)seg / SEG * 2f * (float)Math.PI;
                    verts[vo++] = r * (float)Math.cos(a);
                    verts[vo++] = y;
                    verts[vo++] = r * (float)Math.sin(a);
                    float cn = (float)Math.cos(a);
                    float sn = (float)Math.sin(a);
                    float len = (float)Math.sqrt(cn*cn + sn*sn);
                    norms[no++] = cn * nx / len;
                    norms[no++] = 0;
                    norms[no++] = sn * nx / len;
                }
            }
            int base = sideStart + side * vpr * 2;
            for (int seg = 0; seg < SEG; seg++) {
                int a = base + seg, b = base + seg + 1;
                int c = base + vpr + seg, d = base + vpr + seg + 1;
                idx[io++] = (short)a; idx[io++] = (short)c; idx[io++] = (short)b;
                idx[io++] = (short)b; idx[io++] = (short)c; idx[io++] = (short)d;
            }
        }

        // --- Rim top ---
        int rimStart = iVerts + oVerts + sVerts;
        for (int pass = 0; pass < 2; pass++) {
            float r = (pass == 0) ? topRadius : (topRadius + rw);
            for (int seg = 0; seg < vpr; seg++) {
                float a = (float)seg / SEG * 2f * (float)Math.PI;
                verts[vo++] = r * (float)Math.cos(a);
                verts[vo++] = rimTopY;
                verts[vo++] = r * (float)Math.sin(a);
                norms[no++] = 0; norms[no++] = 1; norms[no++] = 0;
            }
        }
        for (int seg = 0; seg < SEG; seg++) {
            int a = rimStart + seg, b = rimStart + seg + 1;
            int c = rimStart + vpr + seg, d = rimStart + vpr + seg + 1;
            idx[io++] = (short)a; idx[io++] = (short)c; idx[io++] = (short)b;
            idx[io++] = (short)b; idx[io++] = (short)c; idx[io++] = (short)d;
        }

        vertexBuffer = ByteBuffer.allocateDirect(verts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(verts).position(0);
        normalBuffer = ByteBuffer.allocateDirect(norms.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        normalBuffer.put(norms).position(0);
        indexBuffer = ByteBuffer.allocateDirect(idx.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        indexBuffer.put(idx).position(0);
    }

    public void render(int posHandle, int normHandle, int colorUniform,
                       float[] mvpMatrix, float[] mvMatrix, int mvpHandle, int mvHandle) {
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(mvHandle, 1, false, mvMatrix, 0);

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(posHandle);

        normalBuffer.position(0);
        GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);
        GLES20.glEnableVertexAttribArray(normHandle);

        GLES20.glUniform4fv(colorUniform, 1, color, 0);
        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, numIndices, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
    }
}
