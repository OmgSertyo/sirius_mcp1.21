package net.minecraft.client.model.geom;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.optifine.Config;
import net.optifine.IRandomEntity;
import net.optifine.RandomEntities;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.entity.model.anim.ModelUpdater;
import net.optifine.model.Attachment;
import net.optifine.model.AttachmentPath;
import net.optifine.model.AttachmentPaths;
import net.optifine.model.AttachmentType;
import net.optifine.model.ModelSprite;
import net.optifine.render.BoxVertexPositions;
import net.optifine.render.RenderPositions;
import net.optifine.render.VertexPosition;
import net.optifine.shaders.Shaders;
import net.optifine.util.MathUtils;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ModelPart {
    public static final float DEFAULT_SCALE = 1.0F;
    public float x;
    public float y;
    public float z;
    public float xRot;
    public float yRot;
    public float zRot;
    public float xScale = 1.0F;
    public float yScale = 1.0F;
    public float zScale = 1.0F;
    public boolean visible = true;
    public boolean skipDraw;
    public final List<ModelPart.Cube> cubes;
    public final Map<String, ModelPart> children;
    private String name;
    public List<ModelPart> childModelsList;
    public List<ModelSprite> spriteList = new ArrayList<>();
    public boolean mirrorV = false;
    private ResourceLocation textureLocation = null;
    private String id = null;
    private ModelUpdater modelUpdater;
    private LevelRenderer renderGlobal = Config.getRenderGlobal();
    private boolean custom;
    private Attachment[] attachments;
    private AttachmentPaths attachmentPaths;
    private boolean attachmentPathsChecked;
    private ModelPart parent;
    public float textureWidth = 64.0F;
    public float textureHeight = 32.0F;
    public float textureOffsetX;
    public float textureOffsetY;
    public boolean mirror;
    public static final Set<Direction> ALL_VISIBLE = EnumSet.allOf(Direction.class);
    private PartPose initialPose = PartPose.ZERO;

    public ModelPart setTextureOffset(float x, float y) {
        this.textureOffsetX = x;
        this.textureOffsetY = y;
        return this;
    }

    public ModelPart setTextureSize(int textureWidthIn, int textureHeightIn) {
        this.textureWidth = (float)textureWidthIn;
        this.textureHeight = (float)textureHeightIn;
        return this;
    }

    public ModelPart(List<ModelPart.Cube> pCubes, Map<String, ModelPart> pChildren) {
        if (pCubes instanceof ImmutableList) {
            pCubes = new ArrayList<>(pCubes);
        }

        this.cubes = pCubes;
        this.children = pChildren;
        this.childModelsList = new ArrayList<>(this.children.values());

        for (ModelPart modelpart : this.childModelsList) {
            modelpart.setParent(this);
        }
    }

    public PartPose storePose() {
        return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
    }

    public PartPose getInitialPose() {
        return this.initialPose;
    }

    public void setInitialPose(PartPose pInitialPose) {
        this.initialPose = pInitialPose;
    }

    public void resetPose() {
        this.loadPose(this.initialPose);
    }

    public void loadPose(PartPose pPartPose) {
        if (!this.custom) {
            this.x = pPartPose.x;
            this.y = pPartPose.y;
            this.z = pPartPose.z;
            this.xRot = pPartPose.xRot;
            this.yRot = pPartPose.yRot;
            this.zRot = pPartPose.zRot;
            this.xScale = 1.0F;
            this.yScale = 1.0F;
            this.zScale = 1.0F;
        }
    }

    public void copyFrom(ModelPart pModelPart) {
        this.xScale = pModelPart.xScale;
        this.yScale = pModelPart.yScale;
        this.zScale = pModelPart.zScale;
        this.xRot = pModelPart.xRot;
        this.yRot = pModelPart.yRot;
        this.zRot = pModelPart.zRot;
        this.x = pModelPart.x;
        this.y = pModelPart.y;
        this.z = pModelPart.z;
    }

    public boolean hasChild(String pName) {
        return this.children.containsKey(pName);
    }

    public ModelPart getChild(String pName) {
        ModelPart modelpart = this.children.get(pName);
        if (modelpart == null) {
            throw new NoSuchElementException("Can't find part " + pName);
        } else {
            return modelpart;
        }
    }

    public void setPos(float pX, float pY, float pZ) {
        this.x = pX;
        this.y = pY;
        this.z = pZ;
    }

    public void setRotation(float pXRot, float pYRot, float pZRot) {
        this.xRot = pXRot;
        this.yRot = pYRot;
        this.zRot = pZRot;
    }

    public void render(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay) {
        this.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, -1);
    }

    public void render(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, int pColor) {
        this.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, pColor, true);
    }

    public void render(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, int colorIn, boolean updateModel) {
        if (this.visible && (!this.cubes.isEmpty() || !this.children.isEmpty() || !this.spriteList.isEmpty())) {
            RenderType rendertype = null;
            BufferBuilder bufferbuilder = null;
            MultiBufferSource.BufferSource multibuffersource$buffersource = null;
            if (this.textureLocation != null) {
                if (this.renderGlobal.renderOverlayEyes) {
                    return;
                }

                multibuffersource$buffersource = bufferIn.getRenderTypeBuffer();
                if (multibuffersource$buffersource != null) {
                    VertexConsumer vertexconsumer = bufferIn.getSecondaryBuilder();
                    rendertype = multibuffersource$buffersource.getLastRenderType();
                    bufferbuilder = multibuffersource$buffersource.getStartedBuffer(rendertype);
                    bufferIn = multibuffersource$buffersource.getBuffer(this.textureLocation, bufferIn);
                    if (vertexconsumer != null) {
                        bufferIn = VertexMultiConsumer.create(vertexconsumer, bufferIn);
                    }
                }
            }

            if (updateModel && CustomEntityModels.isActive()) {
                this.updateModel();
            }

            matrixStackIn.pushPose();
            this.translateAndRotate(matrixStackIn);
            if (!this.skipDraw) {
                this.compile(matrixStackIn.last(), bufferIn, packedLightIn, packedOverlayIn, colorIn);
            }

            int j = this.childModelsList.size();

            for (int i = 0; i < j; i++) {
                ModelPart modelpart = this.childModelsList.get(i);
                modelpart.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, colorIn, false);
            }

            int k = this.spriteList.size();

            for (int l = 0; l < k; l++) {
                ModelSprite modelsprite = this.spriteList.get(l);
                modelsprite.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, colorIn);
            }

            matrixStackIn.popPose();
            if (multibuffersource$buffersource != null) {
                multibuffersource$buffersource.restoreRenderState(rendertype, bufferbuilder);
            }
        }
    }

    public void visit(PoseStack pPoseStack, ModelPart.Visitor pVisitor) {
        this.visit(pPoseStack, pVisitor, "");
    }

    private void visit(PoseStack pPoseStack, ModelPart.Visitor pVisitor, String pPath) {
        if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
            pPoseStack.pushPose();
            this.translateAndRotate(pPoseStack);
            PoseStack.Pose posestack$pose = pPoseStack.last();

            for (int i = 0; i < this.cubes.size(); i++) {
                pVisitor.visit(posestack$pose, pPath, i, this.cubes.get(i));
            }

            String s = pPath + "/";
            this.children.forEach((nameIn, partIn) -> partIn.visit(pPoseStack, pVisitor, s + nameIn));
            pPoseStack.popPose();
        }
    }

    public void translateAndRotate(PoseStack pPoseStack) {
        pPoseStack.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
            pPoseStack.mulPose(new Quaternionf().rotationZYX(this.zRot, this.yRot, this.xRot));
        }

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            pPoseStack.scale(this.xScale, this.yScale, this.zScale);
        }
    }

    private void compile(PoseStack.Pose pPose, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, int pColor) {
        boolean flag = Config.isShaders() && Shaders.useVelocityAttrib && Config.isMinecraftThread();
        int i = this.cubes.size();

        for (int j = 0; j < i; j++) {
            ModelPart.Cube modelpart$cube = this.cubes.get(j);
            VertexPosition[][] avertexposition = null;
            if (flag) {
                IRandomEntity irandomentity = RandomEntities.getRandomEntityRendered();
                if (irandomentity != null) {
                    avertexposition = modelpart$cube.getBoxVertexPositions(irandomentity.getId());
                }
            }

            modelpart$cube.compile(pPose, pBuffer, pPackedLight, pPackedOverlay, pColor, avertexposition);
        }
    }

    public ModelPart.Cube getRandomCube(RandomSource pRandom) {
        return this.cubes.get(pRandom.nextInt(this.cubes.size()));
    }

    public boolean isEmpty() {
        return this.cubes.isEmpty();
    }

    public void offsetPos(Vector3f pOffset) {
        this.x = this.x + pOffset.x();
        this.y = this.y + pOffset.y();
        this.z = this.z + pOffset.z();
    }

    public void offsetRotation(Vector3f pOffset) {
        this.xRot = this.xRot + pOffset.x();
        this.yRot = this.yRot + pOffset.y();
        this.zRot = this.zRot + pOffset.z();
    }

    public void offsetScale(Vector3f pOffset) {
        this.xScale = this.xScale + pOffset.x();
        this.yScale = this.yScale + pOffset.y();
        this.zScale = this.zScale + pOffset.z();
    }

    public Stream<ModelPart> getAllParts() {
        return Stream.concat(Stream.of(this), this.children.values().stream().flatMap(ModelPart::getAllParts));
    }

    public void addSprite(float posX, float posY, float posZ, int sizeX, int sizeY, int sizeZ, float sizeAdd) {
        this.spriteList.add(new ModelSprite(this, this.textureOffsetX, this.textureOffsetY, posX, posY, posZ, sizeX, sizeY, sizeZ, sizeAdd));
    }

    public ResourceLocation getTextureLocation() {
        return this.textureLocation;
    }

    public void setTextureLocation(ResourceLocation textureLocation) {
        this.textureLocation = textureLocation;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addBox(float[][] faceUvs, float x, float y, float z, float dx, float dy, float dz, float delta) {
        this.cubes.add(new ModelPart.Cube(faceUvs, x, y, z, dx, dy, dz, delta, delta, delta, this.mirror, this.textureWidth, this.textureHeight));
    }

    public void addBox(float x, float y, float z, float width, float height, float depth, float delta) {
        this.addBox(this.textureOffsetX, this.textureOffsetY, x, y, z, width, height, depth, delta, delta, delta, this.mirror, false);
    }

    private void addBox(
        float texOffX,
        float texOffY,
        float x,
        float y,
        float z,
        float width,
        float height,
        float depth,
        float deltaX,
        float deltaY,
        float deltaZ,
        boolean mirror,
        boolean dummyIn
    ) {
        this.cubes
            .add(
                new ModelPart.Cube(
                    texOffX, texOffY, x, y, z, width, height, depth, deltaX, deltaY, deltaZ, mirror, this.textureWidth, this.textureHeight, ALL_VISIBLE
                )
            );
    }

    public ModelPart getChildModelDeep(String name) {
        if (name == null) {
            return null;
        } else if (this.children.containsKey(name)) {
            return this.getChild(name);
        } else {
            if (this.children != null) {
                for (String s : this.children.keySet()) {
                    ModelPart modelpart = this.children.get(s);
                    ModelPart modelpart1 = modelpart.getChildModelDeep(name);
                    if (modelpart1 != null) {
                        return modelpart1;
                    }
                }
            }

            return null;
        }
    }

    public ModelPart getChildById(String id) {
        if (id == null) {
            return null;
        } else {
            if (this.children != null) {
                for (String s : this.children.keySet()) {
                    ModelPart modelpart = this.children.get(s);
                    if (id.equals(modelpart.getId())) {
                        return modelpart;
                    }
                }
            }

            return null;
        }
    }

    public ModelPart getChildDeepById(String id) {
        if (id == null) {
            return null;
        } else {
            ModelPart modelpart = this.getChildById(id);
            if (modelpart != null) {
                return modelpart;
            } else {
                if (this.children != null) {
                    for (String s : this.children.keySet()) {
                        ModelPart modelpart1 = this.children.get(s);
                        ModelPart modelpart2 = modelpart1.getChildDeepById(id);
                        if (modelpart2 != null) {
                            return modelpart2;
                        }
                    }
                }

                return null;
            }
        }
    }

    public ModelUpdater getModelUpdater() {
        return this.modelUpdater;
    }

    public void setModelUpdater(ModelUpdater modelUpdater) {
        this.modelUpdater = modelUpdater;
    }

    public void addChildModel(String name, ModelPart part) {
        if (part != null) {
            this.children.put(name, part);
            this.childModelsList = new ArrayList<>(this.children.values());
            part.setParent(this);
            if (part.getName() == null) {
                part.setName(name);
            }
        }
    }

    public String getUniqueChildModelName(String name) {
        String s = name;

        for (int i = 2; this.children.containsKey(name); i++) {
            name = s + "-" + i;
        }

        return name;
    }

    private void updateModel() {
        if (this.modelUpdater != null) {
            this.modelUpdater.update();
        } else {
            int i = this.childModelsList.size();

            for (int j = 0; j < i; j++) {
                ModelPart modelpart = this.childModelsList.get(j);
                modelpart.updateModel();
            }
        }
    }

    public boolean isCustom() {
        return this.custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public ModelPart getParent() {
        return this.parent;
    }

    public void setParent(ModelPart parent) {
        this.parent = parent;
    }

    public Attachment[] getAttachments() {
        return this.attachments;
    }

    public void setAttachments(Attachment[] attachments) {
        this.attachments = attachments;
    }

    public boolean applyAttachmentTransform(AttachmentType typeIn, PoseStack matrixStackIn) {
        if (this.attachmentPathsChecked && this.attachmentPaths == null) {
            return false;
        } else {
            AttachmentPath attachmentpath = this.getAttachmentPath(typeIn);
            if (attachmentpath == null) {
                return false;
            } else {
                attachmentpath.applyTransform(matrixStackIn);
                return true;
            }
        }
    }

    private AttachmentPath getAttachmentPath(AttachmentType typeIn) {
        if (!this.attachmentPathsChecked) {
            this.attachmentPathsChecked = true;
            this.attachmentPaths = new AttachmentPaths();
            this.collectAttachmentPaths(new ArrayList<>(), this.attachmentPaths);
            if (this.attachmentPaths.isEmpty()) {
                this.attachmentPaths = null;
            }
        }

        return this.attachmentPaths == null ? null : this.attachmentPaths.getVisiblePath(typeIn);
    }

    private void collectAttachmentPaths(List<ModelPart> parents, AttachmentPaths paths) {
        parents.add(this);
        if (this.attachments != null) {
            paths.addPaths(parents, this.attachments);
        }

        for (ModelPart modelpart : this.childModelsList) {
            modelpart.collectAttachmentPaths(parents, paths);
        }

        parents.remove(parents.size() - 1);
    }

    @Override
    public String toString() {
        return "name: "
            + this.name
            + ", id: "
            + this.id
            + ", boxes: "
            + (this.cubes != null ? this.cubes.size() : null)
            + ", submodels: "
            + (this.children != null ? this.children.size() : null)
            + ", custom: "
            + this.custom;
    }

    public static class Cube {
        private final ModelPart.Polygon[] polygons;
        public final float minX;
        public final float minY;
        public final float minZ;
        public final float maxX;
        public final float maxY;
        public final float maxZ;
        private BoxVertexPositions boxVertexPositions;
        private RenderPositions[] renderPositions;

        public Cube(
            int pTexCoordU,
            int pTexCoordV,
            float pOriginX,
            float pOriginY,
            float pOriginZ,
            float pDimensionX,
            float pDimensionY,
            float pDimensionZ,
            float pGtowX,
            float pGrowY,
            float pGrowZ,
            boolean pMirror,
            float pTexScaleU,
            float pTexScaleV,
            Set<Direction> pVisibleFaces
        ) {
            this(
                (float)pTexCoordU,
                (float)pTexCoordV,
                pOriginX,
                pOriginY,
                pOriginZ,
                pDimensionX,
                pDimensionY,
                pDimensionZ,
                pGtowX,
                pGrowY,
                pGrowZ,
                pMirror,
                pTexScaleU,
                pTexScaleV,
                pVisibleFaces
            );
        }

        public Cube(
            float texOffX,
            float texOffY,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth,
            float deltaX,
            float deltaY,
            float deltaZ,
            boolean mirror,
            float texWidth,
            float texHeight,
            Set<Direction> directionsIn
        ) {
            this.minX = x;
            this.minY = y;
            this.minZ = z;
            this.maxX = x + width;
            this.maxY = y + height;
            this.maxZ = z + depth;
            this.polygons = new ModelPart.Polygon[directionsIn.size()];
            float f = x + width;
            float f1 = y + height;
            float f2 = z + depth;
            x -= deltaX;
            y -= deltaY;
            z -= deltaZ;
            f += deltaX;
            f1 += deltaY;
            f2 += deltaZ;
            if (mirror) {
                float f3 = f;
                f = x;
                x = f3;
            }

            ModelPart.Vertex modelpart$vertex7 = new ModelPart.Vertex(x, y, z, 0.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex = new ModelPart.Vertex(f, y, z, 0.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex1 = new ModelPart.Vertex(f, f1, z, 8.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex2 = new ModelPart.Vertex(x, f1, z, 8.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex3 = new ModelPart.Vertex(x, y, f2, 0.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex4 = new ModelPart.Vertex(f, y, f2, 0.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex5 = new ModelPart.Vertex(f, f1, f2, 8.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex6 = new ModelPart.Vertex(x, f1, f2, 8.0F, 0.0F);
            float f4 = texOffX + depth;
            float f5 = texOffX + depth + width;
            float f6 = texOffX + depth + width + width;
            float f7 = texOffX + depth + width + depth;
            float f8 = texOffX + depth + width + depth + width;
            float f9 = texOffY + depth;
            float f10 = texOffY + depth + height;
            int i = 0;
            if (directionsIn.contains(Direction.DOWN)) {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[]{modelpart$vertex4, modelpart$vertex3, modelpart$vertex7, modelpart$vertex},
                    f4,
                    texOffY,
                    f5,
                    f9,
                    texWidth,
                    texHeight,
                    mirror,
                    Direction.DOWN
                );
            }

            if (directionsIn.contains(Direction.UP)) {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[]{modelpart$vertex1, modelpart$vertex2, modelpart$vertex6, modelpart$vertex5},
                    f5,
                    f9,
                    f6,
                    texOffY,
                    texWidth,
                    texHeight,
                    mirror,
                    Direction.UP
                );
            }

            if (directionsIn.contains(Direction.WEST)) {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[]{modelpart$vertex7, modelpart$vertex3, modelpart$vertex6, modelpart$vertex2},
                    texOffX,
                    f9,
                    f4,
                    f10,
                    texWidth,
                    texHeight,
                    mirror,
                    Direction.WEST
                );
            }

            if (directionsIn.contains(Direction.NORTH)) {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[]{modelpart$vertex, modelpart$vertex7, modelpart$vertex2, modelpart$vertex1},
                    f4,
                    f9,
                    f5,
                    f10,
                    texWidth,
                    texHeight,
                    mirror,
                    Direction.NORTH
                );
            }

            if (directionsIn.contains(Direction.EAST)) {
                this.polygons[i++] = new ModelPart.Polygon(
                    new ModelPart.Vertex[]{modelpart$vertex4, modelpart$vertex, modelpart$vertex1, modelpart$vertex5},
                    f5,
                    f9,
                    f7,
                    f10,
                    texWidth,
                    texHeight,
                    mirror,
                    Direction.EAST
                );
            }

            if (directionsIn.contains(Direction.SOUTH)) {
                this.polygons[i] = new ModelPart.Polygon(
                    new ModelPart.Vertex[]{modelpart$vertex3, modelpart$vertex4, modelpart$vertex5, modelpart$vertex6},
                    f7,
                    f9,
                    f8,
                    f10,
                    texWidth,
                    texHeight,
                    mirror,
                    Direction.SOUTH
                );
            }

            this.renderPositions = collectRenderPositions(this.polygons);
        }

        public Cube(
            float[][] faceUvs,
            float x,
            float y,
            float z,
            float width,
            float height,
            float depth,
            float deltaX,
            float deltaY,
            float deltaZ,
            boolean mirorIn,
            float texWidth,
            float texHeight
        ) {
            this.minX = x;
            this.minY = y;
            this.minZ = z;
            this.maxX = x + width;
            this.maxY = y + height;
            this.maxZ = z + depth;
            this.polygons = new ModelPart.Polygon[6];
            float f = x + width;
            float f1 = y + height;
            float f2 = z + depth;
            x -= deltaX;
            y -= deltaY;
            z -= deltaZ;
            f += deltaX;
            f1 += deltaY;
            f2 += deltaZ;
            if (mirorIn) {
                float f3 = f;
                f = x;
                x = f3;
            }

            ModelPart.Vertex modelpart$vertex7 = new ModelPart.Vertex(x, y, z, 0.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex = new ModelPart.Vertex(f, y, z, 0.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex1 = new ModelPart.Vertex(f, f1, z, 8.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex2 = new ModelPart.Vertex(x, f1, z, 8.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex3 = new ModelPart.Vertex(x, y, f2, 0.0F, 0.0F);
            ModelPart.Vertex modelpart$vertex4 = new ModelPart.Vertex(f, y, f2, 0.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex5 = new ModelPart.Vertex(f, f1, f2, 8.0F, 8.0F);
            ModelPart.Vertex modelpart$vertex6 = new ModelPart.Vertex(x, f1, f2, 8.0F, 0.0F);
            this.polygons[2] = this.makeTexturedQuad(
                new ModelPart.Vertex[]{modelpart$vertex4, modelpart$vertex3, modelpart$vertex7, modelpart$vertex},
                faceUvs[1],
                true,
                texWidth,
                texHeight,
                mirorIn,
                Direction.DOWN
            );
            this.polygons[3] = this.makeTexturedQuad(
                new ModelPart.Vertex[]{modelpart$vertex1, modelpart$vertex2, modelpart$vertex6, modelpart$vertex5},
                faceUvs[0],
                true,
                texWidth,
                texHeight,
                mirorIn,
                Direction.UP
            );
            this.polygons[1] = this.makeTexturedQuad(
                new ModelPart.Vertex[]{modelpart$vertex7, modelpart$vertex3, modelpart$vertex6, modelpart$vertex2},
                faceUvs[5],
                false,
                texWidth,
                texHeight,
                mirorIn,
                Direction.WEST
            );
            this.polygons[4] = this.makeTexturedQuad(
                new ModelPart.Vertex[]{modelpart$vertex, modelpart$vertex7, modelpart$vertex2, modelpart$vertex1},
                faceUvs[2],
                false,
                texWidth,
                texHeight,
                mirorIn,
                Direction.NORTH
            );
            this.polygons[0] = this.makeTexturedQuad(
                new ModelPart.Vertex[]{modelpart$vertex4, modelpart$vertex, modelpart$vertex1, modelpart$vertex5},
                faceUvs[4],
                false,
                texWidth,
                texHeight,
                mirorIn,
                Direction.EAST
            );
            this.polygons[5] = this.makeTexturedQuad(
                new ModelPart.Vertex[]{modelpart$vertex3, modelpart$vertex4, modelpart$vertex5, modelpart$vertex6},
                faceUvs[3],
                false,
                texWidth,
                texHeight,
                mirorIn,
                Direction.SOUTH
            );
            this.renderPositions = collectRenderPositions(this.polygons);
        }

        private static RenderPositions[] collectRenderPositions(ModelPart.Polygon[] quads) {
            Map<Vector3f, RenderPositions> map = new LinkedHashMap<>();

            for (int i = 0; i < quads.length; i++) {
                ModelPart.Polygon modelpart$polygon = quads[i];
                if (modelpart$polygon != null) {
                    for (int j = 0; j < modelpart$polygon.vertices.length; j++) {
                        ModelPart.Vertex modelpart$vertex = modelpart$polygon.vertices[j];
                        RenderPositions renderpositions = map.get(modelpart$vertex.pos);
                        if (renderpositions == null) {
                            renderpositions = new RenderPositions(modelpart$vertex.pos);
                            map.put(modelpart$vertex.pos, renderpositions);
                        }

                        modelpart$vertex.renderPositions = renderpositions;
                    }
                }
            }

            return map.values().toArray(new RenderPositions[map.size()]);
        }

        private ModelPart.Polygon makeTexturedQuad(
            ModelPart.Vertex[] positionTextureVertexs,
            float[] faceUvs,
            boolean reverseUV,
            float textureWidth,
            float textureHeight,
            boolean mirrorIn,
            Direction directionIn
        ) {
            if (faceUvs == null) {
                return null;
            } else {
                return reverseUV
                    ? new ModelPart.Polygon(
                        positionTextureVertexs, faceUvs[2], faceUvs[3], faceUvs[0], faceUvs[1], textureWidth, textureHeight, mirrorIn, directionIn
                    )
                    : new ModelPart.Polygon(
                        positionTextureVertexs, faceUvs[0], faceUvs[1], faceUvs[2], faceUvs[3], textureWidth, textureHeight, mirrorIn, directionIn
                    );
            }
        }

        public VertexPosition[][] getBoxVertexPositions(int key) {
            if (this.boxVertexPositions == null) {
                this.boxVertexPositions = new BoxVertexPositions();
            }

            return this.boxVertexPositions.get(key);
        }

        public void compile(PoseStack.Pose pPose, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, int pColor) {
            this.compile(pPose, pBuffer, pPackedLight, pPackedOverlay, pColor, null);
        }

        public void compile(
            PoseStack.Pose matrixEntryIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, int colorIn, VertexPosition[][] boxPos
        ) {
            Matrix4f matrix4f = matrixEntryIn.pose();
            Vector3f vector3f = bufferIn.getTempVec3f();

            for (RenderPositions renderpositions : this.renderPositions) {
                MathUtils.transform(matrix4f, renderpositions.getPositionDiv16(), renderpositions.getPositionRender());
            }

            boolean flag = bufferIn.canAddVertexFast();
            int i = this.polygons.length;

            for (int j = 0; j < i; j++) {
                ModelPart.Polygon modelpart$polygon = this.polygons[j];
                if (modelpart$polygon != null) {
                    if (boxPos != null) {
                        bufferIn.setQuadVertexPositions(boxPos[j]);
                    }

                    Vector3f vector3f1 = matrixEntryIn.transformNormal(modelpart$polygon.normal, vector3f);
                    float f = vector3f1.x();
                    float f1 = vector3f1.y();
                    float f2 = vector3f1.z();
                    if (flag) {
                        int k = colorIn;
                        byte b0 = BufferBuilder.normalIntValue(f);
                        byte b1 = BufferBuilder.normalIntValue(f1);
                        byte b2 = BufferBuilder.normalIntValue(f2);
                        int l = (b2 & 255) << 16 | (b1 & 255) << 8 | b0 & 255;

                        for (ModelPart.Vertex modelpart$vertex1 : modelpart$polygon.vertices) {
                            Vector3f vector3f3 = modelpart$vertex1.renderPositions.getPositionRender();
                            bufferIn.addVertexFast(
                                vector3f3.x,
                                vector3f3.y,
                                vector3f3.z,
                                k,
                                modelpart$vertex1.u,
                                modelpart$vertex1.v,
                                packedOverlayIn,
                                packedLightIn,
                                l
                            );
                        }
                    } else {
                        for (ModelPart.Vertex modelpart$vertex : modelpart$polygon.vertices) {
                            Vector3f vector3f2 = modelpart$vertex.renderPositions.getPositionRender();
                            bufferIn.addVertex(
                                vector3f2.x,
                                vector3f2.y,
                                vector3f2.z,
                                colorIn,
                                modelpart$vertex.u,
                                modelpart$vertex.v,
                                packedOverlayIn,
                                packedLightIn,
                                f,
                                f1,
                                f2
                            );
                        }
                    }
                }
            }
        }
    }

    static class Polygon {
        public final ModelPart.Vertex[] vertices;
        public final Vector3f normal;

        public Polygon(
            ModelPart.Vertex[] pVertices,
            float pU1,
            float pV1,
            float pU2,
            float pV2,
            float pTextureWidth,
            float pTextureHeight,
            boolean pMirror,
            Direction pDirection
        ) {
            this.vertices = pVertices;
            float f = 0.0F / pTextureWidth;
            float f1 = 0.0F / pTextureHeight;
            if (Config.isAntialiasing()) {
                f = 0.05F / pTextureWidth;
                f1 = 0.05F / pTextureHeight;
                if (pU2 < pU1) {
                    f = -f;
                }

                if (pV2 < pV1) {
                    f1 = -f1;
                }
            }

            pVertices[0] = pVertices[0].remap(pU2 / pTextureWidth - f, pV1 / pTextureHeight + f1);
            pVertices[1] = pVertices[1].remap(pU1 / pTextureWidth + f, pV1 / pTextureHeight + f1);
            pVertices[2] = pVertices[2].remap(pU1 / pTextureWidth + f, pV2 / pTextureHeight - f1);
            pVertices[3] = pVertices[3].remap(pU2 / pTextureWidth - f, pV2 / pTextureHeight - f1);
            if (pMirror) {
                int i = pVertices.length;

                for (int j = 0; j < i / 2; j++) {
                    ModelPart.Vertex modelpart$vertex = pVertices[j];
                    pVertices[j] = pVertices[i - 1 - j];
                    pVertices[i - 1 - j] = modelpart$vertex;
                }
            }

            this.normal = pDirection.step();
            if (pMirror) {
                this.normal.mul(-1.0F, 1.0F, 1.0F);
            }
        }
    }

    static class Vertex {
        public final Vector3f pos;
        public final float u;
        public final float v;
        public RenderPositions renderPositions;

        public Vertex(float pX, float pY, float pZ, float pU, float pV) {
            this(new Vector3f(pX, pY, pZ), pU, pV);
        }

        public ModelPart.Vertex remap(float pU, float pV) {
            return new ModelPart.Vertex(this.pos, pU, pV);
        }

        public Vertex(Vector3f pPos, float pU, float pV) {
            this.pos = pPos;
            this.u = pU;
            this.v = pV;
        }
    }

    @FunctionalInterface
    public interface Visitor {
        void visit(PoseStack.Pose pPose, String pPath, int pIndex, ModelPart.Cube pCube);
    }
}
