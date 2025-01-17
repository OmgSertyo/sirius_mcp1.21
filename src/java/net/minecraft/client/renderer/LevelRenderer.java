package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.BlockPosM;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.CustomSky;
import net.optifine.DynamicLights;
import net.optifine.EmissiveTextures;
import net.optifine.Lagometer;
import net.optifine.SmartAnimations;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;
import net.optifine.render.ChunkVisibility;
import net.optifine.render.RenderEnv;
import net.optifine.render.RenderStateManager;
import net.optifine.render.RenderUtils;
import net.optifine.render.VboRegion;
import net.optifine.shaders.RenderStage;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.util.BiomeUtils;
import net.optifine.util.GpuMemory;
import net.optifine.util.MathUtils;
import net.optifine.util.PairInt;
import net.optifine.util.RandomUtils;
import net.optifine.util.RenderChunkUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

public class LevelRenderer implements ResourceManagerReloadListener, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int SECTION_SIZE = 16;
    public static final int HALF_SECTION_SIZE = 8;
    private static final float SKY_DISC_RADIUS = 512.0F;
    private static final int MIN_FOG_DISTANCE = 32;
    private static final int RAIN_RADIUS = 10;
    private static final int RAIN_DIAMETER = 21;
    private static final int TRANSPARENT_SORT_COUNT = 15;
    private static final ResourceLocation MOON_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    private static final ResourceLocation SUN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    protected static final ResourceLocation CLOUDS_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
    private static final ResourceLocation END_SKY_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/end_sky.png");
    private static final ResourceLocation FORCEFIELD_LOCATION = ResourceLocation.withDefaultNamespace("textures/misc/forcefield.png");
    private static final ResourceLocation RAIN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/rain.png");
    private static final ResourceLocation SNOW_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/snow.png");
    public static final Direction[] DIRECTIONS = Direction.values();
    private final Minecraft minecraft;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    private final RenderBuffers renderBuffers;
    @Nullable
    protected ClientLevel level;
    private final SectionOcclusionGraph sectionOcclusionGraph = new SectionOcclusionGraph();
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>(10000);
    private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
    @Nullable
    private ViewArea viewArea;
    @Nullable
    private VertexBuffer starBuffer;
    @Nullable
    private VertexBuffer skyBuffer;
    @Nullable
    private VertexBuffer darkBuffer;
    private boolean generateClouds = true;
    @Nullable
    private VertexBuffer cloudBuffer;
    private final RunningTrimmedMean frameTimes = new RunningTrimmedMean(100);
    private int ticks;
    private final Int2ObjectMap<BlockDestructionProgress> destroyingBlocks = new Int2ObjectOpenHashMap<>();
    private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress = new Long2ObjectOpenHashMap<>();
    private final Map<BlockPos, SoundInstance> playingJukeboxSongs = Maps.newHashMap();
    @Nullable
    private RenderTarget entityTarget;
    @Nullable
    private PostChain entityEffect;
    @Nullable
    private RenderTarget translucentTarget;
    @Nullable
    private RenderTarget itemEntityTarget;
    @Nullable
    private RenderTarget particlesTarget;
    @Nullable
    private RenderTarget weatherTarget;
    @Nullable
    private RenderTarget cloudsTarget;
    @Nullable
    private PostChain transparencyChain;
    private int lastCameraSectionX = Integer.MIN_VALUE;
    private int lastCameraSectionY = Integer.MIN_VALUE;
    private int lastCameraSectionZ = Integer.MIN_VALUE;
    private double prevCamX = Double.MIN_VALUE;
    private double prevCamY = Double.MIN_VALUE;
    private double prevCamZ = Double.MIN_VALUE;
    private double prevCamRotX = Double.MIN_VALUE;
    private double prevCamRotY = Double.MIN_VALUE;
    private int prevCloudX = Integer.MIN_VALUE;
    private int prevCloudY = Integer.MIN_VALUE;
    private int prevCloudZ = Integer.MIN_VALUE;
    private Vec3 prevCloudColor = Vec3.ZERO;
    @Nullable
    private CloudStatus prevCloudsType;
    @Nullable
    private SectionRenderDispatcher sectionRenderDispatcher;
    private int lastViewDistance = -1;
    private int renderedEntities;
    private int culledEntities;
    private Frustum cullingFrustum;
    private boolean captureFrustum;
    @Nullable
    private Frustum capturedFrustum;
    private final Vector4f[] frustumPoints = new Vector4f[8];
    private final Vector3d frustumPos = new Vector3d(0.0, 0.0, 0.0);
    private double xTransparentOld;
    private double yTransparentOld;
    private double zTransparentOld;
    private int rainSoundTime;
    private final float[] rainSizeX = new float[1024];
    private final float[] rainSizeZ = new float[1024];
    private Set<SectionRenderDispatcher.RenderSection> chunksToResortTransparency = new LinkedHashSet<>();
    private int countChunksToUpdate = 0;
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> renderInfosTerrain = new ObjectArrayList<>(1024);
    private LongOpenHashSet renderInfosEntities = new LongOpenHashSet(1024);
    private List<SectionRenderDispatcher.RenderSection> renderInfosTileEntities = new ArrayList<>(1024);
    private ObjectArrayList renderInfosTerrainNormal = new ObjectArrayList(1024);
    private LongOpenHashSet renderInfosEntitiesNormal = new LongOpenHashSet(1024);
    private List renderInfosTileEntitiesNormal = new ArrayList(1024);
    private ObjectArrayList renderInfosTerrainShadow = new ObjectArrayList(1024);
    private LongOpenHashSet renderInfosEntitiesShadow = new LongOpenHashSet(1024);
    private List renderInfosTileEntitiesShadow = new ArrayList(1024);
    protected int renderDistance = 0;
    protected int renderDistanceSq = 0;
    protected int renderDistanceXZSq = 0;
    private int countTileEntitiesRendered;
    private RenderEnv renderEnv = new RenderEnv(Blocks.AIR.defaultBlockState(), new BlockPos(0, 0, 0));
    public boolean renderOverlayDamaged = false;
    public boolean renderOverlayEyes = false;
    private boolean firstWorldLoad = false;
    private static int renderEntitiesCounter = 0;
    public int loadVisibleChunksCounter = -1;
    public static MessageSignature loadVisibleChunksMessageId = new MessageSignature(RandomUtils.getRandomBytes(256));
    private static boolean ambientOcclusion = false;
    private Map<String, List<Entity>> mapEntityLists = new HashMap<>();
    private Map<RenderType, Map> mapRegionLayers = new LinkedHashMap<>();
    private int frameId;
    private boolean debugFixTerrainFrustumShadow;

    public LevelRenderer(Minecraft pMinecraft, EntityRenderDispatcher pEntityRenderDispatcher, BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, RenderBuffers pRenderBuffers) {
        this.minecraft = pMinecraft;
        this.entityRenderDispatcher = pEntityRenderDispatcher;
        this.blockEntityRenderDispatcher = pBlockEntityRenderDispatcher;
        this.renderBuffers = pRenderBuffers;

        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                float f = (float)(j - 16);
                float f1 = (float)(i - 16);
                float f2 = Mth.sqrt(f * f + f1 * f1);
                this.rainSizeX[i << 5 | j] = -f1 / f2;
                this.rainSizeZ[i << 5 | j] = f / f2;
            }
        }

        this.createStars();
        this.createLightSky();
        this.createDarkSky();
    }

    private void renderSnowAndRain(LightTexture pLightTexture, float pPartialTick, double pCamX, double pCamY, double pCamZ) {
        if (!Reflector.IForgeDimensionSpecialEffects_renderSnowAndRain.exists()
            || !Reflector.callBoolean(
                this.level.effects(),
                Reflector.IForgeDimensionSpecialEffects_renderSnowAndRain,
                this.level,
                this.ticks,
                pPartialTick,
                pLightTexture,
                pCamX,
                pCamY,
                pCamZ
            )) {
            float f = this.minecraft.level.getRainLevel(pPartialTick);
            if (!(f <= 0.0F)) {
                if (Config.isRainOff()) {
                    return;
                }

                pLightTexture.turnOnLightLayer();
                Level level = this.minecraft.level;
                int i = Mth.floor(pCamX);
                int j = Mth.floor(pCamY);
                int k = Mth.floor(pCamZ);
                Tesselator tesselator = Tesselator.getInstance();
                BufferBuilder bufferbuilder = null;
                RenderSystem.disableCull();
                RenderSystem.enableBlend();
                RenderSystem.enableDepthTest();
                int l = 5;
                if (Config.isRainFancy()) {
                    l = 10;
                }

                RenderSystem.depthMask(Minecraft.useShaderTransparency());
                if (Config.isShaders()) {
                    GlStateManager._depthMask(Shaders.isRainDepth());
                }

                int i1 = -1;
                float f1 = (float)this.ticks + pPartialTick;
                RenderSystem.setShader(GameRenderer::getParticleShader);
                BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                for (int j1 = k - l; j1 <= k + l; j1++) {
                    for (int k1 = i - l; k1 <= i + l; k1++) {
                        int l1 = (j1 - k + 16) * 32 + k1 - i + 16;
                        double d0 = (double)this.rainSizeX[l1] * 0.5;
                        double d1 = (double)this.rainSizeZ[l1] * 0.5;
                        blockpos$mutableblockpos.set((double)k1, pCamY, (double)j1);
                        Biome biome = level.getBiome(blockpos$mutableblockpos).value();
                        if (biome.hasPrecipitation()) {
                            int i2 = level.getHeight(Heightmap.Types.MOTION_BLOCKING, k1, j1);
                            int j2 = j - l;
                            int k2 = j + l;
                            if (j2 < i2) {
                                j2 = i2;
                            }

                            if (k2 < i2) {
                                k2 = i2;
                            }

                            int l2 = i2;
                            if (i2 < j) {
                                l2 = j;
                            }

                            if (j2 != k2) {
                                RandomSource randomsource = RandomSource.create((long)(k1 * k1 * 3121 + k1 * 45238971 ^ j1 * j1 * 418711 + j1 * 13761));
                                blockpos$mutableblockpos.set(k1, j2, j1);
                                Biome.Precipitation biome$precipitation = biome.getPrecipitationAt(blockpos$mutableblockpos);
                                if (biome$precipitation == Biome.Precipitation.RAIN) {
                                    if (i1 != 0) {
                                        if (i1 >= 0) {
                                            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                                        }

                                        i1 = 0;
                                        RenderSystem.setShaderTexture(0, RAIN_LOCATION);
                                        bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                                    }

                                    int i3 = this.ticks & 131071;
                                    int j3 = k1 * k1 * 3121 + k1 * 45238971 + j1 * j1 * 418711 + j1 * 13761 & 0xFF;
                                    float f2 = 3.0F + randomsource.nextFloat();
                                    float f3 = -((float)(i3 + j3) + pPartialTick) / 32.0F * f2;
                                    float f4 = f3 % 32.0F;
                                    double d2 = (double)k1 + 0.5 - pCamX;
                                    double d3 = (double)j1 + 0.5 - pCamZ;
                                    float f6 = (float)Math.sqrt(d2 * d2 + d3 * d3) / (float)l;
                                    float f7 = ((1.0F - f6 * f6) * 0.5F + 0.5F) * f;
                                    blockpos$mutableblockpos.set(k1, l2, j1);
                                    int k3 = getLightColor(level, blockpos$mutableblockpos);
                                    bufferbuilder.addVertex(
                                            (float)((double)k1 - pCamX - d0 + 0.5),
                                            (float)((double)k2 - pCamY),
                                            (float)((double)j1 - pCamZ - d1 + 0.5)
                                        )
                                        .setUv(0.0F, (float)j2 * 0.25F + f4)
                                        .setColor(1.0F, 1.0F, 1.0F, f7)
                                        .setLight(k3);
                                    bufferbuilder.addVertex(
                                            (float)((double)k1 - pCamX + d0 + 0.5),
                                            (float)((double)k2 - pCamY),
                                            (float)((double)j1 - pCamZ + d1 + 0.5)
                                        )
                                        .setUv(1.0F, (float)j2 * 0.25F + f4)
                                        .setColor(1.0F, 1.0F, 1.0F, f7)
                                        .setLight(k3);
                                    bufferbuilder.addVertex(
                                            (float)((double)k1 - pCamX + d0 + 0.5),
                                            (float)((double)j2 - pCamY),
                                            (float)((double)j1 - pCamZ + d1 + 0.5)
                                        )
                                        .setUv(1.0F, (float)k2 * 0.25F + f4)
                                        .setColor(1.0F, 1.0F, 1.0F, f7)
                                        .setLight(k3);
                                    bufferbuilder.addVertex(
                                            (float)((double)k1 - pCamX - d0 + 0.5),
                                            (float)((double)j2 - pCamY),
                                            (float)((double)j1 - pCamZ - d1 + 0.5)
                                        )
                                        .setUv(0.0F, (float)k2 * 0.25F + f4)
                                        .setColor(1.0F, 1.0F, 1.0F, f7)
                                        .setLight(k3);
                                } else if (biome$precipitation == Biome.Precipitation.SNOW) {
                                    if (i1 != 1) {
                                        if (i1 >= 0) {
                                            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                                        }

                                        i1 = 1;
                                        RenderSystem.setShaderTexture(0, SNOW_LOCATION);
                                        bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                                    }

                                    float f8 = -((float)(this.ticks & 511) + pPartialTick) / 512.0F;
                                    float f9 = (float)(randomsource.nextDouble() + (double)f1 * 0.01 * (double)((float)randomsource.nextGaussian()));
                                    float f10 = (float)(randomsource.nextDouble() + (double)(f1 * (float)randomsource.nextGaussian()) * 0.001);
                                    double d4 = (double)k1 + 0.5 - pCamX;
                                    double d5 = (double)j1 + 0.5 - pCamZ;
                                    float f11 = (float)Math.sqrt(d4 * d4 + d5 * d5) / (float)l;
                                    float f5 = ((1.0F - f11 * f11) * 0.3F + 0.5F) * f;
                                    blockpos$mutableblockpos.set(k1, l2, j1);
                                    int j4 = getLightColor(level, blockpos$mutableblockpos);
                                    int k4 = j4 >> 16 & 65535;
                                    int l4 = j4 & 65535;
                                    int l3 = (k4 * 3 + 240) / 4;
                                    int i4 = (l4 * 3 + 240) / 4;
                                    bufferbuilder.addVertex(
                                            (float)((double)k1 - pCamX - d0 + 0.5),
                                            (float)((double)k2 - pCamY),
                                            (float)((double)j1 - pCamZ - d1 + 0.5)
                                        )
                                        .setUv(0.0F + f9, (float)j2 * 0.25F + f8 + f10)
                                        .setColor(1.0F, 1.0F, 1.0F, f5)
                                        .setUv2(i4, l3);
                                    bufferbuilder.addVertex(
                                            (float)((double)k1 - pCamX + d0 + 0.5),
                                            (float)((double)k2 - pCamY),
                                            (float)((double)j1 - pCamZ + d1 + 0.5)
                                        )
                                        .setUv(1.0F + f9, (float)j2 * 0.25F + f8 + f10)
                                        .setColor(1.0F, 1.0F, 1.0F, f5)
                                        .setUv2(i4, l3);
                                    bufferbuilder.addVertex(
                                            (float)((double)k1 - pCamX + d0 + 0.5),
                                            (float)((double)j2 - pCamY),
                                            (float)((double)j1 - pCamZ + d1 + 0.5)
                                        )
                                        .setUv(1.0F + f9, (float)k2 * 0.25F + f8 + f10)
                                        .setColor(1.0F, 1.0F, 1.0F, f5)
                                        .setUv2(i4, l3);
                                    bufferbuilder.addVertex(
                                            (float)((double)k1 - pCamX - d0 + 0.5),
                                            (float)((double)j2 - pCamY),
                                            (float)((double)j1 - pCamZ - d1 + 0.5)
                                        )
                                        .setUv(0.0F + f9, (float)k2 * 0.25F + f8 + f10)
                                        .setColor(1.0F, 1.0F, 1.0F, f5)
                                        .setUv2(i4, l3);
                                }
                            }
                        }
                    }
                }

                if (i1 >= 0) {
                    BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                }

                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                pLightTexture.turnOffLightLayer();
            }
        }
    }

    public void tickRain(Camera pCamera) {
        if (!Reflector.IForgeDimensionSpecialEffects_tickRain.exists()
            || !Reflector.callBoolean(this.level.effects(), Reflector.IForgeDimensionSpecialEffects_tickRain, this.level, this.ticks, pCamera)) {
            float f = this.minecraft.level.getRainLevel(1.0F) / (Minecraft.useFancyGraphics() ? 1.0F : 2.0F);
            if (!Config.isRainFancy()) {
                f /= 2.0F;
            }

            if (!(f <= 0.0F) && Config.isRainSplash()) {
                RandomSource randomsource = RandomSource.create((long)this.ticks * 312987231L);
                LevelReader levelreader = this.minecraft.level;
                BlockPos blockpos = BlockPos.containing(pCamera.getPosition());
                BlockPos blockpos1 = null;
                int i = (int)(100.0F * f * f) / (this.minecraft.options.particles().get() == ParticleStatus.DECREASED ? 2 : 1);

                for (int j = 0; j < i; j++) {
                    int k = randomsource.nextInt(21) - 10;
                    int l = randomsource.nextInt(21) - 10;
                    BlockPos blockpos2 = levelreader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos.offset(k, 0, l));
                    if (blockpos2.getY() > levelreader.getMinBuildHeight()
                        && blockpos2.getY() <= blockpos.getY() + 10
                        && blockpos2.getY() >= blockpos.getY() - 10) {
                        Biome biome = levelreader.getBiome(blockpos2).value();
                        if (biome.getPrecipitationAt(blockpos2) == Biome.Precipitation.RAIN) {
                            blockpos1 = blockpos2.below();
                            if (this.minecraft.options.particles().get() == ParticleStatus.MINIMAL) {
                                break;
                            }

                            double d0 = randomsource.nextDouble();
                            double d1 = randomsource.nextDouble();
                            BlockState blockstate = levelreader.getBlockState(blockpos1);
                            FluidState fluidstate = levelreader.getFluidState(blockpos1);
                            VoxelShape voxelshape = blockstate.getCollisionShape(levelreader, blockpos1);
                            double d2 = voxelshape.max(Direction.Axis.Y, d0, d1);
                            double d3 = (double)fluidstate.getHeight(levelreader, blockpos1);
                            double d4 = Math.max(d2, d3);
                            ParticleOptions particleoptions = !fluidstate.is(FluidTags.LAVA)
                                    && !blockstate.is(Blocks.MAGMA_BLOCK)
                                    && !CampfireBlock.isLitCampfire(blockstate)
                                ? ParticleTypes.RAIN
                                : ParticleTypes.SMOKE;
                            this.minecraft
                                .level
                                .addParticle(
                                    particleoptions,
                                    (double)blockpos1.getX() + d0,
                                    (double)blockpos1.getY() + d4,
                                    (double)blockpos1.getZ() + d1,
                                    0.0,
                                    0.0,
                                    0.0
                                );
                        }
                    }
                }

                if (blockpos1 != null && randomsource.nextInt(3) < this.rainSoundTime++) {
                    this.rainSoundTime = 0;
                    if (blockpos1.getY() > blockpos.getY() + 1
                        && levelreader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos).getY() > Mth.floor((float)blockpos.getY())) {
                        this.minecraft.level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
                    } else {
                        this.minecraft.level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        if (this.entityEffect != null) {
            this.entityEffect.close();
        }

        if (this.transparencyChain != null) {
            this.transparencyChain.close();
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        this.initOutline();
        if (Minecraft.useShaderTransparency()) {
            this.initTransparency();
        }
    }

    public void initOutline() {
        if (this.entityEffect != null) {
            this.entityEffect.close();
        }

        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("shaders/post/entity_outline.json");

        try {
            this.entityEffect = new PostChain(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), resourcelocation);
            this.entityEffect.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
            this.entityTarget = this.entityEffect.getTempTarget("final");
        } catch (IOException ioexception) {
            LOGGER.warn("Failed to load shader: {}", resourcelocation, ioexception);
            this.entityEffect = null;
            this.entityTarget = null;
        } catch (JsonSyntaxException jsonsyntaxexception) {
            LOGGER.warn("Failed to parse shader: {}", resourcelocation, jsonsyntaxexception);
            this.entityEffect = null;
            this.entityTarget = null;
        }
    }

    private void initTransparency() {
        this.deinitTransparency();
        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("shaders/post/transparency.json");

        try {
            PostChain postchain = new PostChain(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), resourcelocation);
            postchain.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
            RenderTarget rendertarget1 = postchain.getTempTarget("translucent");
            RenderTarget rendertarget2 = postchain.getTempTarget("itemEntity");
            RenderTarget rendertarget3 = postchain.getTempTarget("particles");
            RenderTarget rendertarget4 = postchain.getTempTarget("weather");
            RenderTarget rendertarget = postchain.getTempTarget("clouds");
            this.transparencyChain = postchain;
            this.translucentTarget = rendertarget1;
            this.itemEntityTarget = rendertarget2;
            this.particlesTarget = rendertarget3;
            this.weatherTarget = rendertarget4;
            this.cloudsTarget = rendertarget;
        } catch (Exception exception1) {
            String s = exception1 instanceof JsonSyntaxException ? "parse" : "load";
            String s1 = "Failed to " + s + " shader: " + resourcelocation;
            LevelRenderer.TransparencyShaderException levelrenderer$transparencyshaderexception = new LevelRenderer.TransparencyShaderException(s1, exception1);
            if (this.minecraft.getResourcePackRepository().getSelectedIds().size() > 1) {
                Component component = this.minecraft
                    .getResourceManager()
                    .listPacks()
                    .findFirst()
                    .map(packResourcesIn -> Component.literal(packResourcesIn.packId()))
                    .orElse(null);
                this.minecraft.options.graphicsMode().set(GraphicsStatus.FANCY);
                this.minecraft.clearResourcePacksOnError(levelrenderer$transparencyshaderexception, component, null);
            } else {
                this.minecraft.options.graphicsMode().set(GraphicsStatus.FANCY);
                this.minecraft.options.save();
                LOGGER.error(LogUtils.FATAL_MARKER, s1, (Throwable)levelrenderer$transparencyshaderexception);
                this.minecraft.emergencySaveAndCrash(new CrashReport(s1, levelrenderer$transparencyshaderexception));
            }
        }
    }

    private void deinitTransparency() {
        if (this.transparencyChain != null) {
            this.transparencyChain.close();
            this.translucentTarget.destroyBuffers();
            this.itemEntityTarget.destroyBuffers();
            this.particlesTarget.destroyBuffers();
            this.weatherTarget.destroyBuffers();
            this.cloudsTarget.destroyBuffers();
            this.transparencyChain = null;
            this.translucentTarget = null;
            this.itemEntityTarget = null;
            this.particlesTarget = null;
            this.weatherTarget = null;
            this.cloudsTarget = null;
        }
    }

    public void doEntityOutline() {
        if (this.shouldShowEntityOutlines()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE
            );
            this.entityTarget.blitToScreen(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), false);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }

    public boolean shouldShowEntityOutlines() {
        return !Config.isShaders() && !Config.isAntialiasing()
            ? !this.minecraft.gameRenderer.isPanoramicMode() && this.entityTarget != null && this.entityEffect != null && this.minecraft.player != null
            : false;
    }

    private void createDarkSky() {
        if (this.darkBuffer != null) {
            this.darkBuffer.close();
        }

        this.darkBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        this.darkBuffer.bind();
        this.darkBuffer.upload(buildSkyDisc(Tesselator.getInstance(), -16.0F));
        VertexBuffer.unbind();
    }

    private void createLightSky() {
        if (this.skyBuffer != null) {
            this.skyBuffer.close();
        }

        this.skyBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        this.skyBuffer.bind();
        this.skyBuffer.upload(buildSkyDisc(Tesselator.getInstance(), 16.0F));
        VertexBuffer.unbind();
    }

    private static MeshData buildSkyDisc(Tesselator pTesselator, float pY) {
        float f = Math.signum(pY) * 512.0F;
        float f1 = 512.0F;
        BufferBuilder bufferbuilder = pTesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        bufferbuilder.addVertex(0.0F, pY, 0.0F);

        for (int i = -180; i <= 180; i += 45) {
            bufferbuilder.addVertex(
                f * Mth.cos((float)i * (float) (Math.PI / 180.0)), pY, 512.0F * Mth.sin((float)i * (float) (Math.PI / 180.0))
            );
        }

        return bufferbuilder.buildOrThrow();
    }

    private void createStars() {
        if (this.starBuffer != null) {
            this.starBuffer.close();
        }

        this.starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        this.starBuffer.bind();
        this.starBuffer.upload(this.drawStars(Tesselator.getInstance()));
        VertexBuffer.unbind();
    }

    private MeshData drawStars(Tesselator pTesselator) {
        RandomSource randomsource = RandomSource.create(10842L);
        int i = 1500;
        float f = 100.0F;
        BufferBuilder bufferbuilder = pTesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for (int j = 0; j < 1500; j++) {
            float f1 = randomsource.nextFloat() * 2.0F - 1.0F;
            float f2 = randomsource.nextFloat() * 2.0F - 1.0F;
            float f3 = randomsource.nextFloat() * 2.0F - 1.0F;
            float f4 = 0.15F + randomsource.nextFloat() * 0.1F;
            float f5 = Mth.lengthSquared(f1, f2, f3);
            if (!(f5 <= 0.010000001F) && !(f5 >= 1.0F)) {
                Vector3f vector3f = new Vector3f(f1, f2, f3).normalize(100.0F);
                float f6 = (float)(randomsource.nextDouble() * (float) Math.PI * 2.0);
                Quaternionf quaternionf = new Quaternionf().rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), vector3f).rotateZ(f6);
                bufferbuilder.addVertex(vector3f.add(new Vector3f(f4, -f4, 0.0F).rotate(quaternionf)));
                bufferbuilder.addVertex(vector3f.add(new Vector3f(f4, f4, 0.0F).rotate(quaternionf)));
                bufferbuilder.addVertex(vector3f.add(new Vector3f(-f4, f4, 0.0F).rotate(quaternionf)));
                bufferbuilder.addVertex(vector3f.add(new Vector3f(-f4, -f4, 0.0F).rotate(quaternionf)));
            }
        }

        return bufferbuilder.buildOrThrow();
    }

    public void setLevel(@Nullable ClientLevel pLevel) {
        this.lastCameraSectionX = Integer.MIN_VALUE;
        this.lastCameraSectionY = Integer.MIN_VALUE;
        this.lastCameraSectionZ = Integer.MIN_VALUE;
        this.entityRenderDispatcher.setLevel(pLevel);
        this.level = pLevel;
        if (Config.isDynamicLights()) {
            DynamicLights.clear();
        }

        ChunkVisibility.reset();
        this.renderEnv.reset(null, null);
        BiomeUtils.onWorldChanged(this.level);
        Shaders.checkWorldChanged(this.level);
        if (pLevel != null) {
            this.allChanged();
        } else {
            if (this.viewArea != null) {
                this.viewArea.releaseAllBuffers();
                this.viewArea = null;
            }

            if (this.sectionRenderDispatcher != null) {
                this.sectionRenderDispatcher.dispose();
            }

            this.sectionRenderDispatcher = null;
            this.globalBlockEntities.clear();
            this.sectionOcclusionGraph.waitAndReset(null);
            this.visibleSections.clear();
            this.clearRenderInfos();
        }
    }

    public void graphicsChanged() {
        if (Minecraft.useShaderTransparency()) {
            this.initTransparency();
        } else {
            this.deinitTransparency();
        }
    }

    public void allChanged() {
        if (this.level != null) {
            this.graphicsChanged();
            this.level.clearTintCaches();
            if (this.sectionRenderDispatcher == null) {
                this.sectionRenderDispatcher = new SectionRenderDispatcher(
                    this.level, this, Util.backgroundExecutor(), this.renderBuffers, this.minecraft.getBlockRenderer(), this.minecraft.getBlockEntityRenderDispatcher()
                );
            } else {
                this.sectionRenderDispatcher.setLevel(this.level);
            }

            this.generateClouds = true;
            ItemBlockRenderTypes.setFancy(Config.isTreesFancy());
            ModelBlockRenderer.updateAoLightValue();
            if (Config.isDynamicLights()) {
                DynamicLights.clear();
            }

            SmartAnimations.update();
            ambientOcclusion = Minecraft.useAmbientOcclusion();
            this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();
            this.renderDistance = this.lastViewDistance * 16;
            this.renderDistanceSq = this.renderDistance * this.renderDistance;
            double d0 = (double)((this.lastViewDistance + 1) * 16);
            this.renderDistanceXZSq = (int)(d0 * d0);
            if (this.viewArea != null) {
                this.viewArea.releaseAllBuffers();
            }

            GpuMemory.bufferFreed(GpuMemory.getBufferAllocated());
            this.sectionRenderDispatcher.blockUntilClear();
            synchronized (this.globalBlockEntities) {
                this.globalBlockEntities.clear();
            }

            this.viewArea = new ViewArea(this.sectionRenderDispatcher, this.level, this.minecraft.options.getEffectiveRenderDistance(), this);
            this.sectionOcclusionGraph.waitAndReset(this.viewArea);
            this.visibleSections.clear();
            this.clearRenderInfos();
            this.killFrustum();
            Entity entity = this.minecraft.getCameraEntity();
            if (entity != null) {
                this.viewArea.repositionCamera(entity.getX(), entity.getZ());
            }
        }
    }

    public void resize(int pWidth, int pHeight) {
        this.needsUpdate();
        if (this.entityEffect != null) {
            this.entityEffect.resize(pWidth, pHeight);
        }

        if (this.transparencyChain != null) {
            this.transparencyChain.resize(pWidth, pHeight);
        }
    }

    public String getSectionStatistics() {
        int i = this.viewArea.sections.length;
        int j = this.countRenderedSections();
        return String.format(
            Locale.ROOT,
            "C: %d/%d %sD: %d, %s",
            j,
            i,
            this.minecraft.smartCull ? "(s) " : "",
            this.lastViewDistance,
            this.sectionRenderDispatcher == null ? "null" : this.sectionRenderDispatcher.getStats()
        );
    }

    public SectionRenderDispatcher getSectionRenderDispatcher() {
        return this.sectionRenderDispatcher;
    }

    public double getTotalSections() {
        return (double)this.viewArea.sections.length;
    }

    public double getLastViewDistance() {
        return (double)this.lastViewDistance;
    }

    public int countRenderedSections() {
        return this.renderInfosTerrain.size();
    }

    public String getEntityStatistics() {
        return "E: "
            + this.renderedEntities
            + "/"
            + this.level.getEntityCount()
            + ", B: "
            + this.culledEntities
            + ", SD: "
            + this.level.getServerSimulationDistance()
            + ", "
            + Config.getVersionDebug();
    }

    private void setupRender(Camera pCamera, Frustum pFrustum, boolean pHasCapturedFrustum, boolean pIsSpectator) {
        Vec3 vec3 = pCamera.getPosition();
        if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
            this.allChanged();
        }

        this.level.getProfiler().push("camera");
        double d0 = this.minecraft.player.getX();
        double d1 = this.minecraft.player.getY();
        double d2 = this.minecraft.player.getZ();
        int i = SectionPos.posToSectionCoord(d0);
        int j = SectionPos.posToSectionCoord(d1);
        int k = SectionPos.posToSectionCoord(d2);
        if (this.lastCameraSectionX != i || this.lastCameraSectionY != j || this.lastCameraSectionZ != k) {
            this.lastCameraSectionX = i;
            this.lastCameraSectionY = j;
            this.lastCameraSectionZ = k;
            this.viewArea.repositionCamera(d0, d2);
        }

        if (Config.isDynamicLights()) {
            DynamicLights.update(this);
        }

        this.sectionRenderDispatcher.setCamera(vec3);
        this.level.getProfiler().popPush("cull");
        this.minecraft.getProfiler().popPush("culling");
        BlockPos blockpos = pCamera.getBlockPosition();
        double d3 = Math.floor(vec3.x / 8.0);
        double d4 = Math.floor(vec3.y / 8.0);
        double d5 = Math.floor(vec3.z / 8.0);
        if (d3 != this.prevCamX || d4 != this.prevCamY || d5 != this.prevCamZ) {
            this.sectionOcclusionGraph.invalidate();
        }

        this.prevCamX = d3;
        this.prevCamY = d4;
        this.prevCamZ = d5;
        this.minecraft.getProfiler().popPush("update");
        Lagometer.timerVisibility.start();
        if (!pHasCapturedFrustum) {
            boolean flag = this.minecraft.smartCull;
            if (pIsSpectator && this.level.getBlockState(blockpos).isSolidRender(this.level, blockpos)) {
                flag = false;
            }

            Entity.setViewScale(Mth.clamp((double)this.minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0, 2.5) * this.minecraft.options.entityDistanceScaling().get());
            this.minecraft.getProfiler().push("section_occlusion_graph");
            this.sectionOcclusionGraph.update(flag, pCamera, pFrustum, this.visibleSections);
            this.minecraft.getProfiler().pop();
            double d6 = Math.floor((double)(pCamera.getXRot() / 2.0F));
            double d7 = Math.floor((double)(pCamera.getYRot() / 2.0F));
            boolean flag1 = false;
            if (this.sectionOcclusionGraph.consumeFrustumUpdate() || d6 != this.prevCamRotX || d7 != this.prevCamRotY) {
                this.applyFrustum(offsetFrustum(pFrustum));
                this.prevCamRotX = d6;
                this.prevCamRotY = d7;
                flag1 = true;
                ShadersRender.frustumTerrainShadowChanged = true;
            }

            if (this.level.getSectionStorage().resetUpdated() || flag1) {
                this.applyFrustumEntities(pFrustum, -1);
                ShadersRender.frustumEntitiesShadowChanged = true;
            }
        }

        Lagometer.timerVisibility.end();
        this.minecraft.getProfiler().pop();
    }

    public static Frustum offsetFrustum(Frustum pFrustum) {
        return new Frustum(pFrustum).offsetToFullyIncludeCameraCube(8);
    }

    private void applyFrustum(Frustum pFrustum) {
        this.applyFrustum(pFrustum, true, -1);
    }

    public void applyFrustum(Frustum frustumIn, boolean updateRenderInfos, int maxChunkDistance) {
        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
        } else {
            this.minecraft.getProfiler().push("apply_frustum");
            if (updateRenderInfos) {
                this.visibleSections.clear();
            }

            this.clearRenderInfosTerrain();
            this.sectionOcclusionGraph.addSectionsInFrustum(frustumIn, this.visibleSections, updateRenderInfos, maxChunkDistance);
            this.minecraft.getProfiler().pop();
        }
    }

    public void addRecentlyCompiledSection(SectionRenderDispatcher.RenderSection pRenderSection) {
        this.sectionOcclusionGraph.onSectionCompiled(pRenderSection);
    }

    private void captureFrustum(Matrix4f pViewMatrix, Matrix4f pProjectionMatrix, double pCamX, double pCamY, double pCamZ, Frustum pCapturedFrustrum) {
        this.capturedFrustum = pCapturedFrustrum;
        Matrix4f matrix4f = new Matrix4f(pProjectionMatrix);
        matrix4f.mul(pViewMatrix);
        matrix4f.invert();
        this.frustumPos.x = pCamX;
        this.frustumPos.y = pCamY;
        this.frustumPos.z = pCamZ;
        this.frustumPoints[0] = new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F);
        this.frustumPoints[1] = new Vector4f(1.0F, -1.0F, -1.0F, 1.0F);
        this.frustumPoints[2] = new Vector4f(1.0F, 1.0F, -1.0F, 1.0F);
        this.frustumPoints[3] = new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F);
        this.frustumPoints[4] = new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F);
        this.frustumPoints[5] = new Vector4f(1.0F, -1.0F, 1.0F, 1.0F);
        this.frustumPoints[6] = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.frustumPoints[7] = new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F);

        for (int i = 0; i < 8; i++) {
            matrix4f.transform(this.frustumPoints[i]);
            this.frustumPoints[i].div(this.frustumPoints[i].w());
        }
    }

    public void prepareCullFrustum(Vec3 pCameraPosition, Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix) {
        this.cullingFrustum = new Frustum(pFrustumMatrix, pProjectionMatrix);
        this.cullingFrustum.prepare(pCameraPosition.x(), pCameraPosition.y(), pCameraPosition.z());
        if (Config.isShaders() && !Shaders.isFrustumCulling()) {
            this.cullingFrustum.disabled = true;
        }
    }

    public void renderLevel(
        DeltaTracker pDeltaTracker, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix
    ) {
        TickRateManager tickratemanager = this.minecraft.level.tickRateManager();
        float f = pDeltaTracker.getGameTimeDeltaPartialTick(false);
        RenderSystem.setShaderGameTime(this.level.getGameTime(), f);
        this.blockEntityRenderDispatcher.prepare(this.level, pCamera, this.minecraft.hitResult);
        this.entityRenderDispatcher.prepare(this.level, pCamera, this.minecraft.crosshairPickEntity);
        ProfilerFiller profilerfiller = this.level.getProfiler();
        profilerfiller.popPush("light_update_queue");
        this.level.pollLightUpdates();
        profilerfiller.popPush("light_updates");
        this.level.getChunkSource().getLightEngine().runLightUpdates();
        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        profilerfiller.popPush("culling");
        boolean flag = this.capturedFrustum != null;
        Frustum frustum;
        if (flag) {
            frustum = this.capturedFrustum;
            frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
        } else {
            frustum = this.cullingFrustum;
        }

        this.minecraft.getProfiler().popPush("captureFrustum");
        if (this.captureFrustum) {
            this.captureFrustum(pFrustumMatrix, pProjectionMatrix, vec3.x, vec3.y, vec3.z, flag ? new Frustum(pFrustumMatrix, pProjectionMatrix) : frustum);
            this.captureFrustum = false;
            frustum = this.capturedFrustum;
            frustum.disabled = Config.isShaders() && !Shaders.isFrustumCulling();
            frustum.prepare(vec3.x, vec3.y, vec3.z);
            this.applyFrustum(frustum, false, -1);
            this.applyFrustumEntities(frustum, -1);
        }

        if (this.debugFixTerrainFrustumShadow) {
            this.captureFrustum(pFrustumMatrix, pProjectionMatrix, vec3.x, vec3.y, vec3.z, ShadersRender.makeShadowFrustum(pCamera, f));
            this.debugFixTerrainFrustumShadow = false;
            frustum = this.capturedFrustum;
            frustum.prepare(vec3.x, vec3.y, vec3.z);
            ShadersRender.frustumTerrainShadowChanged = true;
            ShadersRender.frustumEntitiesShadowChanged = true;
            ShadersRender.applyFrustumShadow(this, frustum);
        }

        profilerfiller.popPush("clear");
        if (Config.isShaders()) {
            Shaders.setViewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
        } else {
            RenderSystem.viewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
        }

        FogRenderer.setupColor(pCamera, f, this.minecraft.level, this.minecraft.options.getEffectiveRenderDistance(), pGameRenderer.getDarkenWorldAmount(f));
        FogRenderer.levelFogColor();
        RenderSystem.clear(16640, Minecraft.ON_OSX);
        boolean flag1 = Config.isShaders();
        if (flag1) {
            Shaders.clearRenderBuffer();
            Shaders.setCamera(pFrustumMatrix, pCamera, f);
            Shaders.renderPrepare();
        }

        float f1 = pGameRenderer.getRenderDistance();
        boolean flag2 = this.minecraft.level.effects().isFoggyAt(Mth.floor(d0), Mth.floor(d1)) || this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
        boolean flag3 = this.minecraft.level.effects().isFoggyAt(Mth.floor(d0), Mth.floor(d1));
        if ((Config.isSkyEnabled() || Config.isSunMoonEnabled() || Config.isStarsEnabled()) && !Shaders.isShadowPass) {
            profilerfiller.popPush("sky");
            if (flag1) {
                Shaders.beginSky();
            }

            RenderSystem.setShader(GameRenderer::getPositionShader);
            this.renderSky(pFrustumMatrix, pProjectionMatrix, f, pCamera, flag3, () -> FogRenderer.setupFog(pCamera, FogRenderer.FogMode.FOG_SKY, f1, flag2, f));
            if (flag1) {
                Shaders.endSky();
            }
        } else {
            GlStateManager._disableBlend();
        }

        ReflectorForge.dispatchRenderStageS(Reflector.RenderLevelStageEvent_Stage_AFTER_SKY, this, pFrustumMatrix, pProjectionMatrix, this.ticks, pCamera, frustum);
        profilerfiller.popPush("fog");
        FogRenderer.setupFog(pCamera, FogRenderer.FogMode.FOG_TERRAIN, Math.max(f1, 32.0F), flag2, f);
        profilerfiller.popPush("terrain_setup");
        this.checkLoadVisibleChunks(pCamera, frustum, this.minecraft.player.isSpectator());
        this.frameId++;
        this.setupRender(pCamera, frustum, flag, this.minecraft.player.isSpectator());
        profilerfiller.popPush("compile_sections");
        this.compileSections(pCamera);
        profilerfiller.popPush("terrain");
        Lagometer.timerTerrain.start();
        if (this.minecraft.options.ofSmoothFps) {
            this.minecraft.getProfiler().popPush("finish");
            GL11.glFinish();
            this.minecraft.getProfiler().popPush("terrain");
        }

        if (Config.isFogOff() && FogRenderer.fogStandard) {
            RenderSystem.setFogAllowed(false);
        }

        this.renderSectionLayer(RenderType.solid(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
        this.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).setBlurMipmap(false, this.minecraft.options.mipmapLevels().get() > 0);
        this.renderSectionLayer(RenderType.cutoutMipped(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
        this.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).restoreLastBlurMipmap();
        this.renderSectionLayer(RenderType.cutout(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
        if (flag1) {
            ShadersRender.endTerrain();
        }

        if (this.level.effects().constantAmbientLight()) {
            Lighting.setupNetherLevel();
        } else {
            Lighting.setupLevel();
        }

        if (flag1) {
            Shaders.beginEntities();
        }

        ItemFrameRenderer.updateItemRenderDistance();
        profilerfiller.popPush("entities");
        renderEntitiesCounter++;
        this.renderedEntities = 0;
        this.culledEntities = 0;
        this.countTileEntitiesRendered = 0;
        if (this.itemEntityTarget != null) {
            this.itemEntityTarget.clear(Minecraft.ON_OSX);
            this.itemEntityTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
            this.minecraft.getMainRenderTarget().bindWrite(false);
        }

        if (this.weatherTarget != null) {
            this.weatherTarget.clear(Minecraft.ON_OSX);
        }

        if (this.shouldShowEntityOutlines()) {
            this.entityTarget.clear(Minecraft.ON_OSX);
            this.minecraft.getMainRenderTarget().bindWrite(false);
        }

        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.mul(pFrustumMatrix);
        RenderSystem.applyModelViewMatrix();
        boolean flag4 = false;
        PoseStack posestack = new PoseStack();
        MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
        if (Config.isFastRender()) {
            RenderStateManager.enableCache();
            multibuffersource$buffersource.enableCache();
        }

        int i = this.level.getMinBuildHeight();
        int j = this.level.getMaxBuildHeight();
        if (Config.isRenderRegions() || Config.isMultiTexture()) {
            GameRenderer.getPositionShader().apply();
        }

        for (Entity entity : this.level.entitiesForRendering()) {
            if (this.shouldRenderEntity(entity, i, j)) {
                boolean flag5 = entity == this.minecraft.player && !this.minecraft.player.isSpectator();
                if (this.entityRenderDispatcher.shouldRender(entity, frustum, d0, d1, d2) || entity.hasIndirectPassenger(this.minecraft.player)) {
                    BlockPos blockpos = entity.blockPosition();
                    if ((this.level.isOutsideBuildHeight(blockpos.getY()) || this.isSectionCompiled(blockpos))
                        && (
                            entity != pCamera.getEntity()
                                || pCamera.isDetached()
                                || pCamera.getEntity() instanceof LivingEntity && ((LivingEntity)pCamera.getEntity()).isSleeping()
                        )
                        && (!(entity instanceof LocalPlayer) || pCamera.getEntity() == entity || flag5)) {
                        String s = entity.getClass().getName();
                        List<Entity> list = this.mapEntityLists.get(s);
                        if (list == null) {
                            list = new ArrayList<>();
                            this.mapEntityLists.put(s, list);
                        }

                        list.add(entity);
                    }
                }
            }
        }

        for (List<Entity> list1 : this.mapEntityLists.values()) {
            for (Entity entity1 : list1) {
                this.renderedEntities++;
                if (entity1.tickCount == 0) {
                    entity1.xOld = entity1.getX();
                    entity1.yOld = entity1.getY();
                    entity1.zOld = entity1.getZ();
                }

                MultiBufferSource multibuffersource1;
                if (this.shouldShowEntityOutlines() && this.minecraft.shouldEntityAppearGlowing(entity1)) {
                    flag4 = true;
                    OutlineBufferSource outlinebuffersource = this.renderBuffers.outlineBufferSource();
                    multibuffersource1 = outlinebuffersource;
                    int k = entity1.getTeamColor();
                    outlinebuffersource.setColor(FastColor.ARGB32.red(k), FastColor.ARGB32.green(k), FastColor.ARGB32.blue(k), 255);
                } else {
                    if (Reflector.IForgeEntity_hasCustomOutlineRendering.exists()
                        && this.shouldShowEntityOutlines()
                        && Reflector.callBoolean(entity1, Reflector.IForgeEntity_hasCustomOutlineRendering, this.minecraft.player)) {
                        flag4 = true;
                    }

                    multibuffersource1 = multibuffersource$buffersource;
                }

                if (flag1) {
                    Shaders.nextEntity(entity1);
                }

                float f2 = pDeltaTracker.getGameTimeDeltaPartialTick(!tickratemanager.isEntityFrozen(entity1));
                this.renderEntity(entity1, d0, d1, d2, f2, posestack, multibuffersource1);
            }

            list1.clear();
        }

        multibuffersource$buffersource.endLastBatch();
        this.checkPoseStack(posestack);
        multibuffersource$buffersource.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        multibuffersource$buffersource.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        multibuffersource$buffersource.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        multibuffersource$buffersource.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
        if (Config.isFastRender()) {
            multibuffersource$buffersource.flushCache();
            RenderStateManager.flushCache();
        }

        if (flag1) {
            Shaders.endEntities();
        }

        ReflectorForge.dispatchRenderStageS(
            Reflector.RenderLevelStageEvent_Stage_AFTER_ENTITIES, this, pFrustumMatrix, pProjectionMatrix, this.ticks, pCamera, frustum
        );
        if (flag1) {
            Shaders.beginBlockEntities();
        }

        profilerfiller.popPush("blockentities");
        SignRenderer.updateTextRenderDistance();
        boolean flag6 = Reflector.IForgeBlockEntity_getRenderBoundingBox.exists();
        Frustum frustum1 = frustum;

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.renderInfosTileEntities) {
            List<BlockEntity> list2 = sectionrenderdispatcher$rendersection.getCompiled().getRenderableBlockEntities();
            if (!list2.isEmpty()) {
                for (BlockEntity blockentity1 : list2) {
                    if (flag6) {
                        AABB aabb = (AABB)Reflector.call(blockentity1, Reflector.IForgeBlockEntity_getRenderBoundingBox);
                        if (aabb != null && !frustum1.isVisible(aabb)) {
                            continue;
                        }
                    }

                    if (flag1) {
                        Shaders.nextBlockEntity(blockentity1);
                    }

                    BlockPos blockpos4 = blockentity1.getBlockPos();
                    MultiBufferSource multibuffersource = multibuffersource$buffersource;
                    posestack.pushPose();
                    posestack.translate((double)blockpos4.getX() - d0, (double)blockpos4.getY() - d1, (double)blockpos4.getZ() - d2);
                    SortedSet<BlockDestructionProgress> sortedset = this.destructionProgress.get(blockpos4.asLong());
                    if (sortedset != null && !sortedset.isEmpty()) {
                        int l = sortedset.last().getProgress();
                        if (l >= 0) {
                            PoseStack.Pose posestack$pose = posestack.last();
                            VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(
                                this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(l)), posestack$pose, 1.0F
                            );
                            multibuffersource = renderTypeIn -> {
                                VertexConsumer vertexconsumer3 = multibuffersource$buffersource.getBuffer(renderTypeIn);
                                return renderTypeIn.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexconsumer3) : vertexconsumer3;
                            };
                        }
                    }

                    if (Reflector.IForgeBlockEntity_hasCustomOutlineRendering.exists()
                        && this.shouldShowEntityOutlines()
                        && Reflector.callBoolean(blockentity1, Reflector.IForgeBlockEntity_hasCustomOutlineRendering, this.minecraft.player)) {
                        flag4 = true;
                    }

                    this.blockEntityRenderDispatcher.render(blockentity1, f, posestack, multibuffersource);
                    posestack.popPose();
                    this.countTileEntitiesRendered++;
                }
            }
        }

        synchronized (this.globalBlockEntities) {
            for (BlockEntity blockentity : this.globalBlockEntities) {
                if (flag6) {
                    AABB aabb1 = (AABB)Reflector.call(blockentity, Reflector.IForgeBlockEntity_getRenderBoundingBox);
                    if (aabb1 != null && !frustum1.isVisible(aabb1)) {
                        continue;
                    }
                }

                if (flag1) {
                    Shaders.nextBlockEntity(blockentity);
                }

                BlockPos blockpos3 = blockentity.getBlockPos();
                posestack.pushPose();
                posestack.translate((double)blockpos3.getX() - d0, (double)blockpos3.getY() - d1, (double)blockpos3.getZ() - d2);
                if (Reflector.IForgeBlockEntity_hasCustomOutlineRendering.exists()
                    && this.shouldShowEntityOutlines()
                    && Reflector.callBoolean(blockentity, Reflector.IForgeBlockEntity_hasCustomOutlineRendering, this.minecraft.player)) {
                    flag4 = true;
                }

                this.blockEntityRenderDispatcher.render(blockentity, f, posestack, multibuffersource$buffersource);
                posestack.popPose();
                this.countTileEntitiesRendered++;
            }
        }

        this.checkPoseStack(posestack);
        multibuffersource$buffersource.endBatch(RenderType.solid());
        multibuffersource$buffersource.endBatch(RenderType.endPortal());
        multibuffersource$buffersource.endBatch(RenderType.endGateway());
        multibuffersource$buffersource.endBatch(Sheets.solidBlockSheet());
        multibuffersource$buffersource.endBatch(Sheets.cutoutBlockSheet());
        multibuffersource$buffersource.endBatch(Sheets.bedSheet());
        multibuffersource$buffersource.endBatch(Sheets.shulkerBoxSheet());
        multibuffersource$buffersource.endBatch(Sheets.signSheet());
        multibuffersource$buffersource.endBatch(Sheets.hangingSignSheet());
        multibuffersource$buffersource.endBatch(Sheets.chestSheet());
        this.renderBuffers.outlineBufferSource().endOutlineBatch();
        if (Config.isFastRender()) {
            multibuffersource$buffersource.disableCache();
            RenderStateManager.disableCache();
        }

        Lagometer.timerTerrain.end();
        if (flag4) {
            this.entityEffect.process(pDeltaTracker.getGameTimeDeltaTicks());
            this.minecraft.getMainRenderTarget().bindWrite(false);
        }

        if (flag1) {
            Shaders.endBlockEntities();
        }

        ReflectorForge.dispatchRenderStageS(
            Reflector.RenderLevelStageEvent_Stage_AFTER_BLOCK_ENTITIES, this, pFrustumMatrix, pProjectionMatrix, this.ticks, pCamera, frustum
        );
        this.renderOverlayDamaged = true;
        profilerfiller.popPush("destroyProgress");

        for (Entry<SortedSet<BlockDestructionProgress>> entry : this.destructionProgress.long2ObjectEntrySet()) {
            BlockPos blockpos2 = BlockPos.of(entry.getLongKey());
            double d3 = (double)blockpos2.getX() - d0;
            double d4 = (double)blockpos2.getY() - d1;
            double d5 = (double)blockpos2.getZ() - d2;
            if (!(d3 * d3 + d4 * d4 + d5 * d5 > 1024.0)) {
                SortedSet<BlockDestructionProgress> sortedset1 = entry.getValue();
                if (sortedset1 != null && !sortedset1.isEmpty()) {
                    int i1 = sortedset1.last().getProgress();
                    posestack.pushPose();
                    posestack.translate((double)blockpos2.getX() - d0, (double)blockpos2.getY() - d1, (double)blockpos2.getZ() - d2);
                    PoseStack.Pose posestack$pose1 = posestack.last();
                    VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(
                        this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(i1)), posestack$pose1, 1.0F
                    );
                    ModelData modeldata = this.level.getModelDataManager().getAt(blockpos2);
                    if (modeldata == null) {
                        modeldata = ModelData.EMPTY;
                    }

                    this.minecraft
                        .getBlockRenderer()
                        .renderBreakingTexture(this.level.getBlockState(blockpos2), blockpos2, this.level, posestack, vertexconsumer1, modeldata);
                    posestack.popPose();
                }
            }
        }

        this.renderOverlayDamaged = false;
        RenderUtils.flushRenderBuffers();
        renderEntitiesCounter--;
        this.checkPoseStack(posestack);
        HitResult hitresult = this.minecraft.hitResult;
        if (pRenderBlockOutline && hitresult != null && hitresult.getType() == HitResult.Type.BLOCK) {
            profilerfiller.popPush("outline");
            BlockPos blockpos1 = ((BlockHitResult)hitresult).getBlockPos();
            BlockState blockstate = this.level.getBlockState(blockpos1);
            if (flag1) {
                ShadersRender.beginOutline();
            }

            if (!Reflector.callBoolean(Reflector.ForgeHooksClient_onDrawHighlight, this, pCamera, hitresult, f, posestack, multibuffersource$buffersource)
                && !blockstate.isAir()
                && this.level.getWorldBorder().isWithinBounds(blockpos1)) {
                VertexConsumer vertexconsumer2 = multibuffersource$buffersource.getBuffer(RenderType.lines());
                this.renderHitOutline(posestack, vertexconsumer2, pCamera.getEntity(), d0, d1, d2, blockpos1, blockstate);
            }

            if (flag1) {
                multibuffersource$buffersource.endBatch(RenderType.lines());
                ShadersRender.endOutline();
            }
        } else if (hitresult != null && hitresult.getType() == HitResult.Type.ENTITY) {
            Reflector.ForgeHooksClient_onDrawHighlight.call(this, pCamera, hitresult, f, posestack, multibuffersource$buffersource);
        }

        this.minecraft.debugRenderer.render(posestack, multibuffersource$buffersource, d0, d1, d2);
        multibuffersource$buffersource.endLastBatch();
        if (flag1) {
            RenderUtils.finishRenderBuffers();
            ShadersRender.beginDebug();
        }

        multibuffersource$buffersource.endBatch(Sheets.translucentCullBlockSheet());
        multibuffersource$buffersource.endBatch(Sheets.bannerSheet());
        multibuffersource$buffersource.endBatch(Sheets.shieldSheet());
        multibuffersource$buffersource.endBatch(RenderType.armorEntityGlint());
        multibuffersource$buffersource.endBatch(RenderType.glint());
        multibuffersource$buffersource.endBatch(RenderType.glintTranslucent());
        multibuffersource$buffersource.endBatch(RenderType.entityGlint());
        multibuffersource$buffersource.endBatch(RenderType.entityGlintDirect());
        multibuffersource$buffersource.endBatch(RenderType.waterMask());
        this.renderBuffers.crumblingBufferSource().endBatch();
        if (flag1) {
            multibuffersource$buffersource.endBatch();
            ShadersRender.endDebug();
            Shaders.preRenderHand();
            Matrix4f matrix4f = MathUtils.copy(RenderSystem.getProjectionMatrix());
            ShadersRender.renderHand0(pGameRenderer, pFrustumMatrix, pCamera, f);
            RenderSystem.setProjectionMatrix(matrix4f, RenderSystem.getVertexSorting());
            Shaders.preWater();
        }

        if (this.transparencyChain != null) {
            multibuffersource$buffersource.endBatch(RenderType.lines());
            multibuffersource$buffersource.endBatch();
            this.translucentTarget.clear(Minecraft.ON_OSX);
            this.translucentTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
            profilerfiller.popPush("translucent");
            this.renderSectionLayer(RenderType.translucent(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            profilerfiller.popPush("string");
            this.renderSectionLayer(RenderType.tripwire(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            this.particlesTarget.clear(Minecraft.ON_OSX);
            this.particlesTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
            RenderStateShard.PARTICLES_TARGET.setupRenderState();
            profilerfiller.popPush("particles");
            this.minecraft.particleEngine.render(pLightTexture, pCamera, f, frustum);
            ReflectorForge.dispatchRenderStageS(
                Reflector.RenderLevelStageEvent_Stage_AFTER_PARTICLES, this, pFrustumMatrix, pProjectionMatrix, this.ticks, pCamera, frustum
            );
            RenderStateShard.PARTICLES_TARGET.clearRenderState();
        } else {
            profilerfiller.popPush("translucent");
            Lagometer.timerTerrain.start();
            if (Shaders.isParticlesBeforeDeferred()) {
                Shaders.beginParticles();
                this.minecraft.particleEngine.render(pLightTexture, pCamera, f, frustum);
                Shaders.endParticles();
                ReflectorForge.dispatchRenderStageS(
                    Reflector.RenderLevelStageEvent_Stage_AFTER_PARTICLES, this, pFrustumMatrix, pProjectionMatrix, this.ticks, pCamera, frustum
                );
            }

            if (flag1) {
                Shaders.beginWater();
            }

            if (this.translucentTarget != null) {
                this.translucentTarget.clear(Minecraft.ON_OSX);
            }

            this.renderSectionLayer(RenderType.translucent(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            if (flag1) {
                Shaders.endWater();
            }

            Lagometer.timerTerrain.end();
            multibuffersource$buffersource.endBatch(RenderType.lines());
            multibuffersource$buffersource.endBatch();
            profilerfiller.popPush("string");
            this.renderSectionLayer(RenderType.tripwire(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            profilerfiller.popPush("particles");
            if (!Shaders.isParticlesBeforeDeferred()) {
                if (flag1) {
                    Shaders.beginParticles();
                }

                this.minecraft.particleEngine.render(pLightTexture, pCamera, f, frustum);
                if (flag1) {
                    Shaders.endParticles();
                }

                ReflectorForge.dispatchRenderStageS(
                    Reflector.RenderLevelStageEvent_Stage_AFTER_PARTICLES, this, pFrustumMatrix, pProjectionMatrix, this.ticks, pCamera, frustum
                );
            }
        }

        RenderSystem.setFogAllowed(true);
        if (this.minecraft.options.getCloudsType() != CloudStatus.OFF) {
            if (this.transparencyChain != null) {
                this.cloudsTarget.clear(Minecraft.ON_OSX);
            }

            profilerfiller.popPush("clouds");
            this.renderClouds(posestack, pFrustumMatrix, pProjectionMatrix, f, d0, d1, d2);
        }

        if (this.transparencyChain != null) {
            RenderStateShard.WEATHER_TARGET.setupRenderState();
            profilerfiller.popPush("weather");
            this.renderSnowAndRain(pLightTexture, f, d0, d1, d2);
            ReflectorForge.dispatchRenderStageS(
                Reflector.RenderLevelStageEvent_Stage_AFTER_WEATHER, this, pFrustumMatrix, pProjectionMatrix, this.ticks, pCamera, frustum
            );
            this.renderWorldBorder(pCamera);
            RenderStateShard.WEATHER_TARGET.clearRenderState();
            this.transparencyChain.process(pDeltaTracker.getGameTimeDeltaTicks());
            this.minecraft.getMainRenderTarget().bindWrite(false);
        } else {
            RenderSystem.depthMask(false);
            profilerfiller.popPush("weather");
            if (flag1) {
                Shaders.beginWeather();
            }

            this.renderSnowAndRain(pLightTexture, f, d0, d1, d2);
            if (flag1) {
                Shaders.endWeather();
            }

            ReflectorForge.dispatchRenderStageS(
                Reflector.RenderLevelStageEvent_Stage_AFTER_WEATHER, this, pFrustumMatrix, pProjectionMatrix, this.ticks, pCamera, frustum
            );
            this.renderWorldBorder(pCamera);
            RenderSystem.depthMask(true);
        }

        this.renderDebug(posestack, multibuffersource$buffersource, pCamera);
        multibuffersource$buffersource.endLastBatch();
        matrix4fstack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        FogRenderer.setupNoFog();
    }

    public void checkPoseStack(PoseStack pPoseStack) {
        if (!pPoseStack.clear()) {
            throw new IllegalStateException("Pose stack not empty");
        }
    }

    public void renderEntity(
        Entity pEntity, double pCamX, double pCamY, double pCamZ, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource
    ) {
        double d0 = Mth.lerp((double)pPartialTick, pEntity.xOld, pEntity.getX());
        double d1 = Mth.lerp((double)pPartialTick, pEntity.yOld, pEntity.getY());
        double d2 = Mth.lerp((double)pPartialTick, pEntity.zOld, pEntity.getZ());
        float f = Mth.lerp(pPartialTick, pEntity.yRotO, pEntity.getYRot());
        this.entityRenderDispatcher
            .render(
                pEntity, d0 - pCamX, d1 - pCamY, d2 - pCamZ, f, pPartialTick, pPoseStack, pBufferSource, this.entityRenderDispatcher.getPackedLightCoords(pEntity, pPartialTick)
            );
    }

    public void renderSectionLayer(RenderType pRenderType, double pX, double pY, double pZ, Matrix4f pFrustrumMatrix, Matrix4f pProjectionMatrix) {
        RenderSystem.assertOnRenderThread();
        pRenderType.setupRenderState();
        boolean flag = Config.isShaders();
        if (pRenderType == RenderType.translucent() && !Shaders.isShadowPass) {
            this.minecraft.getProfiler().push("translucent_sort");
            double d0 = pX - this.xTransparentOld;
            double d1 = pY - this.yTransparentOld;
            double d2 = pZ - this.zTransparentOld;
            if (d0 * d0 + d1 * d1 + d2 * d2 > 1.0) {
                int i = SectionPos.posToSectionCoord(pX);
                int j = SectionPos.posToSectionCoord(pY);
                int k = SectionPos.posToSectionCoord(pZ);
                boolean flag1 = i != SectionPos.posToSectionCoord(this.xTransparentOld)
                    || k != SectionPos.posToSectionCoord(this.zTransparentOld)
                    || j != SectionPos.posToSectionCoord(this.yTransparentOld);
                this.xTransparentOld = pX;
                this.yTransparentOld = pY;
                this.zTransparentOld = pZ;
                int l = 0;
                this.chunksToResortTransparency.clear();

                for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 : this.renderInfosTerrain) {
                    if (l < 15
                        && (flag1 || sectionrenderdispatcher$rendersection1.isAxisAlignedWith(i, j, k))
                        && sectionrenderdispatcher$rendersection1.getCompiled().isLayerUsed(pRenderType)) {
                        this.chunksToResortTransparency.add(sectionrenderdispatcher$rendersection1);
                        l++;
                    }
                }
            }

            this.minecraft.getProfiler().pop();
        }

        this.minecraft.getProfiler().push("filterempty");
        this.minecraft.getProfiler().popPush(() -> "render_" + pRenderType);
        boolean flag2 = pRenderType != RenderType.translucent();
        ObjectListIterator<SectionRenderDispatcher.RenderSection> objectlistiterator = this.renderInfosTerrain
            .listIterator(flag2 ? 0 : this.renderInfosTerrain.size());
        ShaderInstance shaderinstance = RenderSystem.getShader();
        shaderinstance.setDefaultUniforms(VertexFormat.Mode.QUADS, pFrustrumMatrix, pProjectionMatrix, this.minecraft.getWindow());
        shaderinstance.apply();
        Uniform uniform = shaderinstance.CHUNK_OFFSET;
        if (flag) {
            ShadersRender.preRenderChunkLayer(pRenderType);
            Shaders.setModelViewMatrix(pFrustrumMatrix);
            Shaders.setProjectionMatrix(pProjectionMatrix);
            Shaders.setTextureMatrix(RenderSystem.getTextureMatrix());
            Shaders.setColorModulator(RenderSystem.getShaderColor());
        }

        boolean flag3 = SmartAnimations.isActive();
        if (flag && Shaders.activeProgramID > 0) {
            uniform = null;
        }

        if (Config.isRenderRegions() && !pRenderType.isNeedsSorting()) {
            int i1 = Integer.MIN_VALUE;
            int j1 = Integer.MIN_VALUE;
            VboRegion vboregion2 = null;
            Map<PairInt, Map<VboRegion, List<VertexBuffer>>> map1 = (Map<PairInt, Map<VboRegion, List<VertexBuffer>>>)this.mapRegionLayers
                .computeIfAbsent(pRenderType, k -> new LinkedHashMap(16));
            Map<VboRegion, List<VertexBuffer>> map2 = null;
            List<VertexBuffer> list1 = null;

            while (flag2 ? objectlistiterator.hasNext() : objectlistiterator.hasPrevious()) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 = flag2
                    ? objectlistiterator.next()
                    : objectlistiterator.previous();
                if (!sectionrenderdispatcher$rendersection2.getCompiled().isEmpty(pRenderType)) {
                    VertexBuffer vertexbuffer2 = sectionrenderdispatcher$rendersection2.getBuffer(pRenderType);
                    VboRegion vboregion = vertexbuffer2.getVboRegion();
                    if (sectionrenderdispatcher$rendersection2.regionX != i1 || sectionrenderdispatcher$rendersection2.regionZ != j1) {
                        PairInt pairint = PairInt.of(sectionrenderdispatcher$rendersection2.regionX, sectionrenderdispatcher$rendersection2.regionZ);
                        map2 = map1.computeIfAbsent(pairint, k -> new LinkedHashMap<>(8));
                        i1 = sectionrenderdispatcher$rendersection2.regionX;
                        j1 = sectionrenderdispatcher$rendersection2.regionZ;
                        vboregion2 = null;
                    }

                    if (vboregion != vboregion2) {
                        list1 = map2.computeIfAbsent(vboregion, k -> new ArrayList<>());
                        vboregion2 = vboregion;
                    }

                    list1.add(vertexbuffer2);
                    if (flag3) {
                        BitSet bitset1 = sectionrenderdispatcher$rendersection2.getCompiled().getAnimatedSprites(pRenderType);
                        if (bitset1 != null) {
                            SmartAnimations.spritesRendered(bitset1);
                        }
                    }
                }
            }

            for (java.util.Map.Entry<PairInt, Map<VboRegion, List<VertexBuffer>>> entry1 : map1.entrySet()) {
                PairInt pairint1 = entry1.getKey();
                Map<VboRegion, List<VertexBuffer>> map = entry1.getValue();

                for (java.util.Map.Entry<VboRegion, List<VertexBuffer>> entry : map.entrySet()) {
                    VboRegion vboregion1 = entry.getKey();
                    List<VertexBuffer> list = entry.getValue();
                    if (!list.isEmpty()) {
                        for (VertexBuffer vertexbuffer : list) {
                            vertexbuffer.draw();
                        }

                        this.drawRegion(pairint1.getLeft(), 0, pairint1.getRight(), pX, pY, pZ, vboregion1, uniform, flag);
                        list.clear();
                    }
                }
            }
        } else {
            while (flag2 ? objectlistiterator.hasNext() : objectlistiterator.hasPrevious()) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = flag2 ? objectlistiterator.next() : objectlistiterator.previous();
                if (!sectionrenderdispatcher$rendersection.getCompiled().isEmpty(pRenderType)) {
                    VertexBuffer vertexbuffer1 = sectionrenderdispatcher$rendersection.getBuffer(pRenderType);
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getOrigin();
                    if (uniform != null) {
                        uniform.set(
                            (float)((double)blockpos.getX() - pX - (double)sectionrenderdispatcher$rendersection.regionDX),
                            (float)((double)blockpos.getY() - pY - (double)sectionrenderdispatcher$rendersection.regionDY),
                            (float)((double)blockpos.getZ() - pZ - (double)sectionrenderdispatcher$rendersection.regionDZ)
                        );
                        uniform.upload();
                    }

                    if (flag) {
                        Shaders.uniform_chunkOffset
                            .setValue(
                                (float)((double)blockpos.getX() - pX - (double)sectionrenderdispatcher$rendersection.regionDX),
                                (float)((double)blockpos.getY() - pY - (double)sectionrenderdispatcher$rendersection.regionDY),
                                (float)((double)blockpos.getZ() - pZ - (double)sectionrenderdispatcher$rendersection.regionDZ)
                            );
                    }

                    if (flag3) {
                        BitSet bitset = sectionrenderdispatcher$rendersection.getCompiled().getAnimatedSprites(pRenderType);
                        if (bitset != null) {
                            SmartAnimations.spritesRendered(bitset);
                        }
                    }

                    vertexbuffer1.bind();
                    vertexbuffer1.draw();
                }
            }
        }

        if (Config.isMultiTexture()) {
            this.minecraft.getTextureManager().bindForSetup(TextureAtlas.LOCATION_BLOCKS);
        }

        if (uniform != null) {
            uniform.set(0.0F, 0.0F, 0.0F);
        }

        if (flag) {
            Shaders.uniform_chunkOffset.setValue(0.0F, 0.0F, 0.0F);
        }

        shaderinstance.clear();
        VertexBuffer.unbind();
        this.minecraft.getProfiler().pop();
        if (flag) {
            ShadersRender.postRenderChunkLayer(pRenderType);
        }

        if (Reflector.ForgeHooksClient_dispatchRenderStageRT.exists()) {
            Reflector.ForgeHooksClient_dispatchRenderStageRT
                .call(pRenderType, this, pFrustrumMatrix, pProjectionMatrix, this.ticks, this.minecraft.gameRenderer.getMainCamera(), this.getFrustum());
        }

        pRenderType.clearRenderState();
    }

    private void drawRegion(int regionX, int regionY, int regionZ, double xIn, double yIn, double zIn, VboRegion vboRegion, Uniform uniform, boolean isShaders) {
        if (uniform != null) {
            uniform.set((float)((double)regionX - xIn), (float)((double)regionY - yIn), (float)((double)regionZ - zIn));
            uniform.upload();
        }

        if (isShaders) {
            Shaders.uniform_chunkOffset.setValue((float)((double)regionX - xIn), (float)((double)regionY - yIn), (float)((double)regionZ - zIn));
        }

        vboRegion.finishDraw();
    }

    private void renderDebug(PoseStack pPoseStack, MultiBufferSource pBuffer, Camera pCamera) {
        if (this.minecraft.sectionPath || this.minecraft.sectionVisibility) {
            if (Config.isShaders()) {
                Shaders.pushUseProgram(Shaders.ProgramBasic);
            }

            double d0 = pCamera.getPosition().x();
            double d1 = pCamera.getPosition().y();
            double d2 = pCamera.getPosition().z();

            for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.visibleSections) {
                SectionOcclusionGraph.Node sectionocclusiongraph$node = this.sectionOcclusionGraph.getNode(sectionrenderdispatcher$rendersection);
                if (sectionocclusiongraph$node != null) {
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getOrigin();
                    pPoseStack.pushPose();
                    pPoseStack.translate((double)blockpos.getX() - d0, (double)blockpos.getY() - d1, (double)blockpos.getZ() - d2);
                    Matrix4f matrix4f = pPoseStack.last().pose();
                    if (this.minecraft.sectionPath) {
                        if (Config.isShaders()) {
                            Shaders.beginLines();
                        }

                        VertexConsumer vertexconsumer1 = pBuffer.getBuffer(RenderType.lines());
                        int i = sectionocclusiongraph$node.step == 0 ? 0 : Mth.hsvToRgb((float)sectionocclusiongraph$node.step / 50.0F, 0.9F, 0.9F);
                        int j = i >> 16 & 0xFF;
                        int k = i >> 8 & 0xFF;
                        int l = i & 0xFF;

                        for (int i1 = 0; i1 < DIRECTIONS.length; i1++) {
                            if (sectionocclusiongraph$node.hasSourceDirection(i1)) {
                                Direction direction = DIRECTIONS[i1];
                                vertexconsumer1.addVertex(matrix4f, 8.0F, 8.0F, 8.0F)
                                    .setColor(j, k, l, 255)
                                    .setNormal((float)direction.getStepX(), (float)direction.getStepY(), (float)direction.getStepZ());
                                vertexconsumer1.addVertex(
                                        matrix4f,
                                        (float)(8 - 16 * direction.getStepX()),
                                        (float)(8 - 16 * direction.getStepY()),
                                        (float)(8 - 16 * direction.getStepZ())
                                    )
                                    .setColor(j, k, l, 255)
                                    .setNormal((float)direction.getStepX(), (float)direction.getStepY(), (float)direction.getStepZ());
                            }
                        }

                        if (Config.isShaders()) {
                            Shaders.endLines();
                        }
                    }

                    if (this.minecraft.sectionVisibility && !sectionrenderdispatcher$rendersection.getCompiled().hasNoRenderableLayers()) {
                        if (Config.isShaders()) {
                            Shaders.beginLines();
                        }

                        VertexConsumer vertexconsumer3 = pBuffer.getBuffer(RenderType.lines());
                        int j1 = 0;

                        for (Direction direction2 : DIRECTIONS) {
                            for (Direction direction1 : DIRECTIONS) {
                                boolean flag = sectionrenderdispatcher$rendersection.getCompiled().facesCanSeeEachother(direction2, direction1);
                                if (!flag) {
                                    j1++;
                                    vertexconsumer3.addVertex(
                                            matrix4f,
                                            (float)(8 + 8 * direction2.getStepX()),
                                            (float)(8 + 8 * direction2.getStepY()),
                                            (float)(8 + 8 * direction2.getStepZ())
                                        )
                                        .setColor(255, 0, 0, 255)
                                        .setNormal((float)direction2.getStepX(), (float)direction2.getStepY(), (float)direction2.getStepZ());
                                    vertexconsumer3.addVertex(
                                            matrix4f,
                                            (float)(8 + 8 * direction1.getStepX()),
                                            (float)(8 + 8 * direction1.getStepY()),
                                            (float)(8 + 8 * direction1.getStepZ())
                                        )
                                        .setColor(255, 0, 0, 255)
                                        .setNormal((float)direction1.getStepX(), (float)direction1.getStepY(), (float)direction1.getStepZ());
                                }
                            }
                        }

                        if (Config.isShaders()) {
                            Shaders.endLines();
                        }

                        if (j1 > 0) {
                            VertexConsumer vertexconsumer4 = pBuffer.getBuffer(RenderType.debugQuads());
                            float f = 0.5F;
                            float f1 = 0.2F;
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        }
                    }

                    pPoseStack.popPose();
                }

                if (Config.isShaders()) {
                    Shaders.popProgram();
                }
            }
        }

        if (this.capturedFrustum != null) {
            if (Config.isShaders()) {
                Shaders.pushUseProgram(Shaders.ProgramBasic);
            }

            pPoseStack.pushPose();
            pPoseStack.translate(
                (float)(this.frustumPos.x - pCamera.getPosition().x),
                (float)(this.frustumPos.y - pCamera.getPosition().y),
                (float)(this.frustumPos.z - pCamera.getPosition().z)
            );
            Matrix4f matrix4f1 = pPoseStack.last().pose();
            VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.debugQuads());
            this.addFrustumQuad(vertexconsumer, matrix4f1, 0, 1, 2, 3, 0, 1, 1);
            this.addFrustumQuad(vertexconsumer, matrix4f1, 4, 5, 6, 7, 1, 0, 0);
            this.addFrustumQuad(vertexconsumer, matrix4f1, 0, 1, 5, 4, 1, 1, 0);
            this.addFrustumQuad(vertexconsumer, matrix4f1, 2, 3, 7, 6, 0, 0, 1);
            this.addFrustumQuad(vertexconsumer, matrix4f1, 0, 4, 7, 3, 0, 1, 0);
            this.addFrustumQuad(vertexconsumer, matrix4f1, 1, 5, 6, 2, 1, 0, 1);
            if (Config.isShaders()) {
                Shaders.beginLines();
            }

            VertexConsumer vertexconsumer2 = pBuffer.getBuffer(RenderType.lines());
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 0);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 1);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 1);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 2);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 2);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 3);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 3);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 0);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 4);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 5);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 5);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 6);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 6);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 7);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 7);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 4);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 0);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 4);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 1);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 5);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 2);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 6);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 3);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, 7);
            if (Config.isShaders()) {
                Shaders.endLines();
            }

            pPoseStack.popPose();
            if (Config.isShaders()) {
                Shaders.popProgram();
            }
        }
    }

    private void addFrustumVertex(VertexConsumer pConsumer, Matrix4f pMatrix, int pVertexIndex) {
        pConsumer.addVertex(pMatrix, this.frustumPoints[pVertexIndex].x(), this.frustumPoints[pVertexIndex].y(), this.frustumPoints[pVertexIndex].z())
            .setColor(-16777216)
            .setNormal(0.0F, 0.0F, -1.0F);
    }

    private void addFrustumQuad(
        VertexConsumer pConsumer, Matrix4f pMatrix, int pIndex1, int pIndex2, int pIndex3, int pIndex4, int pRed, int pGreen, int pBlue
    ) {
        float f = 0.25F;
        pConsumer.addVertex(pMatrix, this.frustumPoints[pIndex1].x(), this.frustumPoints[pIndex1].y(), this.frustumPoints[pIndex1].z())
            .setColor((float)pRed, (float)pGreen, (float)pBlue, 0.25F);
        pConsumer.addVertex(pMatrix, this.frustumPoints[pIndex2].x(), this.frustumPoints[pIndex2].y(), this.frustumPoints[pIndex2].z())
            .setColor((float)pRed, (float)pGreen, (float)pBlue, 0.25F);
        pConsumer.addVertex(pMatrix, this.frustumPoints[pIndex3].x(), this.frustumPoints[pIndex3].y(), this.frustumPoints[pIndex3].z())
            .setColor((float)pRed, (float)pGreen, (float)pBlue, 0.25F);
        pConsumer.addVertex(pMatrix, this.frustumPoints[pIndex4].x(), this.frustumPoints[pIndex4].y(), this.frustumPoints[pIndex4].z())
            .setColor((float)pRed, (float)pGreen, (float)pBlue, 0.25F);
    }

    public void captureFrustum() {
        this.captureFrustum = true;
    }

    public void killFrustum() {
        this.capturedFrustum = null;
    }

    public void tick() {
        if (this.level.tickRateManager().runsNormally()) {
            this.ticks++;
        }

        if (this.ticks % 20 == 0) {
            Iterator<BlockDestructionProgress> iterator = this.destroyingBlocks.values().iterator();

            while (iterator.hasNext()) {
                BlockDestructionProgress blockdestructionprogress = iterator.next();
                int i = blockdestructionprogress.getUpdatedRenderTick();
                if (this.ticks - i > 400) {
                    iterator.remove();
                    this.removeProgress(blockdestructionprogress);
                }
            }
        }

        if (Config.isRenderRegions() && this.ticks % 20 == 0) {
            this.mapRegionLayers.clear();
        }
    }

    private void removeProgress(BlockDestructionProgress pProgress) {
        long i = pProgress.getPos().asLong();
        Set<BlockDestructionProgress> set = this.destructionProgress.get(i);
        set.remove(pProgress);
        if (set.isEmpty()) {
            this.destructionProgress.remove(i);
        }
    }

    private void renderEndSky(PoseStack pPoseStack) {
        if (Config.isSkyEnabled()) {
            RenderSystem.enableBlend();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, END_SKY_LOCATION);
            Tesselator tesselator = Tesselator.getInstance();

            for (int i = 0; i < 6; i++) {
                pPoseStack.pushPose();
                if (i == 1) {
                    pPoseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                }

                if (i == 2) {
                    pPoseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                }

                if (i == 3) {
                    pPoseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
                }

                if (i == 4) {
                    pPoseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                }

                if (i == 5) {
                    pPoseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                }

                Matrix4f matrix4f = pPoseStack.last().pose();
                BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                int j = -14145496;
                if (Config.isCustomColors()) {
                    int k = FastColor.ARGB32.red(j);
                    int l = FastColor.ARGB32.green(j);
                    int i1 = FastColor.ARGB32.blue(j);
                    Vec3 vec3 = new Vec3((double)k / 255.0, (double)l / 255.0, (double)i1 / 255.0);
                    vec3 = CustomColors.getWorldSkyColor(vec3, this.level, this.minecraft.getCameraEntity(), 0.0F);
                    k = (int)(vec3.x * 255.0);
                    l = (int)(vec3.y * 255.0);
                    i1 = (int)(vec3.z * 255.0);
                    j = FastColor.ARGB32.color(255, k, l, i1);
                }

                bufferbuilder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(0.0F, 0.0F).setColor(j);
                bufferbuilder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(0.0F, 16.0F).setColor(j);
                bufferbuilder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(16.0F, 16.0F).setColor(j);
                bufferbuilder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(16.0F, 0.0F).setColor(j);
                BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                pPoseStack.popPose();
            }

            CustomSky.renderSky(this.level, pPoseStack, 0.0F);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    public void renderSky(Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix, float pPartialTick, Camera pCamera, boolean pIsFoggy, Runnable pSkyFogSetup) {
        pSkyFogSetup.run();
        if (!Reflector.IForgeDimensionSpecialEffects_renderSky.exists()
            || !Reflector.callBoolean(
                this.level.effects(),
                Reflector.IForgeDimensionSpecialEffects_renderSky,
                this.level,
                this.ticks,
                pPartialTick,
                pCamera,
                pFrustumMatrix,
                pIsFoggy,
                pSkyFogSetup
            )) {
            if (!pIsFoggy) {
                FogType fogtype = pCamera.getFluidInCamera();
                if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA && !this.doesMobEffectBlockSky(pCamera)) {
                    PoseStack posestack = new PoseStack();
                    posestack.mulPose(pFrustumMatrix);
                    if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.END) {
                        this.renderEndSky(posestack);
                    } else if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
                        boolean flag = Config.isShaders();
                        if (flag) {
                            Shaders.disableTexture2D();
                        }

                        Vec3 vec3 = this.level.getSkyColor(this.minecraft.gameRenderer.getMainCamera().getPosition(), pPartialTick);
                        vec3 = CustomColors.getSkyColor(
                            vec3,
                            this.minecraft.level,
                            this.minecraft.getCameraEntity().getX(),
                            this.minecraft.getCameraEntity().getY() + 1.0,
                            this.minecraft.getCameraEntity().getZ()
                        );
                        if (flag) {
                            Shaders.setSkyColor(vec3);
                            RenderSystem.setColorToAttribute(true);
                        }

                        float f = (float)vec3.x;
                        float f1 = (float)vec3.y;
                        float f2 = (float)vec3.z;
                        FogRenderer.levelFogColor();
                        Tesselator tesselator = Tesselator.getInstance();
                        RenderSystem.depthMask(false);
                        if (flag) {
                            Shaders.enableFog();
                        }

                        RenderSystem.setShaderColor(f, f1, f2, 1.0F);
                        if (flag) {
                            Shaders.preSkyList(posestack);
                        }

                        ShaderInstance shaderinstance = RenderSystem.getShader();
                        if (Config.isSkyEnabled()) {
                            this.skyBuffer.bind();
                            this.skyBuffer.drawWithShader(posestack.last().pose(), pProjectionMatrix, shaderinstance);
                            VertexBuffer.unbind();
                        }

                        if (flag) {
                            Shaders.disableFog();
                        }

                        RenderSystem.enableBlend();
                        float[] afloat = this.level.effects().getSunriseColor(this.level.getTimeOfDay(pPartialTick), pPartialTick);
                        if (afloat != null && Config.isSunMoonEnabled()) {
                            RenderSystem.setShader(GameRenderer::getPositionColorShader);
                            if (flag) {
                                Shaders.disableTexture2D();
                            }

                            if (flag) {
                                Shaders.setRenderStage(RenderStage.SUNSET);
                            }

                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                            posestack.pushPose();
                            posestack.mulPose(Axis.XP.rotationDegrees(90.0F));
                            float f3 = Mth.sin(this.level.getSunAngle(pPartialTick)) < 0.0F ? 180.0F : 0.0F;
                            posestack.mulPose(Axis.ZP.rotationDegrees(f3));
                            posestack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                            float f4 = afloat[0];
                            float f5 = afloat[1];
                            float f6 = afloat[2];
                            Matrix4f matrix4f = posestack.last().pose();
                            BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                            bufferbuilder.addVertex(matrix4f, 0.0F, 100.0F, 0.0F).setColor(f4, f5, f6, afloat[3]);
                            int i = 16;

                            for (int j = 0; j <= 16; j++) {
                                float f7 = (float)j * (float) (Math.PI * 2) / 16.0F;
                                float f8 = Mth.sin(f7);
                                float f9 = Mth.cos(f7);
                                bufferbuilder.addVertex(matrix4f, f8 * 120.0F, f9 * 120.0F, -f9 * 40.0F * afloat[3])
                                    .setColor(afloat[0], afloat[1], afloat[2], 0.0F);
                            }

                            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
                            posestack.popPose();
                        }

                        if (flag) {
                            Shaders.enableTexture2D();
                        }

                        RenderSystem.blendFuncSeparate(
                            GlStateManager.SourceFactor.SRC_ALPHA,
                            GlStateManager.DestFactor.ONE,
                            GlStateManager.SourceFactor.ONE,
                            GlStateManager.DestFactor.ZERO
                        );
                        posestack.pushPose();
                        float f10 = 1.0F - this.level.getRainLevel(pPartialTick);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, f10);
                        posestack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                        CustomSky.renderSky(this.level, posestack, pPartialTick);
                        if (flag) {
                            Shaders.preCelestialRotate(posestack);
                        }

                        posestack.mulPose(Axis.XP.rotationDegrees(this.level.getTimeOfDay(pPartialTick) * 360.0F));
                        if (flag) {
                            Shaders.postCelestialRotate(posestack);
                        }

                        Matrix4f matrix4f1 = posestack.last().pose();
                        float f11 = 30.0F;
                        RenderSystem.setShader(GameRenderer::getPositionTexShader);
                        if (Config.isSunTexture()) {
                            if (flag) {
                                Shaders.setRenderStage(RenderStage.SUN);
                            }

                            RenderSystem.setShaderTexture(0, SUN_LOCATION);
                            BufferBuilder bufferbuilder1 = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                            bufferbuilder1.addVertex(matrix4f1, -f11, 100.0F, -f11).setUv(0.0F, 0.0F);
                            bufferbuilder1.addVertex(matrix4f1, f11, 100.0F, -f11).setUv(1.0F, 0.0F);
                            bufferbuilder1.addVertex(matrix4f1, f11, 100.0F, f11).setUv(1.0F, 1.0F);
                            bufferbuilder1.addVertex(matrix4f1, -f11, 100.0F, f11).setUv(0.0F, 1.0F);
                            BufferUploader.drawWithShader(bufferbuilder1.buildOrThrow());
                        }

                        f11 = 20.0F;
                        if (Config.isMoonTexture()) {
                            if (flag) {
                                Shaders.setRenderStage(RenderStage.MOON);
                            }

                            RenderSystem.setShaderTexture(0, MOON_LOCATION);
                            int k = this.level.getMoonPhase();
                            int l = k % 4;
                            int i1 = k / 4 % 2;
                            float f13 = (float)(l + 0) / 4.0F;
                            float f14 = (float)(i1 + 0) / 2.0F;
                            float f15 = (float)(l + 1) / 4.0F;
                            float f16 = (float)(i1 + 1) / 2.0F;
                            BufferBuilder bufferbuilder2 = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                            bufferbuilder2.addVertex(matrix4f1, -f11, -100.0F, f11).setUv(f15, f16);
                            bufferbuilder2.addVertex(matrix4f1, f11, -100.0F, f11).setUv(f13, f16);
                            bufferbuilder2.addVertex(matrix4f1, f11, -100.0F, -f11).setUv(f13, f14);
                            bufferbuilder2.addVertex(matrix4f1, -f11, -100.0F, -f11).setUv(f15, f14);
                            BufferUploader.drawWithShader(bufferbuilder2.buildOrThrow());
                        }

                        if (flag) {
                            Shaders.disableTexture2D();
                        }

                        float f12 = this.level.getStarBrightness(pPartialTick) * f10;
                        if (f12 > 0.0F && Config.isStarsEnabled() && !CustomSky.hasSkyLayers(this.level)) {
                            if (flag) {
                                Shaders.setRenderStage(RenderStage.STARS);
                            }

                            RenderSystem.setShaderColor(f12, f12, f12, f12);
                            FogRenderer.setupNoFog();
                            this.starBuffer.bind();
                            this.starBuffer.drawWithShader(posestack.last().pose(), pProjectionMatrix, GameRenderer.getPositionShader());
                            VertexBuffer.unbind();
                            pSkyFogSetup.run();
                        }

                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                        if (flag) {
                            Shaders.enableFog();
                        }

                        posestack.popPose();
                        if (flag) {
                            Shaders.disableTexture2D();
                        }

                        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
                        double d0 = this.minecraft.player.getEyePosition(pPartialTick).y - this.level.getLevelData().getHorizonHeight(this.level);
                        boolean flag1 = false;
                        if (d0 < 0.0) {
                            if (flag) {
                                Shaders.setRenderStage(RenderStage.VOID);
                            }

                            posestack.pushPose();
                            posestack.translate(0.0F, 12.0F, 0.0F);
                            this.darkBuffer.bind();
                            this.darkBuffer.drawWithShader(posestack.last().pose(), pProjectionMatrix, shaderinstance);
                            VertexBuffer.unbind();
                            posestack.popPose();
                            flag1 = true;
                        }

                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        if (flag) {
                            RenderSystem.setColorToAttribute(false);
                        }

                        RenderSystem.depthMask(true);
                    }
                }
            }
        }
    }

    private boolean doesMobEffectBlockSky(Camera pCamera) {
        return pCamera.getEntity() instanceof LivingEntity livingentity
            ? livingentity.hasEffect(MobEffects.BLINDNESS) || livingentity.hasEffect(MobEffects.DARKNESS)
            : false;
    }

    public void renderClouds(PoseStack pPoseStack, Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix, float pPartialTick, double pCamX, double pCamY, double pCamZ) {
        if (!Config.isCloudsOff()) {
            if (!Reflector.IForgeDimensionSpecialEffects_renderClouds.exists()
                || !Reflector.callBoolean(
                    this.level.effects(),
                    Reflector.IForgeDimensionSpecialEffects_renderClouds,
                    this.level,
                    this.ticks,
                    pPartialTick,
                    pPoseStack,
                    pCamX,
                    pCamY,
                    pCamZ,
                    pFrustumMatrix
                )) {
                float f = this.level.effects().getCloudHeight();
                if (!Float.isNaN(f)) {
                    if (Config.isShaders()) {
                        Shaders.beginClouds();
                    }

                    float f1 = 12.0F;
                    float f2 = 4.0F;
                    double d0 = 2.0E-4;
                    double d1 = (double)(((float)this.ticks + pPartialTick) * 0.03F);
                    double d2 = (pCamX + d1) / 12.0;
                    double d3 = (double)(f - (float)pCamY + 0.33F);
                    d3 += this.minecraft.options.ofCloudsHeight * 128.0;
                    double d4 = pCamZ / 12.0 + 0.33F;
                    d2 -= (double)(Mth.floor(d2 / 2048.0) * 2048);
                    d4 -= (double)(Mth.floor(d4 / 2048.0) * 2048);
                    float f3 = (float)(d2 - (double)Mth.floor(d2));
                    float f4 = (float)(d3 / 4.0 - (double)Mth.floor(d3 / 4.0)) * 4.0F;
                    float f5 = (float)(d4 - (double)Mth.floor(d4));
                    Vec3 vec3 = this.level.getCloudColor(pPartialTick);
                    int i = (int)Math.floor(d2);
                    int j = (int)Math.floor(d3 / 4.0);
                    int k = (int)Math.floor(d4);
                    if (i != this.prevCloudX
                        || j != this.prevCloudY
                        || k != this.prevCloudZ
                        || this.minecraft.options.getCloudsType() != this.prevCloudsType
                        || this.prevCloudColor.distanceToSqr(vec3) > 2.0E-4) {
                        this.prevCloudX = i;
                        this.prevCloudY = j;
                        this.prevCloudZ = k;
                        this.prevCloudColor = vec3;
                        this.prevCloudsType = this.minecraft.options.getCloudsType();
                        this.generateClouds = true;
                    }

                    if (this.generateClouds) {
                        this.generateClouds = false;
                        if (this.cloudBuffer != null) {
                            this.cloudBuffer.close();
                        }

                        this.cloudBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                        this.cloudBuffer.bind();
                        this.cloudBuffer.upload(this.buildClouds(Tesselator.getInstance(), d2, d3, d4, vec3));
                        VertexBuffer.unbind();
                    }

                    FogRenderer.levelFogColor();
                    pPoseStack.pushPose();
                    pPoseStack.mulPose(pFrustumMatrix);
                    pPoseStack.scale(12.0F, 1.0F, 12.0F);
                    pPoseStack.translate(-f3, f4, -f5);
                    if (this.cloudBuffer != null) {
                        this.cloudBuffer.bind();
                        int l = this.prevCloudsType == CloudStatus.FANCY ? 0 : 1;

                        for (int i1 = l; i1 < 2; i1++) {
                            RenderType rendertype = i1 == 0 ? RenderType.cloudsDepthOnly() : RenderType.clouds();
                            rendertype.setupRenderState();
                            ShaderInstance shaderinstance = RenderSystem.getShader();
                            this.cloudBuffer.drawWithShader(pPoseStack.last().pose(), pProjectionMatrix, shaderinstance);
                            rendertype.clearRenderState();
                        }

                        VertexBuffer.unbind();
                    }

                    pPoseStack.popPose();
                    if (Config.isShaders()) {
                        Shaders.endClouds();
                    }
                }
            }
        }
    }

    private MeshData buildClouds(Tesselator pTesselator, double pX, double pY, double pZ, Vec3 pCloudColor) {
        float f = 4.0F;
        float f1 = 0.00390625F;
        int i = 8;
        int j = 4;
        float f2 = 9.765625E-4F;
        float f3 = (float)Mth.floor(pX) * 0.00390625F;
        float f4 = (float)Mth.floor(pZ) * 0.00390625F;
        float f5 = (float)pCloudColor.x;
        float f6 = (float)pCloudColor.y;
        float f7 = (float)pCloudColor.z;
        float f8 = f5 * 0.9F;
        float f9 = f6 * 0.9F;
        float f10 = f7 * 0.9F;
        float f11 = f5 * 0.7F;
        float f12 = f6 * 0.7F;
        float f13 = f7 * 0.7F;
        float f14 = f5 * 0.8F;
        float f15 = f6 * 0.8F;
        float f16 = f7 * 0.8F;
        BufferBuilder bufferbuilder = pTesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
        float f17 = (float)Math.floor(pY / 4.0) * 4.0F;
        if (Config.isCloudsFancy()) {
            for (int k = -3; k <= 4; k++) {
                for (int l = -3; l <= 4; l++) {
                    float f18 = (float)(k * 8);
                    float f19 = (float)(l * 8);
                    if (f17 > -5.0F) {
                        bufferbuilder.addVertex(f18 + 0.0F, f17 + 0.0F, f19 + 8.0F)
                            .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                            .setColor(f11, f12, f13, 0.8F)
                            .setNormal(0.0F, -1.0F, 0.0F);
                        bufferbuilder.addVertex(f18 + 8.0F, f17 + 0.0F, f19 + 8.0F)
                            .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                            .setColor(f11, f12, f13, 0.8F)
                            .setNormal(0.0F, -1.0F, 0.0F);
                        bufferbuilder.addVertex(f18 + 8.0F, f17 + 0.0F, f19 + 0.0F)
                            .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                            .setColor(f11, f12, f13, 0.8F)
                            .setNormal(0.0F, -1.0F, 0.0F);
                        bufferbuilder.addVertex(f18 + 0.0F, f17 + 0.0F, f19 + 0.0F)
                            .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                            .setColor(f11, f12, f13, 0.8F)
                            .setNormal(0.0F, -1.0F, 0.0F);
                    }

                    if (f17 <= 5.0F) {
                        bufferbuilder.addVertex(f18 + 0.0F, f17 + 4.0F - 9.765625E-4F, f19 + 8.0F)
                            .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                            .setColor(f5, f6, f7, 0.8F)
                            .setNormal(0.0F, 1.0F, 0.0F);
                        bufferbuilder.addVertex(f18 + 8.0F, f17 + 4.0F - 9.765625E-4F, f19 + 8.0F)
                            .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                            .setColor(f5, f6, f7, 0.8F)
                            .setNormal(0.0F, 1.0F, 0.0F);
                        bufferbuilder.addVertex(f18 + 8.0F, f17 + 4.0F - 9.765625E-4F, f19 + 0.0F)
                            .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                            .setColor(f5, f6, f7, 0.8F)
                            .setNormal(0.0F, 1.0F, 0.0F);
                        bufferbuilder.addVertex(f18 + 0.0F, f17 + 4.0F - 9.765625E-4F, f19 + 0.0F)
                            .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                            .setColor(f5, f6, f7, 0.8F)
                            .setNormal(0.0F, 1.0F, 0.0F);
                    }

                    if (k > -1) {
                        for (int i1 = 0; i1 < 8; i1++) {
                            bufferbuilder.addVertex(f18 + (float)i1 + 0.0F, f17 + 0.0F, f19 + 8.0F)
                                .setUv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                                .setColor(f8, f9, f10, 0.8F)
                                .setNormal(-1.0F, 0.0F, 0.0F);
                            bufferbuilder.addVertex(f18 + (float)i1 + 0.0F, f17 + 4.0F, f19 + 8.0F)
                                .setUv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                                .setColor(f8, f9, f10, 0.8F)
                                .setNormal(-1.0F, 0.0F, 0.0F);
                            bufferbuilder.addVertex(f18 + (float)i1 + 0.0F, f17 + 4.0F, f19 + 0.0F)
                                .setUv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                                .setColor(f8, f9, f10, 0.8F)
                                .setNormal(-1.0F, 0.0F, 0.0F);
                            bufferbuilder.addVertex(f18 + (float)i1 + 0.0F, f17 + 0.0F, f19 + 0.0F)
                                .setUv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                                .setColor(f8, f9, f10, 0.8F)
                                .setNormal(-1.0F, 0.0F, 0.0F);
                        }
                    }

                    if (k <= 1) {
                        for (int j2 = 0; j2 < 8; j2++) {
                            bufferbuilder.addVertex(f18 + (float)j2 + 1.0F - 9.765625E-4F, f17 + 0.0F, f19 + 8.0F)
                                .setUv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                                .setColor(f8, f9, f10, 0.8F)
                                .setNormal(1.0F, 0.0F, 0.0F);
                            bufferbuilder.addVertex(f18 + (float)j2 + 1.0F - 9.765625E-4F, f17 + 4.0F, f19 + 8.0F)
                                .setUv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4)
                                .setColor(f8, f9, f10, 0.8F)
                                .setNormal(1.0F, 0.0F, 0.0F);
                            bufferbuilder.addVertex(f18 + (float)j2 + 1.0F - 9.765625E-4F, f17 + 4.0F, f19 + 0.0F)
                                .setUv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                                .setColor(f8, f9, f10, 0.8F)
                                .setNormal(1.0F, 0.0F, 0.0F);
                            bufferbuilder.addVertex(f18 + (float)j2 + 1.0F - 9.765625E-4F, f17 + 0.0F, f19 + 0.0F)
                                .setUv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4)
                                .setColor(f8, f9, f10, 0.8F)
                                .setNormal(1.0F, 0.0F, 0.0F);
                        }
                    }

                    if (l > -1) {
                        for (int k2 = 0; k2 < 8; k2++) {
                            bufferbuilder.addVertex(f18 + 0.0F, f17 + 4.0F, f19 + (float)k2 + 0.0F)
                                .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4)
                                .setColor(f14, f15, f16, 0.8F)
                                .setNormal(0.0F, 0.0F, -1.0F);
                            bufferbuilder.addVertex(f18 + 8.0F, f17 + 4.0F, f19 + (float)k2 + 0.0F)
                                .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4)
                                .setColor(f14, f15, f16, 0.8F)
                                .setNormal(0.0F, 0.0F, -1.0F);
                            bufferbuilder.addVertex(f18 + 8.0F, f17 + 0.0F, f19 + (float)k2 + 0.0F)
                                .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4)
                                .setColor(f14, f15, f16, 0.8F)
                                .setNormal(0.0F, 0.0F, -1.0F);
                            bufferbuilder.addVertex(f18 + 0.0F, f17 + 0.0F, f19 + (float)k2 + 0.0F)
                                .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4)
                                .setColor(f14, f15, f16, 0.8F)
                                .setNormal(0.0F, 0.0F, -1.0F);
                        }
                    }

                    if (l <= 1) {
                        for (int l2 = 0; l2 < 8; l2++) {
                            bufferbuilder.addVertex(f18 + 0.0F, f17 + 4.0F, f19 + (float)l2 + 1.0F - 9.765625E-4F)
                                .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4)
                                .setColor(f14, f15, f16, 0.8F)
                                .setNormal(0.0F, 0.0F, 1.0F);
                            bufferbuilder.addVertex(f18 + 8.0F, f17 + 4.0F, f19 + (float)l2 + 1.0F - 9.765625E-4F)
                                .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4)
                                .setColor(f14, f15, f16, 0.8F)
                                .setNormal(0.0F, 0.0F, 1.0F);
                            bufferbuilder.addVertex(f18 + 8.0F, f17 + 0.0F, f19 + (float)l2 + 1.0F - 9.765625E-4F)
                                .setUv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4)
                                .setColor(f14, f15, f16, 0.8F)
                                .setNormal(0.0F, 0.0F, 1.0F);
                            bufferbuilder.addVertex(f18 + 0.0F, f17 + 0.0F, f19 + (float)l2 + 1.0F - 9.765625E-4F)
                                .setUv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4)
                                .setColor(f14, f15, f16, 0.8F)
                                .setNormal(0.0F, 0.0F, 1.0F);
                        }
                    }
                }
            }
        } else {
            int j1 = 1;
            int k1 = 32;

            for (int l1 = -32; l1 < 32; l1 += 32) {
                for (int i2 = -32; i2 < 32; i2 += 32) {
                    bufferbuilder.addVertex((float)(l1 + 0), f17, (float)(i2 + 32))
                        .setUv((float)(l1 + 0) * 0.00390625F + f3, (float)(i2 + 32) * 0.00390625F + f4)
                        .setColor(f5, f6, f7, 0.8F)
                        .setNormal(0.0F, -1.0F, 0.0F);
                    bufferbuilder.addVertex((float)(l1 + 32), f17, (float)(i2 + 32))
                        .setUv((float)(l1 + 32) * 0.00390625F + f3, (float)(i2 + 32) * 0.00390625F + f4)
                        .setColor(f5, f6, f7, 0.8F)
                        .setNormal(0.0F, -1.0F, 0.0F);
                    bufferbuilder.addVertex((float)(l1 + 32), f17, (float)(i2 + 0))
                        .setUv((float)(l1 + 32) * 0.00390625F + f3, (float)(i2 + 0) * 0.00390625F + f4)
                        .setColor(f5, f6, f7, 0.8F)
                        .setNormal(0.0F, -1.0F, 0.0F);
                    bufferbuilder.addVertex((float)(l1 + 0), f17, (float)(i2 + 0))
                        .setUv((float)(l1 + 0) * 0.00390625F + f3, (float)(i2 + 0) * 0.00390625F + f4)
                        .setColor(f5, f6, f7, 0.8F)
                        .setNormal(0.0F, -1.0F, 0.0F);
                }
            }
        }

        return bufferbuilder.buildOrThrow();
    }

    private void compileSections(Camera pCamera) {
        this.minecraft.getProfiler().push("populate_sections_to_compile");
        LevelLightEngine levellightengine = this.level.getLightEngine();
        RenderRegionCache renderregioncache = new RenderRegionCache();
        renderregioncache.compileStarted();
        BlockPos blockpos = pCamera.getBlockPosition();
        List<SectionRenderDispatcher.RenderSection> list = Lists.newArrayList();
        Lagometer.timerChunkUpdate.start();

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.visibleSections) {
            SectionPos sectionpos = sectionrenderdispatcher$rendersection.getSectionPosition();
            if (sectionrenderdispatcher$rendersection.isDirty() && levellightengine.lightOnInSection(sectionpos)) {
                if (sectionrenderdispatcher$rendersection.needsBackgroundPriorityUpdate()) {
                    list.add(sectionrenderdispatcher$rendersection);
                } else {
                    boolean flag = false;
                    if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.NEARBY) {
                        BlockPos blockpos1 = sectionrenderdispatcher$rendersection.getOrigin().offset(8, 8, 8);
                        flag = blockpos1.distSqr(blockpos) < 768.0 || sectionrenderdispatcher$rendersection.isDirtyFromPlayer();
                    } else if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
                        flag = sectionrenderdispatcher$rendersection.isDirtyFromPlayer();
                    }

                    if (flag) {
                        this.minecraft.getProfiler().push("build_near_sync");
                        this.sectionRenderDispatcher.rebuildSectionSync(sectionrenderdispatcher$rendersection, renderregioncache);
                        sectionrenderdispatcher$rendersection.setNotDirty();
                        this.minecraft.getProfiler().pop();
                    } else {
                        list.add(sectionrenderdispatcher$rendersection);
                    }
                }
            }
        }

        Lagometer.timerChunkUpdate.end();
        Lagometer.timerChunkUpload.start();
        this.minecraft.getProfiler().popPush("upload");
        this.sectionRenderDispatcher.uploadAllPendingUploads();
        this.viewArea.clearUnusedVbos();
        this.minecraft.getProfiler().popPush("schedule_async_compile");
        if (this.chunksToResortTransparency.size() > 0) {
            Iterator<SectionRenderDispatcher.RenderSection> iterator = this.chunksToResortTransparency.iterator();
            if (iterator.hasNext()) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = iterator.next();
                if (this.sectionRenderDispatcher.updateTransparencyLater(sectionrenderdispatcher$rendersection1)) {
                    iterator.remove();
                }
            }
        }

        double d1 = 0.0;
        int i = Config.getUpdatesPerFrame();
        this.countChunksToUpdate = list.size();

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 : list) {
            boolean flag1 = sectionrenderdispatcher$rendersection2.isChunkRegionEmpty();
            boolean flag2 = sectionrenderdispatcher$rendersection2.needsBackgroundPriorityUpdate();
            if (sectionrenderdispatcher$rendersection2.isDirty()) {
                sectionrenderdispatcher$rendersection2.rebuildSectionAsync(this.sectionRenderDispatcher, renderregioncache);
                sectionrenderdispatcher$rendersection2.setNotDirty();
                if (!flag1 && !flag2) {
                    double d0 = 2.0 * RenderChunkUtils.getRelativeBufferSize(sectionrenderdispatcher$rendersection2);
                    d1 += d0;
                    if (d1 > (double)i) {
                        break;
                    }
                }
            }
        }

        renderregioncache.compileFinished();
        Lagometer.timerChunkUpload.end();
        this.minecraft.getProfiler().pop();
    }

    private void renderWorldBorder(Camera pCamera) {
        WorldBorder worldborder = this.level.getWorldBorder();
        double d0 = (double)(this.minecraft.options.getEffectiveRenderDistance() * 16);
        if (!(pCamera.getPosition().x < worldborder.getMaxX() - d0)
            || !(pCamera.getPosition().x > worldborder.getMinX() + d0)
            || !(pCamera.getPosition().z < worldborder.getMaxZ() - d0)
            || !(pCamera.getPosition().z > worldborder.getMinZ() + d0)) {
            if (Config.isShaders()) {
                Shaders.pushProgram();
                Shaders.useProgram(Shaders.ProgramTexturedLit);
                Shaders.setRenderStage(RenderStage.WORLD_BORDER);
            }

            double d1 = 1.0 - worldborder.getDistanceToBorder(pCamera.getPosition().x, pCamera.getPosition().z) / d0;
            d1 = Math.pow(d1, 4.0);
            d1 = Mth.clamp(d1, 0.0, 1.0);
            double d2 = pCamera.getPosition().x;
            double d3 = pCamera.getPosition().z;
            double d4 = (double)this.minecraft.gameRenderer.getDepthFar();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
            );
            RenderSystem.setShaderTexture(0, FORCEFIELD_LOCATION);
            RenderSystem.depthMask(Minecraft.useShaderTransparency());
            int i = worldborder.getStatus().getColor();
            float f = (float)(i >> 16 & 0xFF) / 255.0F;
            float f1 = (float)(i >> 8 & 0xFF) / 255.0F;
            float f2 = (float)(i & 0xFF) / 255.0F;
            RenderSystem.setShaderColor(f, f1, f2, (float)d1);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.polygonOffset(-3.0F, -3.0F);
            RenderSystem.enablePolygonOffset();
            RenderSystem.disableCull();
            float f3 = (float)(Util.getMillis() % 3000L) / 3000.0F;
            float f4 = (float)(-Mth.frac(pCamera.getPosition().y * 0.5));
            float f5 = f4 + (float)d4;
            BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            double d5 = Math.max((double)Mth.floor(d3 - d0), worldborder.getMinZ());
            double d6 = Math.min((double)Mth.ceil(d3 + d0), worldborder.getMaxZ());
            float f6 = (float)(Mth.floor(d5) & 1) * 0.5F;
            if (d2 > worldborder.getMaxX() - d0) {
                float f7 = f6;

                for (double d7 = d5; d7 < d6; f7 += 0.5F) {
                    double d8 = Math.min(1.0, d6 - d7);
                    float f8 = (float)d8 * 0.5F;
                    bufferbuilder.addVertex((float)(worldborder.getMaxX() - d2), (float)(-d4), (float)(d7 - d3)).setUv(f3 - f7, f3 + f5);
                    bufferbuilder.addVertex((float)(worldborder.getMaxX() - d2), (float)(-d4), (float)(d7 + d8 - d3)).setUv(f3 - (f8 + f7), f3 + f5);
                    bufferbuilder.addVertex((float)(worldborder.getMaxX() - d2), (float)d4, (float)(d7 + d8 - d3)).setUv(f3 - (f8 + f7), f3 + f4);
                    bufferbuilder.addVertex((float)(worldborder.getMaxX() - d2), (float)d4, (float)(d7 - d3)).setUv(f3 - f7, f3 + f4);
                    d7++;
                }
            }

            if (d2 < worldborder.getMinX() + d0) {
                float f9 = f6;

                for (double d9 = d5; d9 < d6; f9 += 0.5F) {
                    double d12 = Math.min(1.0, d6 - d9);
                    float f12 = (float)d12 * 0.5F;
                    bufferbuilder.addVertex((float)(worldborder.getMinX() - d2), (float)(-d4), (float)(d9 - d3)).setUv(f3 + f9, f3 + f5);
                    bufferbuilder.addVertex((float)(worldborder.getMinX() - d2), (float)(-d4), (float)(d9 + d12 - d3)).setUv(f3 + f12 + f9, f3 + f5);
                    bufferbuilder.addVertex((float)(worldborder.getMinX() - d2), (float)d4, (float)(d9 + d12 - d3)).setUv(f3 + f12 + f9, f3 + f4);
                    bufferbuilder.addVertex((float)(worldborder.getMinX() - d2), (float)d4, (float)(d9 - d3)).setUv(f3 + f9, f3 + f4);
                    d9++;
                }
            }

            d5 = Math.max((double)Mth.floor(d2 - d0), worldborder.getMinX());
            d6 = Math.min((double)Mth.ceil(d2 + d0), worldborder.getMaxX());
            f6 = (float)(Mth.floor(d5) & 1) * 0.5F;
            if (d3 > worldborder.getMaxZ() - d0) {
                float f10 = f6;

                for (double d10 = d5; d10 < d6; f10 += 0.5F) {
                    double d13 = Math.min(1.0, d6 - d10);
                    float f13 = (float)d13 * 0.5F;
                    bufferbuilder.addVertex((float)(d10 - d2), (float)(-d4), (float)(worldborder.getMaxZ() - d3)).setUv(f3 + f10, f3 + f5);
                    bufferbuilder.addVertex((float)(d10 + d13 - d2), (float)(-d4), (float)(worldborder.getMaxZ() - d3)).setUv(f3 + f13 + f10, f3 + f5);
                    bufferbuilder.addVertex((float)(d10 + d13 - d2), (float)d4, (float)(worldborder.getMaxZ() - d3)).setUv(f3 + f13 + f10, f3 + f4);
                    bufferbuilder.addVertex((float)(d10 - d2), (float)d4, (float)(worldborder.getMaxZ() - d3)).setUv(f3 + f10, f3 + f4);
                    d10++;
                }
            }

            if (d3 < worldborder.getMinZ() + d0) {
                float f11 = f6;

                for (double d11 = d5; d11 < d6; f11 += 0.5F) {
                    double d14 = Math.min(1.0, d6 - d11);
                    float f14 = (float)d14 * 0.5F;
                    bufferbuilder.addVertex((float)(d11 - d2), (float)(-d4), (float)(worldborder.getMinZ() - d3)).setUv(f3 - f11, f3 + f5);
                    bufferbuilder.addVertex((float)(d11 + d14 - d2), (float)(-d4), (float)(worldborder.getMinZ() - d3)).setUv(f3 - (f14 + f11), f3 + f5);
                    bufferbuilder.addVertex((float)(d11 + d14 - d2), (float)d4, (float)(worldborder.getMinZ() - d3)).setUv(f3 - (f14 + f11), f3 + f4);
                    bufferbuilder.addVertex((float)(d11 - d2), (float)d4, (float)(worldborder.getMinZ() - d3)).setUv(f3 - f11, f3 + f4);
                    d11++;
                }
            }

            MeshData meshdata = bufferbuilder.build();
            if (meshdata != null) {
                BufferUploader.drawWithShader(meshdata);
            }

            RenderSystem.enableCull();
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
            );
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.depthMask(true);
            if (Config.isShaders()) {
                Shaders.popProgram();
                Shaders.setRenderStage(RenderStage.NONE);
            }
        }
    }

    private void renderHitOutline(
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        Entity pEntity,
        double pCamX,
        double pCamY,
        double pCamZ,
        BlockPos pPos,
        BlockState pState
    ) {
        if (!Config.isCustomEntityModels() || !CustomEntityModels.isCustomModel(pState)) {
            renderShape(
                pPoseStack,
                pConsumer,
                pState.getShape(this.level, pPos, CollisionContext.of(pEntity)),
                (double)pPos.getX() - pCamX,
                (double)pPos.getY() - pCamY,
                (double)pPos.getZ() - pCamZ,
                0.0F,
                0.0F,
                0.0F,
                0.4F
            );
        }
    }

    private static Vec3 mixColor(float pHue) {
        float f = 5.99999F;
        int i = (int)(Mth.clamp(pHue, 0.0F, 1.0F) * 5.99999F);
        float f1 = pHue * 5.99999F - (float)i;

        return switch (i) {
            case 0 -> new Vec3(1.0, (double)f1, 0.0);
            case 1 -> new Vec3((double)(1.0F - f1), 1.0, 0.0);
            case 2 -> new Vec3(0.0, 1.0, (double)f1);
            case 3 -> new Vec3(0.0, 1.0 - (double)f1, 1.0);
            case 4 -> new Vec3((double)f1, 0.0, 1.0);
            case 5 -> new Vec3(1.0, 0.0, 1.0 - (double)f1);
            default -> throw new IllegalStateException("Unexpected value: " + i);
        };
    }

    private static Vec3 shiftHue(float pRed, float pGreen, float pBlue, float pHue) {
        Vec3 vec3 = mixColor(pHue).scale((double)pRed);
        Vec3 vec31 = mixColor((pHue + 0.33333334F) % 1.0F).scale((double)pGreen);
        Vec3 vec32 = mixColor((pHue + 0.6666667F) % 1.0F).scale((double)pBlue);
        Vec3 vec33 = vec3.add(vec31).add(vec32);
        double d0 = Math.max(Math.max(1.0, vec33.x), Math.max(vec33.y, vec33.z));
        return new Vec3(vec33.x / d0, vec33.y / d0, vec33.z / d0);
    }

    public static void renderVoxelShape(
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        VoxelShape pShape,
        double pX,
        double pY,
        double pZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha,
        boolean p_286443_
    ) {
        List<AABB> list = pShape.toAabbs();
        if (!list.isEmpty()) {
            int i = p_286443_ ? list.size() : list.size() * 8;
            renderShape(pPoseStack, pConsumer, Shapes.create(list.get(0)), pX, pY, pZ, pRed, pGreen, pBlue, pAlpha);

            for (int j = 1; j < list.size(); j++) {
                AABB aabb = list.get(j);
                float f = (float)j / (float)i;
                Vec3 vec3 = shiftHue(pRed, pGreen, pBlue, f);
                renderShape(
                    pPoseStack,
                    pConsumer,
                    Shapes.create(aabb),
                    pX,
                    pY,
                    pZ,
                    (float)vec3.x,
                    (float)vec3.y,
                    (float)vec3.z,
                    pAlpha
                );
            }
        }
    }

    private static void renderShape(
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        VoxelShape pShape,
        double pX,
        double pY,
        double pZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        PoseStack.Pose posestack$pose = pPoseStack.last();
        pShape.forAllEdges(
            (x0, y0, z0, x1, y1, z1) -> {
                float f = (float)(x1 - x0);
                float f1 = (float)(y1 - y0);
                float f2 = (float)(z1 - z0);
                float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
                f /= f3;
                f1 /= f3;
                f2 /= f3;
                pConsumer.addVertex(posestack$pose, (float)(x0 + pX), (float)(y0 + pY), (float)(z0 + pZ))
                    .setColor(pRed, pGreen, pBlue, pAlpha)
                    .setNormal(posestack$pose, f, f1, f2);
                pConsumer.addVertex(posestack$pose, (float)(x1 + pX), (float)(y1 + pY), (float)(z1 + pZ))
                    .setColor(pRed, pGreen, pBlue, pAlpha)
                    .setNormal(posestack$pose, f, f1, f2);
            }
        );
    }

    public static void renderLineBox(
        VertexConsumer pConsumer,
        double pMinX,
        double pMinY,
        double pMinZ,
        double pMaxX,
        double pMaxY,
        double pMaxZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        renderLineBox(
            new PoseStack(),
            pConsumer,
            pMinX,
            pMinY,
            pMinZ,
            pMaxX,
            pMaxY,
            pMaxZ,
            pRed,
            pGreen,
            pBlue,
            pAlpha,
            pRed,
            pGreen,
            pBlue
        );
    }

    public static void renderLineBox(
        PoseStack pPoseStack, VertexConsumer pBuffer, AABB pBox, float pRed, float pGreen, float pBlue, float pAlpha
    ) {
        renderLineBox(
            pPoseStack,
            pBuffer,
            pBox.minX,
            pBox.minY,
            pBox.minZ,
            pBox.maxX,
            pBox.maxY,
            pBox.maxZ,
            pRed,
            pGreen,
            pBlue,
            pAlpha,
            pRed,
            pGreen,
            pBlue
        );
    }

    public static void renderLineBox(
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        double pMinX,
        double pMinY,
        double pMinZ,
        double pMaxX,
        double pMaxY,
        double pMaxZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        renderLineBox(
            pPoseStack,
            pConsumer,
            pMinX,
            pMinY,
            pMinZ,
            pMaxX,
            pMaxY,
            pMaxZ,
            pRed,
            pGreen,
            pBlue,
            pAlpha,
            pRed,
            pGreen,
            pBlue
        );
    }

    public static void renderLineBox(
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        double pMinX,
        double pMinY,
        double pMinZ,
        double pMaxX,
        double pMaxY,
        double pMaxZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha,
        float pRed2,
        float pGreen2,
        float pBlue2
    ) {
        PoseStack.Pose posestack$pose = pPoseStack.last();
        float f = (float)pMinX;
        float f1 = (float)pMinY;
        float f2 = (float)pMinZ;
        float f3 = (float)pMaxX;
        float f4 = (float)pMaxY;
        float f5 = (float)pMaxZ;
        pConsumer.addVertex(posestack$pose, f, f1, f2).setColor(pRed, pGreen2, pBlue2, pAlpha).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f3, f1, f2).setColor(pRed, pGreen2, pBlue2, pAlpha).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f, f1, f2).setColor(pRed2, pGreen, pBlue2, pAlpha).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f, f4, f2).setColor(pRed2, pGreen, pBlue2, pAlpha).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f, f1, f2).setColor(pRed2, pGreen2, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        pConsumer.addVertex(posestack$pose, f, f1, f5).setColor(pRed2, pGreen2, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        pConsumer.addVertex(posestack$pose, f3, f1, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f3, f4, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f3, f4, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, -1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f, f4, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, -1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f, f4, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        pConsumer.addVertex(posestack$pose, f, f4, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        pConsumer.addVertex(posestack$pose, f, f4, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, -1.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f, f1, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, -1.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f, f1, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f3, f1, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f3, f1, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, -1.0F);
        pConsumer.addVertex(posestack$pose, f3, f1, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, -1.0F);
        pConsumer.addVertex(posestack$pose, f, f4, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f3, f4, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f3, f1, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f3, f4, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        pConsumer.addVertex(posestack$pose, f3, f4, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        pConsumer.addVertex(posestack$pose, f3, f4, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
    }

    public static void addChainedFilledBoxVertices(
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        double pMinX,
        double pMinY,
        double pMinZ,
        double pMaxX,
        double pMaxY,
        double pMaxZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        addChainedFilledBoxVertices(
            pPoseStack,
            pConsumer,
            (float)pMinX,
            (float)pMinY,
            (float)pMinZ,
            (float)pMaxX,
            (float)pMaxY,
            (float)pMaxZ,
            pRed,
            pGreen,
            pBlue,
            pAlpha
        );
    }

    public static void addChainedFilledBoxVertices(
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        float pMinX,
        float pMinY,
        float pMinZ,
        float pMaxX,
        float pMaxY,
        float pMaxZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        Matrix4f matrix4f = pPoseStack.last().pose();
        pConsumer.addVertex(matrix4f, pMinX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMinX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pConsumer.addVertex(matrix4f, pMaxX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
    }

    public static void renderFace(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        Direction pFace,
        float pX1,
        float pY1,
        float pZ1,
        float pX2,
        float pY2,
        float pZ2,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        Matrix4f matrix4f = pPoseStack.last().pose();
        switch (pFace) {
            case DOWN:
                pBuffer.addVertex(matrix4f, pX1, pY1, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY1, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY1, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY1, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                break;
            case UP:
                pBuffer.addVertex(matrix4f, pX1, pY2, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY2, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY2, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY2, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                break;
            case NORTH:
                pBuffer.addVertex(matrix4f, pX1, pY1, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY2, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY2, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY1, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                break;
            case SOUTH:
                pBuffer.addVertex(matrix4f, pX1, pY1, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY1, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY2, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY2, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                break;
            case WEST:
                pBuffer.addVertex(matrix4f, pX1, pY1, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY1, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY2, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY2, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                break;
            case EAST:
                pBuffer.addVertex(matrix4f, pX2, pY1, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY2, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY2, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY1, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
        }
    }

    public void blockChanged(BlockGetter pLevel, BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {
        this.setBlockDirty(pPos, (pFlags & 8) != 0);
    }

    private void setBlockDirty(BlockPos pPos, boolean pReRenderOnMainThread) {
        for (int i = pPos.getZ() - 1; i <= pPos.getZ() + 1; i++) {
            for (int j = pPos.getX() - 1; j <= pPos.getX() + 1; j++) {
                for (int k = pPos.getY() - 1; k <= pPos.getY() + 1; k++) {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i), pReRenderOnMainThread);
                }
            }
        }
    }

    public void setBlocksDirty(int pMinX, int pMinY, int pMinZ, int pMaxX, int pMaxY, int pMaxZ) {
        for (int i = pMinZ - 1; i <= pMaxZ + 1; i++) {
            for (int j = pMinX - 1; j <= pMaxX + 1; j++) {
                for (int k = pMinY - 1; k <= pMaxY + 1; k++) {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i));
                }
            }
        }
    }

    public void setBlockDirty(BlockPos pPos, BlockState pOldState, BlockState pNewState) {
        if (this.minecraft.getModelManager().requiresRender(pOldState, pNewState)) {
            this.setBlocksDirty(
                pPos.getX(), pPos.getY(), pPos.getZ(), pPos.getX(), pPos.getY(), pPos.getZ()
            );
        }
    }

    public void setSectionDirtyWithNeighbors(int pSectionX, int pSectionY, int pSectionZ) {
        for (int i = pSectionZ - 1; i <= pSectionZ + 1; i++) {
            for (int j = pSectionX - 1; j <= pSectionX + 1; j++) {
                for (int k = pSectionY - 1; k <= pSectionY + 1; k++) {
                    this.setSectionDirty(j, k, i);
                }
            }
        }
    }

    public void setSectionDirty(int pSectionX, int pSectionY, int pSectionZ) {
        this.setSectionDirty(pSectionX, pSectionY, pSectionZ, false);
    }

    private void setSectionDirty(int pSectionX, int pSectionY, int pSectionZ, boolean pReRenderOnMainThread) {
        this.viewArea.setDirty(pSectionX, pSectionY, pSectionZ, pReRenderOnMainThread);
    }

    public void playJukeboxSong(Holder<JukeboxSong> pSong, BlockPos pPos) {
        if (this.level != null) {
            this.stopJukeboxSong(pPos);
            JukeboxSong jukeboxsong = pSong.value();
            SoundEvent soundevent = jukeboxsong.soundEvent().value();
            SoundInstance soundinstance = SimpleSoundInstance.forJukeboxSong(soundevent, Vec3.atCenterOf(pPos));
            this.playingJukeboxSongs.put(pPos, soundinstance);
            this.minecraft.getSoundManager().play(soundinstance);
            this.minecraft.gui.setNowPlaying(jukeboxsong.description());
            this.notifyNearbyEntities(this.level, pPos, true);
        }
    }

    private void stopJukeboxSong(BlockPos pPos) {
        SoundInstance soundinstance = this.playingJukeboxSongs.remove(pPos);
        if (soundinstance != null) {
            this.minecraft.getSoundManager().stop(soundinstance);
        }
    }

    public void stopJukeboxSongAndNotifyNearby(BlockPos pPos) {
        this.stopJukeboxSong(pPos);
        if (this.level != null) {
            this.notifyNearbyEntities(this.level, pPos, false);
        }
    }

    private void notifyNearbyEntities(Level pLevel, BlockPos pPos, boolean pPlaying) {
        for (LivingEntity livingentity : pLevel.getEntitiesOfClass(LivingEntity.class, new AABB(pPos).inflate(3.0))) {
            livingentity.setRecordPlayingNearby(pPos, pPlaying);
        }
    }

    public void addParticle(
        ParticleOptions pOptions,
        boolean pForce,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        this.addParticle(pOptions, pForce, false, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    public void addParticle(
        ParticleOptions pOptions,
        boolean pForce,
        boolean pDecreased,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        try {
            this.addParticleInternal(pOptions, pForce, pDecreased, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception while adding particle");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being added");
            crashreportcategory.setDetail("ID", BuiltInRegistries.PARTICLE_TYPE.getKey(pOptions.getType()));
            crashreportcategory.setDetail(
                "Parameters", () -> ParticleTypes.CODEC.encodeStart(this.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), pOptions).toString()
            );
            crashreportcategory.setDetail("Position", () -> CrashReportCategory.formatLocation(this.level, pX, pY, pZ));
            throw new ReportedException(crashreport);
        }
    }

    private <T extends ParticleOptions> void addParticle(
        T pOptions, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
    ) {
        this.addParticle(pOptions, pOptions.getType().getOverrideLimiter(), pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    @Nullable
    private Particle addParticleInternal(
        ParticleOptions pOptions,
        boolean pForce,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        return this.addParticleInternal(pOptions, pForce, false, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    @Nullable
    private Particle addParticleInternal(
        ParticleOptions pOptions,
        boolean pForce,
        boolean pDecreased,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        Camera camera = this.minecraft.gameRenderer.getMainCamera();
        ParticleStatus particlestatus = this.calculateParticleLevel(pDecreased);
        if (pOptions == ParticleTypes.EXPLOSION_EMITTER && !Config.isAnimatedExplosion()) {
            return null;
        } else if (pOptions == ParticleTypes.EXPLOSION && !Config.isAnimatedExplosion()) {
            return null;
        } else if (pOptions == ParticleTypes.POOF && !Config.isAnimatedExplosion()) {
            return null;
        } else if (pOptions == ParticleTypes.UNDERWATER && !Config.isWaterParticles()) {
            return null;
        } else if (pOptions == ParticleTypes.SMOKE && !Config.isAnimatedSmoke()) {
            return null;
        } else if (pOptions == ParticleTypes.LARGE_SMOKE && !Config.isAnimatedSmoke()) {
            return null;
        } else if (pOptions == ParticleTypes.ENTITY_EFFECT && !Config.isPotionParticles()) {
            return null;
        } else if (pOptions == ParticleTypes.EFFECT && !Config.isPotionParticles()) {
            return null;
        } else if (pOptions == ParticleTypes.INSTANT_EFFECT && !Config.isPotionParticles()) {
            return null;
        } else if (pOptions == ParticleTypes.WITCH && !Config.isPotionParticles()) {
            return null;
        } else if (pOptions == ParticleTypes.PORTAL && !Config.isPortalParticles()) {
            return null;
        } else if (pOptions == ParticleTypes.FLAME && !Config.isAnimatedFlame()) {
            return null;
        } else if (pOptions == ParticleTypes.SOUL_FIRE_FLAME && !Config.isAnimatedFlame()) {
            return null;
        } else if (pOptions == ParticleTypes.DUST && !Config.isAnimatedRedstone()) {
            return null;
        } else if (pOptions == ParticleTypes.DRIPPING_WATER && !Config.isDrippingWaterLava()) {
            return null;
        } else if (pOptions == ParticleTypes.DRIPPING_LAVA && !Config.isDrippingWaterLava()) {
            return null;
        } else if (pOptions == ParticleTypes.FIREWORK && !Config.isFireworkParticles()) {
            return null;
        } else {
            if (!pForce) {
                double d0 = 1024.0;
                if (pOptions == ParticleTypes.CRIT) {
                    d0 = 38416.0;
                }

                if (camera.getPosition().distanceToSqr(pX, pY, pZ) > d0) {
                    return null;
                }

                if (particlestatus == ParticleStatus.MINIMAL) {
                    return null;
                }
            }

            Particle particle = this.minecraft.particleEngine.createParticle(pOptions, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            if (pOptions == ParticleTypes.BUBBLE) {
                CustomColors.updateWaterFX(particle, this.level, pX, pY, pZ, this.renderEnv);
            }

            if (pOptions == ParticleTypes.SPLASH) {
                CustomColors.updateWaterFX(particle, this.level, pX, pY, pZ, this.renderEnv);
            }

            if (pOptions == ParticleTypes.RAIN) {
                CustomColors.updateWaterFX(particle, this.level, pX, pY, pZ, this.renderEnv);
            }

            if (pOptions == ParticleTypes.MYCELIUM) {
                CustomColors.updateMyceliumFX(particle);
            }

            if (pOptions == ParticleTypes.PORTAL) {
                CustomColors.updatePortalFX(particle);
            }

            if (pOptions == ParticleTypes.DUST) {
                CustomColors.updateReddustFX(particle, this.level, pX, pY, pZ);
            }

            if (pOptions == ParticleTypes.LAVA) {
                CustomColors.updateLavaFX(particle);
            }

            return particle;
        }
    }

    private ParticleStatus calculateParticleLevel(boolean pDecreased) {
        ParticleStatus particlestatus = this.minecraft.options.particles().get();
        if (pDecreased && particlestatus == ParticleStatus.MINIMAL && this.level.random.nextInt(10) == 0) {
            particlestatus = ParticleStatus.DECREASED;
        }

        if (particlestatus == ParticleStatus.DECREASED && this.level.random.nextInt(3) == 0) {
            particlestatus = ParticleStatus.MINIMAL;
        }

        return particlestatus;
    }

    public void clear() {
    }

    public void globalLevelEvent(int pType, BlockPos pPos, int pData) {
        switch (pType) {
            case 1023:
            case 1028:
            case 1038:
                Camera camera = this.minecraft.gameRenderer.getMainCamera();
                if (camera.isInitialized()) {
                    double d0 = (double)pPos.getX() - camera.getPosition().x;
                    double d1 = (double)pPos.getY() - camera.getPosition().y;
                    double d2 = (double)pPos.getZ() - camera.getPosition().z;
                    double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                    double d4 = camera.getPosition().x;
                    double d5 = camera.getPosition().y;
                    double d6 = camera.getPosition().z;
                    if (d3 > 0.0) {
                        d4 += d0 / d3 * 2.0;
                        d5 += d1 / d3 * 2.0;
                        d6 += d2 / d3 * 2.0;
                    }

                    if (pType == 1023) {
                        this.level.playLocalSound(d4, d5, d6, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
                    } else if (pType == 1038) {
                        this.level.playLocalSound(d4, d5, d6, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
                    } else {
                        this.level.playLocalSound(d4, d5, d6, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0F, 1.0F, false);
                    }
                }
        }
    }

    public void levelEvent(int pType, BlockPos pPos, int pData) {
        RandomSource randomsource = this.level.random;
        switch (pType) {
            case 1000:
                this.level.playLocalSound(pPos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1001:
                this.level.playLocalSound(pPos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F, false);
                break;
            case 1002:
                this.level.playLocalSound(pPos, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.2F, false);
                break;
            case 1004:
                this.level.playLocalSound(pPos, SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
                break;
            case 1009:
                if (pData == 0) {
                    this.level
                        .playLocalSound(
                            pPos,
                            SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.BLOCKS,
                            0.5F,
                            2.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.8F,
                            false
                        );
                } else if (pData == 1) {
                    this.level
                        .playLocalSound(
                            pPos,
                            SoundEvents.GENERIC_EXTINGUISH_FIRE,
                            SoundSource.BLOCKS,
                            0.7F,
                            1.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.4F,
                            false
                        );
                }
                break;
            case 1010:
                this.level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG).getHolder(pData).ifPresent(songIn -> this.playJukeboxSong(songIn, pPos));
                break;
            case 1011:
                this.stopJukeboxSongAndNotifyNearby(pPos);
                break;
            case 1015:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 10.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1016:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 10.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1017:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 10.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1018:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1019:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1020:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1021:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1022:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1024:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1025:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.05F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1026:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1027:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1029:
                this.level.playLocalSound(pPos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1030:
                this.level.playLocalSound(pPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1031:
                this.level.playLocalSound(pPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1032:
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, randomsource.nextFloat() * 0.4F + 0.8F, 0.25F));
                break;
            case 1033:
                this.level.playLocalSound(pPos, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1034:
                this.level.playLocalSound(pPos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1035:
                this.level.playLocalSound(pPos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1039:
                this.level.playLocalSound(pPos, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1040:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1041:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.HUSK_CONVERTED_TO_ZOMBIE, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1042:
                this.level.playLocalSound(pPos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1043:
                this.level.playLocalSound(pPos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1044:
                this.level.playLocalSound(pPos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1045:
                this.level.playLocalSound(pPos, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1046:
                this.level.playLocalSound(pPos, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1047:
                this.level.playLocalSound(pPos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 1048:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.SKELETON_CONVERTED_TO_STRAY, SoundSource.HOSTILE, 2.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, false
                    );
                break;
            case 1049:
                this.level.playLocalSound(pPos, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1050:
                this.level.playLocalSound(pPos, SoundEvents.CRAFTER_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 1051:
                this.level
                    .playLocalSound(pPos, SoundEvents.WIND_CHARGE_THROW, SoundSource.BLOCKS, 0.5F, 0.4F / (this.level.getRandom().nextFloat() * 0.4F + 0.8F), false);
            case 2010:
                this.shootParticles(pData, pPos, randomsource, ParticleTypes.WHITE_SMOKE);
                break;
            case 1500:
                ComposterBlock.handleFill(this.level, pPos, pData > 0);
                break;
            case 1501:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.8F, false
                    );

                for (int k1 = 0; k1 < 8; k1++) {
                    this.level
                        .addParticle(
                            ParticleTypes.LARGE_SMOKE,
                            (double)pPos.getX() + randomsource.nextDouble(),
                            (double)pPos.getY() + 1.2,
                            (double)pPos.getZ() + randomsource.nextDouble(),
                            0.0,
                            0.0,
                            0.0
                        );
                }
                break;
            case 1502:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.5F, 2.6F + (randomsource.nextFloat() - randomsource.nextFloat()) * 0.8F, false
                    );

                for (int j1 = 0; j1 < 5; j1++) {
                    double d11 = (double)pPos.getX() + randomsource.nextDouble() * 0.6 + 0.2;
                    double d13 = (double)pPos.getY() + randomsource.nextDouble() * 0.6 + 0.2;
                    double d15 = (double)pPos.getZ() + randomsource.nextDouble() * 0.6 + 0.2;
                    this.level.addParticle(ParticleTypes.SMOKE, d11, d13, d15, 0.0, 0.0, 0.0);
                }
                break;
            case 1503:
                this.level.playLocalSound(pPos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);

                for (int i1 = 0; i1 < 16; i1++) {
                    double d10 = (double)pPos.getX() + (5.0 + randomsource.nextDouble() * 6.0) / 16.0;
                    double d12 = (double)pPos.getY() + 0.8125;
                    double d14 = (double)pPos.getZ() + (5.0 + randomsource.nextDouble() * 6.0) / 16.0;
                    this.level.addParticle(ParticleTypes.SMOKE, d10, d12, d14, 0.0, 0.0, 0.0);
                }
                break;
            case 1504:
                PointedDripstoneBlock.spawnDripParticle(this.level, pPos, this.level.getBlockState(pPos));
                break;
            case 1505:
                BoneMealItem.addGrowthParticles(this.level, pPos, pData);
                this.level.playLocalSound(pPos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 2000:
                this.shootParticles(pData, pPos, randomsource, ParticleTypes.SMOKE);
                break;
            case 2001:
                BlockState blockstate = Block.stateById(pData);
                if (!blockstate.isAir()) {
                    SoundType soundtype = blockstate.getSoundType();
                    if (Reflector.IForgeBlockState_getSoundType3.exists()) {
                        soundtype = (SoundType)Reflector.call(blockstate, Reflector.IForgeBlockState_getSoundType3, this.level, pPos, null);
                    }

                    this.level
                        .playLocalSound(
                            pPos, soundtype.getBreakSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F, false
                        );
                }

                this.level.addDestroyBlockEffect(pPos, blockstate);
                break;
            case 2002:
            case 2007:
                Vec3 vec3 = Vec3.atBottomCenterOf(pPos);

                for (int i = 0; i < 8; i++) {
                    this.addParticle(
                        new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SPLASH_POTION)),
                        vec3.x,
                        vec3.y,
                        vec3.z,
                        randomsource.nextGaussian() * 0.15,
                        randomsource.nextDouble() * 0.2,
                        randomsource.nextGaussian() * 0.15
                    );
                }

                float f2 = (float)(pData >> 16 & 0xFF) / 255.0F;
                float f = (float)(pData >> 8 & 0xFF) / 255.0F;
                float f1 = (float)(pData >> 0 & 0xFF) / 255.0F;
                ParticleOptions particleoptions = pType == 2007 ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;

                for (int l1 = 0; l1 < 100; l1++) {
                    double d1 = randomsource.nextDouble() * 4.0;
                    double d3 = randomsource.nextDouble() * Math.PI * 2.0;
                    double d5 = Math.cos(d3) * d1;
                    double d18 = 0.01 + randomsource.nextDouble() * 0.5;
                    double d21 = Math.sin(d3) * d1;
                    Particle particle = this.addParticleInternal(
                        particleoptions,
                        particleoptions.getType().getOverrideLimiter(),
                        vec3.x + d5 * 0.1,
                        vec3.y + 0.3,
                        vec3.z + d21 * 0.1,
                        d5,
                        d18,
                        d21
                    );
                    if (particle != null) {
                        float f11 = 0.75F + randomsource.nextFloat() * 0.25F;
                        particle.setColor(f2 * f11, f * f11, f1 * f11);
                        particle.setPower((float)d1);
                    }
                }

                this.level.playLocalSound(pPos, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
                break;
            case 2003:
                double d0 = (double)pPos.getX() + 0.5;
                double d2 = (double)pPos.getY();
                double d4 = (double)pPos.getZ() + 0.5;

                for (int l2 = 0; l2 < 8; l2++) {
                    this.addParticle(
                        new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ENDER_EYE)),
                        d0,
                        d2,
                        d4,
                        randomsource.nextGaussian() * 0.15,
                        randomsource.nextDouble() * 0.2,
                        randomsource.nextGaussian() * 0.15
                    );
                }

                for (double d16 = 0.0; d16 < Math.PI * 2; d16 += Math.PI / 20) {
                    this.addParticle(
                        ParticleTypes.PORTAL, d0 + Math.cos(d16) * 5.0, d2 - 0.4, d4 + Math.sin(d16) * 5.0, Math.cos(d16) * -5.0, 0.0, Math.sin(d16) * -5.0
                    );
                    this.addParticle(
                        ParticleTypes.PORTAL, d0 + Math.cos(d16) * 5.0, d2 - 0.4, d4 + Math.sin(d16) * 5.0, Math.cos(d16) * -7.0, 0.0, Math.sin(d16) * -7.0
                    );
                }
                break;
            case 2004:
                for (int k2 = 0; k2 < 20; k2++) {
                    double d17 = (double)pPos.getX() + 0.5 + (randomsource.nextDouble() - 0.5) * 2.0;
                    double d20 = (double)pPos.getY() + 0.5 + (randomsource.nextDouble() - 0.5) * 2.0;
                    double d23 = (double)pPos.getZ() + 0.5 + (randomsource.nextDouble() - 0.5) * 2.0;
                    this.level.addParticle(ParticleTypes.SMOKE, d17, d20, d23, 0.0, 0.0, 0.0);
                    this.level.addParticle(ParticleTypes.FLAME, d17, d20, d23, 0.0, 0.0, 0.0);
                }
                break;
            case 2006:
                for (int j2 = 0; j2 < 200; j2++) {
                    float f4 = randomsource.nextFloat() * 4.0F;
                    float f6 = randomsource.nextFloat() * (float) (Math.PI * 2);
                    double d19 = (double)(Mth.cos(f6) * f4);
                    double d22 = 0.01 + randomsource.nextDouble() * 0.5;
                    double d24 = (double)(Mth.sin(f6) * f4);
                    Particle particle1 = this.addParticleInternal(
                        ParticleTypes.DRAGON_BREATH,
                        false,
                        (double)pPos.getX() + d19 * 0.1,
                        (double)pPos.getY() + 0.3,
                        (double)pPos.getZ() + d24 * 0.1,
                        d19,
                        d22,
                        d24
                    );
                    if (particle1 != null) {
                        particle1.setPower(f4);
                    }
                }

                if (pData == 1) {
                    this.level.playLocalSound(pPos, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0F, randomsource.nextFloat() * 0.1F + 0.9F, false);
                }
                break;
            case 2008:
                this.level
                    .addParticle(
                        ParticleTypes.EXPLOSION,
                        (double)pPos.getX() + 0.5,
                        (double)pPos.getY() + 0.5,
                        (double)pPos.getZ() + 0.5,
                        0.0,
                        0.0,
                        0.0
                    );
                break;
            case 2009:
                for (int i2 = 0; i2 < 8; i2++) {
                    this.level
                        .addParticle(
                            ParticleTypes.CLOUD,
                            (double)pPos.getX() + randomsource.nextDouble(),
                            (double)pPos.getY() + 1.2,
                            (double)pPos.getZ() + randomsource.nextDouble(),
                            0.0,
                            0.0,
                            0.0
                        );
                }
                break;
            case 2011:
                ParticleUtils.spawnParticleInBlock(this.level, pPos, pData, ParticleTypes.HAPPY_VILLAGER);
                break;
            case 2012:
                ParticleUtils.spawnParticleInBlock(this.level, pPos, pData, ParticleTypes.HAPPY_VILLAGER);
                break;
            case 2013:
                ParticleUtils.spawnSmashAttackParticles(this.level, pPos, pData);
                break;
            case 3000:
                this.level
                    .addParticle(
                        ParticleTypes.EXPLOSION_EMITTER,
                        true,
                        (double)pPos.getX() + 0.5,
                        (double)pPos.getY() + 0.5,
                        (double)pPos.getZ() + 0.5,
                        0.0,
                        0.0,
                        0.0
                    );
                this.level
                    .playLocalSound(
                        pPos,
                        SoundEvents.END_GATEWAY_SPAWN,
                        SoundSource.BLOCKS,
                        10.0F,
                        (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F,
                        false
                    );
                break;
            case 3001:
                this.level.playLocalSound(pPos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 64.0F, 0.8F + this.level.random.nextFloat() * 0.3F, false);
                break;
            case 3002:
                if (pData >= 0 && pData < Direction.Axis.VALUES.length) {
                    ParticleUtils.spawnParticlesAlongAxis(
                        Direction.Axis.VALUES[pData], this.level, pPos, 0.125, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(10, 19)
                    );
                } else {
                    ParticleUtils.spawnParticlesOnBlockFaces(this.level, pPos, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(3, 5));
                }
                break;
            case 3003:
                ParticleUtils.spawnParticlesOnBlockFaces(this.level, pPos, ParticleTypes.WAX_ON, UniformInt.of(3, 5));
                this.level.playLocalSound(pPos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                break;
            case 3004:
                ParticleUtils.spawnParticlesOnBlockFaces(this.level, pPos, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
                break;
            case 3005:
                ParticleUtils.spawnParticlesOnBlockFaces(this.level, pPos, ParticleTypes.SCRAPE, UniformInt.of(3, 5));
                break;
            case 3006:
                int j = pData >> 6;
                if (j > 0) {
                    if (randomsource.nextFloat() < 0.3F + (float)j * 0.1F) {
                        float f3 = 0.15F + 0.02F * (float)j * (float)j * randomsource.nextFloat();
                        float f5 = 0.4F + 0.3F * (float)j * randomsource.nextFloat();
                        this.level.playLocalSound(pPos, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, f3, f5, false);
                    }

                    byte b0 = (byte)(pData & 63);
                    IntProvider intprovider = UniformInt.of(0, j);
                    float f7 = 0.005F;
                    Supplier<Vec3> supplier = () -> new Vec3(
                            Mth.nextDouble(randomsource, -0.005F, 0.005F),
                            Mth.nextDouble(randomsource, -0.005F, 0.005F),
                            Mth.nextDouble(randomsource, -0.005F, 0.005F)
                        );
                    if (b0 == 0) {
                        for (Direction direction : Direction.values()) {
                            float f15 = direction == Direction.DOWN ? (float) Math.PI : 0.0F;
                            double d9 = direction.getAxis() == Direction.Axis.Y ? 0.65 : 0.57;
                            ParticleUtils.spawnParticlesOnBlockFace(this.level, pPos, new SculkChargeParticleOptions(f15), intprovider, direction, supplier, d9);
                        }
                    } else {
                        for (Direction direction1 : MultifaceBlock.unpack(b0)) {
                            float f12 = direction1 == Direction.UP ? (float) Math.PI : 0.0F;
                            double d25 = 0.35;
                            ParticleUtils.spawnParticlesOnBlockFace(this.level, pPos, new SculkChargeParticleOptions(f12), intprovider, direction1, supplier, 0.35);
                        }
                    }
                } else {
                    this.level.playLocalSound(pPos, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                    boolean flag1 = this.level.getBlockState(pPos).isCollisionShapeFullBlock(this.level, pPos);
                    int i3 = flag1 ? 40 : 20;
                    float f8 = flag1 ? 0.45F : 0.25F;
                    float f9 = 0.07F;

                    for (int j3 = 0; j3 < i3; j3++) {
                        float f10 = 2.0F * randomsource.nextFloat() - 1.0F;
                        float f13 = 2.0F * randomsource.nextFloat() - 1.0F;
                        float f14 = 2.0F * randomsource.nextFloat() - 1.0F;
                        this.level
                            .addParticle(
                                ParticleTypes.SCULK_CHARGE_POP,
                                (double)pPos.getX() + 0.5 + (double)(f10 * f8),
                                (double)pPos.getY() + 0.5 + (double)(f13 * f8),
                                (double)pPos.getZ() + 0.5 + (double)(f14 * f8),
                                (double)(f10 * 0.07F),
                                (double)(f13 * 0.07F),
                                (double)(f14 * 0.07F)
                            );
                    }
                }
                break;
            case 3007:
                for (int k = 0; k < 10; k++) {
                    this.level
                        .addParticle(
                            new ShriekParticleOption(k * 5),
                            false,
                            (double)pPos.getX() + 0.5,
                            (double)pPos.getY() + SculkShriekerBlock.TOP_Y,
                            (double)pPos.getZ() + 0.5,
                            0.0,
                            0.0,
                            0.0
                        );
                }

                BlockState blockstate2 = this.level.getBlockState(pPos);
                boolean flag = blockstate2.hasProperty(BlockStateProperties.WATERLOGGED) && blockstate2.getValue(BlockStateProperties.WATERLOGGED);
                if (!flag) {
                    this.level
                        .playLocalSound(
                            (double)pPos.getX() + 0.5,
                            (double)pPos.getY() + SculkShriekerBlock.TOP_Y,
                            (double)pPos.getZ() + 0.5,
                            SoundEvents.SCULK_SHRIEKER_SHRIEK,
                            SoundSource.BLOCKS,
                            2.0F,
                            0.6F + this.level.random.nextFloat() * 0.4F,
                            false
                        );
                }
                break;
            case 3008:
                BlockState blockstate1 = Block.stateById(pData);
                if (blockstate1.getBlock() instanceof BrushableBlock brushableblock) {
                    this.level.playLocalSound(pPos, brushableblock.getBrushCompletedSound(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
                }

                this.level.addDestroyBlockEffect(pPos, blockstate1);
                break;
            case 3009:
                ParticleUtils.spawnParticlesOnBlockFaces(this.level, pPos, ParticleTypes.EGG_CRACK, UniformInt.of(3, 6));
                break;
            case 3011:
                TrialSpawner.addSpawnParticles(this.level, pPos, randomsource, TrialSpawner.FlameParticle.decode(pData).particleType);
                break;
            case 3012:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                    );
                TrialSpawner.addSpawnParticles(this.level, pPos, randomsource, TrialSpawner.FlameParticle.decode(pData).particleType);
                break;
            case 3013:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                    );
                TrialSpawner.addDetectPlayerParticles(this.level, pPos, randomsource, pData, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER);
                break;
            case 3014:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.TRIAL_SPAWNER_EJECT_ITEM, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                    );
                TrialSpawner.addEjectItemParticles(this.level, pPos, randomsource);
                break;
            case 3015:
                if (this.level.getBlockEntity(pPos) instanceof VaultBlockEntity vaultblockentity) {
                    VaultBlockEntity.Client.emitActivationParticles(
                        this.level,
                        vaultblockentity.getBlockPos(),
                        vaultblockentity.getBlockState(),
                        vaultblockentity.getSharedData(),
                        pData == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME
                    );
                    this.level
                        .playLocalSound(
                            pPos,
                            SoundEvents.VAULT_ACTIVATE,
                            SoundSource.BLOCKS,
                            1.0F,
                            (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                            true
                        );
                }
                break;
            case 3016:
                VaultBlockEntity.Client.emitDeactivationParticles(this.level, pPos, pData == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME);
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.VAULT_DEACTIVATE, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                    );
                break;
            case 3017:
                TrialSpawner.addEjectItemParticles(this.level, pPos, randomsource);
                break;
            case 3018:
                for (int l = 0; l < 10; l++) {
                    double d6 = randomsource.nextGaussian() * 0.02;
                    double d7 = randomsource.nextGaussian() * 0.02;
                    double d8 = randomsource.nextGaussian() * 0.02;
                    this.level
                        .addParticle(
                            ParticleTypes.POOF,
                            (double)pPos.getX() + randomsource.nextDouble(),
                            (double)pPos.getY() + randomsource.nextDouble(),
                            (double)pPos.getZ() + randomsource.nextDouble(),
                            d6,
                            d7,
                            d8
                        );
                }

                this.level
                    .playLocalSound(
                        pPos, SoundEvents.COBWEB_PLACE, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                    );
                break;
            case 3019:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                    );
                TrialSpawner.addDetectPlayerParticles(this.level, pPos, randomsource, pData, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
                break;
            case 3020:
                this.level
                    .playLocalSound(
                        pPos,
                        SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE,
                        SoundSource.BLOCKS,
                        pData == 0 ? 0.3F : 1.0F,
                        (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F,
                        true
                    );
                TrialSpawner.addDetectPlayerParticles(this.level, pPos, randomsource, 0, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
                TrialSpawner.addBecomeOminousParticles(this.level, pPos, randomsource);
                break;
            case 3021:
                this.level
                    .playLocalSound(
                        pPos, SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM, SoundSource.BLOCKS, 1.0F, (randomsource.nextFloat() - randomsource.nextFloat()) * 0.2F + 1.0F, true
                    );
                TrialSpawner.addSpawnParticles(this.level, pPos, randomsource, TrialSpawner.FlameParticle.decode(pData).particleType);
        }
    }

    public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {
        if (pProgress >= 0 && pProgress < 10) {
            BlockDestructionProgress blockdestructionprogress1 = this.destroyingBlocks.get(pBreakerId);
            if (blockdestructionprogress1 != null) {
                this.removeProgress(blockdestructionprogress1);
            }

            if (blockdestructionprogress1 == null
                || blockdestructionprogress1.getPos().getX() != pPos.getX()
                || blockdestructionprogress1.getPos().getY() != pPos.getY()
                || blockdestructionprogress1.getPos().getZ() != pPos.getZ()) {
                blockdestructionprogress1 = new BlockDestructionProgress(pBreakerId, pPos);
                this.destroyingBlocks.put(pBreakerId, blockdestructionprogress1);
            }

            blockdestructionprogress1.setProgress(pProgress);
            blockdestructionprogress1.updateTick(this.ticks);
            this.destructionProgress.computeIfAbsent(blockdestructionprogress1.getPos().asLong(), keyIn -> Sets.newTreeSet()).add(blockdestructionprogress1);
        } else {
            BlockDestructionProgress blockdestructionprogress = this.destroyingBlocks.remove(pBreakerId);
            if (blockdestructionprogress != null) {
                this.removeProgress(blockdestructionprogress);
            }
        }
    }

    public boolean hasRenderedAllSections() {
        return this.sectionRenderDispatcher.isQueueEmpty();
    }

    public void onChunkLoaded(ChunkPos pChunkPos) {
        this.sectionOcclusionGraph.onChunkLoaded(pChunkPos);
    }

    public void needsUpdate() {
        this.sectionOcclusionGraph.invalidate();
        this.generateClouds = true;
    }

    public int getCountRenderers() {
        return this.viewArea.sections.length;
    }

    public int getCountEntitiesRendered() {
        return this.renderedEntities;
    }

    public int getCountTileEntitiesRendered() {
        return this.countTileEntitiesRendered;
    }

    public int getCountLoadedChunks() {
        if (this.level == null) {
            return 0;
        } else {
            ClientChunkCache clientchunkcache = this.level.getChunkSource();
            return clientchunkcache == null ? 0 : clientchunkcache.getLoadedChunksCount();
        }
    }

    public int getCountChunksToUpdate() {
        return this.countChunksToUpdate;
    }

    public SectionRenderDispatcher.RenderSection getRenderChunk(BlockPos pos) {
        return this.viewArea.getRenderSectionAt(pos);
    }

    public ClientLevel getWorld() {
        return this.level;
    }

    private void clearRenderInfos() {
        this.clearRenderInfosTerrain();
        this.clearRenderInfosEntities();
    }

    private void clearRenderInfosTerrain() {
        if (renderEntitiesCounter > 0) {
            this.renderInfosTerrain = new ObjectArrayList<>(this.renderInfosTerrain.size() + 16);
            this.renderInfosTileEntities = new ArrayList<>(this.renderInfosTileEntities.size() + 16);
        } else {
            this.renderInfosTerrain.clear();
            this.renderInfosTileEntities.clear();
        }
    }

    private void clearRenderInfosEntities() {
        if (renderEntitiesCounter > 0) {
            this.renderInfosEntities = new LongOpenHashSet(this.renderInfosEntities.size() + 16);
        } else {
            this.renderInfosEntities.clear();
        }
    }

    public void onPlayerPositionSet() {
        if (this.firstWorldLoad) {
            this.allChanged();
            this.firstWorldLoad = false;
        }
    }

    public void pauseChunkUpdates() {
        if (this.sectionRenderDispatcher != null) {
            this.sectionRenderDispatcher.pauseChunkUpdates();
        }
    }

    public void resumeChunkUpdates() {
        if (this.sectionRenderDispatcher != null) {
            this.sectionRenderDispatcher.resumeChunkUpdates();
        }
    }

    public int getFrameCount() {
        return this.frameId;
    }

    public RenderBuffers getRenderTypeTextures() {
        return this.renderBuffers;
    }

    public LongOpenHashSet getRenderChunksEntities() {
        return this.renderInfosEntities;
    }

    private void addEntitySection(LongOpenHashSet set, EntitySectionStorage storage, BlockPos pos) {
        long i = SectionPos.asLong(pos);
        EntitySection entitysection = storage.getSection(i);
        if (entitysection != null) {
            set.add(i);
        }
    }

    private boolean hasEntitySection(EntitySectionStorage storage, BlockPos pos) {
        long i = SectionPos.asLong(pos);
        EntitySection entitysection = storage.getSection(i);
        return entitysection != null;
    }

    public List<SectionRenderDispatcher.RenderSection> getRenderInfos() {
        return this.visibleSections;
    }

    public List<SectionRenderDispatcher.RenderSection> getRenderInfosTerrain() {
        return this.renderInfosTerrain;
    }

    public List<SectionRenderDispatcher.RenderSection> getRenderInfosTileEntities() {
        return this.renderInfosTileEntities;
    }

    private void checkLoadVisibleChunks(Camera activeRenderInfo, Frustum icamera, boolean spectator) {
        if (this.loadVisibleChunksCounter == 0) {
            this.loadAllVisibleChunks(activeRenderInfo, icamera, spectator);
            this.minecraft.gui.getChat().deleteMessage(loadVisibleChunksMessageId);
        }

        if (this.loadVisibleChunksCounter >= 0) {
            this.loadVisibleChunksCounter--;
        }
    }

    private void loadAllVisibleChunks(Camera activeRenderInfo, Frustum icamera, boolean spectator) {
        int i = this.minecraft.options.ofChunkUpdates;
        boolean flag = this.minecraft.options.ofLazyChunkLoading;

        try {
            this.minecraft.options.ofChunkUpdates = 1000;
            this.minecraft.options.ofLazyChunkLoading = false;
            LevelRenderer levelrenderer = Config.getRenderGlobal();
            int j = levelrenderer.getCountLoadedChunks();
            long k = System.currentTimeMillis();
            Config.dbg("Loading visible chunks");
            long l = System.currentTimeMillis() + 5000L;
            int i1 = 0;
            boolean flag1 = false;

            do {
                flag1 = false;

                for (int j1 = 0; j1 < 100; j1++) {
                    levelrenderer.needsUpdate();
                    levelrenderer.setupRender(activeRenderInfo, icamera, false, spectator);
                    Config.sleep(1L);
                    this.compileSections(activeRenderInfo);
                    if (levelrenderer.getCountChunksToUpdate() > 0) {
                        flag1 = true;
                    }

                    if (!levelrenderer.hasRenderedAllSections()) {
                        flag1 = true;
                    }

                    i1 += levelrenderer.getCountChunksToUpdate();

                    while (!levelrenderer.hasRenderedAllSections()) {
                        int k1 = levelrenderer.getCountChunksToUpdate();
                        this.compileSections(activeRenderInfo);
                        if (k1 == levelrenderer.getCountChunksToUpdate()) {
                            break;
                        }
                    }

                    i1 -= levelrenderer.getCountChunksToUpdate();
                    if (!flag1) {
                        break;
                    }
                }

                if (levelrenderer.getCountLoadedChunks() != j) {
                    flag1 = true;
                    j = levelrenderer.getCountLoadedChunks();
                }

                if (System.currentTimeMillis() > l) {
                    Config.log("Chunks loaded: " + i1);
                    l = System.currentTimeMillis() + 5000L;
                }
            } while (flag1);

            Config.log("Chunks loaded: " + i1);
            Config.log("Finished loading visible chunks");
            SectionRenderDispatcher.renderChunksUpdated = 0;
        } finally {
            this.minecraft.options.ofChunkUpdates = i;
            this.minecraft.options.ofLazyChunkLoading = flag;
        }
    }

    public void applyFrustumEntities(Frustum camera, int maxChunkDistance) {
        this.renderInfosEntities.clear();
        int i = (int)camera.getCameraX() >> 4 << 4;
        int j = (int)camera.getCameraY() >> 4 << 4;
        int k = (int)camera.getCameraZ() >> 4 << 4;
        int l = maxChunkDistance * maxChunkDistance;
        EntitySectionStorage<?> entitysectionstorage = this.level.getSectionStorage();
        BlockPosM blockposm = new BlockPosM();
        LongSet longset = entitysectionstorage.getSectionKeys();
        LongIterator longiterator = longset.iterator();

        while (longiterator.hasNext()) {
            long i1 = longiterator.nextLong();
            blockposm.setXyz(
                SectionPos.sectionToBlockCoord(SectionPos.x(i1)), SectionPos.sectionToBlockCoord(SectionPos.y(i1)), SectionPos.sectionToBlockCoord(SectionPos.z(i1))
            );
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSectionAt(blockposm);
            if (sectionrenderdispatcher$rendersection != null && camera.isVisible(sectionrenderdispatcher$rendersection.getBoundingBox())) {
                if (maxChunkDistance > 0) {
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getOrigin();
                    int j1 = i - blockpos.getX();
                    int k1 = j - blockpos.getY();
                    int l1 = k - blockpos.getZ();
                    int i2 = j1 * j1 + k1 * k1 + l1 * l1;
                    if (i2 > l) {
                        continue;
                    }
                }

                this.renderInfosEntities.add(i1);
            }
        }
    }

    public void setShadowRenderInfos(boolean shadowInfos) {
        if (shadowInfos) {
            this.renderInfosTerrain = this.renderInfosTerrainShadow;
            this.renderInfosEntities = this.renderInfosEntitiesShadow;
            this.renderInfosTileEntities = this.renderInfosTileEntitiesShadow;
        } else {
            this.renderInfosTerrain = this.renderInfosTerrainNormal;
            this.renderInfosEntities = this.renderInfosEntitiesNormal;
            this.renderInfosTileEntities = this.renderInfosTileEntitiesNormal;
        }
    }

    public int getRenderedChunksShadow() {
        return !Config.isShadersShadows() ? -1 : this.renderInfosTerrainShadow.size();
    }

    public int getCountEntitiesRenderedShadow() {
        return !Config.isShadersShadows() ? -1 : ShadersRender.countEntitiesRenderedShadow;
    }

    public int getCountTileEntitiesRenderedShadow() {
        if (!Config.isShaders()) {
            return -1;
        } else {
            return !Shaders.hasShadowMap ? -1 : ShadersRender.countTileEntitiesRenderedShadow;
        }
    }

    public void captureFrustumShadow() {
        this.debugFixTerrainFrustumShadow = true;
    }

    public boolean isDebugFrustum() {
        return this.capturedFrustum != null;
    }

    public void onChunkRenderNeedsUpdate(SectionRenderDispatcher.RenderSection renderChunk) {
        if (!renderChunk.getCompiled().hasTerrainBlockEntities()) {
            ;
        }
    }

    public boolean needsFrustumUpdate() {
        return this.sectionOcclusionGraph.needsFrustumUpdate();
    }

    public boolean shouldRenderEntity(Entity entity, int minWorldY, int maxWorldY) {
        if (entity instanceof Display) {
            return true;
        } else {
            if (entity instanceof LivingEntity livingentity && livingentity.getScale() > 1.0F) {
                return true;
            }

            BlockPos blockpos = entity.blockPosition();
            return this.renderInfosEntities.contains(SectionPos.asLong(blockpos)) || blockpos.getY() <= minWorldY || blockpos.getY() >= maxWorldY;
        }
    }

    public Frustum getFrustum() {
        return this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
    }

    public int getTicks() {
        return this.ticks;
    }

    public void updateGlobalBlockEntities(Collection<BlockEntity> pBlockEntitiesToRemove, Collection<BlockEntity> pBlockEntitiesToAdd) {
        synchronized (this.globalBlockEntities) {
            this.globalBlockEntities.removeAll(pBlockEntitiesToRemove);
            this.globalBlockEntities.addAll(pBlockEntitiesToAdd);
        }
    }

    public static int getLightColor(BlockAndTintGetter pLevel, BlockPos pPos) {
        return getLightColor(pLevel, pLevel.getBlockState(pPos), pPos);
    }

    public static int getLightColor(BlockAndTintGetter pLevel, BlockState pState, BlockPos pPos) {
        return getPackedLightmapCoords(pLevel, pState, pPos, ambientOcclusion);
    }

    public static int getPackedLightmapCoords(BlockAndTintGetter lightReaderIn, BlockState blockStateIn, BlockPos blockPosIn, boolean ambientOcclusionIn) {
        if (EmissiveTextures.isRenderEmissive() && Config.isMinecraftThread()) {
            return LightTexture.MAX_BRIGHTNESS;
        } else if (blockStateIn.emissiveRendering(lightReaderIn, blockPosIn)) {
            return 15794417;
        } else {
            int i = lightReaderIn.getBrightness(LightLayer.SKY, blockPosIn);
            int j = lightReaderIn.getBrightness(LightLayer.BLOCK, blockPosIn);
            int k = blockStateIn.getLightValue(lightReaderIn, blockPosIn);
            if (j < k) {
                j = k;
            }

            int l = i << 20 | j << 4;
            if (Config.isDynamicLights() && lightReaderIn instanceof BlockGetter && (!ambientOcclusionIn || !blockStateIn.isSolidRender(lightReaderIn, blockPosIn))) {
                l = DynamicLights.getCombinedLight(blockPosIn, l);
            }

            return l;
        }
    }

    public boolean isSectionCompiled(BlockPos pPos) {
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSectionAt(pPos);
        return sectionrenderdispatcher$rendersection != null
            && sectionrenderdispatcher$rendersection.compiled.get() != SectionRenderDispatcher.CompiledSection.UNCOMPILED;
    }

    @Nullable
    public RenderTarget entityTarget() {
        return this.entityTarget;
    }

    @Nullable
    public RenderTarget getTranslucentTarget() {
        return this.translucentTarget;
    }

    @Nullable
    public RenderTarget getItemEntityTarget() {
        return this.itemEntityTarget;
    }

    @Nullable
    public RenderTarget getParticlesTarget() {
        return this.particlesTarget;
    }

    @Nullable
    public RenderTarget getWeatherTarget() {
        return this.weatherTarget;
    }

    @Nullable
    public RenderTarget getCloudsTarget() {
        return this.cloudsTarget;
    }

    private void shootParticles(int pDirection, BlockPos pPos, RandomSource pRandom, SimpleParticleType pParticleType) {
        Direction direction = Direction.from3DDataValue(pDirection);
        int i = direction.getStepX();
        int j = direction.getStepY();
        int k = direction.getStepZ();
        double d0 = (double)pPos.getX() + (double)i * 0.6 + 0.5;
        double d1 = (double)pPos.getY() + (double)j * 0.6 + 0.5;
        double d2 = (double)pPos.getZ() + (double)k * 0.6 + 0.5;

        for (int l = 0; l < 10; l++) {
            double d3 = pRandom.nextDouble() * 0.2 + 0.01;
            double d4 = d0 + (double)i * 0.01 + (pRandom.nextDouble() - 0.5) * (double)k * 0.5;
            double d5 = d1 + (double)j * 0.01 + (pRandom.nextDouble() - 0.5) * (double)j * 0.5;
            double d6 = d2 + (double)k * 0.01 + (pRandom.nextDouble() - 0.5) * (double)i * 0.5;
            double d7 = (double)i * d3 + pRandom.nextGaussian() * 0.01;
            double d8 = (double)j * d3 + pRandom.nextGaussian() * 0.01;
            double d9 = (double)k * d3 + pRandom.nextGaussian() * 0.01;
            this.addParticle(pParticleType, d4, d5, d6, d7, d8, d9);
        }
    }

    public static class TransparencyShaderException extends RuntimeException {
        public TransparencyShaderException(String pMessage, Throwable pCause) {
            super(pMessage, pCause);
        }
    }
}