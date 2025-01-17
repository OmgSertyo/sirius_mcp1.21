package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsGenericErrorScreen;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.ObjIntConsumer;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.optifine.reflect.Reflector;

public class TitleScreen extends Screen {
    private static final Component TITLE = Component.translatable("narrator.screen.title");
    private static final Component COPYRIGHT_TEXT = Component.translatable("title.credits");
    @Nullable
    private SplashRenderer splash;
    private float panoramaFade = 1.0F;
    private boolean fading;
    private long fadeInStart;
    private final LogoRenderer logoRenderer;

    public TitleScreen() {
        this(false);
    }

    public TitleScreen(boolean pFading) {
        this(pFading, null);
    }

    public TitleScreen(boolean pFading, @Nullable LogoRenderer pLogoRenderer) {
        super(TITLE);
        this.fading = pFading;
        this.logoRenderer = Objects.requireNonNullElseGet(pLogoRenderer, () -> new LogoRenderer(false));
    }

    public static CompletableFuture<Void> preloadResources(TextureManager pTexMngr, Executor pBackgroundExecutor) {
        return CompletableFuture.allOf(
            pTexMngr.preload(LogoRenderer.MINECRAFT_LOGO, pBackgroundExecutor),
            pTexMngr.preload(LogoRenderer.MINECRAFT_EDITION, pBackgroundExecutor),
            pTexMngr.preload(PanoramaRenderer.PANORAMA_OVERLAY, pBackgroundExecutor),
            CUBE_MAP.preload(pTexMngr, pBackgroundExecutor)
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        if (this.splash == null) {
            this.splash = this.minecraft.getSplashManager().getSplash();

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());

            int i = calendar.get(5);
            int j = calendar.get(2) + 1;

            if (i == 8 && j == 4) {
                this.splash = new SplashRenderer("Happy birthday, OptiFine!");
            }

            if (i == 14 && j == 8) {
                this.splash = new SplashRenderer("Happy birthday, sp614x!");
            }
        }

        int l = this.font.width(COPYRIGHT_TEXT);
        int i1 = this.width - l - 2;
        int k = this.height / 4 + 48;

        this.addRenderableWidget(
                Button.builder(Component.translatable("menu.singleplayer"), btnIn -> this.minecraft.setScreen(new SelectWorldScreen(this)))
                        .bounds(this.width / 2 - 100, k, 200, 20)
                        .build()
        );
        this.addRenderableWidget(Button.builder(Component.translatable("menu.multiplayer"), btnIn -> {
            Screen screen = new JoinMultiplayerScreen(this);
            this.minecraft.setScreen(screen);
        }).bounds(this.width / 2 - 100, k + 24, 200, 20).build());

        Screen errorRealms = new RealmsGenericErrorScreen(Component.translatable("mco.error.invalid.session.title"), Component.translatable("mco.error.invalid.session.message"), this);

        this.addRenderableWidget(
                Button.builder(Component.translatable("menu.online"), btnIn -> this.minecraft.setScreen(errorRealms))
                        .bounds(this.width / 2 - 100, k + 48, 200, 20)
                        .build()
        );

        SpriteIconButton spriteiconbutton = this.addRenderableWidget(
                CommonButtons.language(20, btnIn -> this.minecraft.setScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())), true)
        );

        spriteiconbutton.setPosition(this.width / 2 - 124, k + 72 + 12);

        this.addRenderableWidget(
                Button.builder(Component.translatable("menu.options"), btnIn -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)))
                        .bounds(this.width / 2 - 100, k + 72 + 12, 98, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.translatable("menu.quit"), btnIn -> this.minecraft.stop())
                        .bounds(this.width / 2 + 2, k + 72 + 12, 98, 20)
                        .build()
        );

        SpriteIconButton spriteiconbutton1 = this.addRenderableWidget(CommonButtons.accessibility(20, btnIn -> this.minecraft.setScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), true));
        spriteiconbutton1.setPosition(this.width / 2 + 104, k + 72 + 12);

        this.addRenderableWidget(new PlainTextButton(i1, this.height - 10, l, 10, COPYRIGHT_TEXT, btnIn -> this.minecraft.setScreen(new CreditsAndAttributionScreen(this)), this.font));
    }


    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.fadeInStart == 0L
                && this.fading) {
            this.fadeInStart = Util.getMillis();
        }

        GlStateManager._disableDepthTest();

        float f = 1.0F;

        if (this.fading) {
            float f1 = (float) (Util.getMillis() - this.fadeInStart) / 2000.0F;

            if (f1 > 1.0F) {
                this.fading = false;
                this.panoramaFade = 1.0F;
            } else {
                f1 = Mth.clamp(f1, 0.0F, 1.0F);
                f = Mth.clampedMap(f1, 0.5F, 1.0F, 0.0F, 1.0F);
                this.panoramaFade = Mth.clampedMap(f1, 0.0F, 0.5F, 0.0F, 1.0F);
            }

            this.fadeWidgets(f);
        }

        this.renderPanorama(pGuiGraphics, pPartialTick);

        int i = Mth.ceil(f * 255.0F) << 24;

        if ((i & -67108864) != 0) {
            super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            this.logoRenderer.renderLogo(pGuiGraphics, this.width, f);
            if (Reflector.ForgeHooksClient_renderMainMenu.exists()) {
                Reflector.callVoid(Reflector.ForgeHooksClient_renderMainMenu, this, pGuiGraphics, this.font, this.width, this.height, i);
            }

            if (this.splash != null && !this.minecraft.options.hideSplashTexts().get()) {
                this.splash.render(pGuiGraphics, this.width, this.font, i);
            }

            String s = "Minecraft " + SharedConstants.getCurrentVersion().getName() + ("release".equalsIgnoreCase(this.minecraft.getVersionType()) ? "" : "/" + this.minecraft.getVersionType());

            pGuiGraphics.drawString(this.font, s, 2, this.height - 10, 16777215 | i);
        }
    }

    private void fadeWidgets(float pAlpha) {
        for (GuiEventListener guieventlistener : this.children()) {
            if (guieventlistener instanceof AbstractWidget abstractwidget) {
                abstractwidget.setAlpha(pAlpha);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
    }

    @Override
    protected void renderPanorama(GuiGraphics pGuiGraphics, float pPartialTick) {
        PANORAMA.render(pGuiGraphics, this.width, this.height, this.panoramaFade, pPartialTick);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
}