package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ForgeRenderTypes;
import net.optifine.Config;
import net.optifine.EmissiveTextures;
import net.optifine.RandomEntities;
import net.optifine.reflect.Reflector;
import net.optifine.render.RenderStateManager;
import net.optifine.render.RenderUtils;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.util.CompoundKey;

public abstract class RenderType extends RenderStateShard {
    private static final int MEGABYTE = 1048576;
    public static final int BIG_BUFFER_SIZE = 4194304;
    public static final int SMALL_BUFFER_SIZE = 786432;
    public static final int TRANSIENT_BUFFER_SIZE = 1536;
    private static final RenderType SOLID = create(
        "solid",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        4194304,
        true,
        false,
        RenderType.CompositeState.builder().setLightmapState(LIGHTMAP).setShaderState(RENDERTYPE_SOLID_SHADER).setTextureState(BLOCK_SHEET_MIPPED).createCompositeState(true)
    );
    private static final RenderType CUTOUT_MIPPED = create(
        "cutout_mipped",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        4194304,
        true,
        false,
        RenderType.CompositeState.builder().setLightmapState(LIGHTMAP).setShaderState(RENDERTYPE_CUTOUT_MIPPED_SHADER).setTextureState(BLOCK_SHEET_MIPPED).createCompositeState(true)
    );
    private static final RenderType CUTOUT = create(
        "cutout",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        786432,
        true,
        false,
        RenderType.CompositeState.builder().setLightmapState(LIGHTMAP).setShaderState(RENDERTYPE_CUTOUT_SHADER).setTextureState(BLOCK_SHEET).createCompositeState(true)
    );
    private static final RenderType TRANSLUCENT = create(
        "translucent", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 786432, true, true, translucentState(RENDERTYPE_TRANSLUCENT_SHADER)
    );
    private static final RenderType TRANSLUCENT_MOVING_BLOCK = create(
        "translucent_moving_block", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 786432, false, true, translucentMovingBlockState()
    );
    private static final Function<ResourceLocation, RenderType> ARMOR_CUTOUT_NO_CULL = Util.memoize(p_292067_0_ -> createArmorCutoutNoCull("armor_cutout_no_cull", p_292067_0_, false));
    private static final Function<ResourceLocation, RenderType> ENTITY_SOLID = Util.memoize(
        p_285690_0_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285690_0_, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
            return create("entity_solid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_CUTOUT = Util.memoize(
        p_285702_0_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285702_0_, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
            return create("entity_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate);
        }
    );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_NO_CULL = Util.memoize(
        (p_285696_0_, p_285696_1_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285696_0_, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(p_285696_1_);
            return create("entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate);
        }
    );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_NO_CULL_Z_OFFSET = Util.memoize(
        (p_285686_0_, p_285686_1_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285686_0_, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(p_285686_1_);
            return create(
                "entity_cutout_no_cull_z_offset", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate
            );
        }
    );
    private static final Function<ResourceLocation, RenderType> ITEM_ENTITY_TRANSLUCENT_CULL = Util.memoize(
        p_285687_0_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285687_0_, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setOutputState(ITEM_ENTITY_TARGET)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                .createCompositeState(true);
            return create("item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_TRANSLUCENT_CULL = Util.memoize(
        p_285695_0_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285695_0_, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
            return create("entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
        }
    );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT = Util.memoize(
        (p_285688_0_, p_285688_1_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285688_0_, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(p_285688_1_);
            return create("entity_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
        }
    );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_TRANSLUCENT_EMISSIVE = Util.memoize(
        (p_285694_0_, p_285694_1_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285694_0_, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(COLOR_WRITE)
                .setOverlayState(OVERLAY)
                .createCompositeState(p_285694_1_);
            return create("entity_translucent_emissive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_SMOOTH_CUTOUT = Util.memoize(
        p_285698_0_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_SMOOTH_CUTOUT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285698_0_, false, false))
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .createCompositeState(true);
            return create("entity_smooth_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, rendertype$compositestate);
        }
    );
    private static final BiFunction<ResourceLocation, Boolean, RenderType> BEACON_BEAM = Util.memoize(
        (p_234329_0_, p_234329_1_) -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_234329_0_, false, false))
                .setTransparencyState(p_234329_1_ ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                .setWriteMaskState(p_234329_1_ ? COLOR_WRITE : COLOR_DEPTH_WRITE)
                .createCompositeState(false);
            return create("beacon_beam", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1536, false, true, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_DECAL = Util.memoize(
        p_285700_0_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_DECAL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285700_0_, false, false))
                .setDepthTestState(EQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(false);
            return create("entity_decal", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_NO_OUTLINE = Util.memoize(
        p_285691_0_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_NO_OUTLINE_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285691_0_, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false);
            return create("entity_no_outline", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> ENTITY_SHADOW = Util.memoize(
        p_285684_0_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_SHADOW_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285684_0_, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false);
            return create("entity_shadow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false, rendertype$compositestate);
        }
    );
    private static final Function<ResourceLocation, RenderType> DRAGON_EXPLOSION_ALPHA = Util.memoize(
        p_285683_0_ -> {
            RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_ALPHA_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(p_285683_0_, false, false))
                .setCullState(NO_CULL)
                .createCompositeState(true);
            return create("entity_alpha", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, rendertype$compositestate);
        }
    );
    private static final BiFunction<ResourceLocation, RenderStateShard.TransparencyStateShard, RenderType> EYES = Util.memoize(
        (p_304056_0_, p_304056_1_) -> {
            RenderStateShard.TextureStateShard renderstateshard$texturestateshard = new RenderStateShard.TextureStateShard(p_304056_0_, false, false);
            return create(
                "eyes",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(renderstateshard$texturestateshard)
                    .setTransparencyState(p_304056_1_)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
            );
        }
    );
    private static final RenderType LEASH = create(
        "leash",
        DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
        VertexFormat.Mode.TRIANGLE_STRIP,
        1536,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_LEASH_SHADER).setTextureState(NO_TEXTURE).setCullState(NO_CULL).setLightmapState(LIGHTMAP).createCompositeState(false)
    );
    private static final RenderType WATER_MASK = create(
        "water_mask",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_WATER_MASK_SHADER).setTextureState(NO_TEXTURE).setWriteMaskState(DEPTH_WRITE).createCompositeState(false)
    );
    private static final RenderType ARMOR_ENTITY_GLINT = create(
        "armor_entity_glint",
        DefaultVertexFormat.POSITION_TEX,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ENTITY, true, false))
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(EQUAL_DEPTH_TEST)
            .setTransparencyState(GLINT_TRANSPARENCY)
            .setTexturingState(ENTITY_GLINT_TEXTURING)
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false)
    );
    private static final RenderType GLINT_TRANSLUCENT = create(
        "glint_translucent",
        DefaultVertexFormat.POSITION_TEX,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ITEM, true, false))
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(EQUAL_DEPTH_TEST)
            .setTransparencyState(GLINT_TRANSPARENCY)
            .setTexturingState(GLINT_TEXTURING)
            .setOutputState(ITEM_ENTITY_TARGET)
            .createCompositeState(false)
    );
    private static final RenderType GLINT = create(
        "glint",
        DefaultVertexFormat.POSITION_TEX,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_GLINT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ITEM, true, false))
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(EQUAL_DEPTH_TEST)
            .setTransparencyState(GLINT_TRANSPARENCY)
            .setTexturingState(GLINT_TEXTURING)
            .createCompositeState(false)
    );
    private static final RenderType ENTITY_GLINT = create(
        "entity_glint",
        DefaultVertexFormat.POSITION_TEX,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ENTITY, true, false))
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(EQUAL_DEPTH_TEST)
            .setTransparencyState(GLINT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setTexturingState(ENTITY_GLINT_TEXTURING)
            .createCompositeState(false)
    );
    private static final RenderType ENTITY_GLINT_DIRECT = create(
        "entity_glint_direct",
        DefaultVertexFormat.POSITION_TEX,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(ItemRenderer.ENCHANTED_GLINT_ENTITY, true, false))
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .setDepthTestState(EQUAL_DEPTH_TEST)
            .setTransparencyState(GLINT_TRANSPARENCY)
            .setTexturingState(ENTITY_GLINT_TEXTURING)
            .createCompositeState(false)
    );
    private static final Function<ResourceLocation, RenderType> CRUMBLING = Util.memoize(
        p_285703_0_ -> {
            RenderStateShard.TextureStateShard renderstateshard$texturestateshard = new RenderStateShard.TextureStateShard(p_285703_0_, false, false);
            return create(
                "crumbling",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                true,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_CRUMBLING_SHADER)
                    .setTextureState(renderstateshard$texturestateshard)
                    .setTransparencyState(CRUMBLING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(false)
            );
        }
    );
    private static final Function<ResourceLocation, RenderType> TEXT = Util.memoize(
        p_304055_0_ -> create(
                "text",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                786432,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_304055_0_, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(false)
            )
    );
    private static final RenderType TEXT_BACKGROUND = create(
        "text_background",
        DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_TEXT_BACKGROUND_SHADER).setTextureState(NO_TEXTURE).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).createCompositeState(false)
    );
    private static final Function<ResourceLocation, RenderType> TEXT_INTENSITY = Util.memoize(
        p_304057_0_ -> create(
                "text_intensity",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                786432,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_INTENSITY_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_304057_0_, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> TEXT_POLYGON_OFFSET = Util.memoize(
        p_285685_0_ -> create(
                "text_polygon_offset",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_285685_0_, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> TEXT_INTENSITY_POLYGON_OFFSET = Util.memoize(
        p_285704_0_ -> create(
                "text_intensity_polygon_offset",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_INTENSITY_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_285704_0_, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setLayeringState(POLYGON_OFFSET_LAYERING)
                    .createCompositeState(false)
            )
    );
    private static final Function<ResourceLocation, RenderType> TEXT_SEE_THROUGH = Util.memoize(
        p_285689_0_ -> create(
                "text_see_through",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_SEE_THROUGH_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_285689_0_, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
            )
    );
    private static final RenderType TEXT_BACKGROUND_SEE_THROUGH = create(
        "text_background_see_through",
        DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH_SHADER)
            .setTextureState(NO_TEXTURE)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setDepthTestState(NO_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
    );
    private static final Function<ResourceLocation, RenderType> TEXT_INTENSITY_SEE_THROUGH = Util.memoize(
        p_285697_0_ -> create(
                "text_intensity_see_through",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                1536,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(p_285697_0_, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
            )
    );
    private static final RenderType LIGHTNING = create(
        "lightning",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_LIGHTNING_SHADER).setWriteMaskState(COLOR_DEPTH_WRITE).setTransparencyState(LIGHTNING_TRANSPARENCY).setOutputState(WEATHER_TARGET).createCompositeState(false)
    );
    private static final RenderType DRAGON_RAYS = create(
        "dragon_rays",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.TRIANGLES,
        1536,
        false,
        false,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_LIGHTNING_SHADER).setWriteMaskState(COLOR_WRITE).setTransparencyState(LIGHTNING_TRANSPARENCY).createCompositeState(false)
    );
    private static final RenderType DRAGON_RAYS_DEPTH = create(
        "dragon_rays_depth",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.TRIANGLES,
        1536,
        false,
        false,
        RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_SHADER).setWriteMaskState(DEPTH_WRITE).createCompositeState(false)
    );
    private static final RenderType TRIPWIRE = create("tripwire", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 1536, true, true, tripwireState());
    private static final RenderType END_PORTAL = create(
        "end_portal",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_END_PORTAL_SHADER)
            .setTextureState(
                RenderStateShard.MultiTextureStateShard.builder()
                    .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                    .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false)
                    .build()
            )
            .createCompositeState(false)
    );
    private static final RenderType END_GATEWAY = create(
        "end_gateway",
        DefaultVertexFormat.POSITION,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_END_GATEWAY_SHADER)
            .setTextureState(
                RenderStateShard.MultiTextureStateShard.builder()
                    .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                    .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false)
                    .build()
            )
            .createCompositeState(false)
    );
    private static final RenderType CLOUDS = createClouds(false);
    private static final RenderType CLOUDS_DEPTH_ONLY = createClouds(true);
    public static final RenderType.CompositeRenderType LINES = create(
        "lines",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false)
    );
    public static final RenderType.CompositeRenderType LINE_STRIP = create(
        "line_strip",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINE_STRIP,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false)
    );
    private static final Function<Double, RenderType.CompositeRenderType> DEBUG_LINE_STRIP = Util.memoize(
        p_285693_0_ -> create(
                "debug_line_strip",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.DEBUG_LINE_STRIP,
                1536,
                RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(p_285693_0_)))
                    .setTransparencyState(NO_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)
            )
    );
    private static final RenderType.CompositeRenderType DEBUG_FILLED_BOX = create(
        "debug_filled_box",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.TRIANGLE_STRIP,
        1536,
        false,
        true,
        RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setLayeringState(VIEW_OFFSET_Z_LAYERING).setTransparencyState(TRANSLUCENT_TRANSPARENCY).createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType DEBUG_QUADS = create(
        "debug_quads",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType DEBUG_STRUCTURE_QUADS = create(
        "debug_structure_quads",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(POSITION_COLOR_SHADER)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType DEBUG_SECTION_QUADS = create(
        "debug_section_quads",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        true,
        RenderType.CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setLayeringState(VIEW_OFFSET_Z_LAYERING).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(CULL).createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType GUI = create(
        "gui",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        786432,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_GUI_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType GUI_OVERLAY = create(
        "gui_overlay",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_GUI_OVERLAY_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType GUI_TEXT_HIGHLIGHT = create(
        "gui_text_highlight",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_GUI_TEXT_HIGHLIGHT_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setColorLogicState(OR_REVERSE_COLOR_LOGIC).createCompositeState(false)
    );
    private static final RenderType.CompositeRenderType GUI_GHOST_RECIPE_OVERLAY = create(
        "gui_ghost_recipe_overlay",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        1536,
        RenderType.CompositeState.builder().setShaderState(RENDERTYPE_GUI_GHOST_RECIPE_OVERLAY_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(GREATER_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false)
    );
    private static final ImmutableList<RenderType> CHUNK_BUFFER_LAYERS = ImmutableList.of(solid(), cutoutMipped(), cutout(), translucent(), tripwire());
    private final VertexFormat format;
    private final VertexFormat.Mode mode;
    private final int bufferSize;
    private final boolean affectsCrumbling;
    private final boolean sortOnUpload;
    private int id = -1;
    public static final RenderType[] CHUNK_RENDER_TYPES;
    private static Map<CompoundKey, RenderType> RENDER_TYPES;
    private int chunkLayerId = -1;

    public int ordinal() {
        return this.id;
    }

    public boolean isNeedsSorting() {
        return this.sortOnUpload;
    }

    private static RenderType[] getChunkRenderTypesArray() {
        RenderType[] arendertype = chunkBufferLayers().toArray(new RenderType[0]);
        int i = 0;

        while (i < arendertype.length) {
            RenderType rendertype = arendertype[i];
            rendertype.id = i++;
        }

        return arendertype;
    }

    public final int getChunkLayerId() {
        return this.chunkLayerId;
    }

    public static RenderType solid() {
        return SOLID;
    }

    public static RenderType cutoutMipped() {
        return CUTOUT_MIPPED;
    }

    public static RenderType cutout() {
        return CUTOUT;
    }

    private static RenderType.CompositeState translucentState(RenderStateShard.ShaderStateShard pState) {
        return RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(pState)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(TRANSLUCENT_TARGET)
            .createCompositeState(true);
    }

    public static RenderType translucent() {
        return TRANSLUCENT;
    }

    private static RenderType.CompositeState translucentMovingBlockState() {
        return RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_SHADER)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .createCompositeState(true);
    }

    public static RenderType translucentMovingBlock() {
        return TRANSLUCENT_MOVING_BLOCK;
    }

    private static RenderType.CompositeRenderType createArmorCutoutNoCull(String pName, ResourceLocation pId, boolean pEqualDepthTest) {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ARMOR_CUTOUT_NO_CULL_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(pId, false, false))
            .setTransparencyState(NO_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setDepthTestState(pEqualDepthTest ? EQUAL_DEPTH_TEST : LEQUAL_DEPTH_TEST)
            .createCompositeState(true);
        return create(pName, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, rendertype$compositestate);
    }

    public static RenderType armorCutoutNoCull(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return ARMOR_CUTOUT_NO_CULL.apply(pLocation);
    }

    public static RenderType createArmorDecalCutoutNoCull(ResourceLocation pId) {
        return createArmorCutoutNoCull("armor_decal_cutout_no_cull", pId, true);
    }

    public static RenderType entitySolid(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return EmissiveTextures.isRenderEmissive() ? ENTITY_CUTOUT.apply(pLocation) : ENTITY_SOLID.apply(pLocation);
    }

    public static RenderType entityCutout(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return ENTITY_CUTOUT.apply(pLocation);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation pLocation, boolean pOutline) {
        pLocation = getCustomTexture(pLocation);
        return ENTITY_CUTOUT_NO_CULL.apply(pLocation, pOutline);
    }

    public static RenderType entityCutoutNoCull(ResourceLocation pLocation) {
        return entityCutoutNoCull(pLocation, true);
    }

    public static RenderType entityCutoutNoCullZOffset(ResourceLocation pLocation, boolean pOutline) {
        pLocation = getCustomTexture(pLocation);
        return ENTITY_CUTOUT_NO_CULL_Z_OFFSET.apply(pLocation, pOutline);
    }

    public static RenderType entityCutoutNoCullZOffset(ResourceLocation pLocation) {
        return entityCutoutNoCullZOffset(pLocation, true);
    }

    public static RenderType itemEntityTranslucentCull(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return ITEM_ENTITY_TRANSLUCENT_CULL.apply(pLocation);
    }

    public static RenderType entityTranslucentCull(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return ENTITY_TRANSLUCENT_CULL.apply(pLocation);
    }

    public static RenderType entityTranslucent(ResourceLocation pLocation, boolean pOutline) {
        pLocation = getCustomTexture(pLocation);
        return ENTITY_TRANSLUCENT.apply(pLocation, pOutline);
    }

    public static RenderType entityTranslucent(ResourceLocation pLocation) {
        return entityTranslucent(pLocation, true);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation pLocation, boolean pOutline) {
        pLocation = getCustomTexture(pLocation);
        return ENTITY_TRANSLUCENT_EMISSIVE.apply(pLocation, pOutline);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return entityTranslucentEmissive(pLocation, true);
    }

    public static RenderType entitySmoothCutout(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return ENTITY_SMOOTH_CUTOUT.apply(pLocation);
    }

    public static RenderType beaconBeam(ResourceLocation pLocation, boolean pColorFlag) {
        pLocation = getCustomTexture(pLocation);
        return BEACON_BEAM.apply(pLocation, pColorFlag);
    }

    public static RenderType entityDecal(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return ENTITY_DECAL.apply(pLocation);
    }

    public static RenderType entityNoOutline(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return ENTITY_NO_OUTLINE.apply(pLocation);
    }

    public static RenderType entityShadow(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return ENTITY_SHADOW.apply(pLocation);
    }

    public static RenderType dragonExplosionAlpha(ResourceLocation pId) {
        pId = getCustomTexture(pId);
        return DRAGON_EXPLOSION_ALPHA.apply(pId);
    }

    public static RenderType eyes(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return EYES.apply(pLocation, ADDITIVE_TRANSPARENCY);
    }

    public static RenderType breezeEyes(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return ENTITY_TRANSLUCENT_EMISSIVE.apply(pLocation, false);
    }

    public static RenderType breezeWind(ResourceLocation pLocation, float pU, float pV) {
        pLocation = getCustomTexture(pLocation);
        return create(
            "breeze_wind",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_BREEZE_WIND_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(pLocation, false, false))
                .setTexturingState(new RenderStateShard.OffsetTexturingStateShard(pU, pV))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .createCompositeState(false)
        );
    }

    public static RenderType energySwirl(ResourceLocation pLocation, float pU, float pV) {
        pLocation = getCustomTexture(pLocation);
        return create(
            "energy_swirl",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(pLocation, false, false))
                .setTexturingState(new RenderStateShard.OffsetTexturingStateShard(pU, pV))
                .setTransparencyState(ADDITIVE_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(false)
        );
    }

    public static RenderType leash() {
        return LEASH;
    }

    public static RenderType waterMask() {
        return WATER_MASK;
    }

    public static RenderType outline(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return RenderType.CompositeRenderType.OUTLINE.apply(pLocation, NO_CULL);
    }

    public static RenderType armorEntityGlint() {
        return ARMOR_ENTITY_GLINT;
    }

    public static RenderType glintTranslucent() {
        return GLINT_TRANSLUCENT;
    }

    public static RenderType glint() {
        return GLINT;
    }

    public static RenderType entityGlint() {
        return ENTITY_GLINT;
    }

    public static RenderType entityGlintDirect() {
        return ENTITY_GLINT_DIRECT;
    }

    public static RenderType crumbling(ResourceLocation pLocation) {
        pLocation = getCustomTexture(pLocation);
        return CRUMBLING.apply(pLocation);
    }

    public static RenderType text(ResourceLocation pLocation) {
        return Reflector.ForgeHooksClient.exists() ? ForgeRenderTypes.getText(pLocation) : TEXT.apply(pLocation);
    }

    public static RenderType textBackground() {
        return TEXT_BACKGROUND;
    }

    public static RenderType textIntensity(ResourceLocation pId) {
        return Reflector.ForgeHooksClient.exists() ? ForgeRenderTypes.getTextIntensity(pId) : TEXT_INTENSITY.apply(pId);
    }

    public static RenderType textPolygonOffset(ResourceLocation pId) {
        return Reflector.ForgeHooksClient.exists() ? ForgeRenderTypes.getTextPolygonOffset(pId) : TEXT_POLYGON_OFFSET.apply(pId);
    }

    public static RenderType textIntensityPolygonOffset(ResourceLocation pId) {
        return Reflector.ForgeHooksClient.exists() ? ForgeRenderTypes.getTextIntensityPolygonOffset(pId) : TEXT_INTENSITY_POLYGON_OFFSET.apply(pId);
    }

    public static RenderType textSeeThrough(ResourceLocation pLocation) {
        return Reflector.ForgeHooksClient.exists() ? ForgeRenderTypes.getTextSeeThrough(pLocation) : TEXT_SEE_THROUGH.apply(pLocation);
    }

    public static RenderType textBackgroundSeeThrough() {
        return TEXT_BACKGROUND_SEE_THROUGH;
    }

    public static RenderType textIntensitySeeThrough(ResourceLocation pId) {
        return Reflector.ForgeHooksClient.exists() ? ForgeRenderTypes.getTextIntensitySeeThrough(pId) : TEXT_INTENSITY_SEE_THROUGH.apply(pId);
    }

    public static RenderType lightning() {
        return LIGHTNING;
    }

    public static RenderType dragonRays() {
        return DRAGON_RAYS;
    }

    public static RenderType dragonRaysDepth() {
        return DRAGON_RAYS_DEPTH;
    }

    private static RenderType.CompositeState tripwireState() {
        return RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(RENDERTYPE_TRIPWIRE_SHADER)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(WEATHER_TARGET)
            .createCompositeState(true);
    }

    public static RenderType tripwire() {
        return TRIPWIRE;
    }

    public static RenderType endPortal() {
        return END_PORTAL;
    }

    public static RenderType endGateway() {
        return END_GATEWAY;
    }

    private static RenderType.CompositeRenderType createClouds(boolean pColor) {
        return create(
            "clouds",
            DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
            VertexFormat.Mode.QUADS,
            786432,
            false,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_CLOUDS_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(LevelRenderer.CLOUDS_LOCATION, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setWriteMaskState(pColor ? DEPTH_WRITE : COLOR_DEPTH_WRITE)
                .setOutputState(CLOUDS_TARGET)
                .createCompositeState(true)
        );
    }

    public static RenderType clouds() {
        return CLOUDS;
    }

    public static RenderType cloudsDepthOnly() {
        return CLOUDS_DEPTH_ONLY;
    }

    public static RenderType lines() {
        return LINES;
    }

    public static RenderType lineStrip() {
        return LINE_STRIP;
    }

    public static RenderType debugLineStrip(double pWidth) {
        return DEBUG_LINE_STRIP.apply(pWidth);
    }

    public static RenderType debugFilledBox() {
        return DEBUG_FILLED_BOX;
    }

    public static RenderType debugQuads() {
        return DEBUG_QUADS;
    }

    public static RenderType debugStructureQuads() {
        return DEBUG_STRUCTURE_QUADS;
    }

    public static RenderType debugSectionQuads() {
        return DEBUG_SECTION_QUADS;
    }

    public static RenderType gui() {
        return GUI;
    }

    public static RenderType guiOverlay() {
        return GUI_OVERLAY;
    }

    public static RenderType guiTextHighlight() {
        return GUI_TEXT_HIGHLIGHT;
    }

    public static RenderType guiGhostRecipeOverlay() {
        return GUI_GHOST_RECIPE_OVERLAY;
    }

    public RenderType(
        String pName,
        VertexFormat pFormat,
        VertexFormat.Mode pMode,
        int pBufferSize,
        boolean pAffectsCrumbling,
        boolean pSortOnUpload,
        Runnable pSetupState,
        Runnable pClearState
    ) {
        super(pName, pSetupState, pClearState);
        this.format = pFormat;
        this.mode = pMode;
        this.bufferSize = pBufferSize;
        this.affectsCrumbling = pAffectsCrumbling;
        this.sortOnUpload = pSortOnUpload;
    }

    static RenderType.CompositeRenderType create(
        String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, RenderType.CompositeState pState
    ) {
        return create(pName, pFormat, pMode, pBufferSize, false, false, pState);
    }

    static RenderType.CompositeRenderType create(
        String pName,
        VertexFormat pFormat,
        VertexFormat.Mode pMode,
        int pBufferSize,
        boolean pAffectsCrumbling,
        boolean pSortOnUpload,
        RenderType.CompositeState pState
    ) {
        return new RenderType.CompositeRenderType(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pState);
    }

    public void draw(MeshData pMeshData) {
        this.setupRenderState();
        if (Config.isShaders()) {
            RenderUtils.setFlushRenderBuffers(false);
            Shaders.pushProgram();
            ShadersRender.preRender(this);
        }

        BufferUploader.drawWithShader(pMeshData);
        if (Config.isShaders()) {
            ShadersRender.postRender(this);
            Shaders.popProgram();
            RenderUtils.setFlushRenderBuffers(true);
        }

        this.clearRenderState();
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static List<RenderType> chunkBufferLayers() {
        return CHUNK_BUFFER_LAYERS;
    }

    public int bufferSize() {
        return this.bufferSize;
    }

    public VertexFormat format() {
        return this.format;
    }

    public VertexFormat.Mode mode() {
        return this.mode;
    }

    public Optional<RenderType> outline() {
        return Optional.empty();
    }

    public boolean isOutline() {
        return false;
    }

    public boolean affectsCrumbling() {
        return this.affectsCrumbling;
    }

    public boolean canConsolidateConsecutiveGeometry() {
        return !this.mode.connectedPrimitives;
    }

    public boolean sortOnUpload() {
        return this.sortOnUpload;
    }

    public static ResourceLocation getCustomTexture(ResourceLocation locationIn) {
        if (Config.isRandomEntities()) {
            locationIn = RandomEntities.getTextureLocation(locationIn);
        }

        if (EmissiveTextures.isActive()) {
            locationIn = EmissiveTextures.getEmissiveTexture(locationIn);
        }

        return locationIn;
    }

    public boolean isEntitySolid() {
        return this.getName().equals("entity_solid");
    }

    public static int getCountRenderStates() {
        return LINES.state.states.size();
    }

    public ResourceLocation getTextureLocation() {
        return null;
    }

    public boolean isGlint() {
        return this.getTextureLocation() == ItemRenderer.ENCHANTED_GLINT_ENTITY | this.getTextureLocation() == ItemRenderer.ENCHANTED_GLINT_ITEM;
    }

    public boolean isAtlasTextureBlocks() {
        ResourceLocation resourcelocation = this.getTextureLocation();
        return resourcelocation == TextureAtlas.LOCATION_BLOCKS;
    }

    static {
        int i = 0;

        for (RenderType rendertype : chunkBufferLayers()) {
            rendertype.chunkLayerId = i++;
        }

        CHUNK_RENDER_TYPES = getChunkRenderTypesArray();
    }

    static final class CompositeRenderType extends RenderType {
        static final BiFunction<ResourceLocation, RenderStateShard.CullStateShard, RenderType> OUTLINE = Util.memoize(
            (p_337854_0_, p_337854_1_) -> RenderType.create(
                    "outline",
                    DefaultVertexFormat.POSITION_TEX_COLOR,
                    VertexFormat.Mode.QUADS,
                    1536,
                    RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_OUTLINE_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(p_337854_0_, false, false))
                        .setCullState(p_337854_1_)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setOutputState(OUTLINE_TARGET)
                        .createCompositeState(RenderType.OutlineProperty.IS_OUTLINE)
                )
        );
        private final RenderType.CompositeState state;
        private final Optional<RenderType> outline;
        private final boolean isOutline;
        private Map<ResourceLocation, RenderType.CompositeRenderType> mapTextured = new HashMap<>();

        CompositeRenderType(
            String pName,
            VertexFormat pFormat,
            VertexFormat.Mode pMode,
            int pBufferSize,
            boolean pAffectsCrumbling,
            boolean pSortOnUpload,
            RenderType.CompositeState pState
        ) {
            super(
                pName,
                pFormat,
                pMode,
                pBufferSize,
                pAffectsCrumbling,
                pSortOnUpload,
                () -> RenderStateManager.setupRenderStates(pState.states),
                () -> RenderStateManager.clearRenderStates(pState.states)
            );
            this.state = pState;
            this.outline = pState.outlineProperty == RenderType.OutlineProperty.AFFECTS_OUTLINE
                ? pState.textureState.cutoutTexture().map(locationIn -> OUTLINE.apply(locationIn, pState.cullState))
                : Optional.empty();
            this.isOutline = pState.outlineProperty == RenderType.OutlineProperty.IS_OUTLINE;
        }

        @Override
        public Optional<RenderType> outline() {
            return this.outline;
        }

        @Override
        public boolean isOutline() {
            return this.isOutline;
        }

        protected final RenderType.CompositeState state() {
            return this.state;
        }

        @Override
        public String toString() {
            return "RenderType[" + this.name + ":" + this.state + "]";
        }

        public RenderType.CompositeRenderType getTextured(ResourceLocation textureLocation) {
            if (textureLocation == null) {
                return this;
            } else {
                Optional<ResourceLocation> optional = this.state.textureState.cutoutTexture();
                if (!optional.isPresent()) {
                    return this;
                } else {
                    ResourceLocation resourcelocation = optional.get();
                    if (resourcelocation == null) {
                        return this;
                    } else if (textureLocation.equals(resourcelocation)) {
                        return this;
                    } else {
                        RenderType.CompositeRenderType rendertype$compositerendertype = this.mapTextured.get(textureLocation);
                        if (rendertype$compositerendertype == null) {
                            RenderType.CompositeState.CompositeStateBuilder rendertype$compositestate$compositestatebuilder = this.state.getCopyBuilder();
                            rendertype$compositestate$compositestatebuilder.setTextureState(
                                new RenderStateShard.TextureStateShard(textureLocation, this.state.textureState.isBlur(), this.state.textureState.isMipmap())
                            );
                            RenderType.CompositeState rendertype$compositestate = rendertype$compositestate$compositestatebuilder.createCompositeState(this.isOutline);
                            rendertype$compositerendertype = create(
                                this.name,
                                this.format(),
                                this.mode(),
                                this.bufferSize(),
                                this.affectsCrumbling(),
                                this.isNeedsSorting(),
                                rendertype$compositestate
                            );
                            this.mapTextured.put(textureLocation, rendertype$compositerendertype);
                        }

                        return rendertype$compositerendertype;
                    }
                }
            }
        }

        @Override
        public ResourceLocation getTextureLocation() {
            Optional<ResourceLocation> optional = this.state.textureState.cutoutTexture();
            return !optional.isPresent() ? null : optional.get();
        }
    }

    protected static final class CompositeState {
        final RenderStateShard.EmptyTextureStateShard textureState;
        private final RenderStateShard.ShaderStateShard shaderState;
        private final RenderStateShard.TransparencyStateShard transparencyState;
        private final RenderStateShard.DepthTestStateShard depthTestState;
        final RenderStateShard.CullStateShard cullState;
        private final RenderStateShard.LightmapStateShard lightmapState;
        private final RenderStateShard.OverlayStateShard overlayState;
        private final RenderStateShard.LayeringStateShard layeringState;
        private final RenderStateShard.OutputStateShard outputState;
        private final RenderStateShard.TexturingStateShard texturingState;
        private final RenderStateShard.WriteMaskStateShard writeMaskState;
        private final RenderStateShard.LineStateShard lineState;
        private final RenderStateShard.ColorLogicStateShard colorLogicState;
        final RenderType.OutlineProperty outlineProperty;
        final ImmutableList<RenderStateShard> states;

        CompositeState(
            RenderStateShard.EmptyTextureStateShard pTextureState,
            RenderStateShard.ShaderStateShard pShaderState,
            RenderStateShard.TransparencyStateShard pTransparencyState,
            RenderStateShard.DepthTestStateShard pDepthState,
            RenderStateShard.CullStateShard pCullState,
            RenderStateShard.LightmapStateShard pLightmapState,
            RenderStateShard.OverlayStateShard pOverlayState,
            RenderStateShard.LayeringStateShard pLayeringState,
            RenderStateShard.OutputStateShard pOutputState,
            RenderStateShard.TexturingStateShard pTexturingState,
            RenderStateShard.WriteMaskStateShard pWriteMaskState,
            RenderStateShard.LineStateShard pLineState,
            RenderStateShard.ColorLogicStateShard pColorLogicState,
            RenderType.OutlineProperty pOutlineProperty
        ) {
            this.textureState = pTextureState;
            this.shaderState = pShaderState;
            this.transparencyState = pTransparencyState;
            this.depthTestState = pDepthState;
            this.cullState = pCullState;
            this.lightmapState = pLightmapState;
            this.overlayState = pOverlayState;
            this.layeringState = pLayeringState;
            this.outputState = pOutputState;
            this.texturingState = pTexturingState;
            this.writeMaskState = pWriteMaskState;
            this.lineState = pLineState;
            this.colorLogicState = pColorLogicState;
            this.outlineProperty = pOutlineProperty;
            this.states = ImmutableList.of(
                this.textureState,
                this.shaderState,
                this.transparencyState,
                this.depthTestState,
                this.cullState,
                this.lightmapState,
                this.overlayState,
                this.layeringState,
                this.outputState,
                this.texturingState,
                this.writeMaskState,
                this.colorLogicState,
                this.lineState
            );
        }

        @Override
        public String toString() {
            return "CompositeState[" + this.states + ", outlineProperty=" + this.outlineProperty + "]";
        }

        public static RenderType.CompositeState.CompositeStateBuilder builder() {
            return new RenderType.CompositeState.CompositeStateBuilder();
        }

        public RenderType.CompositeState.CompositeStateBuilder getCopyBuilder() {
            RenderType.CompositeState.CompositeStateBuilder rendertype$compositestate$compositestatebuilder = new RenderType.CompositeState.CompositeStateBuilder();
            rendertype$compositestate$compositestatebuilder.setTextureState(this.textureState);
            rendertype$compositestate$compositestatebuilder.setShaderState(this.shaderState);
            rendertype$compositestate$compositestatebuilder.setTransparencyState(this.transparencyState);
            rendertype$compositestate$compositestatebuilder.setDepthTestState(this.depthTestState);
            rendertype$compositestate$compositestatebuilder.setCullState(this.cullState);
            rendertype$compositestate$compositestatebuilder.setLightmapState(this.lightmapState);
            rendertype$compositestate$compositestatebuilder.setOverlayState(this.overlayState);
            rendertype$compositestate$compositestatebuilder.setLayeringState(this.layeringState);
            rendertype$compositestate$compositestatebuilder.setOutputState(this.outputState);
            rendertype$compositestate$compositestatebuilder.setTexturingState(this.texturingState);
            rendertype$compositestate$compositestatebuilder.setWriteMaskState(this.writeMaskState);
            rendertype$compositestate$compositestatebuilder.setLineState(this.lineState);
            return rendertype$compositestate$compositestatebuilder;
        }

        public static class CompositeStateBuilder {
            private RenderStateShard.EmptyTextureStateShard textureState = RenderStateShard.NO_TEXTURE;
            private RenderStateShard.ShaderStateShard shaderState = RenderStateShard.NO_SHADER;
            private RenderStateShard.TransparencyStateShard transparencyState = RenderStateShard.NO_TRANSPARENCY;
            private RenderStateShard.DepthTestStateShard depthTestState = RenderStateShard.LEQUAL_DEPTH_TEST;
            private RenderStateShard.CullStateShard cullState = RenderStateShard.CULL;
            private RenderStateShard.LightmapStateShard lightmapState = RenderStateShard.NO_LIGHTMAP;
            private RenderStateShard.OverlayStateShard overlayState = RenderStateShard.NO_OVERLAY;
            private RenderStateShard.LayeringStateShard layeringState = RenderStateShard.NO_LAYERING;
            private RenderStateShard.OutputStateShard outputState = RenderStateShard.MAIN_TARGET;
            private RenderStateShard.TexturingStateShard texturingState = RenderStateShard.DEFAULT_TEXTURING;
            private RenderStateShard.WriteMaskStateShard writeMaskState = RenderStateShard.COLOR_DEPTH_WRITE;
            private RenderStateShard.LineStateShard lineState = RenderStateShard.DEFAULT_LINE;
            private RenderStateShard.ColorLogicStateShard colorLogicState = RenderStateShard.NO_COLOR_LOGIC;

            CompositeStateBuilder() {
            }

            public RenderType.CompositeState.CompositeStateBuilder setTextureState(RenderStateShard.EmptyTextureStateShard pTextureState) {
                this.textureState = pTextureState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setShaderState(RenderStateShard.ShaderStateShard pShaderState) {
                this.shaderState = pShaderState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setTransparencyState(RenderStateShard.TransparencyStateShard pTransparencyState) {
                this.transparencyState = pTransparencyState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setDepthTestState(RenderStateShard.DepthTestStateShard pDepthTestState) {
                this.depthTestState = pDepthTestState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setCullState(RenderStateShard.CullStateShard pCullState) {
                this.cullState = pCullState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setLightmapState(RenderStateShard.LightmapStateShard pLightmapState) {
                this.lightmapState = pLightmapState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setOverlayState(RenderStateShard.OverlayStateShard pOverlayState) {
                this.overlayState = pOverlayState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setLayeringState(RenderStateShard.LayeringStateShard pLayerState) {
                this.layeringState = pLayerState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setOutputState(RenderStateShard.OutputStateShard pOutputState) {
                this.outputState = pOutputState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setTexturingState(RenderStateShard.TexturingStateShard pTexturingState) {
                this.texturingState = pTexturingState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setWriteMaskState(RenderStateShard.WriteMaskStateShard pWriteMaskState) {
                this.writeMaskState = pWriteMaskState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setLineState(RenderStateShard.LineStateShard pLineState) {
                this.lineState = pLineState;
                return this;
            }

            public RenderType.CompositeState.CompositeStateBuilder setColorLogicState(RenderStateShard.ColorLogicStateShard pColorLogicState) {
                this.colorLogicState = pColorLogicState;
                return this;
            }

            public RenderType.CompositeState createCompositeState(boolean pOutline) {
                return this.createCompositeState(pOutline ? RenderType.OutlineProperty.AFFECTS_OUTLINE : RenderType.OutlineProperty.NONE);
            }

            public RenderType.CompositeState createCompositeState(RenderType.OutlineProperty pOutlineState) {
                return new RenderType.CompositeState(
                    this.textureState,
                    this.shaderState,
                    this.transparencyState,
                    this.depthTestState,
                    this.cullState,
                    this.lightmapState,
                    this.overlayState,
                    this.layeringState,
                    this.outputState,
                    this.texturingState,
                    this.writeMaskState,
                    this.lineState,
                    this.colorLogicState,
                    pOutlineState
                );
            }
        }
    }

    static enum OutlineProperty {
        NONE("none"),
        IS_OUTLINE("is_outline"),
        AFFECTS_OUTLINE("affects_outline");

        private final String name;

        private OutlineProperty(final String pName) {
            this.name = pName;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}