package com.dragonminez.mixin.client;

import com.dragonminez.Env;
import com.dragonminez.LogUtil;
import com.dragonminez.Reference;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** DMZ's optional post effects must not invoke vanilla's world-closing shader recovery. */
@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {
    @Shadow @Final private TextureManager textureManager;
    @Shadow @Final private Projection postChainProjection;
    @Shadow @Final private ProjectionMatrixBuffer postChainProjectionMatrixBuffer;

    @Unique
    private ShaderManager.Configs dmz$loadedConfigs = ShaderManager.Configs.EMPTY;
    @Unique
    private final Map<Identifier, Optional<PostChain>> dmz$optionalPostChains = new HashMap<>();

    @Inject(method = "getPostChain", at = @At("HEAD"), cancellable = true)
    private void dmz$loadOptionalPostEffect(Identifier id, Set<Identifier> allowedTargets,
            CallbackInfoReturnable<PostChain> cir) {
        if (!Reference.MOD_ID.equals(id.getNamespace())) return;
        // getPostChain catches even a missing id by starting resource recovery. On a LAN
        // host that closes the integrated world and disconnects every player. Load only
        // our optional effects outside that path; vanilla/other mods retain their behavior.
        Optional<PostChain> cached = dmz$optionalPostChains.get(id);
        if (cached == null) {
            cached = dmz$compileOptionalPostEffect(id, allowedTargets);
            dmz$optionalPostChains.put(id, cached);
        }
        cir.setReturnValue(cached.orElse(null));
    }

    @Unique
    private Optional<PostChain> dmz$compileOptionalPostEffect(Identifier id, Set<Identifier> allowedTargets) {
        PostChainConfig config = dmz$loadedConfigs.postChains().get(id);
        if (config == null) {
            LogUtil.warn(Env.CLIENT, "Skipping missing optional post effect {}", id);
            return Optional.empty();
        }
        try {
            return Optional.of(PostChain.load(config, textureManager, allowedTargets, id,
                    postChainProjection, postChainProjectionMatrixBuffer));
        } catch (Exception exception) {
            LogUtil.warn(Env.CLIENT, "Skipping invalid optional post effect {}: {}", id, exception.getMessage());
            return Optional.empty();
        }
    }

    @Inject(method = "apply(Lnet/minecraft/client/renderer/ShaderManager$Configs;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("TAIL"))
    private void dmz$reloadOptionalPostEffects(ShaderManager.Configs configs, ResourceManager manager,
            ProfilerFiller profiler, CallbackInfo ci) {
        dmz$closeOptionalPostEffects();
        dmz$loadedConfigs = configs;
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void dmz$closeOptionalPostEffects(CallbackInfo ci) {
        dmz$closeOptionalPostEffects();
    }

    @Unique
    private void dmz$closeOptionalPostEffects() {
        for (Optional<PostChain> chain : dmz$optionalPostChains.values()) chain.ifPresent(PostChain::close);
        dmz$optionalPostChains.clear();
        dmz$loadedConfigs = ShaderManager.Configs.EMPTY;
    }
}
