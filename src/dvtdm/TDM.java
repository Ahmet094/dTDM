package dvtdm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class TDM extends JavaPlugin implements Listener {

    HashMap<String, Integer> lobby = new HashMap();
    HashMap<String, Integer> rot = new HashMap();
    HashMap<String, Integer> blau = new HashMap();
    HashMap<String, Integer> imTeam = new HashMap();
    HashMap<String, String> hasKit = new HashMap();
    FileConfiguration conf;
    int killsRed;
    int killsBlue;
    int timer;
    int killsNeeded;
    boolean gameRunning = false;
    int bonusPoints;
    public static File configFile;
    public FileConfiguration playersFile;

    @Override
    public void onEnable() {
        try {
            conf = this.getConfig();
            conf.options().copyDefaults(true);
            conf.addDefault("Players", "placeHolder");
            conf.addDefault("Players", "placeHolder");
            saveConfig();
            this.getServer().getPluginManager().registerEvents(this, this);
            timer = conf.getInt("Options.timerTillGamestart");
            killsNeeded = conf.getInt("Options.killsNeededToWin");
            bonusPoints = conf.getInt("Options.bonusPointsForWinners");


            configFile = new File(getDataFolder(), "players.yml");
            configFile.createNewFile();
            playersFile = new YamlConfiguration();

        } catch (IOException ex) {
            Logger.getLogger(TDM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (args.length == 0) {
            }
            if (args.length == 1) {
                if (label.equalsIgnoreCase("setlobby")) {
                    if (args[0].equalsIgnoreCase("red")) {
                        addLocToConfig(p.getLocation(), "LobbyRed");
                        p.sendMessage(ChatColor.DARK_GREEN + "Lobby for " + ChatColor.DARK_RED + "[Team Red] " + ChatColor.DARK_GREEN + "was successfully set!");
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("blue")) {
                        addLocToConfig(p.getLocation(), "LobbyBlue");
                        p.sendMessage(ChatColor.DARK_GREEN + "Lobby for " + ChatColor.DARK_BLUE + "[Team Blue] " + ChatColor.DARK_GREEN + "was successfully set!");
                        return true;
                    }
                }
                if (label.equalsIgnoreCase("setspawn")) {
                    if (args[0].equalsIgnoreCase("login")) {
                        addLocToConfig(p.getLocation(), "spawnLogin");
                        p.sendMessage(ChatColor.DARK_GREEN + "Login-Lobby was successfully definäd!");
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("red")) {
                        addLocToConfig(p.getLocation(), "spawnRed");
                        p.sendMessage(ChatColor.DARK_GREEN + "Spawn location for " + ChatColor.DARK_RED + "[Team Red] " + ChatColor.DARK_GREEN + "was defined!");
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("blue")) {
                        addLocToConfig(p.getLocation(), "spawnBlue");
                        p.sendMessage(ChatColor.DARK_GREEN + "Spawn location for " + ChatColor.DARK_BLUE + "[Team Blue] " + ChatColor.DARK_GREEN + "was defined!");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void addLocToConfig(Location l, String name) {
        String world = l.getWorld().getName();
        int x = l.getBlockX();
        int y = l.getBlockY();
        int z = l.getBlockZ();

        conf.set("Locations." + name + ".world", world);
        conf.set("Locations." + name + ".x", x);
        conf.set("Locations." + name + ".y", y);
        conf.set("Locations." + name + ".z", z);

        if (name == "spawnLogin") {
            l.getWorld().setSpawnLocation(x, y, z);
        }

        saveConfig();
    }

    public void portToLoc(Player p, String name) {
        World w = this.getServer().getWorld(conf.getString("Locations." + name + ".world"));
        int x = conf.getInt("Locations." + name + ".x");
        int y = conf.getInt("Locations." + name + ".y");
        int z = conf.getInt("Locations." + name + ".z");
        p.teleport(new Location(w, x, y, z));
    }

    public void assignTeam(Player p) {
        if (!imTeam.containsKey(p.getName())) {
            if (blau.size() > rot.size()) {
                rot.put(p.getName(), 0);
                p.sendMessage(ChatColor.DARK_GREEN + "You got assigned to " + ChatColor.DARK_RED + "[Team Red]");
                portToLoc(p, "LobbyRed");
            } else if (blau.size() < rot.size()) {
                blau.put(p.getName(), 0);
                p.sendMessage(ChatColor.DARK_GREEN + "You got assigned to " + ChatColor.DARK_BLUE + "[Team Blue]");
                portToLoc(p, "LobbyBlue");
            } else {
                blau.put(p.getName(), 0);
                p.sendMessage(ChatColor.DARK_GREEN + "You got assigned to " + ChatColor.DARK_BLUE + "[Team Blue]");
                portToLoc(p, "LobbyBlue");
            }
            imTeam.put(p.getName(), null);
        } else {
            p.sendMessage(ChatColor.RED + "You're already assigned to a team!");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String pName = p.getName();

        p.getInventory().clear();

        int x = 0;
        if (!conf.getConfigurationSection("Players").contains(pName)) {
            conf.set("Players." + pName + ".points", x);
            conf.set("Players." + pName + ".kills", x);
            conf.set("Players." + pName + ".deaths", x);
            conf.set("Players." + pName + ".matches", x);
            conf.set("Players." + pName + ".kits", "");
            saveConfig();
        }
        portToLoc(e.getPlayer(), "spawnLogin");
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        e.getPlayer().getInventory().clear();
    }

    @EventHandler
    public void interaktionen(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getClickedBlock().getType() == Material.STONE_BUTTON) {
                Block b = e.getClickedBlock();
                Location l = b.getLocation().add(0, 1, 0);
                Player p = e.getPlayer();
                String pName = p.getName();
                if (l.getBlock().getType() == Material.WALL_SIGN) {
                    Sign s = (Sign) b.getLocation().add(0, 1, 0).getBlock().getState();
                    if (s.getLine(1).equalsIgnoreCase("[ready to play]")) {
                        assignTeam(e.getPlayer());

                        if (gameRunning == false) {
                            if (enoughPlayers()) {
                                startGame();
                            }
                        }
                    }

                    ///////////////////////////////////////////////////////
                    if (s.getLine(0).equalsIgnoreCase("[buy kit]")) {
                        if (conf.getConfigurationSection("Classes").contains(s.getLine(2))) {
                            if (!conf.getConfigurationSection("Players").getStringList(pName + ".kits").contains(s.getLine(2))) {
                                int costs = conf.getInt("Classes." + s.getLine(2) + ".costs");
                                int points = conf.getInt("Players." + pName + ".points");
                                if (points >= costs) {
                                    int cash = points - costs;
                                    conf.set("Players." + pName + ".points", cash);
                                    List list = conf.getConfigurationSection("Players").getStringList(pName + ".kits");
                                    list.add(s.getLine(2));
                                    conf.set("Players." + pName + ".kits", list);
                                    p.sendMessage(ChatColor.GREEN + "You just purchased " + ChatColor.GOLD + s.getLine(2) + ChatColor.GREEN + "!");
                                    saveConfig();
                                } else {
                                    p.sendMessage(ChatColor.RED + "You don't have enough points to purchase " + ChatColor.GOLD + s.getLine(2) + ChatColor.RED + "!");
                                }
                            } else {
                                p.sendMessage(ChatColor.RED + "You already own the " + ChatColor.GOLD + s.getLine(2) + ChatColor.RED + " class!");
                            }
                        } else {
                            p.sendMessage(ChatColor.RED + "The " + ChatColor.GOLD + s.getLine(2) + ChatColor.RED + " class isn't defined in the config!");
                        }
                    }
                    //////////////////////////////////////////////////////
                    if ((!s.getLine(1).equalsIgnoreCase("[ready to play]")) && (!s.getLine(0).equalsIgnoreCase("[buy kit]"))) {
                        if (conf.getConfigurationSection("Classes").contains(s.getLine(1))) {
                            if (conf.getConfigurationSection("Players").getStringList(pName + ".kits").contains(s.getLine(1))) {
                                p.getInventory().clear();

                                try {
                                    String items = conf.getString("Classes." + s.getLine(1) + ".items");
                                    String[] indiItems = items.split(",");

                                    for (String s1 : indiItems) {
                                        String[] itemAmounts = s1.split("-");
                                        ItemStack item = new ItemStack(
                                                Integer.valueOf(itemAmounts[0]),
                                                Integer.valueOf(itemAmounts[1]));
                                        p.getInventory().addItem(item);
                                    }
                                } catch (Exception exc) {
                                    exc.printStackTrace();
                                }
                                p.updateInventory();


                                p.sendMessage(ChatColor.GREEN + "You selected " + ChatColor.GOLD + s.getLine(1) + ChatColor.GREEN + "!");
                                hasKit.put(p.getName(), s.getLine(1));
                                if (gameRunning) {
                                    if (blau.containsKey(pName)) {
                                        portToLoc(p, "spawnBlue");
                                    }
                                    if (rot.containsKey(pName)) {
                                        portToLoc(p, "spawnRed");
                                    }
                                }
                            } else {
                                p.sendMessage(ChatColor.RED + "You have to buy " + ChatColor.GOLD + s.getLine(1) + ChatColor.RED + " first!");
                            }
                        } else {
                            p.sendMessage(ChatColor.RED + "This class isn't defined in the config!");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void deaddd(PlayerDeathEvent e) {
        Player p = (Player) e.getEntity();
        String pName = p.getName();

        if (p.getKiller() instanceof Player) {
            String kName = p.getKiller().getName();
            if (blau.containsKey(p.getName())) {
                announce(ChatColor.BLUE + pName + ChatColor.YELLOW + " got killed by " + ChatColor.RED + kName + ChatColor.YELLOW + "!");
            }
            if (rot.containsKey(p.getName())) {
                announce(ChatColor.RED + pName + ChatColor.YELLOW + " got killed by " + ChatColor.BLUE + kName + ChatColor.YELLOW + "!");
            }
            Player k = (Player) p.getKiller();

            conf.set("Players." + k.getName() + ".points", conf.getInt("Players." + k.getName() + ".points") + 100);

            conf.set("Players." + k.getName() + ".kills", conf.getInt("Players." + k.getName() + ".kills") + 1);

            k.setLevel(conf.getInt("Players." + k.getName() + ".points"));
        }

        if (blau.containsKey(p.getName())) {
            killsRed++;
        }
        if (rot.containsKey(p.getName())) {
            killsBlue++;
        }

        conf.set("Players." + pName + ".deaths", conf.getInt("Players." + pName + ".deaths") + 1);

        e.setDroppedExp(0);
        p.getInventory().clear();
        e.setNewLevel(conf.getInt("Players." + pName + ".points"));
        e.setDeathMessage("");
        saveConfig();
        checkGameEnd();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        final Player p = e.getPlayer();


        this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {

            @Override
            public void run() {

                if (blau.containsKey(p.getName())) {
                    portToLoc(p, "LobbyBlue");
                }
                if (rot.containsKey(p.getName())) {
                    portToLoc(p, "LobbyRed");
                }
            }
        }, 1);
    }

    public void announce(String m) {
        this.getServer().broadcastMessage(m);
    }

    @EventHandler
    public void xpChange(PlayerExpChangeEvent e) {
        e.setAmount(0);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        e.setCancelled(true);
    }

    public boolean enoughPlayers() {
        if (imTeam.size() >= conf.getInt("Options.minimumPlayers")) {
            return true;
        } else {
            return false;
        }
    }

    private void startGame() {



        if (timer != 30) {
            announce(ChatColor.AQUA + "Game will start in " + ChatColor.GOLD + timer + ChatColor.AQUA + " seconds!");
        }

        int taskID = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {

            @Override
            public void run() {
                if (timer == 30) {
                    announce(ChatColor.AQUA + "Game will start in " + ChatColor.GOLD + "30 " + ChatColor.AQUA + "seconds!");
                }
                if (timer == 15) {
                    announce(ChatColor.AQUA + "Game will start in " + ChatColor.GOLD + "15 " + ChatColor.AQUA + "seconds!");
                }
                if ((timer <= 10) && (timer > 0)) {
                    announce(ChatColor.AQUA + "Game will start in " + ChatColor.GOLD + timer + ChatColor.AQUA + " seconds!");
                }
                timer--;
                if (timer <= 0) {
                    cancelTimer();
                    checkKits();
                    portPlayersGameSpawn();
                    gameRunning = true;
                }
            }
        }, 0, 20);


    }

    public void portPlayersGameSpawn() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (imTeam.containsKey(p.getName())) {
                if (rot.containsKey(p.getName())) {
                    portToLoc(p, "spawnRed");
                }
                if (blau.containsKey(p.getName())) {
                    portToLoc(p, "spawnBlue");
                }
            }
        }
    }

    public void cancelTimer() {
        this.getServer().getScheduler().cancelAllTasks();
    }

    public boolean checkKits() {

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (imTeam.containsKey(p.getName())) {
                if (!hasKit.containsKey(p.getName())) {
                    p.sendMessage(ChatColor.RED + "You haven't chose a kit and get a wooden sword to fight with!");
                    p.getInventory().setItemInHand(new ItemStack(Material.WOOD_SWORD, 1));
                    return true;
                }
            }
        }
        return false;
    }

    public void checkGameEnd() {

        if (killsRed == killsNeeded) {
            announce(ChatColor.AQUA + " ╔═════════════════════════════════════════");
            announce(ChatColor.AQUA + " ╠");
            announce(ChatColor.AQUA + " ╠ " + ChatColor.GOLD + " The winner of this round is " + ChatColor.DARK_RED + "[TeamRed]" + ChatColor.GOLD + "!");
            announce(ChatColor.AQUA + " ╠");
            announce(ChatColor.AQUA + " ╠                  Kills: " + ChatColor.RED + killsRed + ChatColor.GOLD + " | " + ChatColor.BLUE + killsBlue);
            announce(ChatColor.AQUA + " ╠");
            announce(ChatColor.AQUA + " ╠  " + ChatColor.DARK_RED + " [TeamRed]" + ChatColor.AQUA + " gets " + ChatColor.GOLD + bonusPoints + " points " + ChatColor.AQUA + "as reward!");
            announce(ChatColor.AQUA + " ╠");
            announce(ChatColor.AQUA + " ╚═════════════════════════════════════════");

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (rot.containsKey(p.getName())) {
                    conf.set("Players." + p.getName() + ".points", conf.getInt("Players." + p.getName() + ".points") + bonusPoints);
                }
            }
            endGame();
        }
        if (killsBlue == killsNeeded) {
            announce(ChatColor.AQUA + " ╔═════════════════════════════════════════");
            announce(ChatColor.AQUA + " ╠");
            announce(ChatColor.AQUA + " ╠ " + ChatColor.GOLD + " The winner of this round is " + ChatColor.DARK_BLUE + "[TeamBlue]" + ChatColor.GOLD + "!");
            announce(ChatColor.AQUA + " ╠");
            announce(ChatColor.AQUA + " ╠                  Kills: " + ChatColor.BLUE + killsBlue + ChatColor.GOLD + " | " + ChatColor.RED + killsRed);
            announce(ChatColor.AQUA + " ╠");
            announce(ChatColor.AQUA + " ╠  " + ChatColor.DARK_BLUE + " [TeamBlue]" + ChatColor.AQUA + " gets " + ChatColor.GOLD + bonusPoints + " points " + ChatColor.AQUA + "as reward!");
            announce(ChatColor.AQUA + " ╠");
            announce(ChatColor.AQUA + " ╚═════════════════════════════════════════");

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (blau.containsKey(p.getName())) {
                    conf.set("Players." + p.getName() + ".points", conf.getInt("Players." + p.getName() + ".points") + bonusPoints);
                }
            }
            endGame();
        }
    }

    public void endGame() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            portToLoc(p, "spawnLogin");
            p.getInventory().clear();
        }
        saveConfig();
        adjustPoints();
        this.getServer().reload();
    }

    public void adjustPoints() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            int points = conf.getInt("Players." + p.getName() + ".points");
            p.setLevel(points);
        }
    }
}
