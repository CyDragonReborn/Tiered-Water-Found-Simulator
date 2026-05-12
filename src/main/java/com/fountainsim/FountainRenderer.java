package com.fountainsim;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class FountainRenderer implements GLSurfaceView.Renderer {
    private FountainScene scene;
    private Camera camera;
    private WaterSoundManager soundManager;
    private long lastTime = System.nanoTime();

    private int bowlProgram, particleProgram, waterProgram;

    private int bowlPosHandle, bowlNormHandle, bowlColorUniform;
    private int bowlMVPHandle, bowlMVHandle, bowlLightUniform;

    private int particlePosHandle, particleCornerHandle;
    private int particleSizeHandle, particleAlphaHandle;
    private int particleMVPHandle, particleColorUniform;
    private int particleCamRightHandle, particleCamUpHandle;

    private int waterPosHandle, waterColorUniform, waterMVPHandle;

    private static final String BOWL_VERT =
        "uniform mat4 uMVPMatrix;\n" +
        "uniform mat4 uMVMatrix;\n" +
        "uniform vec3 uLightDir;\n" +
        "attribute vec4 aPosition;\n" +
        "attribute vec3 aNormal;\n" +
        "varying float vLighting;\n" +
        "void main() {\n" +
        "  gl_Position = uMVPMatrix * aPosition;\n" +
        "  vec3 n = normalize(mat3(uMVMatrix) * aNormal);\n" +
        "  vLighting = max(0.25, dot(n, normalize(uLightDir)));\n" +
        "}";

    private static final String BOWL_FRAG =
        "precision mediump float;\n" +
        "uniform vec4 uColor;\n" +
        "varying float vLighting;\n" +
        "void main() {\n" +
        "  gl_FragColor = vec4(uColor.rgb * vLighting, uColor.a);\n" +
        "}";

    private static final String PARTICLE_VERT =
        "uniform mat4 uMVPMatrix;\n" +
        "uniform vec3 uCamRight;\n" +
        "uniform vec3 uCamUp;\n" +
        "attribute vec3 aPosition;\n" +
        "attribute vec2 aCorner;\n" +
        "attribute float aSize;\n" +
        "attribute float aAlpha;\n" +
        "varying float vAlpha;\n" +
        "varying vec2 vCorner;\n" +
        "void main() {\n" +
        "  vec3 pos = aPosition + uCamRight * aCorner.x * aSize + uCamUp * aCorner.y * aSize;\n" +
        "  gl_Position = uMVPMatrix * vec4(pos, 1.0);\n" +
        "  vAlpha = aAlpha;\n" +
        "  vCorner = aCorner;\n" +
        "}";

    private static final String PARTICLE_FRAG =
        "precision mediump float;\n" +
        "uniform vec4 uColor;\n" +
        "varying float vAlpha;\n" +
        "varying vec2 vCorner;\n" +
        "void main() {\n" +
        "  float d = length(vCorner);\n" +
        "  if (d > 1.0) discard;\n" +
        "  float a = smoothstep(1.0, 0.0, d) * vAlpha;\n" +
        "  gl_FragColor = vec4(uColor.rgb, uColor.a * a);\n" +
        "}";

    private static final String WATER_VERT =
        "uniform mat4 uMVPMatrix;\n" +
        "attribute vec4 aPosition;\n" +
        "void main() {\n" +
        "  gl_Position = uMVPMatrix * aPosition;\n" +
        "}";

    private static final String WATER_FRAG =
        "precision mediump float;\n" +
        "uniform vec4 uColor;\n" +
        "void main() {\n" +
        "  gl_FragColor = uColor;\n" +
        "}";

    public FountainRenderer(WaterSoundManager soundManager) {
        this.soundManager = soundManager;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.08f, 0.10f, 0.12f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        bowlProgram = createProgram(BOWL_VERT, BOWL_FRAG);
        particleProgram = createProgram(PARTICLE_VERT, PARTICLE_FRAG);
        waterProgram = createProgram(WATER_VERT, WATER_FRAG);

        bowlPosHandle = GLES20.glGetAttribLocation(bowlProgram, "aPosition");
        bowlNormHandle = GLES20.glGetAttribLocation(bowlProgram, "aNormal");
        bowlColorUniform = GLES20.glGetUniformLocation(bowlProgram, "uColor");
        bowlMVPHandle = GLES20.glGetUniformLocation(bowlProgram, "uMVPMatrix");
        bowlMVHandle = GLES20.glGetUniformLocation(bowlProgram, "uMVMatrix");
        bowlLightUniform = GLES20.glGetUniformLocation(bowlProgram, "uLightDir");

        particlePosHandle = GLES20.glGetAttribLocation(particleProgram, "aPosition");
        particleCornerHandle = GLES20.glGetAttribLocation(particleProgram, "aCorner");
        particleSizeHandle = GLES20.glGetAttribLocation(particleProgram, "aSize");
        particleAlphaHandle = GLES20.glGetAttribLocation(particleProgram, "aAlpha");
        particleMVPHandle = GLES20.glGetUniformLocation(particleProgram, "uMVPMatrix");
        particleColorUniform = GLES20.glGetUniformLocation(particleProgram, "uColor");
        particleCamRightHandle = GLES20.glGetUniformLocation(particleProgram, "uCamRight");
        particleCamUpHandle = GLES20.glGetUniformLocation(particleProgram, "uCamUp");

        waterPosHandle = GLES20.glGetAttribLocation(waterProgram, "aPosition");
        waterColorUniform = GLES20.glGetUniformLocation(waterProgram, "uColor");
        waterMVPHandle = GLES20.glGetUniformLocation(waterProgram, "uMVPMatrix");

        camera = new Camera();
        scene = new FountainScene(camera);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float aspect = (float) width / (float) height;
        camera.setProjection(45f, aspect, 0.1f, 50f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = (now - lastTime) / 1e9f;
        lastTime = now;
        dt = Math.min(dt, 0.05f);

        scene.update(dt);
        camera.updateViewMatrix();

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        scene.renderBowls(bowlProgram,
            bowlPosHandle, bowlNormHandle, bowlColorUniform,
            bowlMVPHandle, bowlMVHandle, bowlLightUniform);

        scene.renderWater(waterProgram,
            waterPosHandle, waterColorUniform, waterMVPHandle);

        scene.renderParticles(particleProgram,
            particlePosHandle, particleCornerHandle,
            particleSizeHandle, particleAlphaHandle,
            particleMVPHandle, particleColorUniform,
            particleCamRightHandle, particleCamUpHandle);
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        return program;
    }

    private int loadShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public FountainScene getScene() { return scene; }
}
