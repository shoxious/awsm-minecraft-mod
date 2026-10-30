package de.shoxi.mcguide;

import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

/** Auto-Handel: im Handelsfenster Feldfrüchte automatisch gegen Smaragde verkaufen. */
final class AutoTrade {
	private static final Set<Item> GOODS = Set.of(Items.PUMPKIN, Items.MELON, Items.WHEAT, Items.CARROT, Items.POTATO, Items.BEETROOT);
	private static int ticks;
	private static int doneMenu = -1;
	private static int traded;

	static void tick(Minecraft mc) {
		if (GuideClient.settings.autoTrade && mc.player != null && mc.gameMode != null) {
			if (mc.gui.screen() instanceof MerchantScreen screen) {
				MerchantMenu menu = (MerchantMenu)screen.getMenu();
				if (menu.containerId != doneMenu && ticks++ % 4 == 0) {
					for (int i = 0; i < menu.getOffers().size(); i++) {
						MerchantOffer offer = (MerchantOffer)menu.getOffers().get(i);
						if (sellable(offer) && count(menu, offer.getCostA()) >= offer.getCostA().getCount()) {
							menu.setSelectionHint(i);
							menu.tryMoveItems(i);
							mc.getConnection().send(new ServerboundSelectTradePacket(i));
							mc.gameMode.handleContainerInput(menu.containerId, 2, 0, ContainerInput.QUICK_MOVE, mc.player);
							traded++;
							return;
						}
					}

					doneMenu = menu.containerId;
					if (traded > 0) {
						mc.player.sendOverlayMessage(Component.literal("Auto-Handel: alles verkauft").withStyle(ChatFormatting.GREEN));
					}
				}
			} else {
				traded = 0;
			}
		}
	}

	private static boolean sellable(MerchantOffer offer) {
		return !offer.isOutOfStock() && offer.getResult().is(Items.EMERALD) && offer.getCostB().isEmpty() && GOODS.contains(offer.getCostA().getItem());
	}

	private static int count(MerchantMenu menu, ItemStack cost) {
		int n = 0;

		for (Slot s : menu.slots) {
			if (s.index != 2 && ItemStack.isSameItemSameComponents(s.getItem(), cost)) {
				n += s.getItem().getCount();
			}
		}

		return n;
	}

	static void toggle(Minecraft mc) {
		GuideState s = GuideClient.settings;
		s.autoTrade = !s.autoTrade;
		s.save();
		doneMenu = -1;
		if (mc.player != null) {
			mc.player
				.sendOverlayMessage(
					Component.literal("Auto-Handel " + (s.autoTrade ? "an" : "aus")).withStyle(s.autoTrade ? ChatFormatting.GREEN : ChatFormatting.GRAY)
				);
		}
	}

	private AutoTrade() {
	}
}
