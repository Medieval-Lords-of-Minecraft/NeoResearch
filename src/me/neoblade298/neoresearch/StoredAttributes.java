package me.neoblade298.neoresearch;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.entity.Player;

import com.sucy.skill.SkillAPI;
import com.sucy.skill.api.player.PlayerData;

public class StoredAttributes {
	private HashMap<String, Integer> stored;
	private HashMap<String, Integer> active;
	public static ArrayList<String> attrs = new ArrayList<String>();
	
	static {
		attrs.add("strength");
		attrs.add("dexterity");
		attrs.add("intelligence");
		attrs.add("spirit");
		attrs.add("endurance");
		attrs.add("unused");
	}
	
	public StoredAttributes() {
		this.stored = new HashMap<String, Integer>();
		this.active = new HashMap<String, Integer>();
	}

	public StoredAttributes(HashMap<String, Integer> attrs) {
		this.stored = attrs;
		this.active = new HashMap<String, Integer>();
	}
	
	public int countAttributes() {
		int count = 0;
		for (String key : stored.keySet()) {
			count += stored.get(key);
		}
		return count;
	}

	public void applyAttributes(Player p) {
		PlayerData data = SkillAPI.getPlayerData(p);
		removeAttributes(p);
		for (String attr : stored.keySet()) {
			if (attr.equals("unused")) {
				continue;
			}
			// Must be deep copy
			int remove = -active.getOrDefault(attr, 0);
			int add = stored.get(attr);
			
			if (remove != 0) data.addBonusAttributes(attr, remove);
			active.put(attr, stored.get(attr));
			if (add != 0) data.addBonusAttributes(attr, add);
		}
	}
	public void removeStoredAttributes() {
		stored.clear();
	}

	public void removeAttributes(Player p) {
		PlayerData data = SkillAPI.getPlayerData(p);
		for (String attr : active.keySet()) {
			data.addBonusAttributes(attr, -active.get(attr));
		}
		active.clear();
	}
	
	public void resetAttributes() {
		active.clear();
	}

	public HashMap<String, Integer> getStoredAttrs() {
		return stored;
	}

	public HashMap<String, Integer> getActiveAttrs() {
		return active;
	}
	
	public int getAttribute(String attr) {
		return stored.getOrDefault(attr, 0);
	}
	
	public void setAttribute(String attr, int num) {
		stored.put(attr, num);
	}
	
	public void addAttribute(String attr, int num) {
		stored.put(attr, stored.getOrDefault(attr, 0) + num);
	}
	
	public void investAttribute(String attr, int num) {
		int unused = stored.getOrDefault("unused", 0);
		if (unused >= num) {
			stored.put("unused", unused - num);
			stored.put(attr, stored.getOrDefault(attr, 0) + num);
		}
		else if (unused >= 1) {
			stored.put("unused", 0);
			stored.put(attr, stored.getOrDefault(attr, 0) + unused);
		}
	}
	
	public void unvestAttribute(String attr, int num) {
		int attrNum = stored.getOrDefault(attr, 0);
		if (attrNum >= num) {
			stored.put("unused", stored.getOrDefault("unused", 0) + num);
			stored.put(attr, attrNum - num);
		}
		else if (attrNum >= 1) {
			stored.put("unused", stored.getOrDefault("unused", 0) + attrNum);
			stored.put(attr, 0);
		}
	}
	
	public void unvestAll() {
		for (String attr : stored.keySet()) {
			if (attr.equals("unused")) {
				continue;
			}
			stored.put("unused", stored.getOrDefault("unused", 0) + stored.get(attr));
			stored.put(attr, 0);
		}
	}

	public String toString() {
		return stored.toString();
	}

}
