package com.shouyun.inventorylens.client.config;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import com.shouyun.inventorylens.client.animation.WorldUiAnimator;
import com.shouyun.inventorylens.config.InventoryLensConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Vanilla styled settings with live edits and an animated local preview. */
public final class InventoryLensConfigScreen extends Screen {
    private static final ResourceLocation CHEST_GUI =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final String[] PAGE_KEYS = {
            "inventorylens.settings.general", "inventorylens.settings.entity",
            "inventorylens.settings.containers", "inventorylens.settings.animations",
            "inventorylens.settings.visibility"
    };
    private static final String[] VISIBILITY_TYPES = {
            "chest", "barrel", "shulker_box", "ender_chest", "hopper", "dispenser",
            "dropper", "furnace", "blast_furnace", "smoker", "brewing_stand", "crafter"
    };
    private final Screen parent;
    private final InventoryLensConfig original;
    private final WorldUiAnimator<String> previewAnimator = new WorldUiAnimator<>();
    private final PreviewModelRenderer previewModels = new PreviewModelRenderer();
    private final long previewStarted = System.nanoTime();
    private InventoryLensConfig working;
    private boolean previewEntity;
    private int page;
    private int visibilityListPage;
    private int leftX;
    private int controlWidth;
    private int previewLeft;

    public InventoryLensConfigScreen(Screen parent) {
        super(Component.translatable("inventorylens.settings.title"));
        this.parent = parent;
        this.original = ConfigManager.copy();
        this.working = ConfigManager.copy();
    }

    @Override protected void init() {
        int mid = width / 2;
        int tabWidth = Math.min(100, (width - 16) / PAGE_KEYS.length);
        int tabsLeft = mid - tabWidth * PAGE_KEYS.length / 2;
        leftX = 8;
        controlWidth = mid - 16;
        previewLeft = mid + 4;
        for (int i = 0; i < PAGE_KEYS.length; i++) {
            final int selected = i;
            Button tab = Button.builder(tr(PAGE_KEYS[i]), button -> {
                page = selected;
                if (page == 1) previewEntity = true;
                if (page == 2) previewEntity = false;
                rebuildWidgets();
            }).bounds(tabsLeft + i * tabWidth, 42, tabWidth - 2, 20).build();
            tab.active = i != page;
            addRenderableWidget(tab);
        }
        addRenderableWidget(Button.builder(previewTargetLabel(), button -> {
            previewEntity = !previewEntity;
            button.setMessage(previewTargetLabel());
        }).bounds(previewLeft + 7, 78, Math.max(80, width - previewLeft - 15), 20).build());

        if (page < 3) {
            InventoryLensConfig.Placement settings = switch (page) {
                case 1 -> working.entity;
                case 2 -> working.container;
                default -> working.general;
            };
            slider("inventorylens.settings.scale", 0, 0.5, 2, 0.05, () -> settings.scale, v -> settings.scale = v, "×");
            slider("inventorylens.settings.horizontal", 1, -2, 2, 0.01,
                    () -> settings.horizontalOffset, v -> settings.horizontalOffset = v, "");
            slider("inventorylens.settings.vertical", 2, -2, 2, 0.01,
                    () -> settings.verticalOffset, v -> settings.verticalOffset = v, "");
            slider("inventorylens.settings.depth", 3, -1, 1, 0.01,
                    () -> settings.depthOffset, v -> settings.depthOffset = v, "");
            if (page == 0) {
                addRenderableWidget(Button.builder(languageLabel(), button -> {
                    working.language = switch (working.language) {
                        case "auto" -> "zh_cn";
                        case "zh_cn" -> "en_us";
                        default -> "auto";
                    };
                    apply();
                    rebuildWidgets();
                }).bounds(leftX, 182, controlWidth, 20).build());
            }
        } else if (page == 3) {
            addRenderableWidget(Button.builder(animationLabel(), button -> {
                working.animation.enabled = !working.animation.enabled;
                button.setMessage(animationLabel());
                apply();
            }).bounds(leftX, 78, controlWidth, 20).build());
            slider("inventorylens.settings.enter", 1, 50, 600, 10,
                    () -> working.animation.enterDurationMs,
                    v -> working.animation.enterDurationMs = (int)v, " ms");
            slider("inventorylens.settings.exit", 2, 50, 600, 10,
                    () -> working.animation.exitDurationMs,
                    v -> working.animation.exitDurationMs = (int)v, " ms");
        } else {
            initVisibility();
        }
        int actionWidth = Math.min(150, (width - 18) / 2);
        addRenderableWidget(Button.builder(tr("inventorylens.settings.done"), button -> {
            apply();
            ConfigManager.save();
            minecraft.setScreen(parent);
        }).bounds(mid - actionWidth - 4, height - 29, actionWidth, 20).build());
        addRenderableWidget(Button.builder(tr("inventorylens.settings.reset"), button ->
                minecraft.setScreen(new ConfirmScreen(confirmed -> {
                    minecraft.setScreen(this);
                    if (confirmed) {
                        working = new InventoryLensConfig();
                        apply();
                        rebuildWidgets();
                    }
                }, tr("inventorylens.settings.reset.title"),
                        tr("inventorylens.settings.reset.detail"))))
                .bounds(mid + 4, height - 29, actionWidth, 20).build());
    }

