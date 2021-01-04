package dev.lambdacraft.status_provider.Mixins;

import dev.lambdacraft.status_provider.StatusMain;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Inject(at = @At("HEAD"), method = "sendMessage")
    public void sendMessage(Text text, CallbackInfo ci) {
        StatusMain.status.ProcessChatMessage(text);
    }
}
