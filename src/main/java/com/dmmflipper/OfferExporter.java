package com.dmmflipper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class OfferExporter
{
	private static final String EXPORT_DIR = System.getProperty("user.home") + "/.runelite/dmm-flipper";
	private static final String EXPORT_FILE = EXPORT_DIR + "/offers.json";
	
	private final Gson gson;
	private final GEOfferTracker offerTracker;
	private final PriceApiClient priceApiClient;
	
	public OfferExporter(GEOfferTracker offerTracker, PriceApiClient priceApiClient)
	{
		this.offerTracker = offerTracker;
		this.priceApiClient = priceApiClient;
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		
		// Ensure export directory exists
		try
		{
			Path exportPath = Paths.get(EXPORT_DIR);
			if (!Files.exists(exportPath))
			{
				Files.createDirectories(exportPath);
				log.info("Created export directory: {}", EXPORT_DIR);
			}
		}
		catch (IOException e)
		{
			log.error("Failed to create export directory", e);
		}
	}
	
	public void exportOffers()
	{
		try
		{
			List<GEOfferTracker.TrackedOffer> activeOffers = offerTracker.getActiveOffers();
			
			// Build export data
			Map<String, Object> exportData = new HashMap<>();
			exportData.put("timestamp", System.currentTimeMillis() / 1000);
			exportData.put("version", "1.0");
			
			List<Map<String, Object>> offersData = new ArrayList<>();
			for (GEOfferTracker.TrackedOffer offer : activeOffers)
			{
				Map<String, Object> offerData = new HashMap<>();
				offerData.put("itemId", offer.getItemId());
				offerData.put("type", offer.isBuying() ? "BUY" : "SELL");
				offerData.put("price", offer.getPrice());
				offerData.put("qty", offer.getQuantity());
				offerData.put("filled", offer.getQuantityFilled());
				offerData.put("timestamp", offer.getTimestamp() / 1000);
				
				// Add item name if available
				ItemInfo itemInfo = priceApiClient.getItemInfo(offer.getItemId());
				if (itemInfo != null)
				{
					offerData.put("itemName", itemInfo.getName());
				}
				
				offersData.add(offerData);
			}
			
			exportData.put("offers", offersData);
			
			// Write to file
			File exportFile = new File(EXPORT_FILE);
			try (FileWriter writer = new FileWriter(exportFile))
			{
				gson.toJson(exportData, writer);
				log.debug("Exported {} offers to {}", offersData.size(), EXPORT_FILE);
			}
		}
		catch (IOException e)
		{
			log.error("Failed to export offers", e);
		}
	}
	
	public String getExportPath()
	{
		return EXPORT_FILE;
	}
}
