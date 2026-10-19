package de.shoxi.mcguide;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Netzwerk-Pakete zwischen Server und Client-Mod. */
public final class GuideNet {
	/** Server → Client: erledigte und manuell abgehakte Ziele. */
	public record Sync(List<String> done, List<String> manual) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GuideServer.MOD_ID, "sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Sync::done,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Sync::manual,
			Sync::new);

		@Override
		public Type<Sync> type() {
			return TYPE;
		}
	}

	/** Client → Server: Ziel an-/abhaken. */
	public record Toggle(String id) implements CustomPacketPayload {
		public static final Type<Toggle> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GuideServer.MOD_ID, "toggle"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Toggle> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, Toggle::id, Toggle::new);

		@Override
		public Type<Toggle> type() {
			return TYPE;
		}
	}

	/** Client → Server: "ich habe den Guide-Mod", Server antwortet mit Sync. */
	public record Hello() implements CustomPacketPayload {
		public static final Type<Hello> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GuideServer.MOD_ID, "hello"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Hello> CODEC = StreamCodec.unit(new Hello());

		@Override
		public Type<Hello> type() {
			return TYPE;
		}
	}

	private GuideNet() {}
}
