package io.github.nicolasgoyon.beaconhold.client.screen;

import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.cost.CostRules;
import io.github.nicolasgoyon.beaconhold.cost.FuelRules;
import io.github.nicolasgoyon.beaconhold.cost.ModTags;
import io.github.nicolasgoyon.beaconhold.menu.BeaconFunctionMenu;
import io.github.nicolasgoyon.beaconhold.menu.FunctionButtons;
import io.github.nicolasgoyon.beaconhold.menu.FunctionList;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The function screen of a beacon. It shows the functions of the registry, the function of the beacon and its state,
 * the cost and the stock of the consumable. The cost does not depend on the function. A click on a function sends the id of its button to the server.
 */
public final class BeaconFunctionScreen extends AbstractContainerScreen<BeaconFunctionMenu> {
    private static final String PREFIX = "screen.beaconhold.functions.";
    private static final int ROWS = 3;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 30;
    private static final int BAR_Y = 120;
    private static final int STOCK_TEXT_X = 30;
    private static final int TEXT_COLOR = 0x404040;
    private static final int ACTIVE_COLOR = 0x2E7D32;
    private static final int INACTIVE_COLOR = 0xA02020;

    private final List<Button> rows = new ArrayList<>();
    private final List<Integer> rowEntries = new ArrayList<>();
    private List<ResourceKey<BeaconFunction>> functions = List.of();
    private int page;
    private boolean pageFromSelection = true;

    public BeaconFunctionScreen(BeaconFunctionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 228;
    }

