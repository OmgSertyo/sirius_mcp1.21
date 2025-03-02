package net.optifine.shaders;

import org.lwjgl.opengl.GL20;

public class ShaderProgramData {
    public int programIDGL;
    public int uniform_texture;
    public int uniform_lightmap;
    public int uniform_normals;
    public int uniform_specular;
    public int uniform_shadow;
    public int uniform_watershadow;
    public int uniform_shadowtex0;
    public int uniform_shadowtex1;
    public int uniform_depthtex0;
    public int uniform_depthtex1;
    public int uniform_shadowcolor;
    public int uniform_shadowcolor0;
    public int uniform_shadowcolor1;
    public int uniform_noisetex;
    public int uniform_gcolor;
    public int uniform_gdepth;
    public int uniform_gnormal;
    public int uniform_composite;
    public int uniform_gaux1;
    public int uniform_gaux2;
    public int uniform_gaux3;
    public int uniform_gaux4;
    public int uniform_colortex0;
    public int uniform_colortex1;
    public int uniform_colortex2;
    public int uniform_colortex3;
    public int uniform_colortex4;
    public int uniform_colortex5;
    public int uniform_colortex6;
    public int uniform_colortex7;
    public int uniform_gdepthtex;
    public int uniform_depthtex2;
    public int uniform_tex;
    public int uniform_heldItemId;
    public int uniform_heldBlockLightValue;
    public int uniform_fogMode;
    public int uniform_fogColor;
    public int uniform_skyColor;
    public int uniform_worldTime;
    public int uniform_moonPhase;
    public int uniform_frameTimeCounter;
    public int uniform_sunAngle;
    public int uniform_shadowAngle;
    public int uniform_rainStrength;
    public int uniform_aspectRatio;
    public int uniform_viewWidth;
    public int uniform_viewHeight;
    public int uniform_near;
    public int uniform_far;
    public int uniform_sunPosition;
    public int uniform_moonPosition;
    public int uniform_upPosition;
    public int uniform_previousCameraPosition;
    public int uniform_cameraPosition;
    public int uniform_gbufferModelView;
    public int uniform_gbufferModelViewInverse;
    public int uniform_gbufferPreviousProjection;
    public int uniform_gbufferProjection;
    public int uniform_gbufferProjectionInverse;
    public int uniform_gbufferPreviousModelView;
    public int uniform_shadowProjection;
    public int uniform_shadowProjectionInverse;
    public int uniform_shadowModelView;
    public int uniform_shadowModelViewInverse;
    public int uniform_wetness;
    public int uniform_eyeAltitude;
    public int uniform_eyeBrightness;
    public int uniform_eyeBrightnessSmooth;
    public int uniform_terrainTextureSize;
    public int uniform_terrainIconSize;
    public int uniform_isEyeInWater;
    public int uniform_hideGUI;
    public int uniform_centerDepthSmooth;
    public int uniform_atlasSize;

