package org.figuramc.figura.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("swingState")
    LivingEntity.SwingState figura$getSwingState();

    @Invoker("getModifiedSwingDuration")
    int figura$getModifiedSwingDuration(net.minecraft.world.item.component.SwingAnimation animation);

    @Intrinsic
    @Invoker("updateWalkAnimation")
    void invokeUpdateWalkAnimation(float distance);
}
