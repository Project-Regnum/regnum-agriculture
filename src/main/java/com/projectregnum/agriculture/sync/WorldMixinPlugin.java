package com.projectregnum.agriculture.sync;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

/** Checks bytecode without loading Minecraft or optional World classes during mixin discovery. */
public final class WorldMixinPlugin implements IMixinConfigPlugin {
    private static boolean replayHookCompatible;

    public static boolean isReplayHookCompatible() { return replayHookCompatible; }
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.endsWith(".WorldGrowthReplayMixin")) return true;
        try {
            var provider = MixinService.getService().getBytecodeProvider();
            ClassNode mixin = provider.getClassNode(mixinClassName);
            MethodNode hook = mixin.methods.stream()
                    .filter(method -> method.name.equals("agriculture$originalTick")).findFirst().orElseThrow();
            // The handler adds one CallbackInfo argument to the target's signature.
            String expected = hook.desc.replaceFirst(
                    "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;\\)V$", ")V");
            replayHookCompatible = provider.getClassNode(targetClassName).methods.stream()
                    .anyMatch(method -> method.name.equals("handleRandomTick") && method.desc.equals(expected));
            return replayHookCompatible;
        } catch (Exception e) {
            // init() validates the same contract and exposes failure with growth blocked.
            return false;
        }
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
}
