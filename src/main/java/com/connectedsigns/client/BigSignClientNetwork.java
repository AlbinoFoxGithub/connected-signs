package com.connectedsigns.client;

import com.connectedsigns.SignGroupCache;
import com.connectedsigns.network.BigSignNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class BigSignClientNetwork {
    public static void registerClient() {

    }

    public static void sendMultiSignUpdate(List<BlockPos> positions, List<String[]> signLines) {
        List<List<String>> linesList = new ArrayList<>();
        for (String[] arr : signLines) {
            linesList.add(List.of(arr));
        }
        BigSignNetwork.MultiSignUpdatePayload payload = new BigSignNetwork.MultiSignUpdatePayload(positions, linesList);
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        payload.write(buf);
        ClientPlayNetworking.send(BigSignNetwork.MULTI_SIGN_UPDATE_ID, buf);
    }

    public static void sendMarkIndividual(BlockPos pos, boolean individual) {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeBoolean(individual);
        ClientPlayNetworking.send(BigSignNetwork.MARK_INDIVIDUAL_ID, buf);
    }

    public static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(
                BigSignNetwork.MARK_INDIVIDUAL_ID,
                (client, handler, buf, responseSender) -> {
                    BlockPos pos = buf.readBlockPos();
                    boolean individual = buf.readBoolean();
                    client.execute(() -> {
                        if (individual) {
                            SignGroupCache.markIndividual(pos);
                        } else {
                            SignGroupCache.unmarkIndividual(pos);
                        }
                    });
                }
        );
    }
}