package com.example.examplemod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = WhoseBed.MODID, version = WhoseBed.VERSION, acceptableRemoteVersions = "*")
public class WhoseBed
{
    public static final String MODID = "whosebed";
    public static final String VERSION = "1.0";

	@Mod.Instance
	public static WhoseBed instance = new WhoseBed();

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
	}

	@EventHandler
	public void init(FMLInitializationEvent e) {
		MinecraftForge.EVENT_BUS.register(new MyEventListener());
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent e) {

	}
}
