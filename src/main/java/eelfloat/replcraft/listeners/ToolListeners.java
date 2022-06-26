package eelfloat.replcraft.listeners;

import eelfloat.replcraft.*;
import eelfloat.replcraft.net.ClientV2;
import eelfloat.replcraft.net.StructureContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ToolListeners implements Listener {
    private void trigger(Player player, ItemStack stack, int x, int y, int z, int range, String reason) {
        ItemMeta itemMeta = stack.getItemMeta();
        if (itemMeta == null) return;

        List<String> lore = itemMeta.getLore();
        if (lore == null) return;

        ReplCraft.plugin.logger.info(String.format(
            "trigger %s %s %s %s %s %s %s %s",
            player.getName(), stack, x, y, z, range, reason, lore
        ));

        lore.stream()
            .filter(str -> str.startsWith("Replized: "))
            .flatMap(str -> {
                return ReplCraft.plugin.websocketServer.clients.values().stream()
                    .filter(client -> client instanceof ClientV2)
                    .flatMap(client -> ((ClientV2) client).items.stream())
                    .filter(item -> {
                        ReplCraft.plugin.logger.info(String.format("Checking %s against %s", str, item.getItemID()));
                        return str.contains(item.getItemID());
                    });
            })
            .forEach(itemCtx -> {
                Optional<StructureMaterial> material = ReplCraft.plugin.frame_materials.stream()
                    .filter(mats -> (
                        mats.validMaterials.stream().anyMatch(mat -> stack.getType() == mat) &&
                        mats.type == StructureType.Item
                    ))
                    .findFirst();
                if (!material.isPresent()) return;
                Structure structure = new ItemVirtualStructure(
                    player,
                    stack,
                    material.get(),
                    null,
                    null,
                    itemCtx.getUsername(),
                    itemCtx.getUUID(),
                    player.getWorld(),
                    x - range,
                    y - range,
                    z - range,
                    x + range,
                    y + range,
                    z + range
                );
                ReplCraft.plugin.logger.info("Creating context");
                StructureContext context = itemCtx.client.createContext(structure, null, reason);
                Bukkit.getScheduler().runTaskLater(ReplCraft.plugin, () -> {
                    ReplCraft.plugin.logger.info("Disposing context");
                    itemCtx.client.disposeContext(context.id);
                }, 20 * 10);
            });
    }

    @EventHandler
    public void onPlayerHoldReplizedItem(PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (item == null) return;

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return;

        List<String> lore = itemMeta.getLore();
        if (lore == null) return;

        for (String str: lore) {
            if (!str.startsWith("Replized:")) continue;

            String[] split = str.split(" ");
            if (split.length < 3) continue;

            UUID uuid = UUID.fromString(split[2]);
            if (uuid.equals(event.getPlayer().getUniqueId())) continue;

            event.getPlayer().sendMessage(String.format(
                "%sWarning: This is a replized tool owned by %s%s%s%s%s. It can interact with your " +
                "surroundings and inventory if used. Only use the tool if you trust its author. This " +
                "includes right-clicking blocks and in the air.",
                ChatColor.GOLD,
                ChatColor.RED,
                ChatColor.BOLD,
                Bukkit.getServer().getOfflinePlayer(uuid).getName(),
                ChatColor.RESET,
                ChatColor.GOLD
            ));
        }
    }

    @EventHandler
    public void onPlayerDamageEntity(EntityDamageByEntityEvent evt) {
        if (!(evt.getDamager() instanceof Player)) return;
        Player player = (Player) evt.getDamager();
        ItemStack stack = player.getInventory().getItemInMainHand();
        Location location = evt.getEntity().getLocation();
        trigger(player, stack, location.getBlockX(), location.getBlockY(), location.getBlockZ(), 5, "itemAttack");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent evt) {
        Player player = evt.getPlayer();
        ItemStack stack = player.getInventory().getItemInMainHand();
        Block block = evt.getBlock();
        trigger(player, stack, block.getX(), block.getY(), block.getZ(), 5, "itemBreakBlock");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent evt) {
        Player player = evt.getPlayer();
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = evt.getClickedBlock();
            trigger(player, stack, block.getX(), block.getY(), block.getZ(), 5, "itemInteractBlock");
        }
        if (evt.getAction() == Action.RIGHT_CLICK_AIR) {
            Location location = player.getLocation();
            trigger(player, stack, location.getBlockX(), location.getBlockY(), location.getBlockZ(), 5, "itemInteractAir");
        }
    }
}