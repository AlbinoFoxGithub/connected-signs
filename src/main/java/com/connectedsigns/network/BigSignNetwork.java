package com.connectedsigns.network;

import com.connectedsigns.SignGroupCache;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class BigSignNetwork {
    public static final Identifier MULTI_SIGN_UPDATE_ID = new Identifier("connected-signs", "multi_sign_update");
    public static final Identifier MARK_INDIVIDUAL_ID = new Identifier("connected-signs", "mark_individual");
    public static final Identifier UNMARK_INDIVIDUAL_ID = new Identifier("connected-signs", "unmark_individual");

    public record MultiSignUpdatePayload(List<BlockPos> positions, List<List<String>> lines) implements FabricPacket {
        public static final PacketType<MultiSignUpdatePayload> TYPE =
                PacketType.create(MULTI_SIGN_UPDATE_ID, MultiSignUpdatePayload::new);

        public MultiSignUpdatePayload(PacketByteBuf buf) {
            this(readAll(buf));
        }

        private MultiSignUpdatePayload(List<?>[] data) {
            this((List<BlockPos>) data[0], (List<List<String>>) data[1]);
        }

        private static List<?>[] readAll(PacketByteBuf buf) {
            int count = buf.readInt();
            List<BlockPos> positions = new ArrayList<>(count);
            List<List<String>> lines = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                positions.add(buf.readBlockPos());
                List<String> signLines = new ArrayList<>(4);
                for (int j = 0; j < 4; j++) {
                    signLines.add(buf.readString());
                }
                lines.add(signLines);
            }
            return new List<?>[]{positions, lines};
        }

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeInt(positions.size());
            for (int i = 0; i < positions.size(); i++) {
                buf.writeBlockPos(positions.get(i));
                List<String> signLines = lines.get(i);
                for (int j = 0; j < 4; j++) {
                    buf.writeString(signLines.size() > j ? signLines.get(j) : "");
                }
            }
        }

        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(
                MULTI_SIGN_UPDATE_ID,
                (server, player, handler, buf, responseSender) -> {
                    MultiSignUpdatePayload packet = new MultiSignUpdatePayload(buf);
                    server.execute(() -> {
                        for (int i = 0; i < packet.positions().size(); i++) {
                            BlockPos pos = packet.positions().get(i);

                            if (player.getWorld().getBlockEntity(pos) instanceof SignBlockEntity sign) {
                                if (player.getBlockPos().isWithinDistance(pos, 8)) {
                                    List<String> signLines = packet.lines().get(i);
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
        ServerPlayNetworking.registerGlobalReceiver(
                MARK_INDIVIDUAL_ID,
                (server, player, handler, buf, responseSender) -> {
                    BlockPos pos = buf.readBlockPos();
                    boolean individual = buf.readBoolean();
                    server.execute(() -> {
                        server.getPlayerManager().sendToAll(
                                ServerPlayNetworking.createS2CPacket(
                                        MARK_INDIVIDUAL_ID,
                                        new PacketByteBuf(new PacketByteBuf(io.netty.buffer.Unpooled.buffer())
                                                .writeBlockPos(pos)
                                                .writeBoolean(individual))
                                )
                        );
                    });
                }
        );
    }
}