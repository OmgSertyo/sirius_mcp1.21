package net.optifine.shaders.uniform;

import org.lwjgl.opengl.GL20;

public class ShaderUniform2f extends ShaderUniformBase {
    private float[][] programValues;
    private static final float VALUE_UNKNOWN = -Float.MAX_VALUE;

    public ShaderUniform2f(String name) {
        super(name);
        this.resetValue();
    }

    public void setValue(float v0, float v1) {
        int i = this.getProgram();
        float[] afloat = this.programValues[i];
        if (afloat[0] != v0 || afloat[1] != v1) {
            afloat[0] = v0;
            afloat[1] = v1;
            int j = this.getLocation();
            if (j >= 0) {
                flushRenderBuffers();
                GL20.glUniform2f(j, v0, v1);
                this.checkGLError();
            }
        }
    }

    public float[] getValue() {
        int i = this.getProgram();
        return this.programValues[i];
    }

    @Override
    protected void onProgramSet(int program) {
        if (program >= this.programValues.length) {
            float[][] afloat = this.programValues;
            float[][] afloat1 = new float[program + 10][];
            System.arraycopy(afloat, 0, afloat1, 0, afloat.length);
            this.programValues = afloat1;
        }

        if (this.programValues[program] == null) {
            this.programValues[program] = new float[]{-Float.MAX_VALUE, -Float.MAX_VALUE};
        }
    }

    @Override
    protected void resetValue() {
        this.programValues = new float[][]{{-Float.MAX_VALUE, -Float.MAX_VALUE}};
    }
}