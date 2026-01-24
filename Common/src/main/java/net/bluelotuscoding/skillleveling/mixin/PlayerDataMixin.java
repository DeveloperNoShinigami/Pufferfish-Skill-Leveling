package net.bluelotuscoding.skillleveling.mixin;

import net.puffish.skillsmod.server.data.PlayerData;
import net.puffish.skillsmod.server.data.CategoryData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;
import net.minecraft.util.Identifier;

import net.bluelotuscoding.skillleveling.mixin_interface.PlayerDataExtension;
import net.bluelotuscoding.skillleveling.mixin_interface.CategoryDataExtension;

@Mixin(value = PlayerData.class, remap = false)
public abstract class PlayerDataMixin implements PlayerDataExtension {

    @Shadow
    private Map<Identifier, CategoryData> categories;

    private ServerPlayerEntity addon$owner;

    @Override
    public void addon$setOwner(ServerPlayerEntity player) {
        this.addon$owner = player;
        // Also set owner for existing categories
        if (categories != null) {
            for (CategoryData data : categories.values()) {
                ((CategoryDataExtension) (Object) data).addon$setOwner(player);
            }
        }
    }

    @Override
    public Object addon$getCategoryData(Identifier categoryId) {
        return categories.get(categoryId);
    }

    @Inject(method = "getOrCreateCategoryData", at = @At("RETURN"))
    private void onGetOrCreateCategoryData(net.puffish.skillsmod.config.CategoryConfig category,
            CallbackInfoReturnable<CategoryData> cir) {
        CategoryData categoryData = cir.getReturnValue();
        if (categoryData instanceof CategoryDataExtension ext) {
            if (addon$owner != null) {
                ext.addon$setOwner(addon$owner);
            }
            ext.addon$setCategoryId(category.id());
        }
    }
}
