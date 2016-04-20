package gr.watchful.whosebed;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;

public class MyEventListener {
	private HashMap<UUID, BlockPos> players;
	private HashMap<BlockPos, HashSet<UUID>> spawns;
	private Boolean playersLoaded;

	public MyEventListener() {
		playersLoaded = false;
		init();
	}

	private void init() {
		players = new HashMap<UUID, BlockPos>();
		spawns = new HashMap<BlockPos, HashSet<UUID>>();
	}

	@SubscribeEvent
	public void onWorldLoadEvent(WorldEvent.Load event) {
		if (!event.getWorld().isRemote) return;
		if (playersLoaded) return;
		init();
		File playerDataFolder = new File(DimensionManager.getCurrentSaveRootDirectory(), "playerdata");
		if (playerDataFolder == null || !playerDataFolder.exists() || !playerDataFolder.isDirectory()) {
			System.out.println("Can't find playerdata folder: "+playerDataFolder.getAbsolutePath());
			return;
		}
		for (File file : playerDataFolder.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".dat");
			}
		})) {
			try {
				NBTTagCompound playerTag = CompressedStreamTools.readCompressed(new FileInputStream(file));

				if (playerTag.hasKey("SpawnX") && playerTag.hasKey("SpawnY") && playerTag.hasKey("SpawnZ")) {
					BlockPos spawnPoint = new BlockPos(playerTag.getInteger("SpawnX"), playerTag.getInteger("SpawnY"), playerTag.getInteger("SpawnZ"));
					UUID uuid = UUID.fromString(file.getName().substring(0,file.getName().length() - 4));
					addSpawn(uuid, spawnPoint);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		playersLoaded = true;
	}

	@SubscribeEvent
	public void onWorldUnloadEvent(WorldEvent.Unload event) {
		System.out.println("Unloading");
		if (event.getWorld().isRemote) playersLoaded = false;
	}

	@SubscribeEvent
	public void onPlayerWakeUpEvent(PlayerWakeUpEvent event) {
		if (event.getEntityPlayer().isServerWorld() && event.shouldSetSpawn()) {
			BlockPos spawnPoint = new BlockPos(event.getEntityPlayer().posX, event.getEntityPlayer().posY, event.getEntityPlayer().posZ);
			updatePlayerSpawn(event.getEntityPlayer(), spawnPoint);
		}
	}

	@SubscribeEvent
	public void onPlayerInteractEvent(PlayerInteractEvent.RightClickBlock event) {
		if (!event.getWorld().isRemote && event.getEntityPlayer().isSneaking() && event.getWorld().getBlockState(event.getPos()).getBlock().equals(Blocks.bed)) {
			BlockPos pos = event.getPos();

			Block block = event.getWorld().getBlockState(pos).getBlock();
			if (block.isBedFoot(event.getWorld(), pos)) {
				pos = pos.offset(block.getBedDirection(event.getWorld().getBlockState(pos), event.getWorld(), pos));
			}

			System.out.println(event.getEntityPlayer().getName()+" clicked a bed at "+pos);
			HashSet<String> players = getPlayersAtSpawn(pos, event.getWorld());

			StringBuilder bldr = new StringBuilder();
			if (players == null || players.size() == 0) bldr.append("No one sleeps here");
			else {
				bldr.append("This is the bed of ");
				int i = 0;
				for (String player : players) {
					if (players.size() != 1 && i != 0) {
						if (players.size() - 1 == i) bldr.append(" and ");
						else bldr.append(", ");
					}
					bldr.append(player);
					i++;
				}
			}

			event.getEntityPlayer().addChatMessage(new TextComponentString(bldr.toString()));
			event.setCanceled(true);
		}
	}

	private HashSet<String> getPlayersAtSpawn(BlockPos location, World world) {
		if (!spawns.containsKey(location)) return null;

		HashSet<String> playerNames = new HashSet<String>();
		for (UUID player : spawns.get(location)) {
			playerNames.add(world.getMinecraftServer().getPlayerProfileCache().getProfileByUUID(player).getName());
		}
		return playerNames;
	}

	private void updatePlayerSpawn(EntityPlayer player, BlockPos spawnPoint) {
		UUID uuid = player.getUniqueID();
		BlockPos oldSpawnPoint = players.get(uuid);
		if (oldSpawnPoint != null) {
			if (oldSpawnPoint.equals(spawnPoint)) return;
			else if (spawns.containsKey(oldSpawnPoint)) {
				if (spawns.get(oldSpawnPoint).size() > 1) {
					spawns.get(oldSpawnPoint).remove(uuid);
				} else {
					spawns.remove(oldSpawnPoint);
				}
			}
		}
		addSpawn(uuid, spawnPoint);
	}

	private void addSpawn(UUID uuid, BlockPos spawnPoint) {
		System.out.println(uuid + " : " + spawnPoint);
		players.put(uuid, spawnPoint);
		if (!spawns.containsKey(spawnPoint)) {
			spawns.put(spawnPoint, new HashSet<UUID>());
		}
		spawns.get(spawnPoint).add(uuid);
	}
}
