package me.neoblade298.neoresearch.inventories;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import de.tr7zw.nbtapi.NBTItem;
import me.neoblade298.neoresearch.Research;
import me.neoblade298.neoresearch.StoredAttributes;

public class ResearchAttributesInventory implements ResearchInventory {
	private final Inventory inv;
	private StoredAttributes attr;
	private Player p;

	public ResearchAttributesInventory(Player p, StoredAttributes attr) {

		int remaining = attr.getAttribute("unused");
		inv = Bukkit.createInventory(p, 9, "§8Research Attrs (" + remaining + " points)");
		this.attr = attr;
		this.p = p;
		
		Research.viewingInventory.put(p, this);

		ItemStack[] contents = inv.getContents();
		contents[0] = createGuiItem(Material.RED_DYE, "§7[§4Strength§7 (§f" + attr.getAttribute("strength") + "§7)]",
				"strength", 100003, "§7Increases §4Red§7 values in", "§c/skills §7by 2%.",
				"", "§7§oLeft click adds, right click removes,", "§7§oShift click for multiples of 10.",
				"§7§oYou have §f" + remaining + " §7§opoints remaining.");
		contents[1] = createGuiItem(Material.YELLOW_DYE, "§7[§eDexterity§7 (§f" + attr.getAttribute("dexterity") + "§7)]",
				"dexterity", 100004, "§7Increases §eYellow§7 values in", "§c/skills §7by 2%.",
				"", "§7§oLeft click adds, right click removes,", "§7§oShift click for multiples of 10.",
				"§7§oYou have §f" + remaining + " §7§opoints remaining.");
		contents[2] = createGuiItem(Material.LIGHT_BLUE_DYE, "§7[§9Intelligence§7 (§f" + attr.getAttribute("intelligence") + "§7)]",
				"intelligence", 100005, "§7Increases §9Blue§7 values in", "§c/skills §7by 2%.",
				"", "§7§oLeft click adds, right click removes,", "§7§oShift click for multiples of 10.",
				"§7§oYou have §f" + remaining + " §7§opoints remaining.");
		contents[3] = createGuiItem(Material.LIME_DYE, "§7[§aSpirit§7 (§f" + attr.getAttribute("spirit") + "§7)]",
				"spirit", 100006, "§7Increases §aGreen§7 values in", "§c/skills §7by 2%.",
				"", "§7§oLeft click adds, right click removes,", "§7§oShift click for multiples of 10.",
				"§7§oYou have §f" + remaining + " §7§opoints remaining.");
		contents[4] = createGuiItem(Material.ORANGE_DYE, "§7[§6Endurance§7 (§f" + attr.getAttribute("endurance") + "§7)]",
				"endurance", 100008, "§7Increases §6Orange§7 values in", "§c/skills §7by 2%.",
				"", "§7§oLeft click adds, right click removes,", "§7§oShift click for multiples of 10.",
				"§7§oYou have §f" + remaining + " §7§opoints remaining.");
		contents[7] = createGuiItem(Material.BARRIER, "§7[§4Reset§7]",
				null, 0, "§7Reset all the points you've put in.");
		contents[8] = createGuiItem(Material.BOOK, "§7[Info]",
				null, 0, "§7You have §f" + attr.getAttribute("unused") + " §7points remaining.", "§7These attributes stack with §c/attr§7.");
		inv.setContents(contents);

		p.openInventory(inv);
	}

	protected ItemStack createGuiItem(final Material material, final String name, String attr, int model, final String... lore) {
		final ItemStack item = new ItemStack(material, 1);
		final ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore(Arrays.asList(lore));
		meta.setCustomModelData(model);
		item.setItemMeta(meta);
		
		if (attr != null) {
			NBTItem nbti = new NBTItem(item);
			nbti.setString("attribute", attr);
			return nbti.getItem();
		}
		return item;
	}

	// Check for clicks on items
	public void handleInventoryClick(final InventoryClickEvent e) {
		if (e.getInventory() != inv)
			return;
		
		if (e.getCurrentItem() != null && e.getCurrentItem().getType().isAir()) 
			return;

		e.setCancelled(true);
		ItemStack item = e.getCurrentItem();
		if (item == null || item.getType().isAir()) {
			return;
		}
		
		if (e.getRawSlot() == 7) {
			attr.unvestAll();
			for (int i = 0; i < 5; i++) {
				item = e.getInventory().getItem(i);
				NBTItem nbti = new NBTItem(item);
				String attribute = nbti.getString("attribute");
				updateItems(item, attribute);
			}
			return;
		}
		
		NBTItem nbti = new NBTItem(item);
		if (!nbti.hasKey("attribute")) 
			return;
		
		String attribute = nbti.getString("attribute");
		if (e.getClick().equals(ClickType.LEFT)) {
			attr.investAttribute(attribute, 1);
			updateItems(item, attribute);
		}
		else if (e.getClick().equals(ClickType.RIGHT)) {
			attr.unvestAttribute(attribute, 1);
			updateItems(item, attribute);
		}
		else if (e.getClick().equals(ClickType.SHIFT_LEFT)) {
			attr.investAttribute(attribute, 10);
			updateItems(item, attribute);
		}
		else if (e.getClick().equals(ClickType.SHIFT_RIGHT)) {
			attr.unvestAttribute(attribute, 10);
			updateItems(item, attribute);
		}

		
	}

	// Cancel dragging in this inventory
	public void handleInventoryDrag(final InventoryDragEvent e) {
		if (e.getInventory().equals(inv)) {
			e.setCancelled(true);
		}
	}
	
	public void handleInventoryClose(final InventoryCloseEvent e) {
		attr.applyAttributes(p);
	}
	
	private void updateItems(ItemStack item, String attribute) {
		ItemMeta meta = item.getItemMeta();
		String name = meta.getDisplayName();
		name = name.substring(0, name.indexOf('(') + 1);
		name += "§f" + attr.getAttribute(attribute) + "§7)]";
		meta.setDisplayName(name);
		item.setItemMeta(meta);
		
		ItemStack[] contents = inv.getContents();
		int remaining = attr.getAttribute("unused");
		for (int i = 0; i <= 8; i++) {
			if (i == 5) {
				i = 7;
				continue;
			}
			meta = contents[i].getItemMeta();
			List<String> lore = meta.getLore();
			if (i != 8) {
				lore.set(5, "§7§oYou have §f" + remaining + " §7§opoints remaining.");
			}
			else {
				lore.set(0, "§7§oYou have §f" + remaining + " §7§opoints remaining.");
			}
			meta.setLore(lore);
			contents[i].setItemMeta(meta);
		}
		inv.setContents(contents);
	}
}
