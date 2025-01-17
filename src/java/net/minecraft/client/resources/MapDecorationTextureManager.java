package net.minecraft.client.resources;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

public class MapDecorationTextureManager extends TextureAtlasHolder {
    public MapDecorationTextureManager(TextureManager pTextureManager) {
        super(pTextureManager, ResourceLocation.withDefaultNamespace("textures/atlas/map_decorations.png"), ResourceLocation.withDefaultNamespace("map_decorations"));
    }

    public TextureAtlasSprite get(MapDecoration pMapDecoration) {
        return this.getSprite(pMapDecoration.getSpriteLocation());
    }
}