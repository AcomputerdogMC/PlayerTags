package net.acomputerdog.playertags;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PluginPlayerTags extends JavaPlugin implements Listener {

    private static final String CLEAR_FORMATTING = ChatColor.RESET.toString() + ChatColor.WHITE.toString();
    private static final long ONE_YEAR = 60 * 60 * 24 * 365; //one year in seconds

    private ChatColor defaultColor;

    private File colorFolder;
    private File ageFolder;

    private String yearBadge;
    private String yearBadgeRaw;
    private String modBadge;
    private String modBadgeRaw;
    private String adminBadge;
    private String adminBadgeRaw;
    private boolean highestRankOnly;

    //don't reset in onEnable or onDisable
    private boolean reloading = false;

    @Override
    public void onEnable() {
        colorFolder = new File(getDataFolder(), "colors");
        if (!colorFolder.isDirectory() && !colorFolder.mkdirs()) {
            getLogger().warning("Unable to create color directory!");
        }
        ageFolder = new File(getDataFolder(), "age");
        if (!ageFolder.isDirectory() && !ageFolder.mkdirs()) {
            getLogger().warning("Unable to create age directory!");
        }

        readConfiguration();

        for (Player p : getServer().getOnlinePlayers()) {
            onPlayerJoin(p);
        }

        if (!reloading) {
            getServer().getPluginManager().registerEvents(this, this);
        }
    }

    @Override
    public void onDisable() {
        colorFolder = null;
        defaultColor = null;
        ageFolder = null;
        yearBadge = null;
        yearBadgeRaw = null;
        modBadge = null;
        modBadgeRaw = null;
        adminBadge = null;
        adminBadgeRaw = null;
    }

    @EventHandler(priority =  EventPriority.MONITOR, ignoreCancelled = true)
    public void playerJoinEvent(PlayerJoinEvent e) {
        onPlayerJoin(e.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName()) {
            case "namecolor":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
                    break;
                }
                if (!sender.hasPermission("playertags.setcolor")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission!");
                    break;
                }
                if (args.length < 1) {
                    sender.sendMessage(ChatColor.RED + "Incorrect usage, use /namecolor <color1> [color2] [color...]");
                    break;
                }
                List<ChatColor> colors = new ArrayList<>(args.length);
                for (String str : args) {
                    ChatColor color = ChatColor.valueOf(str.toUpperCase());
                    if (color.isFormat() && !sender.hasPermission("playertags.setstyle")) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission for styles; skipping: " + str);
                    } else {
                        colors.add(color);
                    }
                }
                Player p = (Player)sender;
                ChatColor[] colorArray = colors.toArray(new ChatColor[colors.size()]);
                setPlayerColor(p, colorArray);
                applyTags(p, colorsToString(colorArray), getBadgesFor(p), getRawBadgesFor(p));
                sender.sendMessage(ChatColor.AQUA + "Color set.");
                break;
            case "playertagsreload":
                reloading = true;
                reloadConfig();
                onDisable();
                onEnable();
                reloading = false;
                sender.sendMessage(ChatColor.YELLOW + "Reloaded.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown command!");
                break;
        }
        return true;
    }

    private void readConfiguration() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        defaultColor = ChatColor.valueOf(getConfig().getString("default_color"));
        yearBadge = getConfig().getString("year_badge");
        yearBadgeRaw = getConfig().getString("year_badge_raw");
        modBadge = getConfig().getString("mod_badge");
        modBadgeRaw = getConfig().getString("mod_badge_raw");
        adminBadge = getConfig().getString("admin_badge");
        adminBadgeRaw = getConfig().getString("admin_badge_raw");
        highestRankOnly = getConfig().getBoolean("highest_rank_only");
    }

    private void onPlayerJoin(Player p) {
        getPlayerAge(p); //read the player's age, or start counting if necessary.
        String colorString = colorsToString(getPlayerColors(p));

        applyTags(p, colorString, getBadgesFor(p), getRawBadgesFor(p));
    }

    private void applyTags(Player p, String color, String badges, String rawBadges) {
        String listName = rawBadges + color + p.getName();
        if (listName.length() <= 16) {
            p.setPlayerListName(listName);
        } else {
            getLogger().warning("Player \"" + p.getName() + "\"'s name is too long!");
        }

        String displayName = badges + CLEAR_FORMATTING + color + p.getName() + CLEAR_FORMATTING;
        p.setDisplayName(displayName);
    }

    private String getBadgesFor(Player p) {
        return buildBadges(p, yearBadge, modBadge, adminBadge);
    }

    private String getRawBadgesFor(Player p) {
        return buildBadges(p, yearBadgeRaw, modBadgeRaw, adminBadgeRaw);
    }

    private String buildBadges(Player p, String year, String mod, String admin) {
        StringBuilder builder = new StringBuilder();
        if (highestRankOnly) {
            if (allowAdminBadge(p)) {
                builder.append(admin);
            } else if (allowModBadge(p)) {
                builder.append(mod);
            } else if (allowYearBadge(p)) {
                builder.append(year);
            }
        } else {
            if (allowYearBadge(p)) {
                builder.append(year);
            }
            if (allowModBadge(p)) {
                builder.append(mod);
            }
            if (allowAdminBadge(p)) {
                builder.append(admin);
            }
        }
        return builder.toString();
    }

    private long unixTime() {
        return System.currentTimeMillis() / 1000L;
    }

    private boolean allowAdminBadge(Player p) {
        return p.hasPermission("playertags.isadmin");
    }

    private boolean allowModBadge(Player p) {
        return p.hasPermission("playertags.ismod");
    }

    private boolean allowYearBadge(Player p) {
        return isYearOld(p) || p.hasPermission("playertags.forceyear");
    }

    private boolean isYearOld(Player p) {
        return unixTime() - getPlayerAge(p) >= ONE_YEAR;
    }

    private String colorsToString(ChatColor[] colors) {
        StringBuilder builder = new StringBuilder(colors.length);
        for (ChatColor color : colors) {
            builder.append(color.toString());
        }
        return builder.toString();
    }

    private ChatColor[] getPlayerColors(Player p) {
        File file = getColorFileFor(p);
        List<ChatColor> colors = new ArrayList<>();
        if (file.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                while (reader.ready()) {
                    String line = reader.readLine();
                    try {
                        colors.add(ChatColor.valueOf(line.toUpperCase()));
                        //return ChatColor.valueOf(line.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Corrupt color: " + line);
                        //return defaultColor;
                    }
                }
            } catch (IOException e) {
                getLogger().severe("Exception reading player color for: " + p.getUniqueId().toString());
                e.printStackTrace();
                //return defaultColor;
            }
        } else {
            setPlayerColor(p, defaultColor);
            //return defaultColor;
        }
        if (colors.isEmpty()) {
            colors.add(defaultColor);
        }
        return colors.toArray(new ChatColor[colors.size()]);
    }

    private long getPlayerAge(Player p) {
        File file = getAgeFileFor(p);
        if (file.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                try {
                    return Long.parseLong(line);
                } catch (NumberFormatException e) {
                    getLogger().warning("Corrupt age: " + line);
                    return unixTime();
                }
            } catch (IOException e) {
                getLogger().severe("Exception reading player color for: " + p.getUniqueId().toString());
                e.printStackTrace();
                return unixTime();
            }
        } else {
            setPlayerAge(p, unixTime());
            //savePlayerAge(p);
            return unixTime();
        }
    }

    private void savePlayerAge(Player p) {
        File file = getAgeFileFor(p);
        try (Writer writer = new FileWriter(file)) {
            writer.write(String.valueOf(unixTime()) + "\n");
        } catch (IOException e) {
            getLogger().warning("Unable to save age for player: " + p.getName());
            e.printStackTrace();
        }
    }

    private void setPlayerColor(Player p, ChatColor... colors) {
        try (Writer writer = new FileWriter(getColorFileFor(p))) {
            for (ChatColor color : colors) {
                writer.write(color.name() + "\n");
            }
        } catch (IOException e) {
            getLogger().severe("Exception saving player color: " + p.getUniqueId().toString());
            e.printStackTrace();
        }
    }

    private void setPlayerAge(Player p, long age) {
        try (Writer writer = new FileWriter(getAgeFileFor(p))) {
            writer.write(String.valueOf(age) + "\n");
        } catch (IOException e) {
            getLogger().severe("Exception saving player age: " + p.getUniqueId().toString());
            e.printStackTrace();
        }
    }

    private File getColorFileFor(Player p) {
        return new File(colorFolder, p.getUniqueId().toString() + ".dat");
    }

    private File getAgeFileFor(Player p) {
        return new File(ageFolder, p.getUniqueId().toString() + ".dat");
    }
}
