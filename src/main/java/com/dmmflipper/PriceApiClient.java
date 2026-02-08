package com.dmmflipper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Singleton
public class PriceApiClient
{
	private static final String API_BASE = "https://prices.runescape.wiki/api/v1/dmm";
	private static final String USER_AGENT = "DMM Flipper RuneLite Plugin";

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final ScheduledExecutorService executor;

	private Map<Integer, ItemInfo> itemMapping = new ConcurrentHashMap<>();
	private Map<Integer, PriceData> latestPrices = new ConcurrentHashMap<>();
	private List<FlipOpportunity> opportunities = new ArrayList<>();
	private ScheduledFuture<?> updateTask;

	@Inject
	public PriceApiClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.gson = gson;
		this.executor = Executors.newSingleThreadScheduledExecutor();
	}

	public void startPriceUpdates()
	{
		// Fetch data in background thread
		executor.execute(() -> {
			fetchItemMapping();
			fetchLatestPrices();
		});

		// Schedule periodic updates (every 60 seconds)
		updateTask = executor.scheduleAtFixedRate(
			this::fetchLatestPrices,
			60,
			60,
			TimeUnit.SECONDS
		);
	}

	public void stopPriceUpdates()
	{
		if (updateTask != null)
		{
			updateTask.cancel(false);
		}
	}

	public void fetchItemMapping()
	{
		Request request = new Request.Builder()
			.url(API_BASE + "/mapping")
			.header("User-Agent", USER_AGENT)
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			if (response.isSuccessful() && response.body() != null)
			{
				String json = response.body().string();
				ItemInfo[] items = gson.fromJson(json, ItemInfo[].class);
				
				itemMapping.clear();
				for (ItemInfo item : items)
				{
					itemMapping.put(item.getId(), item);
				}
				
				log.info("Loaded {} items", itemMapping.size());
			}
		}
		catch (IOException e)
		{
			log.error("Error fetching item mapping", e);
		}
	}

	public void fetchLatestPrices()
	{
		// Clear before fetching fresh data
		latestPrices.clear();
		
		// Try both latest and 5m endpoints to get more coverage
		fetchPricesFromEndpoint(API_BASE + "/latest");
		fetchPricesFromEndpoint(API_BASE + "/5m");
		
		log.info("Total items after fetching all endpoints: {}", latestPrices.size());
	}

	private void fetchPricesFromEndpoint(String endpoint)
	{
		Request request = new Request.Builder()
			.url(endpoint)
			.header("User-Agent", USER_AGENT)
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			if (response.isSuccessful() && response.body() != null)
			{
				String json = response.body().string();
				
				// Log first item to see structure
				if (endpoint.contains("latest"))
				{
					log.info("Sample API response (first 500 chars): {}", json.substring(0, Math.min(500, json.length())));
				}
				
				JsonObject root = gson.fromJson(json, JsonObject.class);
				JsonObject data = root.getAsJsonObject("data");

				int newItems = 0;
				int updatedItems = 0;
				boolean loggedSample = false;
				
				for (String itemIdStr : data.keySet())
				{
					int itemId = Integer.parseInt(itemIdStr);
					JsonObject priceObj = data.getAsJsonObject(itemIdStr);
					
					// Log first item structure
					if (!loggedSample && endpoint.contains("latest"))
					{
						log.info("Sample item data for ID {}: {}", itemId, priceObj.toString());
						loggedSample = true;
					}
					
					PriceData priceData = new PriceData();
					priceData.setHigh(priceObj.has("high") ? priceObj.get("high").getAsInt() : 0);
					priceData.setLow(priceObj.has("low") ? priceObj.get("low").getAsInt() : 0);
					priceData.setHighTime(priceObj.has("highTime") ? priceObj.get("highTime").getAsLong() : 0);
					priceData.setLowTime(priceObj.has("lowTime") ? priceObj.get("lowTime").getAsLong() : 0);
					priceData.setHighVolume(priceObj.has("highPriceVolume") ? priceObj.get("highPriceVolume").getAsInt() : 0);
					priceData.setLowVolume(priceObj.has("lowPriceVolume") ? priceObj.get("lowPriceVolume").getAsInt() : 0);

					// Only add/update if we don't have this item or if this data is newer
					PriceData existing = latestPrices.get(itemId);
					if (existing == null)
					{
						latestPrices.put(itemId, priceData);
						newItems++;
					}
					else
					{
						// Update if this data is more recent
						if (priceData.getHighTime() > existing.getHighTime() || 
							priceData.getLowTime() > existing.getLowTime())
						{
							latestPrices.put(itemId, priceData);
							updatedItems++;
						}
					}
					
					// Log high-value items
					ItemInfo itemInfo = itemMapping.get(itemId);
					if (itemInfo != null && priceData.getHigh() > 2000000)
					{
						log.info("High-value item from {}: {} (ID: {}) - Buy: {}, Sell: {}", 
							endpoint, itemInfo.getName(), itemId, priceData.getLow(), priceData.getHigh());
					}
				}

				log.info("Loaded from {}: {} new items, {} updated items, {} total", 
					endpoint, newItems, updatedItems, latestPrices.size());
			}
		}
		catch (IOException e)
		{
			log.error("Error fetching prices from " + endpoint, e);
		}
	}

	public List<FlipOpportunity> calculateOpportunities(int minProfit, int minROI, int maxROI, int maxAgeMinutes, int budget)
	{
		List<FlipOpportunity> opps = new ArrayList<>();
		long currentTime = System.currentTimeMillis() / 1000;
		
		int totalItems = 0;
		int filteredByPrice = 0;
		int filteredByAge = 0;
		int filteredByProfit = 0;
		int filteredByBudget = 0;
		int filteredByROI = 0;

		log.info("Starting opportunity calculation with filters: minProfit={}, minROI={}%, maxROI={}%, maxAge={}min, budget={}",
			minProfit, minROI, maxROI, maxAgeMinutes, budget);

		for (Map.Entry<Integer, PriceData> entry : latestPrices.entrySet())
		{
			int itemId = entry.getKey();
			PriceData priceData = entry.getValue();
			totalItems++;

			// Skip if no prices
			if (priceData.getHigh() == 0 || priceData.getLow() == 0)
			{
				filteredByPrice++;
				continue;
			}

			// Check age - both buy and sell must be recent
			long buyTime = priceData.getLowTime();
			long sellTime = priceData.getHighTime();
			
			// Skip if either timestamp is missing
			if (buyTime == 0 || sellTime == 0)
			{
				filteredByAge++;
				continue;
			}
			
			int buyAgeMinutes = (int) ((currentTime - buyTime) / 60);
			int sellAgeMinutes = (int) ((currentTime - sellTime) / 60);
			
			// Get item info for logging
			ItemInfo itemInfo = itemMapping.get(itemId);
			String itemName = itemInfo != null ? itemInfo.getName() : "Unknown";
			
			// Log Vesta's longsword specifically
			if (itemName.toLowerCase().contains("vesta"))
			{
				log.info("Found Vesta item: {} (ID: {}) - Buy: {}, Sell: {}, BuyAge: {}m, SellAge: {}m, BuyTime: {}, SellTime: {}, CurrentTime: {}",
					itemName, itemId, priceData.getLow(), priceData.getHigh(), 
					buyAgeMinutes, sellAgeMinutes, buyTime, sellTime, currentTime);
			}
			
			// Skip if either buy or sell is too old
			if (buyAgeMinutes > maxAgeMinutes || sellAgeMinutes > maxAgeMinutes)
			{
				filteredByAge++;
				if (itemName.toLowerCase().contains("vesta"))
				{
					log.info("Vesta item FILTERED by age: buyAge={}m, sellAge={}m, maxAge={}m", 
						buyAgeMinutes, sellAgeMinutes, maxAgeMinutes);
				}
				continue;
			}
			
			// Use the most recent for display
			long latestTime = Math.max(buyTime, sellTime);
			int ageMinutes = (int) ((currentTime - latestTime) / 60);

			// Get item info (already fetched above for logging)
			if (itemInfo == null)
			{
				continue;
			}

			int buyPrice = priceData.getLow();
			int sellPrice = priceData.getHigh();

			// Calculate GE tax (1% capped at 5M)
			int geTax = Math.min((int) (sellPrice * 0.01), 5_000_000);

			// Calculate profit
			int profit = sellPrice - buyPrice - geTax;

			if (profit <= 0)
			{
				continue;
			}

			// Calculate ROI
			double roi = (profit / (double) buyPrice) * 100;

			// Filter by thresholds
			if (profit < minProfit)
			{
				filteredByProfit++;
				continue;
			}
			
			if (roi < minROI || roi > maxROI)
			{
				filteredByROI++;
				// Log high-value items that are filtered by ROI
				if (profit > 50000)
				{
					log.info("High-profit item filtered by ROI: {} (ID: {}) - Profit: {}, ROI: {:.2f}%, Buy: {}, Sell: {}",
						itemInfo.getName(), itemId, profit, roi, buyPrice, sellPrice);
				}
				continue;
			}

			// Check if affordable
			int limit = itemInfo.getLimit() > 0 ? itemInfo.getLimit() : 1;
			if (buyPrice * limit > budget && buyPrice > budget)
			{
				filteredByBudget++;
				continue;
			}

			FlipOpportunity opp = new FlipOpportunity(
				itemId,
				itemInfo.getName(),
				buyPrice,
				sellPrice,
				profit,
				roi,
				geTax,
				limit,
				ageMinutes,
				priceData.getLowVolume(),
				priceData.getHighVolume(),
				"unknown",
				true
			);

			opps.add(opp);
		}

		// Sort by profit
		opps.sort((a, b) -> Integer.compare(b.getProfit(), a.getProfit()));

		log.info("Opportunity calculation: {} total items, {} filtered (no price: {}, age: {}, profit: {}, ROI: {}, budget: {}), {} opportunities found",
			totalItems, filteredByPrice + filteredByAge + filteredByProfit + filteredByROI + filteredByBudget,
			filteredByPrice, filteredByAge, filteredByProfit, filteredByROI, filteredByBudget, opps.size());
		
		// Log top 5 opportunities for debugging
		if (!opps.isEmpty())
		{
			log.info("Top 5 opportunities:");
			for (int i = 0; i < Math.min(5, opps.size()); i++)
			{
				FlipOpportunity opp = opps.get(i);
				log.info("  {}. {} - Profit: {}, ROI: {:.2f}%, Buy: {}, Sell: {}, Limit: {}",
					i + 1, opp.getItemName(), opp.getProfit(), opp.getRoi(), 
					opp.getBuyPrice(), opp.getSellPrice(), opp.getLimit());
			}
		}

		this.opportunities = opps;
		return opps;
	}

	public List<FlipOpportunity> getOpportunities()
	{
		return opportunities;
	}

	public PriceData getPriceData(int itemId)
	{
		return latestPrices.get(itemId);
	}

	public ItemInfo getItemInfo(int itemId)
	{
		return itemMapping.get(itemId);
	}
}
