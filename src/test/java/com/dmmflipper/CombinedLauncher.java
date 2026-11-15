package com.dmmflipper;

import com.harbourmaster.HarbourmasterPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Dev-mode launcher that loads DMM Flipper and Harbourmaster together. */
public class CombinedLauncher
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(DMMFlipperPlugin.class, HarbourmasterPlugin.class);
		RuneLite.main(args);
	}
}
