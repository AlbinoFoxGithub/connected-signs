package com.connectedsigns.client;

import com.connectedsigns.SignGroup;
import com.connectedsigns.SignGroupCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

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

			MinecraftClient.getInstance().setScreen(new MultiSignEditScreen(group, world, pos));
			return ActionResult.FAIL;
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SignGroupCache.clear());

		WorldRenderEvents.LAST.register(context -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.world == null) return;

			Camera camera = context.camera();

			for (List<BlockPos> group : SignGroupCache.getAllGroups()) {
				if (group.isEmpty()) continue;

				BlockPos pos = group.get(0);
				if (!(client.world.getBlockEntity(pos) instanceof SignBlockEntity sign)) continue;

				SignText text = sign.getFrontText();
				boolean hasText = false;
				for (int i = 0; i < 4; i++) {
					if (!text.getMessage(i, false).getString().isEmpty()) {
						hasText = true;
						break;
					}
				}
				if (!hasText) continue;

				BlockPos lastPos = group.get(group.size() - 1);
				double centerX = (pos.getX() + lastPos.getX()) / 2.0 + 0.5;
				double centerY = pos.getY() + 0.5;
				double centerZ = pos.getZ() + 0.5;

				double dx = centerX - camera.getPos().x;
				double dy = centerY - camera.getPos().y;
				double dz = centerZ - camera.getPos().z;

				if (dx * dx + dy * dy + dz * dz > 1024) continue;

				Direction facing = client.world.getBlockState(pos).get(WallSignBlock.FACING);
				float yaw = switch (facing) {
					case NORTH -> 180f;
						case SOUTH -> 0f;
						case EAST -> 90f;
						case WEST -> 270f;
						default -> 0f;
				};

				MatrixStack matrices = context.matrixStack();
				matrices.push();
				matrices.translate(dx, dy, dz);
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
				matrices.translate(0, 0, -0.42);

				float scale = 0.025f;
				matrices.scale(-scale, -scale, scale);

				VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();

				for (int i = 0; i < group.size(); i++) {
					String line = text.getMessage(i, false).getString();
					if (line.isEmpty()) continue;

					float x = -client.textRenderer.getWidth(line) / 2.0f;
					float y = (i - 1.5f) * 10f;
					client.textRenderer.draw(line, x, y, 0x000000, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.NORMAL, 0, 0xf000f0);
				}
				immediate.draw();
				matrices.pop();
			}
		});
	}
}