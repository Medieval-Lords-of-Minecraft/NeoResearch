package me.neoblade298.neoresearch.inventories;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import me.neoblade298.neoresearch.Research;

public class InventoryListeners implements Listener {
	Research main;
	public InventoryListeners(Research main) {
		this.main = main;
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		if (Research.viewingInventory.containsKey(p)) {
			Research.viewingInventory.get(p).handleInventoryClick(e);
		}
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent e) {
		Player p = (Player) e.getWhoClicked();
		if (Research.viewingInventory.containsKey(p)) {
			Research.viewingInventory.get(p).handleInventoryDrag(e);
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent e) {
		Player p = (Player) e.getPlayer();
		if (Research.viewingInventory.containsKey(p)) {
			Research.viewingInventory.get(p).handleInventoryClose(e);
		}
		Research.viewingInventory.remove(p);
	}
}
