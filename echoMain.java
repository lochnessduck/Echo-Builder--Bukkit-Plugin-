package echo.echo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class echoMain extends JavaPlugin 
{
	// check this website: http://bobby-tables.com/java.html
	//for information on preventing user input (via sign text) to allow possible hacking routes
	//shouldn't be possible though since you're merely comparing certain texts
	public static echoMain plugin;
	public final echoSignManagement signManager = new echoSignManagement(this); //hold onto an instance of the signManagement thingy
	public final Logger logger = Logger.getLogger("Echo Echo plugin plugin");
	public Block MasterSign = null;
	public Block SlaveSign = null;
	Map<String,List<Sign>> signMap = new HashMap<String, List<Sign>>();
	Map<String,String> activeMap = new HashMap<String, String>(); //a player who has an no active echo should have "" as the active echo
	public final boolean verbose = false;
	
	@Override
	public void onEnable()
	{
		logger.info("Echo Echo enabled enabled");
		final PluginManager pm = getServer().getPluginManager();
		
		//World world2 = getServer().getWorld(world.toString())
		pm.registerEvents(new echoListener(this), this);
	}
	
	public void onDisable()
	{
		// message saying it's disabled
		logger.info("Echo Echo disabled disabled");
	}

}
