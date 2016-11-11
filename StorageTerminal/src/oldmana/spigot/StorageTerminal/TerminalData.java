package oldmana.spigot.StorageTerminal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import oldmana.spigot.StorageTerminal.Terminal.TerminalChest;

public class TerminalData
{
	private FileConfiguration data;
	
	private Main main;
	
	public TerminalData(Main main)
	{
		data = YamlConfiguration.loadConfiguration(new File(main.getDataFolder(), "terminals.yml"));
		
		this.main = main;
	}
	
	public List<Terminal> loadTerminals()
	{
		List<Terminal> list = new ArrayList<Terminal>();
		for (String key : data.getKeys(false))
		{
			String[] signData = key.split("~");
			Location signLoc = new Location(Bukkit.getServer().getWorld(signData[0]), Integer.parseInt(signData[1]), Integer.parseInt(signData[2]), Integer.parseInt(signData[3]));
			List<Chest> chests = new ArrayList<Chest>();
			OfflinePlayer owner;
			List<OfflinePlayer> members = new ArrayList<OfflinePlayer>();
			boolean restricted;
			ConfigurationSection terminalSec = data.getConfigurationSection(key);
			ConfigurationSection chestSec = terminalSec.getConfigurationSection("Chests");
			for (String key2 : chestSec.getKeys(false))
			{
				ConfigurationSection chestLoc = chestSec.getConfigurationSection(key2);
				Location l = new Location(Bukkit.getServer().getWorld(chestLoc.getString("World")), chestLoc.getInt("X"), chestLoc.getInt("Y"), chestLoc.getInt("Z"));
				Block b = l.getBlock();
				if (b.getType() != Material.CHEST)
				{
					System.out.println("[StorageTerminal] ERROR: The block at " + chestLoc.getString("World") + "," + chestLoc.getInt("X") + "," + chestLoc.getInt("Y") + "," + chestLoc.getInt("Z") + " is registered as a chest by the sign at " + ".");
				}
				else
				{
					chests.add((Chest) b.getState());
				}
			}
			owner = Bukkit.getServer().getOfflinePlayer(UUID.fromString(terminalSec.getString("Owner")));
			for (String member : terminalSec.getStringList("Members"))
			{
				members.add(Bukkit.getServer().getOfflinePlayer(UUID.fromString(member)));
			}
			restricted = terminalSec.getBoolean("Restricted-Access");
			
			Terminal t = new Terminal(main, signLoc.getBlock(), chests, owner, members, restricted);
			Bukkit.getServer().getPluginManager().registerEvents(t, main);
			list.add(t);
		}
		return list;
	}
	
	public void saveTerminals(List<Terminal> list)
	{
		for (Terminal t : list)
		{
			Location sLoc = t.getSign().getLocation();
			ConfigurationSection terminalSec = data.createSection(sLoc.getWorld().getName() + "~" + sLoc.getBlockX() + "~" + sLoc.getBlockY() + "~" + sLoc.getBlockZ());
			ConfigurationSection chestSec = terminalSec.createSection("Chests");
			List<TerminalChest> chests = t.getChests();
			for (int i = 0 ; i < chests.size() ; i++)
			{
				Chest c = chests.get(i).getChest();
				ConfigurationSection sec = chestSec.createSection(String.valueOf(i + 1));
				sec.set("World", c.getWorld().getName());
				sec.set("X", c.getX());
				sec.set("Y", c.getY());
				sec.set("Z", c.getZ());
			}
			terminalSec.set("Owner", t.getOwner().getUniqueId().toString());
			List<String> members = new ArrayList<String>();
			for (OfflinePlayer p : t.getMembers())
			{
				members.add(p.getUniqueId().toString());
			}
			terminalSec.set("Members", members);
			terminalSec.set("Restricted-Access", t.isRestricted());
		}
		try
		{
			data.save(new File(main.getDataFolder(), "terminals.yml"));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
