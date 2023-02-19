package me.neoblade298.neoresearch;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import me.neoblade298.neocore.bukkit.NeoCore;
import me.neoblade298.neocore.bukkit.info.BossInfo;
import me.neoblade298.neocore.bukkit.info.InfoAPI;
import me.neoblade298.neocore.bukkit.io.IOComponent;
import me.neoblade298.neoresearch.ResearchItem;
import me.neoblade298.neoresearch.inventories.InventoryListeners;
import me.neoblade298.neoresearch.inventories.ResearchInventory;

import com.sucy.skill.SkillAPI;
import com.sucy.skill.api.event.PlayerAttributeLoadEvent;
import com.sucy.skill.api.event.PlayerAttributeUnloadEvent;
import de.tr7zw.nbtapi.NBTItem;
import io.lumine.mythic.api.mobs.MobManager;
import io.lumine.mythic.bukkit.MythicBukkit;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Research extends JavaPlugin implements Listener, IOComponent {
	// SQL
	private static String url, user, pass;
	public static HashMap<Player, ResearchInventory> viewingInventory = new HashMap<Player, ResearchInventory>();
	private static HashMap<String, ResearchItem> researchItems;
	private static HashMap<String, ArrayList<ResearchItem>> mobMap;
	private static HashMap<String, String> displayNameMap;
	private static HashMap<UUID, PlayerStats> playerStats;
	private static HashMap<Integer, Integer> toNextLvl;
	private static HashMap<UUID, HashMap<Integer, StoredAttributes>> playerAttrs;
	private static HashMap<String, HashMap<String, Integer>> converter;
	private static ArrayList<String> enabledWorlds;
	private static HashSet<String> minibosses;
	public static Random rand;
	public static boolean debug = false;
	
	public HashMap<UUID, Long> lastSave;

	public String broadcast, levelup, discovery;

	public void onEnable() {
		Bukkit.getServer().getLogger().info("NeoResearch Enabled");
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getPluginManager().registerEvents(new InventoryListeners(this), this);
		this.getCommand("nr").setExecutor(new Commands(this));
		enabledWorlds = new ArrayList<String>();
		enabledWorlds.add("Dev");
		enabledWorlds.add("ClassPVP");
		enabledWorlds.add("Argyll");
		NeoCore.registerIOComponent(this, this, "ResearchManager");

		loadConfig();
	}

	public void onDisable() {
		org.bukkit.Bukkit.getServer().getLogger().info("NeoResearch Disabled");
		super.onDisable();
	}

	public void loadConfig() {
		File file = new File(getDataFolder(), "config.yml");
		playerStats = new HashMap<UUID, PlayerStats>();
		playerAttrs = new HashMap<UUID, HashMap<Integer, StoredAttributes>>();
		converter = new HashMap<String, HashMap<String, Integer>>();
		minibosses = new HashSet<String>();
		lastSave = new HashMap<UUID, Long>();
		rand = new Random();

		// Save config if doesn't exist
		if (!file.exists()) {
			saveResource("config.yml", false);
		}
		ConfigurationSection cfg = YamlConfiguration.loadConfiguration(file);

		// SQL
		ConfigurationSection sql = cfg.getConfigurationSection("sql");
		url = "jdbc:mysql://" + sql.getString("host") + ":" + sql.getString("port") + "/" + 
				sql.getString("db") + sql.getString("flags");
		user = sql.getString("username");
		pass = sql.getString("password");

		// General
		ConfigurationSection general = cfg.getConfigurationSection("general");
		broadcast = general.getString("research_complete_command").replaceAll("&", "§");
		levelup = general.getString("research_levelup").replaceAll("&", "§");
		discovery = general.getString("discovery");

		// Exp
		toNextLvl = new HashMap<Integer, Integer>();
		ConfigurationSection expSec = cfg.getConfigurationSection("exp");
		for (String lvl : expSec.getKeys(false)) {
			toNextLvl.put(Integer.parseInt(lvl), expSec.getInt(lvl));
		}

		// Load research items into mobMap, researchItems,
		researchItems = new HashMap<String, ResearchItem>();
		mobMap = new HashMap<String, ArrayList<ResearchItem>>();
		displayNameMap = new HashMap<String, String>();
		
		MobManager mm = MythicBukkit.inst().getMobManager();
		ConfigurationSection rItems = cfg.getConfigurationSection("research_items");

		for (String rItem : rItems.getKeys(false)) {
			try {
				ConfigurationSection rItemSec = rItems.getConfigurationSection(rItem);
				ResearchItem researchItem = new ResearchItem(rItem);
				
				// exp
				researchItem.setExp(rItemSec.getInt("exp"));
				// id
				researchItem.setId(rItemSec.getString("id"));
				
				// attributes
				researchItem.setAttrs(rItemSec.getInt("attrs"));
				
				// kill goals
				ConfigurationSection goalsSec = rItemSec.getConfigurationSection("goals");
				HashMap<String, Integer> goals = new HashMap<String, Integer>();
				for (String mob : goalsSec.getKeys(false)) {
					try {
						goals.put(mob, goalsSec.getInt(mob));
		
						// Add to mob map, research book min, display name map
						if (mobMap.containsKey(mob)) {
							mobMap.get(mob).add(researchItem);
							String mobdisplay = mob;
							if (mm.getMythicMob(mob).isPresent()) {
								 mobdisplay = mm.getMythicMob(mob).get().getDisplayName().get();
							}
							displayNameMap.put(mob, mobdisplay);
						}
						else {
							ArrayList<ResearchItem> list = new ArrayList<ResearchItem>();
							list.add(researchItem);
							mobMap.put(mob, list);
						}
					} catch (Exception e) {
						Bukkit.getLogger().log(Level.WARNING, "[NeoResearch] Failed to load mob " + mob);
						e.printStackTrace();
					}
				}
				researchItem.setGoals(goals);
				researchItems.put(researchItem.getId(), researchItem);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
		// Converter
		ConfigurationSection collections = cfg.getConfigurationSection("converter");
		for (String col : collections.getKeys(false)) {
			String perm = col.replaceAll(",", ".");
			ConfigurationSection colMobs = collections.getConfigurationSection(col);
			HashMap<String, Integer> mobValues = new HashMap<String, Integer>();
			for (String mob : colMobs.getKeys(false)) {
				int points = colMobs.getInt(mob);
				mobValues.put(mob, points);
				if (points == 15) {
					minibosses.add(mob);
				}
			}
			converter.put(perm, mobValues);
		}
	}

	@Override
	public void preloadPlayer(OfflinePlayer p, Statement stmt) {	}
	
	@Override
	public void loadPlayer(Player p, Statement stmt) {
		UUID uuid = p.getUniqueId();
		// Only load if their account doesn't exist, attempts to fix double load
		if (playerAttrs.containsKey(uuid)) {
			return;
		}

		playerAttrs.put(uuid, new HashMap<Integer, StoredAttributes>());
		Research main = this;
		int level = 5, exp = 0;
		int expectedAttrs = 0;
		HashMap<String, Integer> researchPoints = new HashMap<String, Integer>();
		HashMap<String, Integer> mobKills = new HashMap<String, Integer>();
		HashMap<String, ResearchItem> completedResearchItems = new HashMap<String, ResearchItem>();
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection con = DriverManager.getConnection(url, user, pass);
			ResultSet rs = stmt.executeQuery("SELECT * FROM research_accounts WHERE uuid = '" + uuid + "';");
			
			// Load in account info
			if (rs.next()) {
				level = rs.getInt(2);
				exp = rs.getInt(3);

				rs = stmt.executeQuery("SELECT * FROM research_points WHERE uuid = '" + uuid + "';");
				while (rs.next()) {
					researchPoints.put(rs.getString(2), rs.getInt(3));
				}

				rs = stmt.executeQuery("SELECT * FROM research_kills WHERE uuid = '" + uuid + "';");
				while (rs.next()) {
					mobKills.put(rs.getString(2), rs.getInt(3));
				}
				
				rs = stmt.executeQuery("SELECT * FROM research_completed WHERE uuid = '" + uuid + "';");
				while (rs.next()) {
					try {
						ResearchItem rItem = researchItems.get(rs.getString(2));
						expectedAttrs += rItem.getAttrs();
						completedResearchItems.put(rItem.getId(), rItem);
					}
					catch (Exception e) {
						Bukkit.getLogger().log(Level.WARNING, "Could not load research item: " + rs.getString(2));
					}
				}

				rs = stmt.executeQuery("SELECT * FROM research_attributes WHERE uuid = '" + uuid + "';");
				HashMap<Integer, StoredAttributes> pAttrs = playerAttrs.get(uuid);
				while (rs.next()) {
					StoredAttributes pAttr = pAttrs.getOrDefault(rs.getInt(4), new StoredAttributes());
					pAttr.addAttribute(rs.getString(2), rs.getInt(3));
					pAttrs.put(rs.getInt(4), pAttr);
				}
				
				for (Integer key : pAttrs.keySet()) {
					StoredAttributes pAttr = pAttrs.get(key);
					int actualAttrs = pAttr.countAttributes();
					if (debug) {
						Bukkit.getLogger().log(Level.INFO, "[NeoResearch] Loading account " + p.getName() + 
								"Account " + key + ": Expected - " + expectedAttrs + ", Actual - " + actualAttrs);
					}
					if (expectedAttrs > actualAttrs) {
						Bukkit.getLogger().log(Level.INFO, "[NeoResearch] Loading account " + p.getName() + 
								" Account " + key + ": Expected - " + expectedAttrs + ", Actual - " + actualAttrs);
						for (Entry<String, Integer> ent : pAttr.getStoredAttrs().entrySet()) {
							Bukkit.getLogger().log(Level.INFO, ent.getKey() + ": " + ent.getValue());
						}
						pAttr.addAttribute("unused", expectedAttrs - actualAttrs);
					}
					else if (expectedAttrs < actualAttrs) {
						Bukkit.getLogger().log(Level.INFO, "[NeoResearch] Loading account " + p.getName() + 
								" Account " + key + ": Expected - " + expectedAttrs + ", Actual - " + actualAttrs);
						for (Entry<String, Integer> ent : pAttr.getStoredAttrs().entrySet()) {
							Bukkit.getLogger().log(Level.INFO, ent.getKey() + ": " + ent.getValue());
						}
						pAttr.removeStoredAttributes();
						pAttr.addAttribute("unused", expectedAttrs);
					}
				}

				int acct = SkillAPI.getPlayerAccountData(p).getActiveId();
				if (pAttrs.containsKey(acct)) {
					pAttrs.get(acct).applyAttributes(p);
				}
				p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
				playerStats.put(uuid, new PlayerStats(main, level, exp, completedResearchItems, researchPoints, mobKills));
			}
			con.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			playerStats.put(uuid, new PlayerStats(main, level, exp, completedResearchItems, researchPoints, mobKills));
		}
	}
	
	@Override
	public void cleanup(Statement insert, Statement delete) {
		try {
			for (Player p : Bukkit.getOnlinePlayers()) {
				savePlayer(p, insert, delete);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public void autosavePlayer(Player p, Statement insert, Statement delete) {
		UUID uuid = p.getUniqueId();
		try {
			PlayerStats stats = playerStats.get(uuid);
			if (playerStats.containsKey(uuid)) {
	
				// Save account
				insert.addBatch("REPLACE INTO research_accounts VALUES ('" + uuid + "','" + stats.getLevel()
				+ "','" + stats.getExp() + "');");
	
				// Save research points
	
				HashMap<String, Integer> mobKills = stats.getMobKills();
				for (String mob : mobKills.keySet()) {
					insert.addBatch("REPLACE INTO research_kills values('" + uuid + "','" + mob + "'," + mobKills.get(mob) + ");");
				}
				HashMap<String, Integer> researchPoints = stats.getResearchPoints();
				for (String mob : researchPoints.keySet()) {
					insert.addBatch("REPLACE INTO research_points values('" + uuid + "','" + mob + "'," + researchPoints.get(mob) + ");");
				}
	
				for (Entry<String, ResearchItem> entry : stats.getCompletedResearchItems().entrySet()) {
					String name = entry.getValue().getId();
					insert.addBatch("REPLACE INTO research_completed values('" + uuid + "','" + name + "');");
				}
			
				// Save each account
				for (Integer key : playerAttrs.get(uuid).keySet()) {
					
					StoredAttributes pAttrs = playerAttrs.get(uuid).get(key);
					for (String attr : StoredAttributes.attrs) {
						insert.addBatch("REPLACE INTO research_attributes values('" + uuid + "','" + attr + "'," + pAttrs.getAttribute(attr) + "," +
								key + ");");
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public void savePlayer(Player p, Statement insert, Statement delete) {
		autosavePlayer(p, insert, delete);
		playerAttrs.remove(p.getUniqueId());
		playerStats.remove(p.getUniqueId());
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		if (e.getHand() != null && e.getHand().equals(EquipmentSlot.OFF_HAND)) return;
		if (!e.getAction().equals(Action.RIGHT_CLICK_AIR) && !e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			return;
		}
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock().getState() instanceof Container) {
			return;
		}
		Player p = e.getPlayer();
		ItemStack main = p.getInventory().getItemInMainHand().clone();
		main.setAmount(1);

		if (main.getType().equals(Material.BOOK) && main.hasItemMeta() && main.getItemMeta().hasCustomModelData() && 
				main.getItemMeta().getCustomModelData() == 100 && main.getItemMeta().hasLore()) {
			String[] args = main.getItemMeta().getLore().get(0).split(" ");

			// "Grants x research points for [mob display name]"
			int amount = Integer.parseInt(args[1]);
			String display = main.getItemMeta().getLore().get(1);

			if (!playerStats.containsKey(p.getUniqueId())) {
				p.sendMessage("§4[§c§lMLMC§4] §cError, player stats not found.");
				return;
			}
			NBTItem nbti = new NBTItem(main);
			String mob = nbti.getString("internalmob");
			int level = nbti.getInteger("level");
			PlayerStats pStat = playerStats.get(p.getUniqueId());
			HashMap<String, Integer> researchPoints = pStat.getResearchPoints();
			HashMap<String, Integer> mobKills = pStat.getMobKills();
			int pLevel = pStat.getLevel();
			
			if (level > pLevel) {
				p.sendMessage("§4[§c§lMLMC§4] §cYou are too low level to research this mob!");
				return;
			}
			
			// Check if the player is sufficiently high level
			int points = researchPoints.containsKey(mob) ? researchPoints.get(mob) + amount : amount;
			
			// Reveal mob in kills if not there
			if (!mobKills.containsKey(mob)) {
				mobKills.put(mob, 0);
			}
			
			researchPoints.put(mob, points);
			p.getInventory().removeItem(main);
			p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.BLOCKS, 1, 1);
			p.sendMessage("§4[§c§lMLMC§4] §7You gained §e" + amount + " §7research points for " + display + "§7!");
			checkItemCompletion(mob, p, points, display);
		}
	}

	public void checkItemCompletion(String mob, Player p, int totalPoints, String display) {
		// Check if new discovery
		PlayerStats stats = playerStats.get(p.getUniqueId());
		HashMap<String, Integer> researchPoints = stats.getResearchPoints();
		if (Research.debug) {
			Bukkit.getLogger().log(Level.INFO, "Checking item completion for: " + p.getName() + ", " + mob);
		}
		
		// Check for research goals that need it
		HashMap<String, ResearchItem> completedItems = stats.getCompletedResearchItems();
		if (mobMap.containsKey(mob)) {
			for (ResearchItem researchItem : mobMap.get(mob)) { // For each relevant research item
				if (Research.debug) {
					Bukkit.getLogger().log(Level.INFO, "- Research item: " + researchItem.getName() + ", completed: " + completedItems.containsKey(researchItem.getId()));
				}
				if (!completedItems.containsKey(researchItem.getId())) { // If the player hasn't completed it
					// Check if research goal is completed for specific mob
					HashMap<String, Integer> goals = researchItem.getGoals();
					boolean completed = true;
					if (goals.get(mob) <= totalPoints) {
						for (String rMob : goals.keySet()) { // Check every objective
							if (!researchPoints.containsKey(rMob) || researchPoints.get(rMob) < goals.get(rMob)) {
								if (Research.debug) {
									Bukkit.getLogger().log(Level.INFO, "- Failed to complete: " + rMob + ", requires: " + goals.get(rMob) + ", has: " +
											researchPoints.getOrDefault(rMob, 0));
								}
								completed = false;
								break; // Haven't completed every item
							}
						}
	
						// Completed a research item
						if (completed) {
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
									broadcast.replaceAll("%player%", p.getName()).replaceAll("%item%", researchItem.getName()));
							completedItems.put(researchItem.getId(), researchItem);
							p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1, 1);
							stats.addExp(p, researchItem.getExp());
							for (Entry<Integer, StoredAttributes> acc : playerAttrs.get(p.getUniqueId()).entrySet()) {
								acc.getValue().addAttribute("unused", 1);
							}
						}
					}
				}
			}
		}
	}

	// Strictly for taking away research points/kills
	public void checkItemDecompletion(String mob, Player p, int totalPoints) {
		// Check for research goals that need it
		PlayerStats stats = playerStats.get(p.getUniqueId());
		HashMap<String, ResearchItem> completedItems = stats.getCompletedResearchItems();
		if (mobMap.containsKey(mob)) {
			for (ResearchItem researchItem : mobMap.get(mob)) { // For each relevant research item
				if (completedItems.containsKey(researchItem.getId())) { // If the player has completed it
					// Check if research goal is completed for specific mob
					HashMap<String, Integer> goals = researchItem.getGoals();
					if (goals.get(mob) > totalPoints) {
						stats.takeExp(p, researchItem.getExp());
						completedItems.remove(researchItem.getId());
					}
				}
			}
		}
		updateBonuses(p);
	}
	
	public void giveResearchPoints(Player p, int amount, String mob, int lvl, boolean announce, String via) {
		UUID uuid = p.getUniqueId();
		if (playerStats.containsKey(uuid)) {
			PlayerStats pStats = playerStats.get(uuid);
			HashMap<String, Integer> mobKills = pStats.getMobKills();
			HashMap<String, Integer> researchPoints = pStats.getResearchPoints();
			String display = MythicBukkit.inst().getMobManager().getMythicMob(mob).get().getDisplayName().get();
			
			// New discovery
			if (!researchPoints.containsKey(mob)) {
				p.sendMessage(discovery.replaceAll("%mob%", display).replaceAll("&", "§"));
				researchPoints.put(mob, 0);
			}
			if (!mobKills.containsKey(mob)) {
				mobKills.put(mob, 0);
			}
			
			int pLevel = pStats.getLevel();
			if (pLevel >= lvl) {
				int points = researchPoints.containsKey(mob) ? researchPoints.get(mob) + amount : amount;
				researchPoints.put(mob, points);
				if (announce) {
					String msg = new String("&4[&c&lMLMC&4] &7You gained &e" + amount + " &7extra research points for " + display);
					if (via != null) {
						msg += " &7via " + via;
					}
					msg += "&7!";
					msg = msg.replaceAll("&", "§");
					p.sendMessage(msg);
				}
				String msg = display + " - §e" + points + " Research Pts";
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
				checkItemCompletion(mob, p, points, display);
			}
		}
	}
	
	public void giveResearchPointsBoss(Player p, int amount, String boss, boolean announce) {
		UUID uuid = p.getUniqueId();
		if (playerStats.containsKey(uuid)) {
			PlayerStats pStats = playerStats.get(uuid);
			HashMap<String, Integer> mobKills = pStats.getMobKills();
			HashMap<String, Integer> researchPoints = pStats.getResearchPoints();
			int pLevel = pStats.getLevel();
			BossInfo bi = InfoAPI.getBossInfo(boss);
			String display = bi.getDisplayWithLevel(false);
			int lvl = bi.getLevel(false);
			
			// Discovery
			if (!researchPoints.containsKey(boss)) {
				p.sendMessage(discovery.replaceAll("%mob%", display).replaceAll("&", "§"));
				researchPoints.put(boss, 0);
			}
			if (!mobKills.containsKey(boss)) {
				mobKills.put(boss, 0);
			}
			
			if (pLevel >= lvl) {
				int points = researchPoints.containsKey(boss) ? researchPoints.get(boss) + amount : amount;
				researchPoints.put(boss, points);
				if (announce) {
					String msg = new String("&4[&c&lMLMC&4] &7You gained &e" + amount + " &7extra research points for " + display + "&7!");
					msg = msg.replaceAll("&", "§");
					p.sendMessage(msg);
				}
				String msg = display + " - §e" + points + " Research Pts";
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
				checkItemCompletion(boss, p, points, display);
			}
			else {
				String msg = display + " - §cResearch level too low!";
				p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
			}
		}
	}

	public void giveResearchKills(Player p, int amount, String mob) {
		UUID uuid = p.getUniqueId();
		if (playerStats.containsKey(uuid)) {
			PlayerStats pStats = playerStats.get(uuid);
			HashMap<String, Integer> mobKills = pStats.getMobKills();
			HashMap<String, Integer> researchPoints = pStats.getResearchPoints();
			if (!researchPoints.containsKey(mob)) {
				researchPoints.put(mob, 0);
			}
			int kills = mobKills.containsKey(mob) ? mobKills.get(mob) + amount : amount;
			mobKills.put(mob, kills);
		}
	}
	
	public void setResearchPoints(Player p, int amount, String mob) {
		UUID uuid = p.getUniqueId();
		if (playerStats.containsKey(uuid)) {
			HashMap<String, Integer> researchPoints = playerStats.get(uuid).getResearchPoints();
			researchPoints.put(mob, amount);
			checkItemDecompletion(mob, p, amount);
		}
	}

	public void setResearchKills(Player p, int amount, String mob) {
		UUID uuid = p.getUniqueId();
		if (playerStats.containsKey(uuid)) {
			HashMap<String, Integer> mobKills = playerStats.get(uuid).getMobKills();
			mobKills.put(mob, amount);
		}
	}

	public void updateBonuses(Player p) {
		UUID uuid = p.getUniqueId();
		if (playerAttrs.containsKey(uuid)) {
			int acc = SkillAPI.getPlayerAccountData(p).getActiveId();
			// Make sure the player has no bonuses already equipped
			removeBonuses(p);
			
			// Activate the player's stored attrs
			StoredAttributes pAttr = playerAttrs.get(uuid).get(acc);
			if (pAttr == null) {
				// Use same attrs as first account
				pAttr = new StoredAttributes(playerAttrs.get(uuid).getOrDefault(1, new StoredAttributes()).getStoredAttrs());
				playerAttrs.get(uuid).put(acc, pAttr);
			}
			pAttr.applyAttributes(p);
		}
	}
	
	public void resetBonuses(Player p) {
		UUID uuid = p.getUniqueId();
		if (playerAttrs.containsKey(uuid)) {
			int acc = SkillAPI.getPlayerAccountData(p).getActiveId();
			StoredAttributes pAttr = playerAttrs.get(uuid).get(acc);
			if (pAttr == null) {
				// Use same attrs as first account
				pAttr = new StoredAttributes(playerAttrs.get(uuid).getOrDefault(1, new StoredAttributes()).getStoredAttrs());
				playerAttrs.get(uuid).put(acc, pAttr);
			}
			pAttr.resetAttributes();
		}
	}
	
	public void removeBonuses(Player p) {
		UUID uuid = p.getUniqueId();
		if (playerAttrs.containsKey(uuid)) {
			int acc = SkillAPI.getPlayerAccountData(p).getActiveId();
			StoredAttributes pAttr = playerAttrs.get(uuid).get(acc);
			if (pAttr == null) {
				// Use same attrs as first account
				pAttr = new StoredAttributes(playerAttrs.get(uuid).getOrDefault(1, new StoredAttributes()).getStoredAttrs());
				playerAttrs.get(uuid).put(acc, pAttr);
			}
			pAttr.removeAttributes(p);
			pAttr.resetAttributes();
		}
	}
	
	public static HashMap<String, ArrayList<ResearchItem>> getMobMap() {
		return mobMap;
	}
	
	public static PlayerStats getPlayerStats(UUID uuid) {
		return playerStats.get(uuid);
	}
	
	public static HashMap<String, ResearchItem> getResearchItems() {
		return researchItems;
	}
	
	public int getNumResearchItems() {
		return researchItems.size();
	}
	
	public static HashMap<Integer, Integer> getNextLevel() {
		return toNextLvl;
	}
	
	public static HashMap<Integer, StoredAttributes> getPlayerAttributeAccounts(Player p) {
		return playerAttrs.get(p.getUniqueId());
	}
	
	public static StoredAttributes getPlayerAttributes(Player p, int acc) {
		return playerAttrs.get(p.getUniqueId()).get(acc);
	}
	
	public static StoredAttributes getPlayerAttributes(Player p) {
		int acc = SkillAPI.getPlayerAccountData(p).getActiveId();
		return playerAttrs.get(p.getUniqueId()).get(acc);
	}
	
	public static boolean isCompleted(Player p, String id) {
		return playerStats.get(p.getUniqueId()).getCompletedResearchItems().containsKey(id);
	}
	
	public static HashMap<String, HashMap<String, Integer>> getConverter() {
		return converter;
	}
	
	public static HashSet<String> getMinibosses() {
		return minibosses;
	}
	
	// Below are situations where research should load
	
	@EventHandler
	public void onAttributeLoad(PlayerAttributeLoadEvent e) {
		updateBonuses(e.getPlayer());
	}
	
	@EventHandler
	public void onAttributeUnload(PlayerAttributeUnloadEvent e) {
		resetBonuses(e.getPlayer());
	}
}