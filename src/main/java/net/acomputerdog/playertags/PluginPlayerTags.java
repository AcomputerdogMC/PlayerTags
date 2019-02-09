package net.acomputerdog.playertags;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Plugin main class
 *
 * TODO database (again wtf with the data files)
 */
public class PluginPlayerTags extends JavaPlugin implements Listener {

    private static final String CLEAR_FORMATTING = ChatColor.RESET.toString() + ChatColor.WHITE.toString();
    private static final long ONE_YEAR = 60 * 60 * 24 * 365; //one year in seconds

    private ChatColor defaultColor;

    private File colorFolder;
    private File ageFolder;

    private boolean enableYearBadge;
    private String yearBadgeChat;
    private String yearBadgeList;
    private int yearBadgePriority = 0;

    private boolean highestPriorityOnly;

    private TagManager tagManager;

    //set to true at end of onEnable
    private boolean isLoaded = false;

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

        tagManager = new TagManager();

        readConfiguration();

        //set colors for online players (in case of reload)
        for (Player p : getServer().getOnlinePlayers()) {
            onPlayerJoin(p);
        }

        if (!isLoaded) {
            getServer().getPluginManager().registerEvents(this, this);
        }

        isLoaded = true;
    }

    @Override
    public void onDisable() {
        tagManager = null;
        colorFolder = null;
        defaultColor = null;
        ageFolder = null;
        yearBadgeChat = null;
        yearBadgeList = null;
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
                    try {
                        ChatColor color = ChatColor.valueOf(str.toUpperCase());
                        if (color.isFormat() && !sender.hasPermission("playertags.setstyle")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission for styles; skipping: " + str);
                        } else {
                            colors.add(color);
                        }
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(ChatColor.RED + "Unrecognized color code: " + str);
                    }
                }
                Player p = (Player)sender;
                ChatColor[] colorArray = colors.toArray(new ChatColor[0]);
                setPlayerColor(p, colorArray);
                applyTags(p, colorsToString(colorArray), getChatBadgesFor(p), getListBadgesFor(p));
                sender.sendMessage(ChatColor.AQUA + "Color set.");
                break;
            case "playertagsreload":
                reloadConfig();
                onDisable();
                onEnable();
                sender.sendMessage(ChatColor.YELLOW + "Reloaded.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown command!");
                break;
        }
        return true;
    }

    private void readConfiguration() {
        saveDefaultConfig();

        defaultColor = ChatColor.valueOf(getConfig().getString("default_color"));
        highestPriorityOnly = getConfig().getBoolean("highest_priority_only");

        enableYearBadge = getConfig().getBoolean("year_badge.enabled");
        yearBadgeChat = getConfig().getString("year_badge.chat_tag");
        yearBadgeList = getConfig().getString("year_badge.list_tag");
        yearBadgePriority = getConfig().getInt("year_badge.priority");

        //get all immediate subkeys of "tags"
        for (String tagID : getConfig().getConfigurationSection("tags").getKeys(false)) {
            //get the config section for this tag
            ConfigurationSection section = getConfig().getConfigurationSection("tags." + tagID);
            boolean tagEnabled = section.getBoolean("enabled", true); //check if section is enabled
            if (tagEnabled) { //skip completely if "enabled" == false
                if (tagManager.containsId(tagID)) {
                    //will only print warning if duplicate AND enabled
                    getLogger().warning(() -> "Duplicate tag: \"" + tagID + "\"");
                } else {
                    //load fields or set defaults
                    int priority = section.getInt("priority", 0);
                    String chatTag = section.getString("chat_tag", "");
                    String listTag = section.getString("list_tag", "");
                    String permission = section.getString("permission", "rank." + tagID);

                    //actually create the tag
                    Tag tag = new Tag(tagID, chatTag, listTag, permission, priority);
                    tagManager.addTag(tag);
                }
            }
        }
    }

    private void onPlayerJoin(Player p) {
        getPlayerAge(p); //read the player's age, or start counting if necessary.
        String colorString = colorsToString(getPlayerColors(p));

        applyTags(p, colorString, getChatBadgesFor(p), getListBadgesFor(p));
    }

    private void applyTags(Player p, String color, String badges, String rawBadges) {
        String listName = rawBadges + color + p.getName();
        p.setPlayerListName(listName);

        String displayName = badges + CLEAR_FORMATTING + color + p.getName() + CLEAR_FORMATTING;
        p.setDisplayName(displayName);
    }

    private String getChatBadgesFor(Player p) {
        return buildBadges(p, yearBadgeChat, false);
    }

    private String getListBadgesFor(Player p) {
        return buildBadges(p, yearBadgeList, true);
    }

    private String buildBadges(Player p, String year, boolean isList) {
        StringBuilder builder = new StringBuilder();
        List<Tag> tags = tagManager.matchTags(t -> p.hasPermission(t.permission));
        if (!tags.isEmpty()) { //make sure there is at least one tag
            if (highestPriorityOnly) { //if we only want the highest priority, then grab the end of the list
                Tag highest = tags.get(tags.size() - 1); //get highest priority tag
                if (enableYearBadge && yearBadgePriority > highest.priority) { //if year is higher priority
                    builder.append(year);
                } else { //if tag is higher priority
                    appendTagString(builder, highest, isList);
                }
            } else { //we want all tags
                int lastPri = Integer.MIN_VALUE; //priority of previous tag
                for (Tag tag : tags) {
                    //check if the yearbadge priority fits here
                    boolean isYearPriority = enableYearBadge && yearBadgePriority >= lastPri && yearBadgePriority < tag.priority;
                    if (isYearPriority && allowYearBadge(p)) {
                        builder.append(year);
                    }
                    appendTagString(builder, tag, isList);
                    lastPri = tag.priority; //check last priority
                }
                if (enableYearBadge && yearBadgePriority >= lastPri) { //check if year has highest priority
                    builder.append(year); //put year in place
                }
            }
        }
        return builder.toString();
    }

    private void appendTagString(StringBuilder builder, Tag tag, boolean isList) {
        if (isList) {
            builder.append(tag.listTag);
        } else {
            builder.append(tag.chatTag);
        }
    }

    private long unixTime() {
        return System.currentTimeMillis() / 1000L;
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
                    // parse and add color
                    String colorName = reader.readLine();
                    ChatColor color = parseColor(colorName);
                    if (color != null) {
                        colors.add(color);
                    } else {
                        throw new IOException("Invalid color: " + colorName);
                    }
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "IOException reading player color for: " + p.getUniqueId().toString(), e);
            }
        } else {
            setPlayerColor(p, defaultColor);
        }
        if (colors.isEmpty()) {
            colors.add(defaultColor);
        }
        return colors.toArray(new ChatColor[0]);
    }

    private long getPlayerAge(Player p) {
        File file = getAgeFileFor(p);
        if (file.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                return Long.parseLong(reader.readLine());
            } catch (NumberFormatException e) {
                getLogger().warning("Corrupt age for: " + p.getUniqueId().toString());
                return unixTime();
            } catch (IOException e) {
                getLogger().severe("Exception reading player color for: " + p.getUniqueId().toString());
                e.printStackTrace();
                return unixTime();
            }
        } else {
            setPlayerAge(p, unixTime());
            return unixTime();
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

    private static ChatColor parseColor(String colorName) {
        if (colorName == null) {
            return null;
        }
        try {
            return ChatColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
