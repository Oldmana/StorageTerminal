package oldmana.spigot.StorageTerminal;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import oldmana.spigot.StorageTerminal.Terminal.TerminalChest;

public class TerminalUI implements Listener
{
	private Inventory inv;
	
	private Terminal terminal;
	
	private List<CompressedItem> items = new ArrayList<CompressedItem>();
	private int page = 1;
	private boolean search = false;
	private List<ItemStack> searchResults = new ArrayList<ItemStack>();
	
	public TerminalUI(Terminal t)
	{
		inv = Bukkit.getServer().createInventory(null, 54, ChatColor.RED + "Storage Terminal");
		
		terminal = t;
		
		for (TerminalChest c : terminal.getChests())
		{
			for (ItemStack item : c.getChest().getInventory().getContents())
			{
				if (item != null)
				{
					boolean stacked = false;
					for (CompressedItem ci : items)
					{
						if (item.isSimilar(ci.getItem()))
						{
							ci.setAmount(ci.getAmount() + item.getAmount());
							stacked = true;
							break;
						}
					}
					if (!stacked)
					{
						items.add(new CompressedItem(item));
					}
				}
			}
		}
		update();
		
		t.getUser().openInventory(inv);
	}
	
	public int getPages()
	{
		return (int) Math.ceil(items.size() / 27.0);
	}
	
	public int getCurrentPage()
	{
		return page;
	}
	