    private Component previewTargetLabel() {
        return tr(previewEntity
                ? "inventorylens.settings.preview.entity" : "inventorylens.settings.preview.container");
    }

    private Component animationLabel() {
        return tr("inventorylens.settings.enabled").copy()
                .append(": ")
                .append(tr(working.animation.enabled ? "inventorylens.settings.on" : "inventorylens.settings.off"));
    }

    private Component languageLabel() {
        String choice = switch (working.language) {
            case "zh_cn" -> "inventorylens.settings.language.zh";
            case "en_us" -> "inventorylens.settings.language.en";
            default -> "inventorylens.settings.language.auto";
        };
        return tr("inventorylens.settings.language").copy().append(": ").append(tr(choice));
    }

    private void initVisibility() {
        addRenderableWidget(Button.builder(visibilityLabel("inventorylens.settings.visibility.entity",
                working.visibility.entity), button -> {
            working.visibility.entity = !working.visibility.entity;
            previewEntity = true;
            apply();
            button.setMessage(visibilityLabel("inventorylens.settings.visibility.entity", working.visibility.entity));
        }).bounds(leftX, 78, controlWidth, 20).build());
        addRenderableWidget(Button.builder(visibilityLabel("inventorylens.settings.visibility.containers",
                working.visibility.containers), button -> {
            working.visibility.containers = !working.visibility.containers;
            previewEntity = false;
            apply();
            button.setMessage(visibilityLabel("inventorylens.settings.visibility.containers", working.visibility.containers));
        }).bounds(leftX, 104, controlWidth, 20).build());

        int rows = Math.max(2, (height - 186) / 24);
        int pages = (VISIBILITY_TYPES.length + rows - 1) / rows;
        visibilityListPage = Math.clamp(visibilityListPage, 0, pages - 1);
        int half = (controlWidth - 4) / 2;
        Button previous = Button.builder(tr("inventorylens.settings.previous"), button -> {
            visibilityListPage--;
            rebuildWidgets();
        }).bounds(leftX, 130, half, 20).build();
        previous.active = visibilityListPage > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(tr("inventorylens.settings.next"), button -> {
            visibilityListPage++;
            rebuildWidgets();
        }).bounds(leftX + half + 4, 130, half, 20).build();
        next.active = visibilityListPage < pages - 1;
        addRenderableWidget(next);

        for (int row = 0; row < rows; row++) {
            int index = visibilityListPage * rows + row;
            if (index >= VISIBILITY_TYPES.length) break;
            String type = VISIBILITY_TYPES[index];
            String key = "inventorylens.settings.visibility." + type;
            addRenderableWidget(Button.builder(visibilityLabel(key, working.visibility.typeEnabled(type)), button -> {
                boolean enabled = !working.visibility.typeEnabled(type);
                working.visibility.setTypeEnabled(type, enabled);
                if ("chest".equals(type)) previewEntity = false;
                apply();
                button.setMessage(visibilityLabel(key, enabled));
            }).bounds(leftX, 156 + row * 24, controlWidth, 20).build());
        }
    }

    private Component visibilityLabel(String key, boolean enabled) {
        return tr(key).copy().append(": ")
                .append(tr(enabled ? "inventorylens.settings.on" : "inventorylens.settings.off"));
    }

    private Component tr(String key, Object... args) {
        return SettingsText.get(working.language, key, args);
    }

    private void slider(String key, int row, double min, double max, double step,
            DoubleSupplier get, DoubleConsumer set, String suffix) {
        addRenderableWidget(new AbstractSliderButton(leftX, 78 + row * 26, controlWidth, 20,
                Component.empty(), (get.getAsDouble() - min) / (max - min)) {
            { updateMessage(); }
            @Override protected void updateMessage() {
                String displayKey = controlWidth < 175 && (key.endsWith("horizontal")
                        || key.endsWith("vertical") || key.endsWith("depth")) ? key + ".short" : key;
                setMessage(tr(displayKey).copy().append(": " +
                        String.format(Locale.ROOT, "%.2f", get.getAsDouble()) + suffix));
            }
            @Override protected void applyValue() {
                set.accept(Math.round((min + value * (max - min)) / step) * step);
                apply();
                updateMessage();
            }
        });
    }

