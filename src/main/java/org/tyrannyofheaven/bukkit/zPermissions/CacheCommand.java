package org.tyrannyofheaven.bukkit.zPermissions;

import static org.tyrannyofheaven.bukkit.util.ToHUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHUtils.sendMessage;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;

import com.avaje.ebean.cache.ServerCache;
import com.avaje.ebean.cache.ServerCacheOptions;
import com.avaje.ebean.cache.ServerCacheStatistics;

/**
 * Handler for cache commands. Avaje DAO-specific.
 * 
 * @author zerothangel
 */
public class CacheCommand implements Runnable {

    private static final int DEFAULT_MONITOR_INTERVAL = 15;

    private static final int TICKS_PER_SECOND = 20;
    
    private final ZPermissionsPlugin plugin;
    
    CacheCommand(ZPermissionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Command(value={"monitor", "mon"}, description="Start/stop periodic cache statistics logging")
    public void monitor(Server server, CommandSender sender, @Option(value="enable", optional=true) Boolean enable, @Option(value={"-i", "--interval"}, valueName="interval") Integer interval) {
        // Defaults
        if (enable == null)
            enable = Boolean.FALSE;
        if (interval == null)
            interval = DEFAULT_MONITOR_INTERVAL;

        server.getScheduler().cancelTasks(plugin);
        if (enable)
            server.getScheduler().scheduleAsyncRepeatingTask(plugin, this, 1 * TICKS_PER_SECOND, interval * TICKS_PER_SECOND);
        sendMessage(sender, colorize("{YELLOW}Cache monitor %s%s{YELLOW}%s."),
                (enable ? ChatColor.GREEN : ChatColor.RED),
                (enable ? "enabled" : "disabled"),
                (enable ? String.format(" (interval: %d second%s)",
                        interval,
                        (interval == 1 ? "" : "s")) : ""));
    }
    
    @Command(value="clear", description="Clear cache")
    public void clear(CommandSender sender) {
        plugin.getDatabase().getServerCacheManager().clearAll();
        sendMessage(sender, colorize("{YELLOW}Cache cleared."));
    }

    @Override
    public void run() {
        plugin.log("Cache statistics:");
        for (Class<?> clazz : plugin.getDatabaseClasses()) {
            ServerCache beanCache = plugin.getDatabase().getServerCacheManager().getBeanCache(clazz);
            ServerCacheOptions beanCacheOptions = beanCache.getOptions();
            ServerCacheStatistics beanCacheStats = beanCache.getStatistics(false);
            plugin.log("  %s:", clazz.getName());
            plugin.debug("    (max-idle=%d, max-ttl=%d, max-size=%d)", beanCacheOptions.getMaxIdleSecs(), beanCacheOptions.getMaxSecsToLive(), beanCacheOptions.getMaxSize());
            plugin.log("    size=%d, hits=%d, misses=%d, ratio=%d", beanCacheStats.getSize(), beanCacheStats.getHitCount(), beanCacheStats.getMissCount(), beanCacheStats.getHitRatio());
        }
    }

}
