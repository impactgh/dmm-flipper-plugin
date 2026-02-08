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
		// Clear old data before fetching fresh data
		latestPrices.clear();
		
		// Try multiple endpoints to get more coverage
		log.info("Fetching prices from multiple endpoints...");
		fetchPricesFromEndpoint(API_BASE + "/latest");
		fetchPricesFromEndpoint(API_BASE + "/5m");
		fetchPricesFromEndpoint(API_BASE + "/1h");
		
		// Count high-value items after all fetches
		int highValueCount = 0;
		for (PriceData pd : latestPrices.values())
		{
			if (pd.getHigh() > 1000000)
			{
				highValueCount++;
			}
		}
		
		log.info("Total items after fetching all endpoints: {}, items >1M: {}", latestPrices.size(), highValueCount);
	}

	private void fetchPricesFromEndpoint(String endpoint)
	{
		log.info("Starting fetch from: {}", endpoint);
		
		Request request = new Request.Builder()
			.url(endpoint)
			.header("User-Agent", USER_AGENT)
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			log.info("Response from {}: success={}, hasBody={}", endpoint, response.isSuccessful(), response.body() != null);
			
			if (response.isSuccessful() && response.body() != null)
			{
				String json = response.body().string();
				log.info("JSON length from {}: {} chars", endpoint, json.length());
				
				// Log first 500 chars to see structure (only for /latest to avoid spam)
				if (endpoint.contains("latest"))
				{
					log.info("Sample API response (first 500 chars): {}", json.substring(0, Math.min(500, json.length())));
				}
				
				JsonObject root = gson.fromJson(json, JsonObject.class);
				JsonObject data = root.getAsJsonObject("data");
				
				log.info("Parsed JSON from {}, data object has {} items", endpoint, data.size());

				int newItems = 0;
				int updatedItems = 0;
				int skippedOlder = 0;
				boolean loggedSample = false;
				int highValueCount = 0;
				
				// Determine if this is a time-series endpoint (5m, 1h) or instant (latest)
				boolean isTimeSeries = endpoint.contains("/5m") || endpoint.contains("/1h");
				
				for (String itemIdStr : data.keySet())
				{
					int itemId = Integer.parseInt(itemIdStr);
					JsonObject priceObj = data.getAsJsonObject(itemIdStr);
					
					// Log first item structure (only for /latest)
					if (!loggedSample && endpoint.contains("latest"))
					{
						log.info("Sample item data for ID {}: {}", itemId, priceObj.toString());
						loggedSample = true;
					}
					
					PriceData priceData = new PriceData();
					
					// Different field names for different endpoints
					if (isTimeSeries)
					{
						// 5m and 1h endpoints use avgHighPrice/avgLowPrice
						// Handle null values properly
						priceData.setHigh(priceObj.has("avgHighPrice") && !priceObj.get("avgHighPrice").isJsonNull() 
							? priceObj.get("avgHighPrice").getAsInt() : 0);
						priceData.setLow(priceObj.has("avgLowPrice") && !priceObj.get("avgLowPrice").isJsonNull() 
							? priceObj.get("avgLowPrice").getAsInt() : 0);
						// Use current time as timestamp for time-series data since they don't have specific timestamps
						long currentTime = System.currentTimeMillis() / 1000;
						priceData.setHighTime(currentTime);
						priceData.setLowTime(currentTime);
						priceData.setHighVolume(priceObj.has("highPriceVolume") && !priceObj.get("highPriceVolume").isJsonNull() 
							? priceObj.get("highPriceVolume").getAsInt() : 0);
						priceData.setLowVolume(priceObj.has("lowPriceVolume") && !priceObj.get("lowPriceVolume").isJsonNull() 
							? priceObj.get("lowPriceVolume").getAsInt() : 0);
					}
					else
					{
						// /latest endpoint uses high/low
						// Handle null values properly
						priceData.setHigh(priceObj.has("high") && !priceObj.get("high").isJsonNull() 
							? priceObj.get("high").getAsInt() : 0);
						priceData.setLow(priceObj.has("low") && !priceObj.get("low").isJsonNull() 
							? priceObj.get("low").getAsInt() : 0);
						priceData.setHighTime(priceObj.has("highTime") && !priceObj.get("highTime").isJsonNull() 
							? priceObj.get("highTime").getAsLong() : 0);
						priceData.setLowTime(priceObj.has("lowTime") && !priceObj.get("lowTime").isJsonNull() 
							? priceObj.get("lowTime").getAsLong() : 0);
						priceData.setHighVolume(priceObj.has("highPriceVolume") && !priceObj.get("highPriceVolume").isJsonNull() 
							? priceObj.get("highPriceVolume").getAsInt() : 0);
						priceData.setLowVolume(priceObj.has("lowPriceVolume") && !priceObj.get("lowPriceVolume").isJsonNull() 
							? priceObj.get("lowPriceVolume").getAsInt() : 0);
					}

					// Count high-value items in this endpoint
					if (priceData.getHigh() > 1000000)
					{
						highValueCount++;
						// Log the first few high-value items
						ItemInfo itemInfo = itemMapping.get(itemId);
						String name = itemInfo != null ? itemInfo.getName() : "Unknown";
						if (highValueCount <= 5)
						{
							log.info("High-value item from {}: {} (ID: {}) - Buy: {}, Sell: {}, BuyTime: {}, SellTime: {}",
								endpoint, name, itemId, priceData.getLow(), priceData.getHigh(),
								priceData.getLowTime(), priceData.getHighTime());
						}
					}

					// Only add/update if we don't have this item or if this data is newer
					PriceData existing = latestPrices.get(itemId);
					if (existing == null)
					{
						latestPrices.put(itemId, priceData);
						newItems++;
					}
					else
					{
						// For time-series data, prefer /latest data (more precise timestamps)
						// Only update if this is /latest or if we don't have data yet
						if (!isTimeSeries)
						{
							// This is /latest data - always prefer it
							latestPrices.put(itemId, priceData);
							updatedItems++;
						}
						else
						{
							// This is time-series data - only use if we don't have /latest data
							// Check if existing data has real timestamps (from /latest)
							if (existing.getHighTime() == 0 && existing.getLowTime() == 0)
							{
								latestPrices.put(itemId, priceData);
								updatedItems++;
							}
							else
							{
								skippedOlder++;
							}
						}
					}
				}

				log.info("Loaded from {}: {} new, {} updated, {} skipped, {} total items, {} items >1M in this endpoint",
					endpoint, newItems, updatedItems, skippedOlder, latestPrices.size(), highValueCount);
			}
		}
		catch (IOException e)
		{
			log.error("Error fetching prices from " + endpoint, e);
		}
		catch (Exception e)
		{
			log.error("Unexpected error fetching prices from " + endpoint, e);
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
		
		// Count high-value items in price data
		int highValueInPrices = 0;
		for (PriceData pd : latestPrices.values())
		{
			if (pd.getHigh() > 1000000)
			{
				highValueInPrices++;
			}
		}
		log.info("Price data contains {} items with sell price >1M", highValueInPrices);

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
			
			// Log expensive items to debug
			if (priceData.getHigh() > 1000000)
			{
				log.info("Processing expensive item: {} (ID: {}) - Buy: {}, Sell: {}, BuyAge: {}m, SellAge: {}m",
					itemName, itemId, priceData.getLow(), priceData.getHigh(), buyAgeMinutes, sellAgeMinutes);
			}
			
			// Skip if either buy or sell is too old
			if (buyAgeMinutes > maxAgeMinutes || sellAgeMinutes > maxAgeMinutes)
			{
				filteredByAge++;
				if (priceData.getHigh() > 1000000)
				{
					log.info("Expensive item FILTERED by age: {} - buyAge={}m, sellAge={}m, maxAge={}m", 
						itemName, buyAgeMinutes, sellAgeMinutes, maxAgeMinutes);
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
				// Log expensive items that are filtered by ROI
				if (priceData.getHigh() > 1000000)
				{
					log.info("Expensive item filtered by ROI: {} (ID: {}) - Profit: {}, ROI: {}%, Buy: {}, Sell: {}, minROI: {}%, maxROI: {}%",
						itemInfo.getName(), itemId, profit, String.format("%.2f", roi), buyPrice, sellPrice, minROI, maxROI);
				}
				continue;
			}

			// Check if affordable
			int limit = itemInfo.getLimit() > 0 ? itemInfo.getLimit() : 1;
			if (buyPrice * limit > budget && buyPrice > budget)
			{
				filteredByBudget++;
				// Log expensive items filtered by budget
				if (priceData.getHigh() > 1000000)
				{
					log.info("Expensive item filtered by budget: {} (ID: {}) - Profit: {}, Buy: {}, Budget: {}, Limit: {}, BuyPrice*Limit: {}",
						itemInfo.getName(), itemId, profit, buyPrice, budget, limit, (buyPrice * limit));
				}
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
				log.info("  {}. {} - Profit: {}, ROI: {}%, Buy: {}, Sell: {}, Limit: {}",
					i + 1, opp.getItemName(), opp.getProfit(), String.format("%.2f", opp.getRoi()), 
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
