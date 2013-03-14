package echo.echo;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Directional;

public class echoBlockPlacement 
{
	echoMain plugin;
	boolean verbose;
	
	public echoBlockPlacement(echoMain em) {
		plugin = em;
	}

	public void findAndPlaceCopyBlocks(Player player, Material blockReplacedType, Block blockPlaced, List<Sign> signs, String signKey)
	{
		findAndPlaceCopyBlocks(player, blockReplacedType, blockPlaced, blockPlaced.getType(), signs, signKey);
	}
	
//	public void findAndPlaceCopyBlocks(Player player, Block blockPlaced, Material blockType, List<Sign> signs, String signKey)
//	{
//		if (plugin.verbose) plugin.logger.info("[echo] going to place copy blocks");
//		Block msign = signs.get(0).getBlock();
//		if (!plugin.signManager.signExists(msign))
//		{
//			//if the master sign doesn't exist, delete that instance of echo map
//			plugin.signManager.removeMastersign(player, signKey);
//			return; // just skip entirely. The master sign was somehow deleted
//		}
//		if (plugin.verbose) plugin.logger.info("[echo] got master sign");
//		double heightDiff = getHeightDifference(msign,blockPlaced);
//		double relativeAngle = relativeFlatAngleBetweenSignAndBlock(msign, blockPlaced) + Math.PI; //change the rotation so it's supposedly correct (with 180 degree flip) IT WORKS! NOW we just need to monitor block breaks. If it breaks the slave we set it back to null
//		double relativeDistance = flatDistanceBetweenSignAndBlock(msign, blockPlaced);
//		//all these calculations are only needed once per block placed. Now we use them to calculate the resulting new blocks' location
//		int signSize = signs.size();
//		for (int i = signSize - 1; i > 0; i--) //decrement the i's 
//		{
//			Block ssign = signs.get(i).getBlock();
//			if (!plugin.signManager.signExists(ssign))
//			{
//				signs.remove(i);  //just straight up remove that sign from the list!
//				continue; //if the sign doesn't exist anymore, skip adding that one
//			}
//			if (hasResourcesToPlaceBlock(player, blockPlaced))
//			{
//				Location locss = getNewBlockLocation(ssign,heightDiff,relativeAngle,relativeDistance);
//				Block blockToChange = getCopyBlock(blockPlaced, locss); //returns the block. YOU CHANGE IT NOW!
//				if (blockToChange.getType() == Material.BEDROCK)
//				{
//					continue; //skip this one. We do NOT ever change bedrock
//				}
//				removeResourceFromPlayer(player, blockType); //remove the resource from the player NOW!
//				changeBlockType(blockToChange, blockType); //change the blockToChange to the type of block, the one we just placed
//				if (blockType != Material.AIR)
//				{
//					if (plugin.verbose) plugin.logger.info("[echo] copying block data");
//					copyBlockData(blockPlaced, blockToChange); //if we are not destroying block (air would indicate we are) then also copy the data of block (gives block orientation, color, or pattern)
//				}
//			}
//		}
//		//update the sign list (in case it was changed)
//		plugin.signManager.updateSignList(signKey,signs);
//	}
	
