package io.github.nicolasgoyon.beaconhold.client.screen;

import io.github.nicolasgoyon.beaconhold.network.OpenFunctionsPayload;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Adds the button "Functions" to the vanilla beacon screen. The button opens the function screen of the beacon. */
public final class BeaconScreenButton {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 20;
    private static final int GAP = 4;

    private BeaconScreenButton() {
    }

    /**
     * Adds the button when the game opens a beacon screen.
     *
     * @param event the event of the screen
     */
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof BeaconScreen screen)) {
            return;
        }
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int right = left + screen.getXSize();
        int x;
        int y;
        if (right + GAP + WIDTH <= screen.width) {
            x = right + GAP;
            y = top;
        } else if (left - GAP - WIDTH >= 0) {
            x = left - GAP - WIDTH;
            y = top;
        } else {
            x = right - WIDTH;
            y = Math.max(0, top - GAP - HEIGHT);
        }
        event.addListener(Button.builder(
                        Component.translatable("screen.beaconhold.functions.open"),
                        button -> PacketDistributor.sendToServer(OpenFunctionsPayload.INSTANCE))
                .bounds(x, y, WIDTH, HEIGHT)
                .build());
    }
}
