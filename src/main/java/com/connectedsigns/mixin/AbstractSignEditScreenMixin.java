package com.connectedsigns.mixin;

import com.connectedsigns.SignGroup;
import com.connectedsigns.client.MultiSignEditScreen;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractSignEditScreen.class)
public abstract class AbstractSignEditScreenMixin {

    @Unique
    private BlockPos connectedSigns$signPos;
    @Unique
    private boolean connectedSigns$initCancelled = false;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void captureSignPos(SignBlockEntity sign, boolean front, boolean filtered, CallbackInfo ci) {
        this.connectedSigns$signPos = sign.getPos();
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void replaceWithCustomScreen(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || connectedSigns$signPos == null) return;

        ci.cancel();
        connectedSigns$initCancelled = true;

        if (client.player != null) {
            var held = client.player.getMainHandStack().getItem();
            if (held instanceof DyeItem || held == Items.GLOW_INK_SAC || held == Items.INK_SAC) {
                client.setScreen(null);
                return;
            }
        }

        List<BlockPos> group = SignGroup.findConnectedSigns(client.world, connectedSigns$signPos);
        client.setScreen(new MultiSignEditScreen(group, client.world, connectedSigns$signPos));
    }

    @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
    private void safeRemoved(CallbackInfo ci) {
        if (connectedSigns$initCancelled) {
            ci.cancel();
        }
    }
}