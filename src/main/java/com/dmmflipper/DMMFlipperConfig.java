package com.dmmflipper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("dmmflipper")
public interface DMMFlipperConfig extends Config
{
	@ConfigItem(
		keyName = "minProfit",
		name = "Min Profit",
		description = "Minimum profit per flip (GP)"
	)
	default int minProfit()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "minROI",
		name = "Min ROI %",
		description = "Minimum return on investment percentage"
	)
	@Range(min = 1, max = 100)
	default int minROI()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "maxROI",
		name = "Max ROI %",
		description = "Maximum ROI to filter fake margins"
	)
	@Range(min = 50, max = 500)
	default int maxROI()
	{
		return 200;
	}

	@ConfigItem(
		keyName = "maxAge",
		name = "Max Age (minutes)",
		description = "Maximum age of price data in minutes"
	)
	@Range(min = 5, max = 120)
	default int maxAge()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "budget",
		name = "Budget (GP)",
		description = "Your available GP for flipping"
	)
	default int budget()
	{
		return 1000000;
	}

	@ConfigItem(
		keyName = "refreshInterval",
		name = "Refresh Interval (seconds)",
		description = "How often to refresh prices"
	)
	@Range(min = 30, max = 300)
	default int refreshInterval()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "staleOfferThreshold",
		name = "Stale Offer Threshold %",
		description = "Alert when offer price differs by this % from current"
	)
	@Range(min = 5, max = 50)
	default int staleOfferThreshold()
	{
		return 10;
	}
}
