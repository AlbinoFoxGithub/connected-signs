package com.connectedsigns.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class BigSignNetwork {
    public static final Identifier MULTI_SIGN_UPDATE_ID = Identifier.of("connected-signs", "multi_sign_update");

    public record MarkIndividualPayload(BlockPos pos, boolean individual) implements CustomPayload {
        public static final CustomPayload.Id<MarkIndividualPayload> ID =
                new CustomPayload.Id<>(Identifier.of("connected-signs", "mark_individual"));

        public static final PacketCodec<RegistryByteBuf, MarkIndividualPayload> CODEC =
                PacketCodec.of(
                        (payload, buf) -> {
                            buf.writeBlockPos(payload.pos());
                            buf.writeBoolean(payload.individual());
                        },
                        buf -> new MarkIndividualPayload(buf.readBlockPos(), buf.readBoolean())
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record MultiSignUpdatePayload(List<BlockPos> positions, List<List<String>> lines) implements CustomPayload {
        public static final CustomPayload.Id<MultiSignUpdatePayload> ID =
                new CustomPayload.Id<>(MULTI_SIGN_UPDATE_ID);

        public static final PacketCodec<RegistryByteBuf, MultiSignUpdatePayload> CODEC =
                PacketCodec.of(
                        MultiSignUpdatePayload::write,
                        MultiSignUpdatePayload::read
                );

        private static void write(MultiSignUpdatePayload payload, RegistryByteBuf buf) {
            buf.writeVarInt(payload.positions().size());
            for (int i = 0; i < payload.positions().size(); i++) {
                buf.writeBlockPos(payload.positions().get(i));
                List<String> signLines = payload.lines().get(i);
                for (int l = 0; l < 4; l++) {
                    buf.writeString(signLines.size() > l ? signLines.get(l) : "");
                }
            }
        }

        private static MultiSignUpdatePayload read(RegistryByteBuf buf) {
            int count = buf.readVarInt();
            List<BlockPos> positions = new ArrayList<>();
            List<List<String>> lines = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                positions.add(buf.readBlockPos());
                List<String> signLines = new ArrayList<>();
                for (int l = 0; l < 4; l++) {
                    signLines.add(buf.readString());
                }
                lines.add(signLines);
            }
            return new MultiSignUpdatePayload(positions, lines);
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void registerServer() {
        try {
            PayloadTypeRegistry.playS2C().register(MultiSignUpdatePayload.ID, MultiSignUpdatePayload.CODEC);
            PayloadTypeRegistry.playS2C().register(MarkIndividualPayload.ID, MarkIndividualPayload.CODEC);
        } catch (IllegalArgumentException ignored) {
        }

        try {
            PayloadTypeRegistry.playC2S().register(MultiSignUpdatePayload.ID, MultiSignUpdatePayload.CODEC);
            PayloadTypeRegistry.playC2S().register(MarkIndividualPayload.ID, MarkIndividualPayload.CODEC);
        } catch (IllegalArgumentException ignored) {
        }

        ServerPlayNetworking.registerGlobalReceiver(
                MarkIndividualPayload.ID,
                (payload, context) -> {
                    context.server().execute(() -> {
                        context.server().getPlayerManager().sendToAll(
                                ServerPlayNetworking.createS2CPacket(
                                        new MarkIndividualPayload(payload.pos(), payload.individual())
                                )
                        );
                    });
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                MultiSignUpdatePayload.ID,
                (payload, context) -> {
                    ServerPlayerEntity player = context.player();
                    context.server().execute(() -> {
                        for (int i = 0; i < payload.positions().size(); i++) {
                            BlockPos pos = payload.positions().get(i);

                            if (player.getWorld().getBlockEntity(pos) instanceof SignBlockEntity sign) {
                                if (player.getBlockPos().isWithinDistance(pos, 8)) {
                                    List<String> signLines = payload.lines().get(i);
                                    sign.changeText(oldText -> {
                                        SignText newText = oldText;
                                        newText = newText.withMessage(0, Text.literal(signLines.get(0)));
                                        newText = newText.withMessage(1, Text.literal(signLines.get(1)));
                                        newText = newText.withMessage(2, Text.literal(signLines.get(2)));
                                        newText = newText.withMessage(3, Text.literal(signLines.get(3)));
                                        return newText;
                                    }, true);
                                    sign.markDirty();
                                    player.getWorld().updateListeners(pos,
                                            player.getWorld().getBlockState(pos),
                                            player.getWorld().getBlockState(pos), 3);
                                }
                            }
                        }
                    });
                }
        );
    }
}