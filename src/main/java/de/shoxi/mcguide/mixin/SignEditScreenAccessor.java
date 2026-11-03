package de.shoxi.mcguide.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;

/** Fuer /lager: Schildtext setzen, bevor das Fenster ihn beim Schliessen an den Server schickt. */
@Mixin(AbstractSignEditScreen.class)
public interface SignEditScreenAccessor {
	@Accessor("messages")
	String[] mcguide$messages();
}
