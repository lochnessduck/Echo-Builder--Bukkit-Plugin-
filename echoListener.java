package echo.echo;


import java.util.List;

import org.bukkit.Material;
//import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
//import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
//import org.bukkit.material.MaterialData;

public class echoListener implements Listener 
{
	echoMain plugin;
	echoBlockPlacement placeBlocks;
	
	public echoListener(echoMain em)
	{
		plugin = em;
		placeBlocks  = new echoBlockPlacement(plugin);
	}
	
	/*
	 * on block break, we check if block was broken naturally. If so, make other copy block == air and then get it's item to drop naturally.
	 * DO NOT USE copyBlock.breakNaturally() because breakNaturally() calls onBlockBreak. It's just silly to have redundant calls like that
	 * Also make sure that the block broken was prior placed. We do not want the player using echo echo as a way to speed up mining.
	 * 
	 */
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockBreak(BlockBreakEvent event)
	{
		if (event.isCancelled()) return; //don't do anything if the event was cancelled
		if (plugin.verbose) plugin.logger.info("[echo] broke a block");
		Block block = event.getBlock();
		Player player = event.getPlayer();
		if (plugin.signManager.isSign(block))
		{
			//then we have a sign. Do not duplicate or break
			//BUT WE NEED TO CHECK IF ITS A MASTER SIGN! (OR A SLAVE SIGN!)
			if (plugin.signManager.isEchoSign(block))
			{
				//yes it's an echo sign. now remove it from whatever list it has
				plugin.signManager.removeEchoSign(player, plugin.signManager.getSignFromBlock(block)); //plugin attempts to determine if it's slave or master and dutifully remove the sign.
			}
			return; //we do nothing. do not copy this
		}
		//Block copyBlock = getCopyBlock(block);
		//changeBlockType(copyBlock,blockType);
		if (plugin.activeMap.containsKey(player.toString())) //if the active map has this player in it
		{
			String signKey = plugin.activeMap.get(player.toString());
			if (signKey.equals(""))
			{
				return; //do nothing if the player has no active echo
			}
			//then we place a new block!
//			placeBlocks.placeBlockInMirroredCircle(plugin.MasterSign, plugin.SlaveSign, block, 0);
			List<Sign> signs = plugin.signMap.get(signKey);
			if (plugin.verbose) plugin.logger.info("[echo] we are about to break blocks!");
			placeBlocks.findAndPlaceCopyBlocks(player, block.getType(), block, Material.AIR, signs, signKey); //block, block refers to 1) that the block we are breaking is the same as the one broken by this event 2) is not used
			//Location loc = placeBlocks.getNewBlockLocation(plugin.MasterSign, plugin.SlaveSign, block); //grabs our new location to place the block based on the two signs and the original placed block
			//Block copyBlock = placeBlocks.getCopyBlock(block,loc); //grabs the physical block to be changed
			//placeBlocks.changeBlockType(copyBlock,blockType); //changes the block that needs to be changed.
		}
	}
	
	
	
	
	
	
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent event)
	{
		if (event.isCancelled()) return; //don't do anything if the event was cancelled
		if (plugin.verbose) plugin.logger.info("[echo] planted a block");
		Block block = event.getBlockPlaced();
		if (plugin.signManager.signExists(block))
		{
			if (plugin.verbose) plugin.logger.info("[echo] you planted a sign. We will not copy that"); //then we have a sign. Do not duplicate or do anything
			return; //we do nothing. do not copy this
		}
		Player player = event.getPlayer();
		//Block copyBlock = getCopyBlock(block);
		//changeBlockType(copyBlock,blockType);
		if (plugin.activeMap.containsKey(player.toString())) //if the active map has this player in it
		{
			if (plugin.verbose) plugin.logger.info("[echo] checking player's active echo");
			String signKey = plugin.activeMap.get(player.toString());
			if (signKey.equals(""))
			{
				return; //do nothing if the player has no active echo
			}
			//then we place a new block!
//			placeBlocks.placeBlockInMirroredCircle(plugin.MasterSign, plugin.SlaveSign, block, 0);
			List<Sign> signs = plugin.signMap.get(signKey);
			if (plugin.verbose) plugin.logger.info("[echo] we are about to add that new block!");
			placeBlocks.findAndPlaceCopyBlocks(player, Material.AIR, block, signs, signKey); //we humbly suggest that we MUST be only placing blocks where there once was air
			//Location loc = placeBlocks.getNewBlockLocation(plugin.MasterSign, plugin.SlaveSign, block); //grabs our new location to place the block based on the two signs and the original placed block
			//Block copyBlock = placeBlocks.getCopyBlock(block,loc); //grabs the physical block to be changed
			//placeBlocks.changeBlockType(copyBlock,blockType); //changes the block that needs to be changed.
		}
	}
	
	//we should make this priority the LOWEST! We do not want to get in the way of block protections
	@EventHandler
	public void onSignTextChange(SignChangeEvent event)
	{
		if (plugin.verbose) plugin.logger.info("[echo] sign text was changed!"); //if (plugin.verbose) plugin.logger.info("[echo]"); //standard debug message
		Block bsign = event.getBlock();
		if (!plugin.signManager.isSign(bsign)) return; //return if the sign just changed isn't a sign
		Sign sign = plugin.signManager.getSignFromBlock(bsign);
		String line2 = event.getLine(1);
		if (plugin.verbose) plugin.logger.info("[echo] about to check if it's an echo sign"); //standard debug message
		if (event.getLine(0).equals("[echo]")) //checking the sign doesn't work! It's the sign, but the actual text is in the event!
		{
			if (plugin.verbose) plugin.logger.info("[echo] yes it's an echo sign"); //standard debug message
			if (line2.equals("master"))
			{
				if (plugin.verbose) plugin.logger.info("[echo] you just created a master sign! we'll add it"); //standard debug message
				plugin.signManager.makeMasterSign(event); //we need to forward the EVENT! Not the sign
			}
			else //else the player is using the easy shortcut by only typing in [echo] in the first line to create a slave sign
			{
				if (plugin.verbose) plugin.logger.info("[echo] you just created a slave sign! we'll add it"); //standard debug message
				plugin.signManager.makeSlaveSign(event);
			}
			return;
		}//if we reach here it was not an echo sign. So we leave that sign alone
		if (plugin.verbose) plugin.logger.info("[echo] no, it was NOT an echo sign");
		
		if (plugin.verbose) plugin.logger.info("[echo] " + sign.getLine(0));
		if (plugin.verbose) plugin.logger.info("[echo] " + sign.getLine(1));
		if (plugin.verbose) plugin.logger.info("[echo] " + sign.getLine(2));
		if (plugin.verbose) plugin.logger.info("[echo] " + sign.getLine(3));
	}
	
	//gets fully executed when the player interacts with a sign by left clicking on it
	@EventHandler
	public void turnEchoOnOff(PlayerInteractEvent event)
	{
		Block block = event.getClickedBlock();
		if (block == null) return; //return if the player clicked on an entity instead
		if (plugin.signManager.isSign(block))
		{
			if (plugin.verbose) plugin.logger.info("[echo] you just clicked on a sign post!"); //standard debug message
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) //the player has right clicked a sign
			{
				if (plugin.verbose) plugin.logger.info("[echo] a right click!"); //standard debug message
				plugin.signManager.changeActiveEcho(event); //forward this to signManagement
			}
		}
	}
	
	
}