    private void apply() { ConfigManager.set(working); }

    @Override public void onClose() {
        ConfigManager.set(original);
        minecraft.setScreen(parent);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderPreview(graphics);
        graphics.drawCenteredString(font, tr("inventorylens.settings.title"), width / 2, 20, 0xFFFFFF);
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Screen.render already calls this. Keep the game visible without its blur pass.
        graphics.fillGradient(0, 0, width, height, 0x9A101820, 0xB0182028);
    }

    private void renderPreview(GuiGraphics graphics) {
        int left = previewLeft;
        int right = width - 8;
        int top = 103;
        int bottom = height - 37;
        if (right - left < 80 || bottom - top < 60) return;
        graphics.fill(left, top, right, bottom, 0xFF454A50);
        graphics.fill(left, top, right, top + 1, 0xFFAEB5BC);
        graphics.fill(left, bottom - 1, right, bottom, 0xFF1E2328);
        graphics.enableScissor(left + 2, top + 2, right - 2, bottom - 2);
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2 + 13;
        previewModels.render(graphics, previewEntity, centerX + (previewEntity ? -43 : 0),
                centerY + (previewEntity ? 48 : 28), Math.min(previewEntity ? 100 : 74, bottom - top - 20));

        int cycle = working.animation.enterDurationMs + 850 + working.animation.exitDurationMs + 350;
        long elapsedMs = (System.nanoTime() - previewStarted) / 1_000_000L;
        boolean shown = elapsedMs % cycle < working.animation.enterDurationMs + 850;
        if (shown) previewAnimator.show("demo"); else previewAnimator.hideOthers(null);
        previewAnimator.update(System.nanoTime(), working.animation);
        WorldUiAnimator.Visual visual = previewAnimator.visual("demo");
        double horizontal = working.horizontal(previewEntity);
        double vertical = working.vertical(previewEntity);
        double depth = working.depth(previewEntity);
        float perspective = (float)Math.clamp(1 + depth * 0.18, 0.65, 1.35);
        float panelScale = (float)((previewEntity ? 0.95 : 0.70) * working.scale(previewEntity)
                * perspective * visual.scale());
        int panelX = centerX + (previewEntity ? 39 : 0) + (int)Math.round(horizontal * 25);
        int panelY = centerY + (previewEntity ? 0 : 18) - (int)Math.round(vertical * 25)
                - (int)Math.round(visual.offsetPixels() * 0.58);
        boolean enabled = previewEntity ? working.visibility.entity : working.visibility.containerEnabled("chest");
        if (visual.alpha() > 0 && enabled) {
            graphics.pose().pushPose();
            graphics.pose().translate(panelX - (previewEntity ? 24 : 88) * panelScale,
                    panelY - (previewEntity ? 42 : 39) * panelScale, 250);
            graphics.pose().scale(panelScale, panelScale, 1);
            if (previewEntity) drawEquipmentPanel(graphics, visual.alpha());
            else drawChestPanel(graphics, visual.alpha());
            graphics.pose().popPose();
        }
        graphics.disableScissor();
        if (!enabled) {
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(0, 0, 300);
                graphics.drawCenteredString(font, tr("inventorylens.settings.preview.hidden"),
                        centerX, centerY - 5, 0xFFFFFFFF);
            } finally {
                graphics.pose().popPose();
            }
        }
        String scale = String.format(Locale.ROOT, "%.2f×", working.scale(previewEntity));
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 300);
            graphics.drawString(font, tr("inventorylens.settings.preview.scale", scale),
                    left + 7, bottom - 12, 0xFFFFFFFF, false);
        } finally {
            graphics.pose().popPose();
        }
    }

    private void drawChestPanel(GuiGraphics graphics, float alpha) {
        // Same vanilla texture regions used by the world-space three-row chest projection.
        graphics.setColor(1, 1, 1, alpha);
        graphics.blit(CHEST_GUI, 0, 0, 0, 0, 176, 71, 256, 256);
        graphics.blit(CHEST_GUI, 0, 71, 0, 215, 176, 7, 256, 256);
        graphics.setColor(1, 1, 1, 1);
    }

    private void drawEquipmentPanel(GuiGraphics graphics, float alpha) {
        int a = Math.round(alpha * 255);
        graphics.fill(0, 0, 48, 84, a << 24 | 0x00C6CCD2);
        graphics.fill(1, 1, 47, 3, a << 24 | 0x00F7FAFC);
        for (int y : new int[] {5, 24, 43, 62}) {
            graphics.fill(5, y, 23, y + 18, a << 24 | 0x009098A1);
        }
        for (int y : new int[] {43, 62}) {
            graphics.fill(25, y, 43, y + 18, a << 24 | 0x009098A1);
        }
    }
}
