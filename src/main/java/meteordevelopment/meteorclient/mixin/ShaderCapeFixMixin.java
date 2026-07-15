package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Pseudo
@Mixin(targets = "meteordevelopment.meteorclient.utils.render.CustomOutlineVertexConsumerProvider", remap = false)
public abstract class ShaderCapeFixMixin {

    /**
     * Yarn-mapped equivalent: VertexConsumerProvider#getBuffer(RenderLayer)
     */
    @Inject(method = "getBuffer", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetBuffer(RenderLayer layer, CallbackInfoReturnable<VertexConsumer> cir) {
        if (layer != null && layer.toString().toLowerCase().contains("cape")) {
            try {
                // Search for the internal buffer provider using reflection to avoid field mapping errors
                for (Field field : this.getClass().getDeclaredFields()) {
                    if (VertexConsumerProvider.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        VertexConsumerProvider innerProvider = (VertexConsumerProvider) field.get(this);
                        
                        if (innerProvider != null) {
                            // Redirect cape rendering to the standard world buffer
                            cir.setReturnValue(innerProvider.getBuffer(layer));
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {
                // Failsafe to prevent crashes if reflection fails
            }
        }
    }
}
