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
		latestPrices.clear();
		
		// Fetch price data from latest (most accurate prices)
		fetchPricesFromEndpoint(API_BASE + "/latest");
		
		// Merge volume data from 24h (better volume metrics)
		mergeVolumeData(API_BASE + "/24h");
		
		int highValueCount = 0;
		for (PriceData pd : latestPrices.values())
		{
			if (pd.getHigh() > 1000000)
			{
				highValueCount++;
			}
		}
		
		log.info("Loaded {} items ({} over 1M)", latestPrices.size(), highValueCount);
	}

	private void mergeVolumeData(String endpoint)
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
				JsonObject root = gson.fromJson(json, JsonObject.class);
				JsonObject data = root.getAsJsonObject("data");

				int merged = 0;

				for (String itemIdStr : data.keySet())
				{
					int itemId = Integer.parseInt(itemIdStr);
					JsonObject priceObj = data.getAsJsonObject(itemIdStr);

					// Only merge volume data into existing entries
					PriceData existing = latestPrices.get(itemId);
					if (existing != null)
					{
						// Merge volume from 24h data
						int highVol = priceObj.has("highPriceVolume") && !priceObj.get("highPriceVolume").isJsonNull()
							? priceObj.get("highPriceVolume").getAsInt() : 0;
						int lowVol = priceObj.has("lowPriceVolume") && !priceObj.get("lowPriceVolume").isJsonNull()
							? priceObj.get("lowPriceVolume").getAsInt() : 0;

						existing.setHighVolume(highVol);
						existing.setLowVolume(lowVol);
						merged++;
					}
				}

				log.info("Merged volume data from {}: {} items updated", endpoint, merged);
			}
		}
		catch (Exception e)
		{
			log.error("Error merging volume data from " + endpoint, e);
		}
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
				JsonObject root = gson.fromJson(json, JsonObject.class);
				JsonObject data = root.getAsJsonObject("data");

				boolean isTimeSeries = endpoint.contains("/5m") || endpoint.contains("/1h");
				
				for (String itemIdStr : data.keySet())
				{
					int itemId = Integer.parseInt(itemIdStr);
					JsonObject priceObj = data.getAsJsonObject(itemIdStr);
					
					PriceData priceData = new PriceData();
					
					if (isTimeSeries)
					{
						priceData.setHigh(priceObj.has("avgHighPrice") && !priceObj.get("avgHighPrice").isJsonNull() 
							? priceObj.get("avgHighPrice").getAsInt() : 0);
						priceData.setLow(priceObj.has("avgLowPrice") && !priceObj.get("avgLowPrice").isJsonNull() 
							? priceObj.get("avgLowPrice").getAsInt() : 0);
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

					PriceData existing = latestPrices.get(itemId);
					if (existing == null)
					{
						latestPrices.put(itemId, priceData);
					}
					else if (!isTimeSeries)
					{
						latestPrices.put(itemId, priceData);
					}
				}
			}
		}
		catch (Exception e)
		{
			log.error("Error fetching prices from " + endpoint, e);
		}
	}

	/**
	 * Best Margin Tab: Optimized for high-ROI, high-margin flips
	 * Strategy: Focus on items with best profit per item, regardless of volume
	 * Target: Experienced flippers looking for maximum profit per flip
	 * Sorting: By absolute profit (margin), then by ROI as tiebreaker
	 */
	public List<FlipOpportunity> calculateOpportunities(int minProfit, int minROI, int maxROI, int maxAgeMinutes, int budget)
	{
		List<FlipOpportunity> opps = new ArrayList<>();
		long currentTime = System.currentTimeMillis() / 1000;

		for (Map.Entry<Integer, PriceData> entry : latestPrices.entrySet())
		{
			int itemId = entry.getKey();
			PriceData priceData = entry.getValue();

			if (priceData.getHigh() == 0 || priceData.getLow() == 0)
			{
				continue;
			}

			long buyTime = priceData.getLowTime();
			long sellTime = priceData.getHighTime();

			if (buyTime == 0 || sellTime == 0)
			{
				continue;
			}

			int buyAgeMinutes = (int) ((currentTime - buyTime) / 60);
			int sellAgeMinutes = (int) ((currentTime - sellTime) / 60);

			// Strict age filter for Best Margin tab - we want fresh data
			if (buyAgeMinutes > maxAgeMinutes || sellAgeMinutes > maxAgeMinutes)
			{
				continue;
			}

			ItemInfo itemInfo = itemMapping.get(itemId);
			if (itemInfo == null)
			{
				continue;
			}

			long latestTime = Math.max(buyTime, sellTime);
			int ageMinutes = (int) ((currentTime - latestTime) / 60);

			int buyPrice = priceData.getLow();
			int sellPrice = priceData.getHigh();
			int geTax = Math.min((int) (sellPrice * 0.01), 5_000_000);
			int profit = sellPrice - buyPrice - geTax;

			if (profit <= 0)
			{
				continue;
			}

			double roi = (profit / (double) buyPrice) * 100;

			if (profit < minProfit || roi < minROI || roi > maxROI)
			{
				continue;
			}

			int limit = itemInfo.getLimit() > 0 ? itemInfo.getLimit() : 1;
			if (buyPrice * limit > budget && buyPrice > budget)
			{
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

		// Sort by profit (margin), then by ROI for tiebreaker
		opps.sort((a, b) -> {
			int profitCompare = Integer.compare(b.getProfit(), a.getProfit());
			if (profitCompare != 0) return profitCompare;
			return Double.compare(b.getRoi(), a.getRoi());
		});

		log.info("Found {} best margin opportunities", opps.size());

		this.opportunities = opps;
		return opps;
	}
	/**
	 * Bulk Volume Tab: Optimized for high-volume items with large buy limits
	 * Strategy: Focus on total profit potential (margin × limit) with confirmed volume
	 * Target: Flippers who want to maximize profit per 4-hour cycle
	 * Sorting: By total profit potential (profit × limit)
	 * Key filters: High buy limits (1000+), confirmed 24h volume (50+)
	 */
	public List<FlipOpportunity> calculateBulkOpportunities(int minProfit, int minROI, int maxROI, int maxAgeMinutes, int budget, int minLimit)
	{
		List<FlipOpportunity> opps = new ArrayList<>();
		long currentTime = System.currentTimeMillis() / 1000;

		for (Map.Entry<Integer, PriceData> entry : latestPrices.entrySet())
		{
			int itemId = entry.getKey();
			PriceData priceData = entry.getValue();

			if (priceData.getHigh() == 0 || priceData.getLow() == 0)
			{
				continue;
			}

			long buyTime = priceData.getLowTime();
			long sellTime = priceData.getHighTime();

			if (buyTime == 0 || sellTime == 0)
			{
				continue;
			}

			int buyAgeMinutes = (int) ((currentTime - buyTime) / 60);
			int sellAgeMinutes = (int) ((currentTime - sellTime) / 60);

			// Relaxed age filter for bulk items (they trade less frequently but in larger quantities)
			int bulkMaxAge = Math.max(maxAgeMinutes, 60);

			if (buyAgeMinutes > bulkMaxAge || sellAgeMinutes > bulkMaxAge)
			{
				continue;
			}

			ItemInfo itemInfo = itemMapping.get(itemId);
			if (itemInfo == null)
			{
				continue;
			}

			long latestTime = Math.max(buyTime, sellTime);
			int ageMinutes = (int) ((currentTime - latestTime) / 60);

			int buyPrice = priceData.getLow();
			int sellPrice = priceData.getHigh();
			int geTax = Math.min((int) (sellPrice * 0.01), 5_000_000);
			int profit = sellPrice - buyPrice - geTax;

			// For bulk, we care about per-item profit first
			if (profit <= 0)
			{
				continue;
			}

			double roi = (profit / (double) buyPrice) * 100;

			if (profit < minProfit || roi < minROI || roi > maxROI)
			{
				continue;
			}

			int limit = itemInfo.getLimit() > 0 ? itemInfo.getLimit() : 1;

			// Filter by minimum limit for bulk trading
			if (limit < minLimit)
			{
				continue;
			}

			// Require decent volume for bulk items (from 24h data)
			// This confirms the item actually trades in volume
			int totalVolume = priceData.getLowVolume() + priceData.getHighVolume();
			if (totalVolume < 50)
			{
				continue;
			}

			if (buyPrice * limit > budget && buyPrice > budget)
			{
				continue;
			}

			// Calculate total profit if buying full limit
			int totalProfit = profit * limit;

			FlipOpportunity opp = new FlipOpportunity(
				itemId,
				itemInfo.getName(),
				buyPrice,
				sellPrice,
				totalProfit,  // Store total profit for sorting
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

		// Sort by total profit potential (profit × limit)
		// This shows items where you can make the most GP per 4-hour cycle
		opps.sort((a, b) -> Integer.compare(b.getProfit(), a.getProfit()));

		log.info("Found {} bulk opportunities (min limit: {}, min volume: 50)", opps.size(), minLimit);

		return opps;
	}
	/**
	 * Active Flipping Tab: Optimized for fast-turnover, high-frequency trading
	 * Strategy: Focus on cheap items (<25k) with very recent prices and confirmed volume
	 * Target: Active traders who want quick flips with minimal wait time
	 * Sorting: By profit-to-volume ratio (efficiency), then by absolute margin
	 * Key filters: Max 5min age, must have volume, affordable items
	 */
	public List<FlipOpportunity> calculateActiveFlippingOpportunities(int minProfit, int maxPrice, int maxAgeMinutes, int budget)
	{
		List<FlipOpportunity> opps = new ArrayList<>();
		long currentTime = System.currentTimeMillis() / 1000;

		for (Map.Entry<Integer, PriceData> entry : latestPrices.entrySet())
		{
			int itemId = entry.getKey();
			PriceData priceData = entry.getValue();

			if (priceData.getHigh() == 0 || priceData.getLow() == 0)
			{
				continue;
			}

			long buyTime = priceData.getLowTime();
			long sellTime = priceData.getHighTime();

			if (buyTime == 0 || sellTime == 0)
			{
				continue;
			}

			int buyAgeMinutes = (int) ((currentTime - buyTime) / 60);
			int sellAgeMinutes = (int) ((currentTime - sellTime) / 60);

			// Active flipping needs VERY recent prices (max 5 minutes)
			// Fresh data = active market = fast turnover
			int activeMaxAge = Math.min(maxAgeMinutes, 5);

			if (buyAgeMinutes > activeMaxAge || sellAgeMinutes > activeMaxAge)
			{
				continue;
			}

			ItemInfo itemInfo = itemMapping.get(itemId);
			if (itemInfo == null)
			{
				continue;
			}

			int buyPrice = priceData.getLow();
			int sellPrice = priceData.getHigh();

			// Filter by max price (for active flipping, focus on cheaper items for fast turnover)
			if (buyPrice > maxPrice)
			{
				continue;
			}

			// Filter out items with no volume data - we need confirmed trading activity
			int totalVolume = priceData.getLowVolume() + priceData.getHighVolume();
			if (totalVolume == 0)
			{
				continue;
			}

			int geTax = Math.min((int) (sellPrice * 0.01), 5_000_000);
			int profit = sellPrice - buyPrice - geTax;

			// For active flipping, even 1gp margin is worth it with high volume
			if (profit < minProfit)
			{
				continue;
			}

			double roi = (profit / (double) buyPrice) * 100;

			int limit = itemInfo.getLimit() > 0 ? itemInfo.getLimit() : 1;

			// Check if affordable
			if (buyPrice * limit > budget && buyPrice > budget)
			{
				continue;
			}

			long latestTime = Math.max(buyTime, sellTime);
			int ageMinutes = (int) ((currentTime - latestTime) / 60);

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

		// Sort by efficiency: items with best margin relative to volume
		// This prioritizes items that will flip quickly with good profit
		// Primary: profit-to-volume ratio (higher = better efficiency)
		// Secondary: absolute margin (tiebreaker)
		opps.sort((a, b) -> {
			int volA = a.getBuyVolume() + a.getSellVolume();
			int volB = b.getBuyVolume() + b.getSellVolume();

			// Calculate efficiency score: profit weighted by volume
			// Higher volume = more likely to sell quickly
			double efficiencyA = volA > 0 ? (double) a.getProfit() * Math.log(volA + 1) : 0;
			double efficiencyB = volB > 0 ? (double) b.getProfit() * Math.log(volB + 1) : 0;

			int efficiencyCompare = Double.compare(efficiencyB, efficiencyA);
			if (efficiencyCompare != 0) return efficiencyCompare;

			// Tiebreaker: absolute margin
			return Integer.compare(b.getProfit(), a.getProfit());
		});

		log.info("Found {} active flipping opportunities", opps.size());

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
