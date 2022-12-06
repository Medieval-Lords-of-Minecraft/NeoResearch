package me.neoblade298.neoresearch.inventories;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public interface ResearchInventory {
	public void handleInventoryClick(InventoryClickEvent e);
	public void handleInventoryDrag(InventoryDragEvent e);
	public void handleInventoryClose(InventoryCloseEvent e);
}
