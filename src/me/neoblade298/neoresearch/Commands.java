package me.neoblade298.neoresearch;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.sucy.skill.SkillAPI;

import de.tr7zw.nbtapi.NBTItem;
import io.lumine.mythic.bukkit.MythicBukkit;
import me.neoblade298.neocore.info.BossInfo;
import me.neoblade298.neocore.info.InfoAPI;
import me.neoblade298.neoresearch.inventories.ResearchAttributesInventory;


public class Commands implements CommandExecutor{
	
	Research main;
	
	public Commands(Research main) {
		this.main = main;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {
		if (sender.hasPermission("mycommand.staff") || sender.isOp()) {
			if (args.length == 0) {
				sender.sendMessage("§c/nr reload");
				sender.sendMessage("§c/nr debug [player]");
				sender.sendMessage("§c/nr create/spawnbook [player] [mob] [level] [points]");
				sender.sendMessage("§c/nr create/spawnbookboss [player] [boss] [points]");
				sender.sendMessage("§c/nr givepoints [player] [mob] [level] [amt]");
				sender.sendMessage("§c/nr givepointsboss [player] [boss] [amt]");
				sender.sendMessage("§c/nr givekills [player] [mob] [amt]");
				sender.sendMessage("§c/nr setpoints/kills [player] [mob] [level (pts only)] [amt]");
				sender.sendMessage("§c/nr (add/set)(level/exp) [player] [amt]");
				sender.sendMessage("§c/nr inspect [player] (mob)");
				sender.sendMessage("§c/nr inspectgoals [player]");
				sender.sendMessage("§c/nr attrs");
				sender.sendMessage("§c/nr debug");
				sender.sendMessage("§c/nr check [player] [research]");
				return true;
			}

			// /nr reload
			else if (args[0].equalsIgnoreCase("reload")) {
				main.loadConfig();
				sender.sendMessage("§4[§c§lMLMC§4] §7Reloaded config");
				return true;
			}
			
			// /nr debug
			else if (args[0].equalsIgnoreCase("debug")) {
				Research.debug = !Research.debug;
				if (Research.debug) {
					sender.sendMessage("§7Debug enabled");
				}
				else {
					sender.sendMessage("§7Debug disabled");
				}
				return true;
			}

			// /nr spawnbook [player] [internalmob] [level] [point amt]
			else if (args[0].equalsIgnoreCase("spawnbook")) {
				Player p = Bukkit.getPlayer(args[1]);
				if (MythicBukkit.inst().getMobManager().getMythicMob(args[2]) == null) {
					sender.sendMessage("§4[§c§lMLMC§4] §cInvalid mob");
					return true;
				}
				String display = MythicBukkit.inst().getMobManager().getMythicMob(args[2]).get().getDisplayName().get();
				int amt = Integer.parseInt(args[4]);

				ItemStack item = new ItemStack(Material.BOOK);
				ItemMeta meta = item.getItemMeta();

				meta.setDisplayName("§6Research Book §7- " + display + " §7(§e" + amt + "§7)");
				ArrayList<String> lore = new ArrayList<String>();
				lore.add("§7Grants§e " + amt + " §7research points for");
				lore.add(display);
				
				int level = Integer.parseInt(args[3]);

				meta.setCustomModelData(100);
				meta.setLore(lore);
				item.setItemMeta(meta);
				NBTItem nbti = new NBTItem(item);
				nbti.setString("internalmob", args[2]);
				nbti.setInteger("level", level);
				p.getInventory().addItem(nbti.getItem());
				sender.sendMessage("§4[§c§lMLMC§4] §7Spawned research book " + display + " §7to player §e" + p.getName());
				return true;
			}
			
			// /nr spawnbookboss [player] [boss] [point amt]
			else if (args[0].equalsIgnoreCase("spawnbookboss")) {
				Player p = Bukkit.getPlayer(args[1]);
				BossInfo bi = InfoAPI.getBossInfo(args[2]);
				int amt = Integer.parseInt(args[3]);

				ItemStack item = new ItemStack(Material.BOOK);
				ItemMeta meta = item.getItemMeta();

				meta.setDisplayName("§6Research Book §7- " + bi.getDisplayWithLevel(false) + " §7(§e" + amt + "§7)");
				ArrayList<String> lore = new ArrayList<String>();
				lore.add("§7Grants§e " + amt + " §7research points for");
				lore.add(bi.getDisplayWithLevel(false));
				
				int level = bi.getLevel(false);

				meta.setCustomModelData(100);
				meta.setLore(lore);
				item.setItemMeta(meta);
				NBTItem nbti = new NBTItem(item);
				nbti.setString("internalmob", bi.getKey());
				nbti.setInteger("level", level);
				p.getInventory().addItem(nbti.getItem());
				sender.sendMessage("§4[§c§lMLMC§4] §7Spawned research book " + bi.getDisplayWithLevel(false) + " §7to player §e" + p.getName());
				return true;
			}

			// /nr createbook [player] [internalmob] [point amt]
			else if (args[0].equalsIgnoreCase("createbook")) {
				Player p = Bukkit.getPlayer(args[1]);
				UUID uuid = p.getUniqueId();
				if (MythicBukkit.inst().getMobManager().getMythicMob(args[2]) == null) {
					sender.sendMessage("§4[§c§lMLMC§4] §cInvalid mob");
					return true;
				}
				String display = MythicBukkit.inst().getMobManager().getMythicMob(args[2]).get().getDisplayName().get();
				int amt = Integer.parseInt(args[3]);
				int needed = amt * 2;
				
				// First check if the player has enough research points
				int currentPoints = Research.getPlayerStats(uuid).getResearchPoints().get(args[2]);
				int min = -1;
				for (ResearchItem rItem : Research.getMobMap().get(args[2])) {
					int goal = rItem.getGoals().get(args[2]);
					if (min < goal && goal <= currentPoints) min = goal;
				}

				if (currentPoints < min + needed) {
					p.sendMessage("§4[§c§lMLMC§4] §cYou need at least §e" + (min + needed) + " §cresearch points to do this!");
					return true;
				}
				
				Research.getPlayerStats(uuid).getResearchPoints().put(args[2], currentPoints - needed);
				ItemStack item = new ItemStack(Material.BOOK);
				ItemMeta meta = item.getItemMeta();

				meta.setDisplayName("§6Research Book §7- " + display + " §7(§e" + amt + "§7)");
				ArrayList<String> lore = new ArrayList<String>();
				lore.add("§7Grants§e " + amt + " §7research points for");
				lore.add(display);
				
				String sLevel = display.split(" ")[1];
				sLevel = sLevel.substring(0, sLevel.length() - 1);
				int level = Integer.parseInt(sLevel);

				meta.setCustomModelData(100);
				meta.setLore(lore);
				item.setItemMeta(meta);
				NBTItem nbti = new NBTItem(item);
				nbti.setString("internalmob", args[2]);
				nbti.setInteger("level", level);
				p.getInventory().addItem(nbti.getItem());
				p.sendMessage("§4[§c§lMLMC§4] §7You created a §e" + amt + " §7point " + display + " §7research book!");
				sender.sendMessage("§4[§c§lMLMC§4] §7Gave research book " + display + " §7to player §e" + p.getName());
				return true;
			}

			// /nr createbookboss [player] [boss] [point amt]
			else if (args[0].equalsIgnoreCase("createbookboss")) {
				Player p = Bukkit.getPlayer(args[1]);
				UUID uuid = p.getUniqueId();
				BossInfo bi = InfoAPI.getBossInfo(args[2]);
				String display = bi.getDisplayWithLevel(false);
				int amt = Integer.parseInt(args[3]);
				int needed = amt * 2;
				
				// First check if the player has enough research points
				int currentPoints = Research.getPlayerStats(uuid).getResearchPoints().get(args[2]);
				int min = -1;
				for (ResearchItem rItem : Research.getMobMap().get(args[2])) {
					int goal = rItem.getGoals().get(args[2]);
					if (min < goal && goal <= currentPoints) min = goal;
				}

				if (currentPoints < min + needed) {
					p.sendMessage("§4[§c§lMLMC§4] §cYou need at least §e" + (min + needed) + " §cresearch points to do this!");
					return true;
				}
				
				Research.getPlayerStats(uuid).getResearchPoints().put(args[2], currentPoints - needed);
				ItemStack item = new ItemStack(Material.BOOK);
				ItemMeta meta = item.getItemMeta();

				meta.setDisplayName("§6Research Book §7- " + bi.getDisplayWithLevel(false) + " §7(§e" + amt + "§7)");
				ArrayList<String> lore = new ArrayList<String>();
				lore.add("§7Grants§e " + amt + " §7research points for");
				lore.add(display);
				
				int level = bi.getLevel(false);

				meta.setCustomModelData(100);
				meta.setLore(lore);
				item.setItemMeta(meta);
				NBTItem nbti = new NBTItem(item);
				nbti.setString("internalmob", bi.getKey());
				nbti.setInteger("level", level);
				p.getInventory().addItem(nbti.getItem());
				p.sendMessage("§4[§c§lMLMC§4] §7You created a §e" + amt + " §7point " + display + " §7research book!");
				sender.sendMessage("§4[§c§lMLMC§4] §7Gave research book " + display + " §7to player §e" + p.getName());
				return true;
			}
			
			// /nr givepoints [player] [internalmob] [level] [amount]
			else if (args[0].equalsIgnoreCase("givepoints")) {
				Player p = Bukkit.getPlayer(args[1]);
				int level = Integer.parseInt(args[3]);
				int amount = Integer.parseInt(args[4]);
				main.giveResearchPoints(p, amount, args[2], level, true, "admin");
				sender.sendMessage("§4[§c§lMLMC§4] §7Gave points for " + args[2] + " §7to player §e" + p.getName());
				return true;
			}
			// /nr givepointsboss [player] [boss] [amount]
			else if (args[0].equalsIgnoreCase("givepointsboss")) {
				Player p = Bukkit.getPlayer(args[1]);
				int amount = Integer.parseInt(args[3]);
				main.giveResearchPointsBoss(p, amount, args[2], true);
				sender.sendMessage("§4[§c§lMLMC§4] §7Gave points for " + args[2] + " §7to player §e" + p.getName());
				return true;
			}
			// /nr givekills [player] [internalmob] [amount]
			else if (args[0].equalsIgnoreCase("givekills")) {
				Player p = Bukkit.getPlayer(args[1]);
				int amount = Integer.parseInt(args[3]);
				main.giveResearchKills(p, amount, args[2]);
				sender.sendMessage("§4[§c§lMLMC§4] §7Gave points for " + args[2] + " §7to player §e" + p.getName());
				return true;
			}
			// /nr setpoints [player] [internalmob] [amount]
			else if (args[0].equalsIgnoreCase("setpoints")) {
				Player p = Bukkit.getPlayer(args[1]);
				int amount = Integer.parseInt(args[3]);
				main.setResearchPoints(p, amount, args[2]);
				sender.sendMessage("§4[§c§lMLMC§4] §7Set points for " + args[2] + " §7to §e" + amount);
				return true;
			}
			// /nr setkills [player] [internalmob] [amount]
			else if (args[0].equalsIgnoreCase("setkills")) {
				Player p = Bukkit.getPlayer(args[1]);
				int amount = Integer.parseInt(args[3]);
				main.setResearchKills(p, amount, args[2]);
				sender.sendMessage("§4[§c§lMLMC§4] §7Set kills for " + args[2] + " §7to §e" + amount);
				return true;
			}
			// /nr setlevel [player] [amount]
			else if (args[0].equalsIgnoreCase("setlevel")) {
				Player p = Bukkit.getPlayer(args[1]);
				int amount = Integer.parseInt(args[2]);
				Research.getPlayerStats(p.getUniqueId()).setLevel(amount);
				sender.sendMessage("§4[§c§lMLMC§4] §7Set level for " + p.getName() + " §7to §e" + amount);
				return true;
			}
			// /nr setexp [player] [amount]
			else if (args[0].equalsIgnoreCase("setexp")) {
				Player p = Bukkit.getPlayer(args[1]);
				int amount = Integer.parseInt(args[2]);
				Research.getPlayerStats(p.getUniqueId()).setExp(amount);
				sender.sendMessage("§4[§c§lMLMC§4] §7Set exp for " + p.getName() + " §7to §e" + amount);
				return true;
			}
			// /nr addexp [player] [amount]
			else if (args[0].equalsIgnoreCase("addexp")) {
				Player p = Bukkit.getPlayer(args[1]);
				int amount = Integer.parseInt(args[2]);
				Research.getPlayerStats(p.getUniqueId()).addExp(p, amount);
				sender.sendMessage("§4[§c§lMLMC§4] §7Added exp for " + p.getName() + " §7to §e" + amount);
				return true;
			}
			// /nr inspect [player]
			else if (args[0].equalsIgnoreCase("inspect") && args.length == 2) {
				Player p = Bukkit.getPlayer(args[1]);
				PlayerStats stats = Research.getPlayerStats(p.getUniqueId());
				sender.sendMessage("§4[§c§lMLMC§4] §e" + p.getName() + " §7is research level §e" + stats.getLevel() +
						" §7with §e" + stats.getExp() + " / " + Research.getNextLevel().get(stats.getLevel()) + " §7exp.");
				return true;
			}
			// /nr check [player] [researchgoal]
			else if (args[0].equalsIgnoreCase("check") && args.length == 3) {
				Player p = Bukkit.getPlayer(args[1]);
				PlayerStats stats = Research.getPlayerStats(p.getUniqueId());
				ResearchItem ri = Research.getResearchItems().get(args[2]);
				for (Entry<String, Integer> e : ri.getGoals().entrySet()) {
					int points = stats.getResearchPoints().getOrDefault(e.getKey(), 0);
					int pointsReq = e.getValue();
					String color = points >= pointsReq ? "§a" : "§c";
					sender.sendMessage("§c" + e.getKey() + "§7: " + color + points + " / " + pointsReq);
				}
				return true;
			}
			// /nr inspect [player] [internalmob]
			else if (args[0].equalsIgnoreCase("inspect") && args.length == 3) {
				Player p = Bukkit.getPlayer(args[1]);
				String mob = args[2];
				PlayerStats stats = Research.getPlayerStats(p.getUniqueId());
				sender.sendMessage("§4[§c§lMLMC§4] §e" + p.getName() + " §7has §e" + stats.getResearchPoints().get(mob) +
						" §7research points and §e" + stats.getMobKills().get(mob) + " §7kills for this mob.");
				return true;
			}
			// /nr inspectgoals [player]
			else if (args[0].equalsIgnoreCase("inspectgoals")) {
				Player p = Bukkit.getPlayer(args[1]);
				PlayerStats stats = Research.getPlayerStats(p.getUniqueId());
				String msg = new String("§4[§c§lMLMC§4] §e" + p.getName() + " §7has: §e");
				for (String id : stats.getCompletedResearchItems().keySet()) {
					msg += id + " ";
				}
				sender.sendMessage(msg);
				return true;
			}
			// /nr takegoal [player] [goal]
			else if (args[0].equalsIgnoreCase("takegoal")) {
				Player p = Bukkit.getPlayer(args[1]);
				PlayerStats stats = Research.getPlayerStats(p.getUniqueId());
				for (String id : stats.getCompletedResearchItems().keySet()) {
					if (id.contains(args[2])) {
						sender.sendMessage("§4[§c§lMLMC§4] §7Successfully removed goal");
						stats.getCompletedResearchItems().remove(id);
						break;
					}
				}
				return true;
			}
			// /nr updateattrs [player]
			else if (args[0].equalsIgnoreCase("updateattrs")) {
				Player p = Bukkit.getPlayer(args[1]);
				if (p != null) {
					main.updateBonuses(p);
				}
				return true;
			}
			// /nr resetattrs [player]
			else if (args[0].equalsIgnoreCase("resetattrs")) {
				Player p = Bukkit.getPlayer(args[1]);
				if (p != null) {
					main.resetBonuses(p);
				}
				return true;
			}
			// /nr removeattrs [player]
			else if (args[0].equalsIgnoreCase("removeattrs")) {
				Player p = Bukkit.getPlayer(args[1]);
				if (p != null) {
					main.removeBonuses(p);
				}
				return true;
			}
		}
		if (args[0].equalsIgnoreCase("attrs") && sender instanceof Player) {
			Player p = (Player) sender;
			int acc = SkillAPI.getPlayerAccountData(p).getActiveId();
			StoredAttributes pAttr = Research.getPlayerAttributes(p);
			if (pAttr == null) {
				// Use same attrs as first account
				pAttr = new StoredAttributes(Research.getPlayerAttributeAccounts(p).getOrDefault(1, new StoredAttributes()).getStoredAttrs());
				Research.getPlayerAttributeAccounts(p).put(acc, pAttr);
			}
			new ResearchAttributesInventory(p, pAttr);
			return true;
		}
		return false;
	}
}