	public void findAndPlaceCopyBlocks(Player player, Material blockReplacedType, Block blockPlaced, Material blockType, List<Sign> signs, String signKey)
	{
		//0) - verify that block is not a disallowed type (one that currently gives errors)
		if (blockNotAllowed(blockPlaced)) return;
		//1) - verify signs
		if (plugin.verbose) plugin.logger.info("[echo] verifying signs! Our current list size is " + Integer.toString(signs.size()) );
		if (!plugin.signManager.verifyAndUpdateSignList(player, signKey)) return;//return if the signs do not verify (IE master sign was deleted)
		//1.5)- get list of signs still valid
		signs = plugin.signMap.get(signKey);
		if (plugin.verbose) plugin.logger.info("[echo] finished verifying signs! Our sign list size is now " + Integer.toString(signs.size()));
		if (signs.size() <= 1) return; // if the sign list is only 1, then we only have the master sign. We don't need to do anything else
		//2)- get all blocks to be changed
		List<Block> blocks = getAllCopyBlocks(player, blockPlaced, signs);
		if (plugin.verbose) plugin.logger.info("[echo] finished verifying signs! Our sign list size is now " + Integer.toString(signs.size()));
		//3)- verify all blocks are same type (AND NOT BEDROCK)
		if (!blocksAreAllSameType(blocks))
		{
			if (blockType == Material.AIR) player.sendMessage("[echo] cannot delete blocks. They are not all the same type");
			else player.sendMessage("[echo] cannot place blocks. Not all places to place a block are empty"); //tell the player why placing blocks failed
			return; //just don't place blocks if they are not of the same type!
		}
		int blockSize = blocks.size();
		if (blockSize <= 0)
		{
			if (plugin.verbose) plugin.logger.info("[echo] list of blocks is 0");
			return; //just make sure we can access the first block
		}
		if (blocks.get(0).getType() == Material.BEDROCK)
		{
			if (plugin.verbose) plugin.logger.info("[echo] the plugin is trying to change bedrock. Aborting");
			return; //we do not ever change bedrock. Notice that this check is done after verifying they are all the same type. So the first block is the same as any other block
		}
		if (blocks.get(0).getType() != blockReplacedType) //make sure that if we broke a block, the block broken is the same as all those that are about to be broken. Same with replacing
		{
			if (plugin.verbose) plugin.logger.info("[echo] the blocks to be replaced are not consistent with the block already replaced. NOT placing more blocks.");
			return;
		}
		if (plugin.verbose) plugin.logger.info("[echo] now to verify player has enough resources");
		//4)- verify player has enough resources
		
		if (!hasResourcesToPlaceBlocks(player, blockType, blockSize + 1)) //we use blockSize + 1 because the block placed has not been subtracted from the inventory count yet.
		{
			player.sendMessage("[echo] cannot place blocks. Not enough " + blockType.toString() + "in your inventory. Need " + Integer.toString(blockSize));
			return; //only continue if we have enough resources to place all blocks
		}
		if (plugin.verbose) plugin.logger.info("[echo] player has enough resources!");
		//5)- place blocks and remove resources
		ItemStack tool = player.getItemInHand(); //get whatever tool the player was using to break that block
		for (int i = 0; i < blockSize; i++)
		{
			Block blockToChange = blocks.get(i);
			if (!removeResourceFromPlayer(player, blockType)) //checks that the player has the resource, and then removes one instance of it.
			{
				if (plugin.verbose) plugin.logger.info("[echo] could not successfully remove a block from the player's inventory.");
				return; //immediately break if the player can no longer place an item.
			}
			else
			{
				boolean dropItems = (player.getGameMode() == GameMode.SURVIVAL); //we only drop items if the player is in SURVIVAL mode
				changeAndCopyBlock(tool, blockPlaced, blockToChange, blockType, dropItems);
			}
		}
	}

	//make sure you have verified all signs prior to this call
	public List<Block> getAllCopyBlocks(Player player, Block blockPlaced, List<Sign> signs)
	{
		Block msign = signs.get(0).getBlock();
		double heightDiff = getHeightDifference(msign,blockPlaced);
		double xDiff = getXdifference(msign, blockPlaced);
		double zDiff = getZdifference(msign, blockPlaced);
		int quarterMsign = getENWSdirection(msign);//get what direction the sign is facing. 0 is E, 1 is N, 2 is W, and 3 is S
		//all these calculations are only needed once per block placed. Now we use them to calculate the resulting new blocks' location
		int signSize = signs.size();
		List<Block> blocks = new ArrayList<Block>();
		for (int i = signSize - 1; i > 0; i--) //decrement the i's 
		{
			Block ssign = signs.get(i).getBlock();
			int quarterSsign = getENWSdirection(ssign);
			int directionDiff = quarterSsign - quarterMsign; //difference in facing direction and how much to rotate. 1 would mean rotate 90deg, 2 = 180 deg, etc.
			Location locss = getNewBlockLocation(ssign, heightDiff,xDiff, zDiff, directionDiff);
			Block blockToChange = getCopyBlock(blockPlaced, locss); //returns the block.
			//RIGHT HERE YOU CAN CHECK IF THE PLAYER HAS PERMISSION TO CHANGE THIS BLOCK!
			blocks.add(blockToChange);
		}
		return blocks;
	}
	
