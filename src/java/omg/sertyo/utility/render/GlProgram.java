package omg.sertyo.utility.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class GlProgram {
    private static final List<Tuple<Function<ResourceManager, ShaderInstance>, Consumer<ShaderInstance>>> REGISTERED_PROGRAMS = new ArrayList<>();

    public ShaderInstance backingProgram;

    public GlProgram(ResourceLocation id, VertexFormat vertexFormat) {
        REGISTERED_PROGRAMS.add(new Tuple<>(resourceFactory -> {
            try {
                return new THShaderProgram(resourceFactory, id.toString(), vertexFormat);
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialized shader program", e);
            }
        }, program -> {
            backingProgram = program;
            setup();
        }));
    }

    public void use() {
        RenderSystem.setShader(() -> backingProgram);
    }

    protected void setup() {

    }

    protected @Nullable Uniform findUniform(String name) {

        if (backingProgram != null) {
            Uniform uniform = backingProgram.getUniform(name);
            if (uniform == null) {
                System.err.println("Uniform " + name + " not found.");
            }
            return uniform;
        }
        return null;
    }

    @ApiStatus.Internal
    public static void forEachProgram(Consumer<Tuple<Function<ResourceManager, ShaderInstance>, Consumer<ShaderInstance>>> loader) {
        REGISTERED_PROGRAMS.forEach(loader);
    }

    public static class THShaderProgram extends ShaderInstance {
        private THShaderProgram(ResourceManager factory, String name, VertexFormat format) throws IOException {
            super(factory, name, format);
        }
    }
}