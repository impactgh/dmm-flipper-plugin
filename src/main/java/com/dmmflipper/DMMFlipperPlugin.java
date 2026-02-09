package com.dmmflipper;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "DMM Flipper",
	description = "Real-time DMM flipping assistant with price tracking",
	tags = {"dmm", "flipping", "grand exchange", "trading"}
)
public class DMMFlipperPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private DMMFlipperConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private PriceApiClient priceApiClient;

	@Inject
	private GEOfferTracker geOfferTracker;

	@Inject
	private FlipHistory flipHistory;

	private DMMFlipperPanel panel;
	private NavigationButton navButton;
	private OfferExporter offerExporter;

	@Override
	protected void startUp() throws Exception
	{
		log.info("DMM Flipper started!");

		// Initialize offer exporter
		offerExporter = new OfferExporter(geOfferTracker, priceApiClient);
		log.info("Exporting GE offers to: {}", offerExporter.getExportPath());

		// Create the panel
		panel = new DMMFlipperPanel(this, priceApiClient, geOfferTracker, flipHistory);
		
		// Create navigation button
		BufferedImage icon = null;
		try
		{
			icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		}
		catch (Exception e)
		{
			log.warn("Could not load icon: {}", e.getMessage());
			// Create a simple default icon if loading fails
			icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		}
		
		navButton = NavigationButton.builder()
			.tooltip("DMM Flipper")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);

		// Start price updates
		priceApiClient.startPriceUpdates();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("DMM Flipper stopped!");
		clientToolbar.removeNavigation(navButton);
		priceApiClient.stopPriceUpdates();
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		// Track GE offers
		geOfferTracker.updateOffer(event);
		panel.updateOfferDisplay();
		panel.updateProfitLabels();
		
		// Export offers for webapp
		if (offerExporter != null)
		{
			offerExporter.exportOffers();
		}
	}

	@Provides
	DMMFlipperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DMMFlipperConfig.class);
	}

	public void refreshFlips()
	{
		priceApiClient.fetchLatestPrices();
	}
}
