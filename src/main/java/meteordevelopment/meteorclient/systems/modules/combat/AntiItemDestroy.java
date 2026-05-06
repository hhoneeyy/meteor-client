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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;

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
        super(Categories.Combat, "anti-item-destroy", "Prevents blowing up loot on version 26.1.2.");
    }

    @Override
    public void onActivate() {
        blockTimer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (blockTimer > 0) blockTimer--;

        // In 26.1.2, mc.level replaces mc.world
        if (mc.level == null || mc.player == null) return;

        for (Player player : mc.level.players()) {
            if (player != mc.player && !player.isAlive() && mc.player.distanceTo(player) < 8) {
                blockTimer = delayTicks.get();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundEntityEventPacket packet) {
            // In 26.1.2, event logic for death (3) is often handled via these status packets
            if (packet.getEventId() == 3) { 
                Entity entity = packet.getEntity(mc.level);
                if (entity instanceof Player && entity != mc.player) {
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

        // Block PLACING (Crystals/Anchors use ServerboundUseItemOnPacket)
        if (event.packet instanceof ServerboundUseItemOnPacket packet) {
            ItemStack stack = mc.player.getItemInHand(packet.getHand());
            if (stack.is(Items.END_CRYSTAL) || stack.is(Items.RESPAWN_ANCHOR) || stack.is(Items.GLOWSTONE)) {
                event.cancel();
            }
        }

        // Block BREAKING (ServerboundInteractPacket handles entity attacks)
        if (event.packet instanceof ServerboundInteractPacket) {
            event.cancel();
        }
    }
}