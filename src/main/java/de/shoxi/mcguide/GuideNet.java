package de.shoxi.mcguide;

import java.util.List;

import net.minecraft.core.BlockPos;
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

	/** Client → Server: welche Kisten in der Nähe sind noch ungeöffnet (haben eine Loot-Tabelle)? */
	public record LootRequest() implements CustomPacketPayload {
		public static final Type<LootRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GuideServer.MOD_ID, "loot_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, LootRequest> CODEC = StreamCodec.unit(new LootRequest());

		@Override
		public Type<LootRequest> type() {
			return TYPE;
		}
	}

	/** Server → Client: ungeöffnete Loot-Blöcke und Loot-Fahrzeuge (Kistenloren, Boote). */
	public record LootSpots(List<BlockPos> blocks, List<Integer> entities) implements CustomPacketPayload {
		public static final Type<LootSpots> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GuideServer.MOD_ID, "loot_spots"));
		public static final StreamCodec<RegistryFriendlyByteBuf, LootSpots> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), LootSpots::blocks,
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), LootSpots::entities,
			LootSpots::new);

		@Override
		public Type<LootSpots> type() {
			return TYPE;
		}
	}

	/** Client → Server: nächste Struktur einer Art suchen (/finde). */
	public record StructureRequest(String kind) implements CustomPacketPayload {
		public static final Type<StructureRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GuideServer.MOD_ID, "structure_request"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StructureRequest> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, StructureRequest::kind, StructureRequest::new);

		@Override
		public Type<StructureRequest> type() {
			return TYPE;
		}
	}

	/** Server → Client: Ergebnis von /finde. */
	public record StructureFound(String kind, boolean found, BlockPos pos) implements CustomPacketPayload {
		public static final Type<StructureFound> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GuideServer.MOD_ID, "structure_found"));
		public static final StreamCodec<RegistryFriendlyByteBuf, StructureFound> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, StructureFound::kind,
			ByteBufCodecs.BOOL, StructureFound::found,
			BlockPos.STREAM_CODEC, StructureFound::pos,
			StructureFound::new);

		@Override
		public Type<StructureFound> type() {
			return TYPE;
		}
	}

	private GuideNet() {}
}