    @Override
    protected void init() {
        super.init();
        functions = FunctionList.sorted();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        rows.clear();
        rowEntries.clear();
        int entries = FunctionButtons.entryCount(functions.size());
        int pages = FunctionButtons.pageCount(entries, ROWS);
        if (pageFromSelection) {
            page = FunctionButtons.pageOf(selectedEntry(), ROWS);
        }
        page = FunctionButtons.clampPage(page, pages);
        for (int row = 0; row < ROWS; row++) {
            int entry = page * ROWS + row;
            if (entry >= entries) {
                break;
            }
            Button button = Button.builder(entryName(entry), b -> click(entry))
                    .bounds(leftPos + 8, topPos + LIST_TOP + row * ROW_HEIGHT, 160, 20)
                    .build();
            if (entry == FunctionButtons.NONE) {
                button.setTooltip(Tooltip.create(Component.translatable(PREFIX + "none.tooltip")));
            }
            rows.add(addRenderableWidget(button));
            rowEntries.add(entry);
        }
        int bottom = topPos + BAR_Y;
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "back"), b -> click(FunctionButtons.BACK))
                .bounds(leftPos + 8, bottom, 60, 20)
                .build());
        if (pages > 1) {
            Button previous = Button.builder(Component.literal("<"), b -> changePage(-1))
                    .bounds(leftPos + 124, bottom, 20, 20)
                    .build();
            previous.active = page > 0;
            addRenderableWidget(previous);
            Button next = Button.builder(Component.literal(">"), b -> changePage(1))
                    .bounds(leftPos + 148, bottom, 20, 20)
                    .build();
            next.active = page < pages - 1;
            addRenderableWidget(next);
        }
        updateRows();
    }

    private void changePage(int delta) {
        pageFromSelection = false;
        page += delta;
        rebuild();
    }

    private void click(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    /** Gives the entry of the function of the beacon: 0 for no function. */
    private int selectedEntry() {
        int index = menu.functionIndex();
        return index < 0 ? FunctionButtons.NONE : FunctionButtons.buttonOf(index);
    }

    private void updateRows() {
        int selected = selectedEntry();
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).active = rowEntries.get(i) != selected;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateRows();
    }

    private Component entryName(int entry) {
        if (entry == FunctionButtons.NONE) {
            return Component.translatable(PREFIX + "none");
        }
        return BeaconFunction.name(functions.get(FunctionButtons.functionOf(entry)));
    }

    private MutableComponent costText() {
        int ticks = menu.ticksPerItem();
        return Component.translatable(PREFIX + "cost", CostRules.minutes(ticks), CostRules.seconds(ticks))
                .append(" ")
                .append(Component.translatable(
                        PREFIX + "cost.stack", CostRules.STACK_SIZE, CostRules.stackDays(ticks)));
    }

    private ItemStack entryIcon(int entry) {
        if (entry == FunctionButtons.NONE) {
            return new ItemStack(Items.BARRIER);
        }
        BeaconFunction function = ModRegistries.FUNCTIONS.get(functions.get(FunctionButtons.functionOf(entry)));
        return function == null ? ItemStack.EMPTY : function.icon();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < rows.size(); i++) {
            Button row = rows.get(i);
            graphics.renderItem(entryIcon(rowEntries.get(i)), row.getX() + 2, row.getY() + 2);
        }
        renderTooltip(graphics, mouseX, mouseY);
        if (menu.getCarried().isEmpty()
                && hoveredSlot != null
                && hoveredSlot.index == BeaconFunctionMenu.STOCK_SLOT
                && !hoveredSlot.hasItem()) {
            graphics.renderComponentTooltip(font, stockTooltip(), mouseX, mouseY);
        }
        int costY = topPos + BeaconFunctionMenu.STOCK_Y + 1;
        if (mouseX >= leftPos + STOCK_TEXT_X && mouseX < leftPos + imageWidth - 4 && mouseY >= costY
                && mouseY < costY + 19) {
            graphics.renderTooltip(font, font.split(costText(), 200), mouseX, mouseY);
        }
    }

    private List<Component> stockTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(PREFIX + "stock.accepts"));
        BuiltInRegistries.ITEM.getTag(ModTags.CONSUMABLES).ifPresent(items -> items.forEach(
                item -> lines.add(Component.literal("- ").append(item.value().getDescription()))));
        return lines;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int right = leftPos + imageWidth;
        int bottom = topPos + imageHeight;
        graphics.renderOutline(leftPos - 1, topPos - 1, imageWidth + 2, imageHeight + 2, 0xFF000000);
        graphics.fill(leftPos, topPos, right, bottom, 0xFFC6C6C6);
        graphics.fill(leftPos, topPos, right - 1, topPos + 1, 0xFFFFFFFF);
        graphics.fill(leftPos, topPos, leftPos + 1, bottom - 1, 0xFFFFFFFF);
        graphics.fill(leftPos + 1, bottom - 1, right, bottom, 0xFF555555);
        graphics.fill(right - 1, topPos + 1, right, bottom, 0xFF555555);
        for (Slot slot : menu.slots) {
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
            graphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, TEXT_COLOR, false);
        Component state;
        int color;
        if (menu.functionIndex() < 0) {
            state = Component.translatable(PREFIX + "state.none");
            color = TEXT_COLOR;
        } else if (menu.isActive()) {
            state = Component.translatable(PREFIX + "state.active");
            color = ACTIVE_COLOR;
        } else {
            state = Component.translatable(PREFIX + "state.inactive");
            color = INACTIVE_COLOR;
        }
        graphics.drawString(font, state, 8, 18, color, false);
        long remaining = menu.remainingTicks();
        int stockY = BeaconFunctionMenu.STOCK_Y + 1;
        if (remaining <= 0) {
            graphics.drawString(font, Component.translatable(PREFIX + "stock.empty"), STOCK_TEXT_X, stockY,
                    INACTIVE_COLOR, false);
        } else if (FuelRules.isLow(remaining)) {
            graphics.drawString(font, Component.translatable(PREFIX + "stock.low", CostRules.days(remaining)),
                    STOCK_TEXT_X, stockY, INACTIVE_COLOR, false);
        } else {
            graphics.drawString(font, Component.translatable(PREFIX + "stock", CostRules.days(remaining)),
                    STOCK_TEXT_X, stockY, TEXT_COLOR, false);
        }
        int ticks = menu.ticksPerItem();
        graphics.drawString(font,
                Component.translatable(PREFIX + "cost.short", CostRules.minutes(ticks), CostRules.seconds(ticks)),
                STOCK_TEXT_X, stockY + 10, TEXT_COLOR, false);
    }
}
