package com.black.minecraft.b0rain.rain;

import com.black.minecraft.b0rain.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import com.black.minecraft.b0rain.B0rain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RainEffectManager {
    private final B0rain plugin;
    private final ConfigManager configManager;
    private BukkitRunnable task;
    private final Set<UUID> playersWithPluginEffect;
    private final PotionEffectType slownessType;
    private final PotionEffect slownessEffect;

    public RainEffectManager(B0rain plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playersWithPluginEffect = new HashSet<>();
        this.slownessType = PotionEffectHelper.getSlownessType();
        this.slownessEffect = PotionEffectHelper.createSlownessEffect(
                configManager.getSlownessDurationTicks(),
                configManager.getSlownessLevel()
        );
        
        if (slownessType == null) {
            plugin.getLogger().severe("Failed to load SLOWNESS potion effect type! Plugin may not work correctly.");
        }
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (slownessType == null || slownessEffect == null) {
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!configManager.isWorldEnabled(player.getWorld().getName())) {
                        continue;
                    }

                    UUID playerId = player.getUniqueId();
                    boolean inRain = RainChecker.isInRain(
                            player,
                            configManager.isCheckThunder(),
                            configManager.isCheckSnow(),
                            configManager.getIgnoredBiomes()
                    );

                    if (inRain) {
                        player.addPotionEffect(slownessEffect);
                        playersWithPluginEffect.add(playerId);
                    } else {
                        if (playersWithPluginEffect.contains(playerId)) {
                            player.removePotionEffect(slownessType);
                            playersWithPluginEffect.remove(playerId);
                        }
                    }
                }
            }
        };

        task.runTaskTimer(plugin, 0L, configManager.getCheckIntervalTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (slownessType != null) {
            for (UUID playerId : new HashSet<>(playersWithPluginEffect)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    player.removePotionEffect(slownessType);
                }
            }
            playersWithPluginEffect.clear();
        }
    }
}