	//iterates through the list of blocks and only returns true if there are >= 1 blocks, AND they are all of the same type.
	public boolean blocksAreAllSameType(List<Block> blocks)
	{
		int blockSize = blocks.size();
		Material typeOfBlock;
		if (blockSize > 0) //if there's at least one type of block in there!
		{
			Block firstBlock = blocks.get(0);
			if (firstBlock == null)
			{
				return false;
			}
			typeOfBlock = firstBlock.getType();
		}
		else
		{
			return false;
		}
		for (int i = 0; i < blockSize; i++)
		{
			Block bl = blocks.get(i);
			if (bl == null)
			{
				return false; //always return false if a block is null.
			}
			if (bl.getType() == typeOfBlock) //so long as the block is consistent with the first block's type, we will pass the test
			{
				continue;
			}
			else
			{
				return false;
			}
		}
		return true; //this is only reached if all other tests pass, meaning there are > 0 blocks, and they are all of the same type.
	}
	
	//returns true if the method is allowed to place a block
	//also it removes the resources in the process
	public boolean hasResourcesToPlaceBlock(Player player, Block block)
	{
		Material blockType = block.getType();
		return hasResourcesToPlaceBlock(player,blockType);
	}
	
	public boolean hasResourcesToPlaceBlock(Player player, Material blockType)
	{
		return hasResourcesToPlaceBlocks(player, blockType, 2); //the player must have at least 2 of the item. 1 for the block he's just placed, and the other for the one extra block being placed
	}
	
	public boolean hasResourcesToPlaceBlocks(Player player, Material blockType, int n)
	{
		if (plugin.verbose) plugin.logger.info("[echo] does player have resources to place " + blockType.toString() + " ? " + Integer.toString(n));
		if (blockType == Material.AIR)
		{
			if (plugin.verbose) plugin.logger.info("[echo] yes player can place air! (duh)");
			return true; //this usually means the player is actually breaking a block. So we are just changing all the blocks to air!
		}
		GameMode mode = player.getGameMode();
		if (mode == GameMode.CREATIVE)
		{
			return true;
		}
		if (mode == GameMode.SURVIVAL)
		{
			PlayerInventory inv = player.getInventory();
			if (inv.contains(blockType))
			{
				return hasResourcesToPlaceBlocks(inv, blockType, n);
			}
		}
		return false; //return false if adventure mode or if the player does not have enough resources
	}
	
	public boolean hasResourcesToPlaceBlocks(PlayerInventory inv, Material blockType, int n)
	{
		if (inv.contains(blockType, n))
		{
			return true; //return true if the items has enough
		}
		if (plugin.verbose) plugin.logger.info("[echo] player doesn't have enough " + blockType.toString() + ": " + Integer.toString(n));
		return false;
	}
	
	
	//always remove 1 
	public boolean removeResourceFromPlayer(Player player, Material blockType)
	{
		GameMode mode = player.getGameMode();
		if (mode == GameMode.CREATIVE)
		{
			return true;
		}
		if (mode == GameMode.SURVIVAL)
		{
			if (blockType == Material.AIR) return true; //say that yes, we removed air from the player's inventory!
			PlayerInventory inv = player.getInventory();
			if (inv.contains(blockType))
			{
				ItemStack items = new ItemStack(blockType,1);
				return removeResourceFromPlayer(player,items);
			}
		}
		return false;
	}
	
	//removes 1 instance of a resource from the player's inventory. Returns true if successful. Returns false if not successful (the player didn't have the resources)
	public boolean removeResourceFromPlayer(Player player, ItemStack items)
	{
		Inventory inv = player.getInventory();
		inv.removeItem(items); //does this remove all of a type or just one of a type?
		//player.updateInventory();//need to look up what can be done about this
		return true;
	}
	
	public Block getCopyBlock(Block block) //grabs the block from the location further away IT WORKS NOW WAS JUST A TYPO W/ LOCATION
	{
		//World world = block.getWorld();
		
		Location loc = block.getLocation(); //used to be loc1.
		//Block copyBlock = block.getRelative(BlockFace.NORTH_EAST);
		//Location loc = block.getRelative(BlockFace.NORTH_EAST).getLocation();
		World world = block.getWorld();
		double xoff = 2D;
		double yoff = 0D;
		double zoff = 2D;
		double x = loc.getX();
		double y = loc.getY();
		double z = loc.getZ();
		loc.setX(x + xoff);
		loc.setY(y + yoff);
		loc.setZ(z + zoff);
		Block copyBlock = world.getBlockAt(loc);
		if (plugin.verbose) plugin.logger.info(loc.toString());
		return copyBlock;
	}
	
