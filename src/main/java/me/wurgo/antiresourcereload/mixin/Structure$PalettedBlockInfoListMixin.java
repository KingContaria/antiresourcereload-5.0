package me.wurgo.antiresourcereload.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.Map;

@Mixin(Structure.PalettedBlockInfoList.class)
public abstract class Structure$PalettedBlockInfoListMixin {

    @ModifyExpressionValue(
            method = "<init>*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;"
            )
    )
    private Map<?, ?> synchronizeBlockToInfosMap(Map<?, ?> blockToInfos) {
        return Collections.synchronizedMap(blockToInfos);
    }
}
