package me.wurgo.antiresourcereload.mixin;

import me.wurgo.antiresourcereload.AntiResourceReload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.command.CommandManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Unique
    private boolean hasLoadedTags;

    @Redirect(
            method = "createIntegratedResourceManager",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resource/ServerResourceManager;reload(Ljava/util/List;Lnet/minecraft/server/command/CommandManager$RegistrationEnvironment;ILjava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
            )
    )
    private CompletableFuture<ServerResourceManager> antiresourcereload_cachedReload(List<ResourcePack> dataPacks, CommandManager.RegistrationEnvironment registrationEnvironment, int i, Executor executor, Executor executor2) throws ExecutionException, InterruptedException {
        if (dataPacks.size() != 1) { AntiResourceReload.log("Using data-packs, reloading."); }
        else if (AntiResourceReload.cache == null) { AntiResourceReload.log("Cached resources unavailable, reloading & caching."); }
        else {
            AntiResourceReload.log("Using cached server resources.");
            if (AntiResourceReload.hasSeenRecipes) {
                ((RecipeManagerAccess) AntiResourceReload.cache.get().getRecipeManager()).invokeApply(AntiResourceReload.recipes, null, null);
            }
            AntiResourceReload.hasSeenRecipes = false;
            return AntiResourceReload.cache;
        }

        CompletableFuture<ServerResourceManager> reloaded = ServerResourceManager.reload(dataPacks, registrationEnvironment, i, executor, executor2);
        
        if (dataPacks.size() == 1) { AntiResourceReload.cache = reloaded; }

        return reloaded;
    }

    @Redirect(
            method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resource/ServerResourceManager;loadRegistryTags()V"
            )
    )
    private void antiresourcereload_skipLoad(ServerResourceManager manager) throws ExecutionException, InterruptedException {
        if (AntiResourceReload.cache != null && manager == AntiResourceReload.cache.get()) {
            if (hasLoadedTags) return;
            hasLoadedTags = true;
        }
        manager.loadRegistryTags();
    }
}