	//grabs the block from the described location in the same world as the block.
	public Block getCopyBlock(Block block, Location loc) //grabs the block from the location further away IT WORKS NOW
	{
		//World world = block.getWorld();
		//Block copyBlock = block.getRelative(BlockFace.NORTH_EAST);
		//Location loc = block.getRelative(BlockFace.NORTH_EAST).getLocation();
		World world = block.getWorld();
		Block copyBlock = world.getBlockAt(loc);
		return copyBlock;
	}
	
	//call this function to change a block to the type specified, and then copy the data (ie orientation, color) from the original to the new one
	//this function does not copy the block data if it is AIR, even though the result is probably the same.
	public void changeAndCopyBlock(ItemStack tool, final Block blockPlaced, final Block blockToChange, Material blockType, boolean dropItems)
	{
		//we need to also check if the blockPlaced is a door. If so we have to change two block! Check that the second block to change is air
//		if (isDoor(blockPlaced))
//		{
//			changeAndCopyDoor(blockPlaced, blockToChange, blockType);
//			return;
//		}
		changeBlockType(tool, blockToChange, blockType, dropItems); //change the blockToChange to the type of block, the one we just placed
		if (blockType != Material.AIR)
		{
			if (plugin.verbose) plugin.logger.info("[echo] copying block data");
			copyBlockData(blockPlaced, blockToChange); //if we are not destroying block (air would indicate we are) then also copy the data of block (gives block orientation, color, or pattern)
		}
	}
	
	//this plugin still has an issue where if a door is placed right next to another, the copied door breaks
	//also it breaks and drops two doors
	//also if you then break the top of the original door, it breaks the top of the other door AND the block above it!
//	public void changeAndCopyDoor(final Block blockPlaced, final Block blockToChange, Material blockType)
//	{
//		if (plugin.verbose) plugin.logger.info("[echo] it's a door! So we are going to change it");
////		Material topType;
//		//NEED TO FIND OUT IF IT"S THE TOP PART OR BOTTOM PART OF DOOR
//		final Block topBlockToChange = blockToChange.getRelative(BlockFace.UP);
//		Block topBlockPlaced = blockPlaced.getRelative(BlockFace.UP); //if we are destroying a door, this still is a door type
//		if (blockType == Material.AIR)
//		{
////			topType = Material.AIR; //if we are destroying the door, make sure to make the upper door get destroyed too
//		}
//		else
//		{ //then we are making a door. Check to see if one fits
//			if (topBlockToChange.getType() != Material.AIR) 
//			{
//				if (plugin.verbose) plugin.logger.info("[echo] could not copy over a door; something was in the way of the top");
//				return; //WE ONLY PLACE A DOOR IF IT HAS EMPTY SPACE
//			}
////			topType = topBlockPlaced.getType();
//		}
//		
//		byte b = blockPlaced.getData(); //always code closed into the doors
//		if (plugin.verbose) plugin.logger.info("[echo] byte for placed door is "+ Byte.toString(b));
//		b = (byte) (b | (1 << 3)); //changes the 3rd bit  (0x8) to 1 (upper half)
//		b = (byte) (b & ~(1 << 2)); //changes the 2nd bit  (0x4) to 0 (closed)
//		byte b2 = blockPlaced.getData();
//		b2 = (byte) (b2 & ~(1 << 5)); //changes the 2nd bit  (0x4) to 0 (closed)   thanks to dkarp @ http://stackoverflow.com/questions/4844342/change-bits-value-in-byte
//		if (plugin.verbose) plugin.logger.info("[echo] byte for echo door is "+ Byte.toString(b2));
//		final byte upperDoorData = b; //(byte)(0x8)
//		final byte lowerDoorData = b2;
//		changeBlockType(blockToChange, blockType); //change the blockToChange to the type of block, the one we just placed
//		changeBlockType(topBlockToChange, blockType); //change the blockToChange to the type of block, the one we just placed
//		if (blockType != Material.AIR)
//		{
////			copyBlockData(blockPlaced, blockToChange);
////			copyBlockData(topBlockPlaced, topBlockToChange);
//			copyBlockData(topBlockToChange, upperDoorData);
//			copyBlockData(blockToChange, lowerDoorData);
//		}
//	}
	
