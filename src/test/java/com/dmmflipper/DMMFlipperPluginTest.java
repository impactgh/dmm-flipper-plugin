package com.dmmflipper;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class DMMFlipperPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DMMFlipperPlugin.class);
		RuneLite.main(args);
	}
}
