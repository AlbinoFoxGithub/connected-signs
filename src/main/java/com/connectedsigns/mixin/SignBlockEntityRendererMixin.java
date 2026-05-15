package com.connectedsigns.mixin;

import com.connectedsigns.SignGroup;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SignBlockEntityRenderer.class)
public abstract class SignBlockEntityRendererMixin {

    @Shadow abstract Vec3d getTextOffset();
    @Shadow abstract void setTextAngles(MatrixStack matrices, boolean front, Vec3d offset);
    @Shadow private static int getColor(SignText signText) { throw new AssertionError(); }
    @Shadow private static int GLOWING_BLACK_COLOR;
    @Shadow private static boolean shouldRender(BlockPos pos, int light) { throw new AssertionError(); }

    @Inject(
            method = "renderText(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/SignText;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IIIZ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderText(BlockPos pos, SignText signText, MatrixStack matrices,
                              VertexConsumerProvider vertexConsumers, int light,
                              int lineHeight, int lineWidth, boolean front, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        List<BlockPos> group = SignGroup.findConnectedSigns(client.world, pos);
        if (group.size() <= 1) return;

        ci.cancel();

        if (!group.get(0).equals(pos) || !front) return;

        int numSigns = group.size();
        TextRenderer tr = client.textRenderer;

        int color = getColor(signText);
        boolean glowing = signText.isGlowing();
        int signColor = signText.getColor().getSignColor();
        int lightValue = (glowing && shouldRender(pos, light)) ? 0xF000F0 : light;

        matrices.push();
        setTextAngles(matrices, front, getTextOffset());

        float centerOffset = (numSigns - 1) * 48.0f;

        for (int i = 0; i < 4; i++) {
            String line = signText.getMessage(i, false).getString();
            if (line.isEmpty()) continue;

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float x = centerOffset - tr.getWidth(line) / 2.0f;
            float y = lineHeight * (i - 2);

            if (glowing) {
                tr.drawWithOutline(
                        Text.literal(line).asOrderedText(),
                        x, y, signColor, GLOWING_BLACK_COLOR,
                        matrix, vertexConsumers, lightValue
                );
            } else {
                tr.draw(line, x, y, color, false, matrix, vertexConsumers,
                        TextRenderer.TextLayerType.NORMAL, 0, lightValue);
            }
        }

        matrices.pop();
    }
}