	public void update()
	{
		inv.clear();
		
		ItemStack separator = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
		ItemMeta im = separator.getItemMeta();
		im.setDisplayName("--------");
		separator.setItemMeta(im);
		for (int i = 27 ; i < 36 ; i++)
		{
			inv.setItem(i, separator);
		}
		for (int i = 0 ; i < 27 ; i++)
		{
			if (i + ((getCurrentPage() - 1) * 27) >= items.size())
			{
				break;
			}
			
			inv.setItem(i, items.get(i + ((getCurrentPage() - 1) * 27)).getItem());
		}
		
		ItemStack add = new ItemStack(Material.ANVIL);
		ItemStack remove = new ItemStack(Material.ANVIL, 1, (short) 2);
		
		ItemStack back = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) (page == 1 ? 7:14));
		ItemStack forward = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) (page == getPages() ? 7:5));
		ItemStack first = new ItemStack(Material.STAINED_GLASS, 1, (short) (page == 1 ? 7:14));
		ItemStack last = new ItemStack(Material.STAINED_GLASS, 1, (short) (page == getPages() ? 7:5));
		
		im = back.getItemMeta();
		im.setDisplayName(ChatColor.GOLD + "Back [Page " + getCurrentPage() + " of " + getPages() + "]");
		back.setItemMeta(im);
		im.setDisplayName(ChatColor.GOLD + "Forward [Page " + getCurrentPage() + " of " + getPages() + "]");
		forward.setItemMeta(im);
		im.setDisplayName(ChatColor.GOLD + "Jump to First [Page " + getCurrentPage() + " of " + getPages() + "]");
		first.setItemMeta(im);
		im.setDisplayName(ChatColor.GOLD + "Jump to Last [Page " + getCurrentPage() + " of " + getPages() + "]");
		last.setItemMeta(im);
		im.setDisplayName(ChatColor.GOLD + "Add Chest to Terminal");
		add.setItemMeta(im);
		im.setDisplayName(ChatColor.GOLD + "Remove Chest from Terminal");
		remove.setItemMeta(im);
		
		inv.setItem(36, add);
		inv.setItem(44, remove);
		
		inv.setItem(45, first);
		inv.setItem(46, back);
		inv.setItem(52, forward);
		inv.setItem(53, last);
		
		
	}
	
	public Player getUser()
	{
		return terminal.getUser();
	}
	
	public int itemAtInStorage(int invViewLoc)
	{
		return (getCurrentPage() - 1) * 27 + invViewLoc;
	}
	
	public CompressedItem getItemInStorage(ItemStack type)
	{
		for (CompressedItem item : items)
		{
			if (type.isSimilar(item.getItem()))
			{
				return item;
			}
		}
		return null;
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event)
	{
		Inventory clicked = event.getClickedInventory();
		System.out.println((clicked == null) + " " + (event.getClickedInventory()));
		if (clicked != null && inv != null && clicked.hashCode() == inv.hashCode())
		{
			event.setCancelled(true);
			// Is the clicked slot air and does the cursor hold an item?
			if (event.getCurrentItem().getType() == Material.AIR && event.getCursor().getType() != Material.AIR)
			{
				ItemStack cursor = event.getCursor();
				CompressedItem target = null;
				for (CompressedItem ci : items)
				{
					if (ci.getItem().isSimilar(cursor))
					{
						target = ci;
						break;
					}
				}
				if (target == null)
				{
					target = new CompressedItem(event.getCursor(), 0);
					items.add(target);
				}
				int before = cursor.getAmount();
				cursor = terminal.addToSystem(event.getCursor());
				target.setAmount(before - (cursor ==  null ? 0:cursor.getAmount()));
				event.setCursor(cursor);
				update();
			}
			// Is the clicked slot in the first three rows and not air?
			else if (event.getSlot() < 27 && event.getCurrentItem().getType() != Material.AIR)
			{
				CompressedItem ci = items.get(itemAtInStorage(event.getSlot()));
				ItemStack selected = ci.getItem();
				if (event.getAction() == InventoryAction.PICKUP_ALL)
				{
					if (ci.getAmount() > selected.getMaxStackSize())
					{
						ItemStack transaction = selected.clone();
						transaction.setAmount(transaction.getMaxStackSize());
						terminal.removeFromSystem(selected, selected.getMaxStackSize());
						ci.setAmount(ci.getAmount() - selected.getMaxStackSize());
						event.setCursor(transaction);
					}
					else
					{
						items.remove(ci);
						terminal.removeFromSystem(selected, ci.getAmount());
						event.setCursor(selected);
					}
					update();
				}
				else if (event.getAction() == InventoryAction.PICKUP_ONE || 
						event.getAction() == InventoryAction.PICKUP_HALF)
				{
					ItemStack cursor = event.getCursor();
					// Is the cursor not at max stack and is it even similar to the item being clicked?
					if (cursor.getAmount() < cursor.getMaxStackSize() && cursor.isSimilar(ci.getItem()))
					{
						terminal.removeFromSystem(selected, 1);
						ci.setAmount(ci.getAmount() - 1);
						cursor.setAmount(cursor.getAmount() + 1);
						event.setCursor(cursor);
					}
					else if (cursor.getType() == Material.AIR)
					{
						terminal.removeFromSystem(selected, 1);
						ci.setAmount(ci.getAmount() - 1);
						cursor = ci.getItem();
						cursor.setAmount(1);
						event.setCursor(cursor);
					}
					update();
				}
				else if (event.isShiftClick())
				{
					// Does the storage contain more than the max stack size?
					if (ci.getAmount() > selected.getMaxStackSize())
					{
						terminal.removeFromSystem(selected, selected.getMaxStackSize());
						ci.setAmount(ci.getAmount() - selected.getMaxStackSize());
						event.getView().getBottomInventory().addItem(ci.getItem());
					}
					else
					{
						event.getWhoClicked().getInventory().addItem(ci.getItem());
						items.remove(ci);
						terminal.removeFromSystem(selected, ci.getAmount());

					}
					update();
				}
			}
			else if (event.getSlot() == 45) // First Page
			{
				page = 1;
				update();
			}
			else if (event.getSlot() == 46) // Back
			{
				page = Math.max(1, page - 1);
				update();
			}
			else if (event.getSlot() == 52) // Forward
			{
				page = Math.min(getPages(), page + 1);
				update();
			}
			else if (event.getSlot() == 53) // Last Page
			{
				page = getPages();
				update();
			}
			else if (event.getSlot() == 36) // Add Chest
			{
				terminal.addChestMode();
				getUser().closeInventory();
			}
			else if (event.getSlot() == 44) // Remove Chest
			{
				terminal.removeChestMode();
				getUser().closeInventory();
			}
		}
		else if (clicked != inv && inv != null && event.getView().getTopInventory().hashCode() == inv.hashCode())
		{
			System.out.println("LJSDP");
			if (event.getCurrentItem().getType() != Material.AIR)
			{
				ItemStack selected = event.getCurrentItem();
				CompressedItem ci = getItemInStorage(selected);
				if (ci == null)
				{
					ci = new CompressedItem(selected, 0);
				}
				ItemStack remainder = terminal.addToSystem(selected);
				//ci.setAmount(ci.getAmount() + (selected.getAmount() - remainder.getAmount()));
				if (remainder != null)
				{
					event.setCurrentItem(remainder);
					ci.setAmount(ci.getAmount() + (selected.getAmount() - remainder.getAmount()));
				}
				else
				{
					event.setCurrentItem(null);
					ci.setAmount(selected.getAmount());
				}
				if (!items.contains(ci))
				{
					items.add(ci);
				}
				event.setCancelled(true);
				update();
			}
		}
	}
	
	public static class CompressedItem
	{
		private ItemStack item;
		private int amount;
		
		public CompressedItem(ItemStack item)
		{
			this.item = item;
			this.amount = item.getAmount();
		}
		
		public CompressedItem(ItemStack item, int amount)
		{
			this.item = item;
			this.amount = amount;
		}
		
		public ItemStack getItem()
		{
			ItemStack clone = item.clone();
			clone.setAmount(Math.min(clone.getMaxStackSize(), amount));
			return clone;
		}
		
		public int getAmount()
		{
			return amount;
		}
		
		public void setAmount(int amount)
		{
			this.amount = amount;
		}
	}
}
