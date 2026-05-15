package com.connectedsigns;

import com.connectedsigns.network.BigSignNetwork;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConnectedSigns implements ModInitializer {
	public static final String MOD_ID = "connected-signs";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Set<BlockPos> autoBreaking = new HashSet<>();
	private static List<BlockPos> pendingGroupBreak = null;

	@Override
	public void onInitialize() {
		BigSignNetwork.registerServer();

		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (autoBreaking.contains(pos)) return true;
			if (!(state.getBlock() instanceof WallSignBlock)) return true;
			pendingGroupBreak = SignGroup.findConnectedSigns(world, pos);
			return true;
		});

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (autoBreaking.contains(pos)) return;
			List<BlockPos> group = pendingGroupBreak;
			pendingGroupBreak = null;
			if (group == null || group.size() <= 1) return;
			for (BlockPos groupPos : group) {
				if (groupPos.equals(pos)) continue;
				autoBreaking.add(groupPos);
				world.breakBlock(groupPos, true, player);
				autoBreaking.remove(groupPos);
			}
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClient) return ActionResult.PASS;
			if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
			BlockPos pos = hitResult.getBlockPos();
			if (!(world.getBlockState(pos).getBlock() instanceof WallSignBlock)) return ActionResult.PASS;

			var stack = player.getStackInHand(hand);
			boolean isDye     = stack.getItem() instanceof DyeItem;
			boolean isGlow    = stack.getItem() == Items.GLOW_INK_SAC;
			boolean isInkSac  = stack.getItem() == Items.INK_SAC;
			if (!isDye && !isGlow && !isInkSac) return ActionResult.PASS;

			List<BlockPos> group = SignGroup.findConnectedSigns(world, pos);
			if (group.size() <= 1) return ActionResult.PASS;

			var dyeColor = isDye ? ((DyeItem) stack.getItem()).getColor() : null;

			for (BlockPos groupPos : group) {
				if (groupPos.equals(pos)) continue;
				if (!(world.getBlockEntity(groupPos) instanceof SignBlockEntity sign)) continue;
				if (isDye) {
					sign.changeText(t -> t.withColor(dyeColor), true);
				} else if (isGlow) {
					sign.changeText(t -> t.withGlowing(true), true);
				} else {
					sign.changeText(t -> t.withGlowing(false), true);
				}
				sign.markDirty();
			}

			return ActionResult.PASS;
		});

		LOGGER.info("[Connected Signs] Initialized [Version 1.0.0]");
	}
}