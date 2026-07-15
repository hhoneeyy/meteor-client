package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class AntiItemDestroy extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delayTicks = sgGeneral.add(new IntSetting.Builder()
        .name("delay-ticks")
        .description("How many ticks to block interaction after a kill.")
        .defaultValue(30)
        .min(0)
        .build()
    );

    private int blockTimer = 0;

    public AntiItemDestroy() {
        super(Categories.Combat, "anti-item-destroy", "Prevents blowing up loot after a kill.");
    }

    @Override
    public void onActivate() {
        blockTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (blockTimer > 0) blockTimer--;

        if (mc.world == null || mc.player == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && !player.isAlive() && mc.player.distanceTo(player) < 8) {
                blockTimer = delayTicks.get();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            // Status 3 = entity death
            if (packet.getStatus() == 3) {
                Entity entity = packet.getEntity(mc.world);
                if (entity instanceof PlayerEntity && entity != mc.player) {
                    if (mc.player.distanceTo(entity) < 10) {
                        blockTimer = delayTicks.get();
                        info("Target killed. Protection active.");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSendPacket(PacketEvent.Send event) {
        if (blockTimer <= 0) return;

        // Block PLACING (Crystals/Anchors use PlayerInteractBlockC2SPacket)
        if (event.packet instanceof PlayerInteractBlockC2SPacket packet) {
            ItemStack stack = mc.player.getStackInHand(packet.getHand());
            if (stack.isOf(Items.END_CRYSTAL) || stack.isOf(Items.RESPAWN_ANCHOR) || stack.isOf(Items.GLOWSTONE)) {
                event.cancel();
            }
        }

        // Block BREAKING (PlayerInteractEntityC2SPacket handles entity attacks)
        if (event.packet instanceof PlayerInteractEntityC2SPacket) {
            event.cancel();
        }
    }
}
