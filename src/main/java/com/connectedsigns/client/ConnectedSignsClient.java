package com.connectedsigns.client;

import com.connectedsigns.SignGroup;
import com.connectedsigns.SignGroupCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class ConnectedSignsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BigSignClientNetwork.registerClient();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world != null) {
				SignGroupCache.setWorld(client.world);
			}
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
			if (!world.isClient) return ActionResult.PASS;

			BlockPos pos = hitResult.getBlockPos();
			if (!(world.getBlockState(pos).getBlock() instanceof WallSignBlock)) return ActionResult.PASS;
			if (!(world.getBlockEntity(pos) instanceof SignBlockEntity)) return ActionResult.PASS;

			if (player.isSneaking()) return ActionResult.PASS;

			var heldItem = player.getMainHandStack().getItem();
			if (heldItem instanceof DyeItem || heldItem == Items.GLOW_INK_SAC || heldItem == Items.INK_SAC) {
				return ActionResult.PASS;
			}

			List<BlockPos> group = SignGroup.findConnectedSigns(world, pos);

			SignGroupCache.registerGroup(group);
			SignGroupCache.setWorld(world);

			MinecraftClient.getInstance().setScreen(new MultiSignEditScreen(group, world));
			return ActionResult.FAIL;
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SignGroupCache.clear());
	}
}