    public ShaderProgramData(int programID) {
        this.programIDGL = programID;
        this.uniform_texture = GL20.glGetUniformLocation(programID, "texture");
        this.uniform_lightmap = GL20.glGetUniformLocation(programID, "lightmap");
        this.uniform_normals = GL20.glGetUniformLocation(programID, "normals");
        this.uniform_specular = GL20.glGetUniformLocation(programID, "specular");
        this.uniform_shadow = GL20.glGetUniformLocation(programID, "shadow");
        this.uniform_watershadow = GL20.glGetUniformLocation(programID, "watershadow");
        this.uniform_shadowtex0 = GL20.glGetUniformLocation(programID, "shadowtex0");
        this.uniform_shadowtex1 = GL20.glGetUniformLocation(programID, "shadowtex1");
        this.uniform_depthtex0 = GL20.glGetUniformLocation(programID, "depthtex0");
        this.uniform_depthtex1 = GL20.glGetUniformLocation(programID, "depthtex1");
        this.uniform_shadowcolor = GL20.glGetUniformLocation(programID, "shadowcolor");
        this.uniform_shadowcolor0 = GL20.glGetUniformLocation(programID, "shadowcolor0");
        this.uniform_shadowcolor1 = GL20.glGetUniformLocation(programID, "shadowcolor1");
        this.uniform_noisetex = GL20.glGetUniformLocation(programID, "noisetex");
        this.uniform_gcolor = GL20.glGetUniformLocation(programID, "gcolor");
        this.uniform_gdepth = GL20.glGetUniformLocation(programID, "gdepth");
        this.uniform_gnormal = GL20.glGetUniformLocation(programID, "gnormal");
        this.uniform_composite = GL20.glGetUniformLocation(programID, "composite");
        this.uniform_gaux1 = GL20.glGetUniformLocation(programID, "gaux1");
        this.uniform_gaux2 = GL20.glGetUniformLocation(programID, "gaux2");
        this.uniform_gaux3 = GL20.glGetUniformLocation(programID, "gaux3");
        this.uniform_gaux4 = GL20.glGetUniformLocation(programID, "gaux4");
        this.uniform_colortex0 = GL20.glGetUniformLocation(programID, "colortex0");
        this.uniform_colortex1 = GL20.glGetUniformLocation(programID, "colortex1");
        this.uniform_colortex2 = GL20.glGetUniformLocation(programID, "colortex2");
        this.uniform_colortex3 = GL20.glGetUniformLocation(programID, "colortex3");
        this.uniform_colortex4 = GL20.glGetUniformLocation(programID, "colortex4");
        this.uniform_colortex5 = GL20.glGetUniformLocation(programID, "colortex5");
        this.uniform_colortex6 = GL20.glGetUniformLocation(programID, "colortex6");
        this.uniform_colortex7 = GL20.glGetUniformLocation(programID, "colortex7");
        this.uniform_gdepthtex = GL20.glGetUniformLocation(programID, "gdepthtex");
        this.uniform_depthtex2 = GL20.glGetUniformLocation(programID, "depthtex2");
        this.uniform_tex = GL20.glGetUniformLocation(programID, "tex");
        this.uniform_heldItemId = GL20.glGetUniformLocation(programID, "heldItemId");
        this.uniform_heldBlockLightValue = GL20.glGetUniformLocation(programID, "heldBlockLightValue");
        this.uniform_fogMode = GL20.glGetUniformLocation(programID, "fogMode");
        this.uniform_fogColor = GL20.glGetUniformLocation(programID, "fogColor");
        this.uniform_skyColor = GL20.glGetUniformLocation(programID, "skyColor");
        this.uniform_worldTime = GL20.glGetUniformLocation(programID, "worldTime");
        this.uniform_moonPhase = GL20.glGetUniformLocation(programID, "moonPhase");
        this.uniform_frameTimeCounter = GL20.glGetUniformLocation(programID, "frameTimeCounter");
        this.uniform_sunAngle = GL20.glGetUniformLocation(programID, "sunAngle");
        this.uniform_shadowAngle = GL20.glGetUniformLocation(programID, "shadowAngle");
        this.uniform_rainStrength = GL20.glGetUniformLocation(programID, "rainStrength");
        this.uniform_aspectRatio = GL20.glGetUniformLocation(programID, "aspectRatio");
        this.uniform_viewWidth = GL20.glGetUniformLocation(programID, "viewWidth");
        this.uniform_viewHeight = GL20.glGetUniformLocation(programID, "viewHeight");
        this.uniform_near = GL20.glGetUniformLocation(programID, "near");
        this.uniform_far = GL20.glGetUniformLocation(programID, "far");
        this.uniform_sunPosition = GL20.glGetUniformLocation(programID, "sunPosition");
        this.uniform_moonPosition = GL20.glGetUniformLocation(programID, "moonPosition");
        this.uniform_upPosition = GL20.glGetUniformLocation(programID, "upPosition");
        this.uniform_previousCameraPosition = GL20.glGetUniformLocation(programID, "previousCameraPosition");
        this.uniform_cameraPosition = GL20.glGetUniformLocation(programID, "cameraPosition");
        this.uniform_gbufferModelView = GL20.glGetUniformLocation(programID, "gbufferModelView");
        this.uniform_gbufferModelViewInverse = GL20.glGetUniformLocation(programID, "gbufferModelViewInverse");
        this.uniform_gbufferPreviousProjection = GL20.glGetUniformLocation(programID, "gbufferPreviousProjection");
        this.uniform_gbufferProjection = GL20.glGetUniformLocation(programID, "gbufferProjection");
        this.uniform_gbufferProjectionInverse = GL20.glGetUniformLocation(programID, "gbufferProjectionInverse");
        this.uniform_gbufferPreviousModelView = GL20.glGetUniformLocation(programID, "gbufferPreviousModelView");
        this.uniform_shadowProjection = GL20.glGetUniformLocation(programID, "shadowProjection");
        this.uniform_shadowProjectionInverse = GL20.glGetUniformLocation(programID, "shadowProjectionInverse");
        this.uniform_shadowModelView = GL20.glGetUniformLocation(programID, "shadowModelView");
        this.uniform_shadowModelViewInverse = GL20.glGetUniformLocation(programID, "shadowModelViewInverse");
        this.uniform_wetness = GL20.glGetUniformLocation(programID, "wetness");
        this.uniform_eyeAltitude = GL20.glGetUniformLocation(programID, "eyeAltitude");
        this.uniform_eyeBrightness = GL20.glGetUniformLocation(programID, "eyeBrightness");
        this.uniform_eyeBrightnessSmooth = GL20.glGetUniformLocation(programID, "eyeBrightnessSmooth");
        this.uniform_terrainTextureSize = GL20.glGetUniformLocation(programID, "terrainTextureSize");
        this.uniform_terrainIconSize = GL20.glGetUniformLocation(programID, "terrainIconSize");
        this.uniform_isEyeInWater = GL20.glGetUniformLocation(programID, "isEyeInWater");
        this.uniform_hideGUI = GL20.glGetUniformLocation(programID, "hideGUI");
        this.uniform_centerDepthSmooth = GL20.glGetUniformLocation(programID, "centerDepthSmooth");
        this.uniform_atlasSize = GL20.glGetUniformLocation(programID, "atlasSize");
    }
}