	public void copyBlockData(final Block blockToChange, final byte b)
	{
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { //just create a time-delayed block-update
            public void run() {
                blockToChange.setData(b); //works! thanks to nisovin @ http://forums.bukkit.org/threads/block-data-for-rotated-logs.90986/
            }
        });
	}
	
	public void copyBlockData(final Block blockPlaced, final Block blockToChange)
	{
		copyBlockData(blockToChange, (byte) blockPlaced.getData());
	}
	
	//give this function the item in the player's hand at time of breaking it
	public void changeBlockType(ItemStack tool, Block block, Material m, boolean dropItems)
	{
		if (plugin.verbose) plugin.logger.info("changing " + block.getType().toString() + "to " + m.toString());
		if (m == Material.AIR)//we are breaking blocks. Check to see if it should drop an item
		{
			if (dropItems) block.breakNaturally(tool); //break that block naturally using the tool given. Perhaps it will drop what you want?
		}
		block.setType(m);
//		block.getLightLevel(); //maybe force an update?
		//now check if something should be dropped?
		//BlockState bs = block.getState();
		//bs.setType(m);
		//bs.update(); //tell it to force an update.
	}
	
	public void changeBlockType(ItemStack tool, Block block, Block type, boolean dropItems)
	{
		changeBlockType(tool, block, type.getType(), dropItems);
	}
	
	public boolean isDoor(Block block)
	{
		if (block.getType() == Material.WOODEN_DOOR || block.getType() == Material.IRON_DOOR_BLOCK)
		{
			return true;
		}
		return false;
	}
	
	//returns true or false if the block is allowed to be copied / echoed
	public boolean blockNotAllowed(Block bl)
	{
		if (bl == null) return true; //obviously, yes the block isn't allowed to be copied if it's null
		Material m = bl.getType();
		if (m == Material.IRON_DOOR_BLOCK || m == Material.WOODEN_DOOR || m == Material.FENCE_GATE || m == Material.TORCH || m == Material.BEDROCK || m == Material.FIRE || m == Material.LEVER || m == Material.STONE_BUTTON || m == Material.TRAP_DOOR || m == Material.TRIPWIRE_HOOK || m == Material.TRIPWIRE || m == Material.BED || m == Material.BED_BLOCK)
		{
			return true;
		}
		return false;
	}
	
	//the sign is a block.sign type. We'll need to convert to org.bukkit.material.Sign to get the face
	//just pass the sign always as a BLOCK! We'll deal with the consequences later
	private double getSignAngle(Block sign)
	{
		double angle = 0; //a default
		BlockFace bf = getFacing(sign); //see if we can convert the sign to a block (we may just need to change the input)
		angle = Math.toRadians(convertBlockFaceToAngleDegrees(bf)); //here we convert to radians immediately
		if (plugin.verbose) plugin.logger.info("angle is "+Double.toString(angle));
		return angle;
	}
	
	private BlockFace getFacing(Block b) 
	{
		Directional dsign = (Directional) b.getType().getNewData(b.getData());
		BlockFace bfsign = dsign.getFacing(); //thanks to bergerkiller for this code (found on http://forums.bukkit.org/threads/change-the-direction-of-a-sign.31582/)
        return bfsign;
    }
	
	//analyzes the position of the sign and block and returns the distance (hypetenuse) between the two assuming a flat land
	//it does not take into account the height difference, as the height difference will always be replicated in placing a copy block,
	//whereas the leg lengths X and Z may not always have the same distance.
	private double flatDistanceBetweenSignAndBlock(Block sign, Block block)
	{
		int xs = sign.getX();
		int zs = sign.getZ();
		//int ys = sign.getY(); //get the height!
		
		int xb = block.getX();
		int zb = block.getZ();
		//int yb = block.getY();
		
		//we just want to calculate the distance from the sign AS IF it were on flat ground. The difference in Y will always be the same because angles do not matter for Y
		double xx = Math.pow((xs - xb), 2D);
		double zz = Math.pow((zs - zb), 2D);
		double d = Math.sqrt(xx + zz);
		return d;
	}
	
	private double getNewRelativeX(double angle, double distance)
	{
		return distance * Math.cos(angle);
	}
	
	private double getNewRelativeZ(double angle, double distance)
	{
		return distance * Math.sin(angle);
	}
	
	private double getXdifference(Block msign, Block blockPlaced)
	{
		double mx = msign.getX();
		double bx = blockPlaced.getX();
		return bx - mx; //returns the amount you should add to ssign's x to get the new block coordinate.
	}
	
	private double getZdifference(Block msign, Block blockPlaced)
	{
		double mz = msign.getZ();
		double bz = blockPlaced.getZ();
		return bz - mz; //returns the amount you'd add to master sign's z to get to the original block
	}
	
	private double getHeightDifference(Block sign, Block block)
	{
		double ys = sign.getY();
		double yb = block.getY();
		return yb - ys; //we use -ys because then we just have to add this height difference to the other sign's Y to get the new Y.
	}
	
	//begin the function that calculates angle between master sign and placed block
	private double flatAngleBetweenSignAndBlock(Block sign, Block block)
	{
		int xs = sign.getX();
		int zs = sign.getZ();
		//int ys = sign.getY(); //get the height!
		
		int xb = block.getX();
		int zb = block.getZ();
		double angle = Math.atan2(zs - zb, xs - xb); //atan 2 returns results from -pi to pi IN RADIANS!
		return angle;
	}
	
	private double relativeFlatAngleBetweenSignAndBlock(Block sign, Block block)
	{
		double as = getSignAngle(sign);
		double ab = flatAngleBetweenSignAndBlock(sign,block);
		double relativeAngle = as - ab;
		
		if (plugin.verbose) plugin.logger.info("master angle is "+Double.toString(relativeAngle));
		if (plugin.verbose) plugin.logger.info("absolute angle is "+Double.toString(ab));
		
		return relativeAngle;
	}
	
	//returns the angle from the slave sign to place a block (IT'S NOW IN ABSOLUTE ANGLE AFTER THIS FUNCTION)
	//requires the block slave sign, and the relative angle of the master sign and the placed block.
	private double absoluteAngleSlaveSign(Block ssign, double relativeAngle) //ssign is slave sign. relative angle is the angle between master
	{
		double ass = getSignAngle(ssign);
		double newRelativeAngle = ass - relativeAngle;
		if (plugin.verbose) plugin.logger.info("slave angle is "+Double.toString(newRelativeAngle));
		return newRelativeAngle;
	}
	
	
	private Location getNewBlockLocation(Block ssign, double heightDiff, double xDiff, double zDiff, int directionDiff)
	{
		if (directionDiff > 0) //aka positive
		{
			for (int i = 0; i < directionDiff; i++)
			{
				double tempX = -zDiff;
				double tempZ = xDiff;
				xDiff = tempX;
				zDiff = tempZ; //rotation of 90deg. We keep rotating in this fashion to get the desired coordinates
			}
		}
		if (directionDiff < 0) //aka negative
		{
			for (int i = 0; i > directionDiff; i--)
			{
				double tempX = zDiff;
				double tempZ = -xDiff;
				xDiff = tempX;
				zDiff = tempZ; //rotation of -90deg. We keep rotating in this fashion to get the desired coordinates
			}
		}
		//and if directionDiff == 0, then we just use the coordinates given
		return getNewBlockLocationCartesian(ssign, xDiff, heightDiff, zDiff);
	}
	
	//requires the slave sign block and the relative angle from the MASTER SIGN to the block placed, and its distance too
	//also requires the height difference (Yblock - Ysign) between master sign and block placed.
	private Location getNewBlockLocation(Block ssign, double heightDifference, double relativeAngle, double relativeDistance)
	{
		double angle = absoluteAngleSlaveSign(ssign,relativeAngle); //get the absolute angle to project the block from sign
		double xr = getNewRelativeX(angle,relativeDistance);
		double zr = getNewRelativeZ(angle,relativeDistance);
		return getNewBlockLocationCartesian(ssign, xr, heightDifference, zr); 
	}
	
	private Location getNewBlockLocationCartesian(Block ssign, double xr, double yr, double zr)
	{
		Location locss = ssign.getLocation();
		double x = locss.getX();
		double z = locss.getZ();
		double y = locss.getY();
		//and now we set the location again to get our new location
		locss.setX(x + xr);
		locss.setY(y + yr);
		locss.setZ(z + zr);
		return locss;
	}
	
	//----------------------------------------------------------------------------------------------------------------
	//----------------------------------------------------------------------------------------------------------------
	// this is the function you should call after having only one slave sign to content with
	//should there be more, you'll want to calculate the height, angle, and distance AND THEN make calls to the getNewBlockLocation
	//while not using the master sign (it's best to not repeat calculations all the time)
	//but you can still call this one. It's just best not to keep repeating the same calculations. Find a way to conserve cpu resources
	public Location getNewBlockLocation(Block msign, Block ssign, Block block)
	{
		double heightDiff = getHeightDifference(msign,block);
		double relativeAngle = relativeFlatAngleBetweenSignAndBlock(msign, block) + Math.PI; //change the rotation so it's supposedly correct (with 180 degree flip) IT WORKS! NOW we just need to monitor block breaks. If it breaks the slave we set it back to null
		double relativeDistance = flatDistanceBetweenSignAndBlock(msign, block);
		Location locss = getNewBlockLocation(ssign,heightDiff,relativeAngle,relativeDistance);
		return locss;
	}
	
	
	//this is just like the above function (The one you should use for quick doubling mirroring) except that it takes in an extra argument
	//that specifies at what fraction of a circle to get block. So in other words you use this in your for loop to get the block that's 120degrees
	//from the current placed block! (to be able to place in that cool geometric way)
	public Location getNewBlockLocation(Block msign, Block ssign, Block block, double fractionOfCircle)
	{
		double heightDiff = getHeightDifference(msign,block);
		double relativeAngle = relativeFlatAngleBetweenSignAndBlock(msign, block) + Math.PI + 2* Math.PI * fractionOfCircle; //change the rotation so it's supposedly correct (with 180 degree flip) IT WORKS! NOW we just need to monitor block breaks. If it breaks the slave we set it back to null
		double relativeDistance = flatDistanceBetweenSignAndBlock(msign, block);
		Location locss = getNewBlockLocation(ssign,heightDiff,relativeAngle,relativeDistance);
		return locss;
	}
	
	private int getENWSdirection(Block msign)
	{
		double as = getSignAngle(msign);
		int sharp = (int) Math.round(as/(Math.PI/2D)); //round to nearest multiple of 90deg.
		if (plugin.verbose) plugin.logger.info("angle of sign rounded is "+Integer.toString(sharp));
		return sharp; //the sharp 90 degree angle (0,90,180,270)
	}
	
	//Minecraft's directional system is incredibly messed up.
	//we assume x is x, and z is y
	//standard cartesian coordinates such that EAST means positive x and NORTH means positive Z (y)
	private double convertBlockFaceToAngleDegrees(BlockFace bf)
	{
		double angle = 0D;
		if (bf.compareTo(BlockFace.EAST) == 0)
		{
			return -90D;
		}
		if (bf.compareTo(BlockFace.EAST_SOUTH_EAST) == 0)
		{
			return -67.5D;
		}
		if (bf.compareTo(BlockFace.SOUTH_EAST) == 0)
		{
			return -45D;
		}
		if (bf.compareTo(BlockFace.SOUTH_SOUTH_EAST) == 0)
		{
			return -22.5D;
		}
		if (bf.compareTo(BlockFace.SOUTH) == 0)
		{
			return 0D;
		}
		if (bf.compareTo(BlockFace.SOUTH_SOUTH_WEST) == 0)
		{
			return 22.5D;
		}
		if (bf.compareTo(BlockFace.SOUTH_WEST) == 0)
		{
			return 45D;
		}
		if (bf.compareTo(BlockFace.WEST_SOUTH_WEST) == 0)
		{
			return 67.5D;
		}
		if (bf.compareTo(BlockFace.WEST) == 0)
		{
			return 90D;
		}
		if (bf.compareTo(BlockFace.WEST_NORTH_WEST) == 0)
		{
			return 112.5D;
		}
		if (bf.compareTo(BlockFace.NORTH_WEST) == 0)
		{
			return 135D;
		}
		if (bf.compareTo(BlockFace.NORTH_NORTH_WEST) == 0)
		{
			return 157.5D;
		}
		if (bf.compareTo(BlockFace.NORTH) == 0)
		{
			return 180D;
		}
		if (bf.compareTo(BlockFace.NORTH_NORTH_EAST) == 0)
		{
			return -157.5D;
		}
		if (bf.compareTo(BlockFace.NORTH_EAST) == 0)
		{
			return -135D;
		}
		if (bf.compareTo(BlockFace.EAST_NORTH_EAST) == 0)
		{
			return -112.5D;
		}
		return angle; //default angle that should NEVER happen.
	}
}
