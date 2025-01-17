package net.minecraft.server.packs;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult.Error;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.optifine.reflect.ReflectorForge;
import org.slf4j.Logger;

public class VanillaPackResources implements PackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackLocationInfo location;
    private final BuiltInMetadata metadata;
    private final Set<String> namespaces;
    private final List<Path> rootPaths;
    private final Map<PackType, List<Path>> pathsForType;

    VanillaPackResources(
        PackLocationInfo pLocation, BuiltInMetadata pMetadata, Set<String> pNamespaces, List<Path> pRootPaths, Map<PackType, List<Path>> pPathsForType
    ) {
        this.location = pLocation;
        this.metadata = pMetadata;
        this.namespaces = pNamespaces;
        this.rootPaths = pRootPaths;
        this.pathsForType = pPathsForType;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... pElements) {
        FileUtil.validatePath(pElements);
        List<String> list = List.of(pElements);

        for (Path path : this.rootPaths) {
            Path path1 = FileUtil.resolvePath(path, list);
            if (Files.exists(path1) && PathPackResources.validatePath(path1)) {
                return IoSupplier.create(path1);
            }
        }

        return null;
    }

    public void listRawPaths(PackType pPackType, ResourceLocation pPackLocation, Consumer<Path> pOutput) {
        FileUtil.decomposePath(pPackLocation.getPath()).ifSuccess(partsIn -> {
            String s = pPackLocation.getNamespace();

            for (Path path : this.pathsForType.get(pPackType)) {
                Path path1 = path.resolve(s);
                pOutput.accept(FileUtil.resolvePath(path1, (List<String>)partsIn));
            }
        }).ifError(errorIn -> LOGGER.error("Invalid path {}: {}", pPackLocation, errorIn.message()));
    }

    @Override
    public void listResources(PackType pPackType, String pNamespace, String pPath, PackResources.ResourceOutput pResourceOutput) {
        PackResources.ResourceOutput packresources$resourceoutput = (locIn, suppIn) -> {
            if (locIn.getPath().startsWith("models/block/template_glass_pane")) {
                IoSupplier<InputStream> iosupplier = this.getResourceOF(pPackType, locIn);
                if (iosupplier != null) {
                    suppIn = iosupplier;
                }
            }

            pResourceOutput.accept(locIn, suppIn);
        };
        FileUtil.decomposePath(pPath).ifSuccess(partsIn -> {
            List<Path> list = this.pathsForType.get(pPackType);
            int i = list.size();
            if (i == 1) {
                getResources(packresources$resourceoutput, pNamespace, list.get(0), (List<String>)partsIn);
            } else if (i > 1) {
                Map<ResourceLocation, IoSupplier<InputStream>> map = new HashMap<>();

                for (int j = 0; j < i - 1; j++) {
                    getResources(map::putIfAbsent, pNamespace, list.get(j), (List<String>)partsIn);
                }

                Path path = list.get(i - 1);
                if (map.isEmpty()) {
                    getResources(packresources$resourceoutput, pNamespace, path, (List<String>)partsIn);
                } else {
                    getResources(map::putIfAbsent, pNamespace, path, (List<String>)partsIn);
                    map.forEach(packresources$resourceoutput);
                }
            }
        }).ifError(errorIn -> LOGGER.error("Invalid path {}: {}", pPath, errorIn.message()));
    }

    private static void getResources(PackResources.ResourceOutput pResourceOutput, String pNamespace, Path pRoot, List<String> pPaths) {
        Path path = pRoot.resolve(pNamespace);
        PathPackResources.listPath(pNamespace, path, pPaths, pResourceOutput);
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType pPackType, ResourceLocation pLocation) {
        IoSupplier<InputStream> iosupplier = this.getResourcesImpl(pPackType, pLocation);
        return iosupplier != null ? iosupplier : this.getResourceOF(pPackType, pLocation);
    }

    @Nullable
    public IoSupplier<InputStream> getResourcesImpl(PackType type, ResourceLocation namespaceIn) {
        return FileUtil.decomposePath(namespaceIn.getPath()).mapOrElse(partsIn -> {
            String s = namespaceIn.getNamespace();

            for (Path path : this.pathsForType.get(type)) {
                Path path1 = FileUtil.resolvePath(path.resolve(s), (List<String>)partsIn);
                if (Files.exists(path1) && PathPackResources.validatePath(path1)) {
                    return IoSupplier.create(path1);
                }
            }

            return null;
        }, errorIn -> {
            LOGGER.error("Invalid path {}: {}", namespaceIn, errorIn.message());
            return null;
        });
    }

    @Override
    public Set<String> getNamespaces(PackType pType) {
        return this.namespaces;
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> pDeserializer) {
        IoSupplier<InputStream> iosupplier = this.getRootResource("pack.mcmeta");
        if (iosupplier != null) {
            try {
                Object object;
                try (InputStream inputstream = iosupplier.get()) {
                    T t = AbstractPackResources.getMetadataFromStream(pDeserializer, inputstream);
                    if (t == null) {
                        return this.metadata.get(pDeserializer);
                    }

                    object = t;
                }

                return (T)object;
            } catch (IOException ioexception) {
            }
        }

        return this.metadata.get(pDeserializer);
    }

    @Override
    public PackLocationInfo location() {
        return this.location;
    }

    @Override
    public void close() {
    }

    public ResourceProvider asProvider() {
        return locIn -> Optional.ofNullable(this.getResource(PackType.CLIENT_RESOURCES, locIn))
                .map(supplierIn -> new Resource(this, (IoSupplier<InputStream>)supplierIn));
    }

    public IoSupplier<InputStream> getResourceOF(PackType type, ResourceLocation locationIn) {
        String s = "/" + type.getDirectory() + "/" + locationIn.getNamespace() + "/" + locationIn.getPath();
        InputStream inputstream = ReflectorForge.getOptiFineResourceStream(s);
        if (inputstream != null) {
            return () -> inputstream;
        } else {
            URL url = VanillaPackResources.class.getResource(s);
            return url != null ? () -> url.openStream() : null;
        }
    }
}