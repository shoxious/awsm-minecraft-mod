package de.shoxi.mcguide.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/** Fuer die Verzauberungs-Infobox: welches Feld liegt unter der Maus, wo steht das Inventar. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("hoveredSlot")
	Slot mcguide$hoveredSlot();

	@Accessor("leftPos")
	int mcguide$leftPos();

	@Accessor("topPos")
	int mcguide$topPos();
}
