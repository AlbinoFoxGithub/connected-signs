package com.connectedsigns.client;

import com.connectedsigns.SignGroupCache;
import com.connectedsigns.network.BigSignNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class BigSignClientNetwork {
    public static void registerClient() {
        try {
            PayloadTypeRegistry.playC2S().register(BigSignNetwork.MultiSignUpdatePayload.ID, BigSignNetwork.MultiSignUpdatePayload.CODEC);
        } catch (IllegalArgumentException ignored) {}
        try {
            PayloadTypeRegistry.playC2S().register(BigSignNetwork.MarkIndividualPayload.ID, BigSignNetwork.MarkIndividualPayload.CODEC);
        } catch (IllegalArgumentException ignored) {}
        try {
            PayloadTypeRegistry.playS2C().register(BigSignNetwork.MarkIndividualPayload.ID, BigSignNetwork.MarkIndividualPayload.CODEC);
        } catch (IllegalArgumentException ignored) {}
    }

    public static void sendMultiSignUpdate(List<BlockPos> positions, List<String[]> signLines) {
        List<List<String>> linesList = new ArrayList<>();
        for (String[] arr : signLines) {
            linesList.add(List.of(arr));
        }
        ClientPlayNetworking.send(
                new BigSignNetwork.MultiSignUpdatePayload(positions, linesList)
        );
    }

    public static void sendMarkIndividual(BlockPos pos, boolean individual) {
        System.out.println("[ConnectedSigns] Sending mark individual packet: " + pos + " = " + individual);
        ClientPlayNetworking.send(
                new BigSignNetwork.MarkIndividualPayload(pos, individual)
        );
    }

    public static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(
                BigSignNetwork.MarkIndividualPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (payload.individual()) {
                            SignGroupCache.markIndividual(payload.pos());
                        } else {
                            SignGroupCache.unmarkIndividual(payload.pos());
                        }
                    });
                }
        );
    }
}