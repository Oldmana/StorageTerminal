package oldmana.spigot.StorageTerminal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import oldmana.spigot.StorageTerminal.TerminalUI.CompressedItem;

import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class Terminal implements Listener
{
	private Main main;
	
	private Block sign;
	private List<TerminalChest> chests;
	
	private OfflinePlayer owner;
	private List<OfflinePlayer> members;
	private boolean restrictedAccess = true;
	
	
	private Player terminalUser;
	private TerminalUI ui;
	
	private boolean addingChest = false;
	private boolean removingChest = false;
	
	
	public Terminal(Main main, Block sign, List<Chest> chests, OfflinePlayer owner, List<OfflinePlayer> members, boolean restrictedAccess)
	{
		this.main = main;
		
		main.getServer().getPluginManager().registerEvents(this, main);
		
		this.sign = sign;
		this.chests = new ArrayList<TerminalChest>();
		if (chests != null)
		{
			for (Chest chest : chests)
			{
				this.chests.add(new TerminalChest(chest));
			}
		}
		
		this.owner = owner;
		if (members != null)
		{
			this.members = members;
		}
		else
		{
			this.members = new ArrayList<OfflinePlayer>();
		}
		this.restrictedAccess = restrictedAccess;
	}
	
	public boolean isOwner(Player player)
	{
		if (player.getUniqueId() == owner.getUniqueId())
		{
			return true;
		}
		return false;
	}
	
	public OfflinePlayer getOwner()
	{
		return owner;
	}
	
	public boolean isMember(Player player)
	{
		if (members.contains((OfflinePlayer) player))
		{
			return true;
		}
		return false;
	}
	
	public List<OfflinePlayer> getMembers()
	{
		return members;
	}
	
	public boolean isRestricted()
	{
		return restrictedAccess;
	}
	
	public Block getSign()
	{
		return sign;
	}
	
	public List<TerminalChest> getChests()
	{
		return chests;
	}
	
	public void addChest(Chest c)
	{
		chests.add(new TerminalChest(c));
	}
	
	public void removeChest(TerminalChest c)
	{
		chests.remove(c);
	}
	
	public boolean isRegisteredChest(Chest c)
	{
		for (TerminalChest chest : chests)
		{
			if (c.getLocation().equals(chest.getChest().getLocation()))
			{
				return true;
			}
		}
		return false;
	}
	
	public TerminalChest getRegisteredChest(Chest c)
	{
		for (TerminalChest tc : chests)
		{
			if (tc.getChest() == c)
			{
				return tc;
			}
		}
		return null;
	}
	
	public void addChestMode()
	{
		addingChest = true;
	}
	
	public void removeChestMode()
	{
		removingChest = true;
	}
	
	public boolean chestInUse()
	{
		for (TerminalChest c : chests)
		{
			if (!c.getChest().getBlockInventory().getViewers().isEmpty())
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean terminalInUse()
	{
		if (terminalUser == null)
		{
			return false;
		}
		return true;
	}
	
	public Player getUser()
	{
		return terminalUser;
	}
	
	public ItemStack addToSystem(ItemStack item)
	{
		int amount = item.getAmount();
		for (TerminalChest c : chests)
		{
			amount = c.addItem(item, item.getAmount());
			if (amount == 0)
			{
				return null;
			}
		}
		item.setAmount(amount);
		return item;
	}
	
	public void removeFromSystem(ItemStack item, int amount)
	{
		for (TerminalChest c : chests)
		{
			if (c.contains(item))
			{
				amount = c.removeItem(item, amount);
			}
		}
	}
	
	public void createUI()
	{
		ui = new TerminalUI(this);
		main.getServer().getPluginManager().registerEvents(ui, main);
	}
	
	public void destroyUI()
	{
		InventoryClickEvent.getHandlerList().unregister(ui);
		ui = null;
	}
	
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK)
		{
			// The terminal sign has been right clicked
			if (event.getClickedBlock().getLocation().equals(sign.getLocation()))
			{
				Player player = event.getPlayer();
				if (chestInUse())
				{
					player.sendMessage("[StorageTerminal] A chest associated with this terminal is currently in use.");
				}
				else if (terminalInUse())
				{
					player.sendMessage("[StorageTerminal] The terminal is currently in use.");
				}
				else
				{
					terminalUser = player;
					createUI();
				}
			}
		}
		else if (event.getAction() == Action.LEFT_CLICK_BLOCK)
		{
			if (event.getClickedBlock().getState() instanceof Chest)
			{
				Chest c = (Chest) event.getClickedBlock().getState();
				if (event.getPlayer() == terminalUser)
				{
					if (addingChest)
					{
						chests.add(new TerminalChest(c));
						terminalUser = null;
						addingChest = false;
						destroyUI();
						event.getPlayer().sendMessage("[StorageTerminal] Added chest to the terminal.");
					}
					else if (removingChest)
					{
						TerminalChest removal = null;
						for (TerminalChest chest : chests)
						{
							if (c.getLocation().equals(chest.getChest().getLocation()))
							{
								removal = chest;
								break;
							}
						}
						if (removal != null)
						{
							chests.remove(removal);
							event.getPlayer().sendMessage("[StorageTerminal] Removed chest from terminal.");
						}
						else
						{
							event.getPlayer().sendMessage("[StorageTerminal] This chest is not registered to the terminal.");
						}
						terminalUser = null;
						removingChest = false;
						destroyUI();
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event)
	{
		if (event.getPlayer() instanceof Player)
		{
			Player p = (Player) event.getPlayer();
			if (terminalUser == p)
			{
				System.out.println("lowell joint");
				if (!addingChest && !removingChest)
				{
					terminalUser = null;
				}
				destroyUI();
			}
		}
	}
	
	@EventHandler (priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent event)
	{
		if (event.getBlock().getState() instanceof Chest)
		{
			Chest c = (Chest) event.getBlock().getState();
			
			if (isRegisteredChest(c))
			{
				if (terminalInUse())
				{
					event.setCancelled(true);
					event.getPlayer().sendMessage("[StorageTerminal] Cannot break chest while terminal is in use.");
				}
				else
				{
					if (!event.isCancelled())
					{
						removeChest(getRegisteredChest(c));
						event.getPlayer().sendMessage("[StorageTerminal] Removed chest from terminal.");
					}
				}
			}
		}
	}
	
	
	public static class TerminalChest
	{
		private Chest chest;
		
		private Map<ItemStack, List<Integer>> mapping;
		
		public TerminalChest(Chest chest)
		{
			this.chest = chest;
		}
		
		public Chest getChest()
		{
			return chest;
		}
		
		public boolean contains(ItemStack item)
		{
			standardize(item);
			item.setAmount(1);
			if (mapping.containsKey(item))
			{
				return true;
			}
			return false;
		}
		
		public int getAmount(ItemStack item)
		{
			standardize(item);
			int amount = 0;
			for (int i : mapping.get(item))
			{
				amount += chest.getInventory().getItem(i).getAmount();
			}
			return amount;
		}
		
		public int addItem(ItemStack item, int amount)
		{
			standardize(item);
			Inventory inv = chest.getInventory();
			if (!mapping.containsKey(item))
			{
				mapping.put(item, new ArrayList<Integer>());
			}
			Iterator<Integer> it = mapping.get(item).iterator();
			while (it.hasNext())
			{
				int slot = it.next();
				ItemStack slotItem = inv.getItem(slot);
				if (slotItem.getAmount() < slotItem.getMaxStackSize())
				{
					if (slotItem.getAmount() + amount <= slotItem.getMaxStackSize())
					{
						slotItem.setAmount(slotItem.getAmount() + amount);
						inv.setItem(slot, slotItem);
						return 0;
					}
					else
					{
						amount -= slotItem.getMaxStackSize() - slotItem.getAmount();
						slotItem.setAmount(slotItem.getMaxStackSize());
						inv.setItem(slot, slotItem);
					}
				}
			}
			
			ItemStack[] contents = inv.getContents();
			for (int i = 0 ; i < contents.length ; i++)
			{
				if (contents[i] == null)
				{
					mapping.get(item).add(i);
					if (amount > item.getMaxStackSize())
					{
						ItemStack slotItem = item.clone();
						slotItem.setAmount(item.getMaxStackSize());
						inv.setItem(i, slotItem);
						amount -= item.getMaxStackSize();
					}
					else
					{
						ItemStack slotItem = item.clone();
						slotItem.setAmount(amount);
						inv.setItem(i, slotItem);
						return 0;
					}
				}
			}
			
			return amount;
		}
		
		public int removeItem(ItemStack item, int amount)
		{
			standardize(item);
			Inventory inv = chest.getInventory();
			if (!mapping.containsKey(item))
			{
				return amount;
			}
			Iterator<Integer> it = mapping.get(item).iterator();
			while (it.hasNext())
			{
				int slot = it.next();
				ItemStack slotItem = inv.getItem(slot);
				if (slotItem.getAmount() == amount)
				{
					inv.clear(slot);
					amount = 0;
					mapping.get(item).remove(slot);
					if (mapping.get(item).isEmpty())
					{
						mapping.remove(item);
					}
					break;
				}
				else if (slotItem.getAmount() > amount)
				{
					slotItem.setAmount(slotItem.getAmount() - amount);
					amount = 0;
					mapping.get(item).remove(slot);
					if (mapping.get(item).isEmpty())
					{
						mapping.remove(item);
					}
					break;
				}
				else
				{
					amount -= slotItem.getAmount();
					inv.clear(slot);
				}
			}
			return amount;
		}
		
		public void constructMapping()
		{
			mapping = new HashMap<ItemStack, List<Integer>>();
			ItemStack[] contents = chest.getInventory().getContents();
			for (int i = 0 ; i < contents.length ; i++)
			{
				ItemStack slotItem = contents[i];
				if (slotItem != null)
				{
					ItemStack standard = slotItem.clone();
					standard.setAmount(1);
					if (!mapping.containsKey(standard))
					{
						mapping.put(standard, new ArrayList<Integer>());
					}
					mapping.get(standard).add(i);
				}
			}
		}
		
		private void standardize(ItemStack item)
		{
			item.setAmount(1);
		}
	}
}
