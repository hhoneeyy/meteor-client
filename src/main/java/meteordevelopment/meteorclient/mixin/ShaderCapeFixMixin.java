package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
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
     * Descriptor must exactly match the target method in 26.1.2:
     * Expected: (Lnet/minecraft/client/renderer/rendertype/RenderType;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V
     */
    @Inject(method = "getBuffer", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetBuffer(RenderType layer, CallbackInfoReturnable<VertexConsumer> cir) {
        if (layer != null && layer.toString().toLowerCase().contains("cape")) {
            try {
                // Search for the internal buffer provider using reflection to avoid field mapping errors
                for (Field field : this.getClass().getDeclaredFields()) {
                    if (MultiBufferSource.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        MultiBufferSource innerProvider = (MultiBufferSource) field.get(this);
                        
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