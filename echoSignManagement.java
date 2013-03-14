package echo.echo;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class echoSignManagement 
{
	echoMain plugin;
	
	echoSignManagement(echoMain em)
	{
		plugin = em;
	}
	
	//asume that the event.getBlock() is truely a sign
	public void makeSlaveSign(SignChangeEvent event)
	{
		if (plugin.verbose) plugin.logger.info("[echo] start of making slave sign"); //standard debug message
		//let's just assume it's a sign
		Block bsign = event.getBlock();
//		if (!isSign(bsign))
//		{
//			if (plugin.verbose) plugin.logger.info("[echo] it's not a sign block! HUGE ERROR in makeSlaveSign"); //standard debug message
//			return; //do nothing if the block is not a sign
//		}
		String signKey = getSignKey(event.getLines());
		Sign sign = getSignFromBlock(bsign);
		if (signKey.equals(""))
		{
			if (plugin.verbose) plugin.logger.info("[echo] signKey is blank! we'll set it to our current active echo"); //standard debug message
			//it was an empty string. So the signKey will equal the player's current active echo
			signKey = getActiveEcho(event.getPlayer());
			if (plugin.verbose) plugin.logger.info("[echo] active echo= " + signKey); //standard debug message
			if (signKey.equals(""))
			{
				//no luck. The sign was not configured correctly and the player has no active echo. Just break that sign
				if (plugin.verbose) plugin.logger.info("[echo] no active echo, and it's an unsigned slave sign? Sorry, we gotta break this one"); //standard debug message
				breakDuplicateSign(sign);
				return;
			}
		}
		else //signKey was actual text! WE NEED TO MAKE SURE THAT IT WILL BE ADDED TO A LIST AND THAT A MASTER SIGN FOR THAT KEY EXISTS
		{
			if (!plugin.signMap.containsKey(signKey))
			{
				wipeSign(event); //wipe that sign to indicate it's not valid
				return; //we EXIT this method if the key from the slave sign has no corresponding master sign (And was just made up)
			}
		}
		writeSlaveSign(event, signKey); //just rewrite the sign with what we know so that it's properly configured
		//if we've reached here then we have a proper signKey and the slave sign is filled in properly.
		//we assume that there is a proper master sign and all for this
		addSlaveSign(sign, signKey);
		//hooray we've added our slave sign! It's all configured and ready to go!
	}
	
	//notice we do not call event handler. This is not called automatically, but by our program's choice
	public void makeMasterSign(SignChangeEvent event)
	{
		if (plugin.verbose) plugin.logger.info("[echo] start of making master sign"); //standard debug message
		//first we check to see if the sign has a duplicate key. We'll wipe the sign if it's a duplicate master.
		//or else make a new master echo and make it active
		//
		String signKey = getSignKey(event.getLines());
		Block bsign = event.getBlock();
//		if (signKey == null)
//		{
//			return; //then the master sign is not a valid sign. So don't do anything. Our mistake
//		}
		Sign sign = getSignFromBlock(bsign);
		if (plugin.signMap.containsKey(signKey))
		{
//			//then we just made a mastersign whose key is already used. First lets check and make sure the sign in question still exists
//			if (signExists(plugin.signMap.get(signKey))) //returns if there are signs indeed... not quite what you were looking for
//			{
//				//we need to create a new name then that is similar.
//				String newname = createNewSignID(signKey); //it should iterate quite a bit
//			}
//			else
//			{
				//then if the sign doesn't exist, we just replace that sign with this one
//			replaceMasterSign(old, (Sign) event.getBlock()); //replace old with new
//			}
			breakDuplicateSign(sign);
		}
		else
		{
			//create new entry for sign and make that player's active echo this newly created one
			addMasterSign(sign,signKey); //add the sign to our cache of keys and signs
			setActiveEcho(event.getPlayer(),signKey); //player will now put blocks on this one
		}
	}

	public void addMasterSign(Sign sign, String signKey)
	{
		if (plugin.verbose) plugin.logger.info("[echo] adding master sign!"); //standard debug message
		List<Sign> signs = new ArrayList<Sign>();
		if (plugin.verbose) plugin.logger.info("[echo] created signs list");
		signs.add(sign);
		if (plugin.verbose) plugin.logger.info("[echo] added msign to array!");
		plugin.signMap.put(signKey, signs);
		if (plugin.verbose) plugin.logger.info("[echo] added master sign");
	}
	
	public void removeEchoSign(Player player, Sign sign)
	{
		String[] lines = sign.getLines();
		String signKey = getSignKey(lines);
		if (lines[1].equals("master"))
		{
			removeMasterSign(player, signKey, sign);
			return; //remove the master sign (and all it's slave signs)
		}
		if (lines[1].equals("slave"))
		{
			removeSlaveSign(sign, signKey);
			return; //done
		}
		//if we reach here, we just let the sign be destroyed. My backup sign checking algorithm will find it's missing and delete it from the list when it gets called.
	}
	
	//call this function when a master sign has been destroyed. It will destroy all other slave signs as well.
	//and remove the entry for that key
	public void removeMasterSign(Player player, String signKey, Sign sign)
	{
		//the objective is to remove ALL slave signs as well
		if (!plugin.signMap.containsKey(signKey)) return; //exit if signMap doesn't have this sign recorded
		if (sign == null) //sign is null if we don't want the sign being checked. We just want to remove the full signMap
		{
			plugin.signMap.remove(signKey); //remove the map now
			setActiveEcho(player,"");
			return;
		}
		List<Sign> signs = plugin.signMap.get(signKey);
		Sign msign = signs.get(0);
		if (plugin.verbose) plugin.logger.info("[echo] " + sign.getLocation().toString() + " : " + msign.getLocation().toString());
		if (sign.getLocation().toString().equals(msign.getLocation().toString())) //find out if the two signs are equal (by checking the string of location
		{
			if (plugin.verbose) plugin.logger.info("[echo] the sign just destroyed was a master sign. NOw deleting it");
			removeMasterSign(player, signKey, null);//yes it's the master sign, delete it! (by self-reference with null sign)
		}
		//we get to here if the master sign deleted was not the same one as in the 
//		List<Sign> signs = plugin.signMap.get(signKey);
		//iterate and remove all slave signs
//		int ss = signs.size(); //don't break the slave signs anymore... that's bad form man
//		for (int i = 1; i < ss; i++) //slave signs start at 1
//		{
//			Sign slave = signs.get(i);
//			breakDuplicateSign(slave);
//		}
		//we assume that the actual master sign in question was broken by the user. Do NOT drop anything for it.
		//now set player's active map to ""?
	}
	
	public void removeSlaveSign(Sign sign, String signKey)
	{
		if (!plugin.signMap.containsKey(signKey)) return; //exit this function if the sign map doesn't have this string
		List<Sign> signs = plugin.signMap.get(signKey);
		if (signs.contains(sign))
		{
			signs.remove(sign);
		}
		updateSignList(signKey, signs); //so if possible, get list of signs
	}
	
	public void addSlaveSign(Sign sign, String signKey)
	{
		if (plugin.verbose) plugin.logger.info("[echo] adding slave sign"); //standard debug message
		List<Sign> signs = plugin.signMap.get(signKey);
		signs.add(sign); //add the sign to the end!
		plugin.signMap.put(signKey, signs);
	}
	
	//iterates through a particular list of signs and verifies each is a sign (does nothing more) and removes from the list those which are no longer signs
	public boolean verifyAndUpdateSignList(Player player, String signKey)
	{
		//1)- verify all signs
		if (!plugin.signMap.containsKey(signKey))
		{
			return false; //we can't do anything if the key is not registered
		}
		List<Sign> signs = plugin.signMap.get(signKey);
		int signSize = signs.size();
		if (signSize > 0)
		{
			if (plugin.verbose) plugin.logger.info("[echo] we're going to verify that the master sign exists"); //standard debug message
			if (!plugin.signManager.isSign(signs.get(0)))
			{
				if (plugin.verbose) plugin.logger.info("[echo] master sign doesn't exist, removing it"); //standard debug message
				removeMasterSign(player, signKey, null); //the master sign isn't valid. Delete all attached signs
				return false;
			}
		}
		for (int i = signSize - 1; i > 0; i--) //check all the slave signs 
		{
			Block bsign = signs.get(i).getBlock();
			if (!plugin.signManager.isSign(bsign))
			{
				signs.remove(i);  //just straight up remove that sign from the list!
				continue; //if the sign doesn't exist anymore, skip adding that one
			}
		}
		//2)- update list of signs
		updateSignList(signKey, signs);
		if (signs.size() > 0) return true; //a last final check on the size of signs and whether there are any left
		return false;
	}
	
	public void updateSignList(String signKey, List<Sign> signs)
	{
		plugin.signMap.put(signKey, signs); //re-update list?
	}
	
	public void breakDuplicateSign(Sign sign)
	{
		if (plugin.verbose) plugin.logger.info("[echo] breaking and dropping sign!"); //standard debug message
		if (sign == null)
		{
			if (plugin.verbose) plugin.logger.info("[echo] cannot break null slave sign");
			return;
		}
		World world = sign.getWorld();
		sign.setType(Material.AIR);
		sign.update();
		Block bsign = sign.getBlock();
		bsign.setType(Material.AIR);
		world.dropItemNaturally(sign.getLocation(), new ItemStack(Material.SIGN, 1));
	}
	
	//replace a master sign with a new sign. The key stays the same, but the location of the sign should be updated
	//to match the new sign
	public void replaceMasterSign(String oldKey, Sign sign)
	{
		
	}
	
	//sets the player's active echo sign. To disable a player's active echo, use an empty string (player, "") 
	//string player is the player.toString() method
	//activeSign is the 3rd line in a master sign
	public void setActiveEcho(String player, String activeSign)
	{
		plugin.activeMap.put(player, activeSign);
	}
	
	//sets the player's active echo sign. To disable a player's active echo, use an empty string (player, "") 
	//player is the event.getPlayer()
	//activeSign is the 3rd line in a master sign
	public void setActiveEcho(Player player, String activeSign)
	{
		String playerStr = player.toString();
		if (!activeSign.equals("")) //if this is going to assign an activeEcho, first check that it has a master sign
		{
			if (!plugin.signMap.containsKey(activeSign)) return; //exit if we don't have that string in signMap
		}
		setActiveEcho(playerStr, activeSign);
		if (activeSign.equals(""))
		{
			player.sendMessage("[echo] disabled");
		}
		else
		{
			player.sendMessage("[echo] " + activeSign + " enabled");
		}
	}
	
	//this is called when a player left clicks on a sign. It is up to this function to determine if Echo should be disabled for that player, or switched to another sign
	//left clicking ANY sign will disable echo. But left clicking a master sign will switch it to that sign
	public void changeActiveEcho(PlayerInteractEvent event)
	{
		if (plugin.verbose) plugin.logger.info("[echo] going to change the active echo"); //standard debug message
		//first find out if the sign is a master sign. If it is, we continue to assign an active (or disable) an echo command. If not, we do nothing
		String signKey = getMasterSignKey(event.getClickedBlock()); //will return the sign key to search in the hashmap or null if the sign was not a master sign
		Player player = event.getPlayer();
		String playerStr = player.toString();
		if (plugin.verbose) plugin.logger.info("[echo] about to check the player's active map"); //standard debug message
		String activeEcho = plugin.activeMap.get(playerStr); //gets a string representing the active echo / signs associated with the player
		if (plugin.verbose) plugin.logger.info("[echo] got the activeEcho string"); //standard debug message
		if (signKey == null) //the sign clicked was not a master sign
		{
			if (plugin.verbose) plugin.logger.info("[echo] the sign is not a master echo sign"); //standard debug message
			setActiveEcho(player,""); //change the active player's echo to nothing aka disable echo.
			return;
		}
		//signKey was not null. Now if the signIs not in the map, we don't add it. If the signKey is in the map, we confirm we've clicked on the right sign!
		if (plugin.verbose) plugin.logger.info("[echo] now checking active echo"); //standard debug message
		if (activeEcho == null || activeEcho.equals("")) //an empty string means the player has no associated active echo command
		{
			//we'll be here if signKey != null, so we assume it's a masterKey
			if (plugin.verbose) plugin.logger.info("[echo] active echo string was '' (nothing)"); //standard debug message
			setActiveEcho(player,signKey); //activate an echo
			return;
		}
		//now we need to consider the case where the player has an activeEcho
		if (plugin.verbose) plugin.logger.info("[echo] the activeEcho string was not blank"); //standard debug message
		if (activeEcho.equals(signKey))
		{
			if (plugin.verbose) plugin.logger.info("[echo] ah the active echo key IS the key we typed!"); //standard debug message
			setActiveEcho(player,""); //disable the echo
			return;
		}
		//as a last option, we just set the active echo to the new sign we clicked
		if (plugin.verbose) plugin.logger.info("[echo] the active echo was not the same as clicked sign"); //standard debug message
		if (plugin.verbose) plugin.logger.info("[echo] now going to change the player's active echo to "+signKey); //standard debug message
		setActiveEcho(player,signKey);
		return;
	}
	
	public String getActiveEcho(Player player)
	{
		if (plugin.activeMap.containsKey(player.toString()))
		{
			return plugin.activeMap.get(player.toString());
		}
		return "";
	}
	
	//returns null if the sign is NOT a master sign. If it is a master sign, it will return the unique key (third line of sign) that signals what other signs are under it's control
	public String getMasterSignKey(Block tempsign)
	{
		if (!isSign(tempsign)) return null; //return null if the sign block is not a sign
		Sign sign = getSignFromBlock(tempsign); //there it is. A cast to (Sign)
		if (isEchoSign(sign)) //if the first line indicates we are in an echo sign
		{
			
			return getMasterSignKey(sign);
		}
		return null;
	}
	
	public String getMasterSignKey(Sign sign)
	{
		if (sign.getLine(1).equals("master")) //if the second line indicates the sign is a master sign
		{
			return getSignKey(sign);
		}
		return null;
	}
	
	public String getSignKey(Sign sign)
	{
		return sign.getLine(2);
	}
	
	public String getSignKey(String[] lines)
	{
		return lines[2];
	}
	
	public boolean signExists(Sign[] signs)
	{
		return (signs.length > 0);
		
	}
	
	public boolean signExists(World world, float x, float y, float z)
	{
		Location loc = new Location(world, x, y, z);
		Block sign = world.getBlockAt(loc);
		return signExists(sign);
	}
	
	//return true if the block is a sign or sign post
	//this needs to be depreciated. It's a no longer useful function (other ones are better/ more precise)
	public boolean signExists(Block block)
	{
		if (plugin.verbose) plugin.logger.info("[echo] checking if block is a sign"); //standard debug message
		if (block == null)
		{
			return false; //return false if the block no longer exists
		}
		if (plugin.verbose) plugin.logger.info("we are checking the block type to see if it's a sign. It's not null, it is: " + block.toString());
		return isSign(block);
	}
	
	public boolean signExists(Sign sign)
	{
		if (sign == null)
		{
			return false;
		}
		return true;
	}
	
	//use this function for checking a list of signs
	public boolean isSign(Sign sign)
	{
		if (sign == null) return false;
		if ((sign instanceof Sign)) 
		{
			if (sign.getWorld().getBlockAt(sign.getLocation()).getState() instanceof Sign)
			{
				return true; //return null if the sign block is not a sign
			}
		}
		return false;
	}
	
	public boolean isSign(Block block)
	{
		if (block == null) return false;
		if ((block.getState() instanceof Sign)) return true;
		if (plugin.verbose) plugin.logger.info("[echo] NOT a sign"); //standard debug message
		return false;
	}
	
	//takes the (Sign) form of a sign. Returns true or false if the first line of the sign is correctly formatted as an echo sign
	//you can convert to sign by using Sign sign = (Sign) block;
	public boolean isEchoSign(Sign sign)
	{
		return (sign.getLine(0).equals("echo") || sign.getLine(0).equals("[echo]"));
	}
	
	public boolean isEchoSign(Block sign)
	{
		if (isSign(sign)) return isEchoSign(getSignFromBlock(sign));
		return false; //returns false if it's not a sign
	}
	
	public void writeSlaveSign(SignChangeEvent event, String signKey)
	{
		writeEchoSign(event,"slave",signKey);
	}
	
	public void writeMasterSign(SignChangeEvent event, String signKey)
	{
		writeEchoSign(event,"master",signKey);
	}
	
	public void writeEchoSign(SignChangeEvent event, String masterORslave, String signKey)
	{
		if (plugin.verbose) plugin.logger.info("[echo] writing text to sign"); //standard debug message
		event.setLine(0, "[echo]");
		event.setLine(1, masterORslave);
		event.setLine(2, signKey);
		//leave the last line untouched
	}
	
	//removes all text from the sign (this helps signify that it didn't work to convert it to an echo sign)
	//or it signifies that it is no longer an echo sign
	public void wipeSign(SignChangeEvent event)
	{
		event.setLine(0, "");
		event.setLine(1, "");
		event.setLine(2, "");
		event.setLine(3, "");
	}
	
	public void wipeSign(Sign sign)
	{
		if (sign instanceof Sign)
		{
			sign.setLine(0, "");
			sign.setLine(1, "");
			sign.setLine(2, "");
			sign.setLine(3, "");
		}
	}
	
	public Sign getSignFromBlock(Block bsign)
	{
		return (Sign) bsign.getState();
	}
	
}
