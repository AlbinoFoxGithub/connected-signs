package com.connectedsigns.client;

import com.connectedsigns.network.BigSignNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class BigSignClientNetwork {
    public static void registerClient() {
        try {
            PayloadTypeRegistry.playC2S().register(
                    BigSignNetwork.MultiSignUpdatePayload.ID,
                    BigSignNetwork.MultiSignUpdatePayload.CODEC
            );
        } catch (IllegalArgumentException e) {
        }
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
}