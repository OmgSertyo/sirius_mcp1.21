package net.minecraft.client.resources.language;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.resources.metadata.language.LanguageMetadataSection;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

public class LanguageManager implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LanguageInfo DEFAULT_LANGUAGE = new LanguageInfo("US", "English", false);
    private Map<String, LanguageInfo> languages = ImmutableMap.of("en_us", DEFAULT_LANGUAGE);
    private String currentCode;
    private final Consumer<ClientLanguage> reloadCallback;

    public LanguageManager(String pCurrentCode, Consumer<ClientLanguage> pReloadFallback) {
        this.currentCode = pCurrentCode;
        this.reloadCallback = pReloadFallback;
    }

    private static Map<String, LanguageInfo> extractLanguages(Stream<PackResources> pPackResources) {
        Map<String, LanguageInfo> map = Maps.newHashMap();
        pPackResources.forEach(p_264712_ -> {
            try {
                LanguageMetadataSection languagemetadatasection = p_264712_.getMetadataSection(LanguageMetadataSection.TYPE);
                if (languagemetadatasection != null) {
                    languagemetadatasection.languages().forEach(map::putIfAbsent);
                }
            } catch (IOException | RuntimeException runtimeexception) {
                LOGGER.warn("Unable to parse language metadata section of resourcepack: {}", p_264712_.packId(), runtimeexception);
            }
        });
        return ImmutableMap.copyOf(map);
    }

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        this.languages = extractLanguages(pResourceManager.listPacks());
        List<String> list = new ArrayList<>(2);
        boolean flag = DEFAULT_LANGUAGE.bidirectional();
        list.add("en_us");
        if (!this.currentCode.equals("en_us")) {
            LanguageInfo languageinfo = this.languages.get(this.currentCode);
            if (languageinfo != null) {
                list.add(this.currentCode);
                flag = languageinfo.bidirectional();
            }
        }

        ClientLanguage clientlanguage = ClientLanguage.loadFrom(pResourceManager, list, flag);
        I18n.setLanguage(clientlanguage);
        Language.inject(clientlanguage);
        this.reloadCallback.accept(clientlanguage);
    }

    public void setSelected(String pSelected) {
        this.currentCode = pSelected;
    }

    public String getSelected() {
        return this.currentCode;
    }

    public SortedMap<String, LanguageInfo> getLanguages() {
        return new TreeMap<>(this.languages);
    }

    @Nullable
    public LanguageInfo getLanguage(String pCode) {
        return this.languages.get(pCode);
    }
}