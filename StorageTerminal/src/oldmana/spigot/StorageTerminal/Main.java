package oldmana.spigot.StorageTerminal;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener
{
	public List<Terminal> terminals;
	
	public TerminalData data;
	
	@Override
	public void onEnable()
	{
		data = new TerminalData(this);
		terminals = data.loadTerminals();
		
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable()
	{
		data.saveTerminals(terminals);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		
		return false;
	}
	
	@EventHandler
	public void onSignChange(SignChangeEvent event)
	{
		if (event.getLine(0).equals("[Terminal]"))
		{
			Terminal t = new Terminal(this, event.getBlock(), null, event.getPlayer(), null, false);
			terminals.add(t);
		}
	}
}
