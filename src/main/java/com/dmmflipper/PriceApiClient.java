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
	/**
	 * Best Margin Tab: Optimized for low-volume, high-margin overnight flips
	 * Strategy: Focus on expensive items with best profit per item (endgame gear, rare items)
	 * Target: Experienced flippers with large banks (50M+) looking for 3-5% returns
	 * Sorting: By absolute profit (margin), then by ROI as tiebreaker
	 * Risk: Higher (less liquidity, price volatility)
	 * Expected: 1.5-2.5M profit per night on 50M bank (best case)
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
		// This shows low-volume, high-margin items (endgame gear strategy)
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
	/**
	 * Bulk Volume Tab: Optimized for high-volume items with large buy limits
	 * Strategy: Focus on items with high buy limits and good margins
	 * Target: Flippers who want to maximize profit with high-volume items
	 * Sorting: By margin (profit per item)
	 * Key filters: High buy limits (1000+), confirmed 24h volume (50+)
	 */
	/**
	 * Bulk Volume Tab: Optimized for overnight flipping with high-volume items
	 * Strategy: Safe, stable, consistent profits with high liquidity items
	 * Based on: Overnight flipping strategy (6-12 hour holds)
	 * Target: Beginners and consistent profit seekers (2% return per night)
	 * Sorting: By efficiency score (profit × volume × limit potential)
	 * Key filters: High buy limits (1000+), high 24h volume (50+), reasonable margins
	 * 
	 * Philosophy: These items trade frequently (high liquidity), have stable prices,
	 * and allow hitting buy limits 2-4 times per night for compounding profits.
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

			// Relaxed age filter for overnight flips (items trade throughout the day)
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

			// Filter by minimum limit for bulk/overnight trading
			if (limit < minLimit)
			{
				continue;
			}

			// High volume requirement - confirms high liquidity (safe, stable flips)
			// These items trade frequently and have consistent demand
			int totalVolume = priceData.getLowVolume() + priceData.getHighVolume();
			if (totalVolume < 50)
			{
				continue;
			}

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

		// Sort by overnight flip efficiency score
		// Formula: profit × log(volume) × sqrt(limit)
		// - profit: absolute GP gain per item
		// - log(volume): liquidity factor (high volume = safer, more consistent)
		// - sqrt(limit): buy limit potential (can hit 2-4x per night)
		// This prioritizes items that are safe, liquid, and have good profit potential
		opps.sort((a, b) -> {
			int volA = a.getBuyVolume() + a.getSellVolume();
			int volB = b.getBuyVolume() + b.getSellVolume();

			// Overnight flip efficiency: combines profit, liquidity, and limit potential
			double efficiencyA = a.getProfit() * Math.log(volA + 1) * Math.sqrt(a.getLimit());
			double efficiencyB = b.getProfit() * Math.log(volB + 1) * Math.sqrt(b.getLimit());

			int efficiencyCompare = Double.compare(efficiencyB, efficiencyA);
			if (efficiencyCompare != 0) return efficiencyCompare;

			// Tiebreaker: total profit potential (profit × limit)
			return Integer.compare(b.getProfit() * b.getLimit(), a.getProfit() * a.getLimit());
		});

		log.info("Found {} bulk/overnight opportunities (min limit: {}, min volume: 50)", opps.size(), minLimit);

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
	/**
	 * Overnight Flipping Tab: Optimized for 6-12 hour flips with multiple buy limit cycles
	 * Strategy: Focus on items with high buy limits and consistent daily price fluctuations
	 * Target: Players who set flips before logging off and capitalize on daily price swings
	 * Sorting: By total profit potential over multiple cycles (profit × limit × estimated cycles)
	 * Key filters: Very high buy limits (5000+), confirmed volume, moderate age acceptable
	 */
	public List<FlipOpportunity> calculateOvernightFlippingOpportunities(int minProfit, int minROI, int maxROI, int maxAgeMinutes, int budget)
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

			// Moderate age filter - overnight items don't need to be super fresh
			// We care more about consistent daily patterns
			int overnightMaxAge = Math.max(maxAgeMinutes, 120);

			if (buyAgeMinutes > overnightMaxAge || sellAgeMinutes > overnightMaxAge)
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

			// Overnight flipping needs VERY high buy limits (5000+)
			// This allows hitting the limit 2-4 times in a 6-12 hour period
			if (limit < 5000)
			{
				continue;
			}

			// Require good volume to confirm consistent trading
			int totalVolume = priceData.getLowVolume() + priceData.getHighVolume();
			if (totalVolume < 100)
			{
				continue;
			}

			if (buyPrice * limit > budget && buyPrice > budget)
			{
				continue;
			}

			long latestTime = Math.max(buyTime, sellTime);
			int ageMinutes = (int) ((currentTime - latestTime) / 60);

			// Calculate profit potential over multiple cycles
			// Assume 2-3 buy limit cycles in a 6-12 hour period (conservative estimate)
			int estimatedCycles = 2;
			int totalProfitPotential = profit * limit * estimatedCycles;

			FlipOpportunity opp = new FlipOpportunity(
				itemId,
				itemInfo.getName(),
				buyPrice,
				sellPrice,
				totalProfitPotential,  // Store multi-cycle profit for sorting
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

		// Sort by total profit potential over multiple cycles
		// This shows items where you can make the most GP overnight
		opps.sort((a, b) -> Integer.compare(b.getProfit(), a.getProfit()));

		log.info("Found {} overnight flipping opportunities (min limit: 5000, min volume: 100)", opps.size());

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
	/**
	 * Overnight Flipping Tab: Optimized for 6-12 hour flips with consistent daily fluctuations
	 * Strategy: Focus on high-volume items with stable daily price swings
	 * Target: Players who flip before logging off and sell when they return
	 * Sorting: By total profit potential over 12 hours (considering multiple buy limit cycles)
	 * Key filters: High volume (liquidity), reasonable ROI (2%+), affordable items
	 */
	public List<FlipOpportunity> calculateOvernightFlippingOpportunities(int minProfit, int minROI, int maxROI, int maxAgeMinutes, int budget)
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

			// For overnight flipping, we want data from the last 24 hours to see daily patterns
			// More relaxed than active flipping since we're looking at daily cycles
			int overnightMaxAge = Math.max(maxAgeMinutes, 120); // At least 2 hours

			if (buyAgeMinutes > overnightMaxAge || sellAgeMinutes > overnightMaxAge)
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
			int geTax = Math.min((int) (sellPrice * 0.01), 5_000_000);
			int profit = sellPrice - buyPrice - geTax;

			if (profit <= 0)
			{
				continue;
			}

			double roi = (profit / (double) buyPrice) * 100;

			// Target 2%+ ROI for overnight flips (as mentioned in video)
			if (roi < Math.max(minROI, 2.0) || roi > maxROI)
			{
				continue;
			}

			// Require decent volume - overnight flipping needs liquidity
			// High volume = safer, more consistent flips
			int totalVolume = priceData.getLowVolume() + priceData.getHighVolume();
			if (totalVolume < 100)
			{
				continue;
			}

			int limit = itemInfo.getLimit() > 0 ? itemInfo.getLimit() : 1;

			// Check if affordable
			if (buyPrice * limit > budget && buyPrice > budget)
			{
				continue;
			}

			// Calculate profit potential over 12 hours (can hit buy limit 3 times in 12 hours)
			// Conservative estimate: 2.5 cycles in 12 hours
			int cycles = 2; // Conservative: 2 full cycles in 12 hours
			int totalProfitPotential = profit * limit * cycles;

			long latestTime = Math.max(buyTime, sellTime);
			int ageMinutes = (int) ((currentTime - latestTime) / 60);

			FlipOpportunity opp = new FlipOpportunity(
				itemId,
				itemInfo.getName(),
				buyPrice,
				sellPrice,
				totalProfitPotential, // Store 12-hour profit potential
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

		// Sort by 12-hour profit potential (profit × limit × cycles)
		// This shows which items will make the most GP overnight
		opps.sort((a, b) -> {
			int profitCompare = Integer.compare(b.getProfit(), a.getProfit());
			if (profitCompare != 0) return profitCompare;

			// Tiebreaker: prefer higher volume (more liquidity = safer)
			int volA = a.getBuyVolume() + a.getSellVolume();
			int volB = b.getBuyVolume() + b.getSellVolume();
			return Integer.compare(volB, volA);
		});

		log.info("Found {} overnight flipping opportunities (min ROI: 2%, min volume: 100)", opps.size());

		return opps;
	}

	/**
	 * Active Flipping Tab: Optimized for fast-turnover, high-frequency trading
	 * Strategy: Focus on cheap items (<25k) with very recent prices and confirmed volume
	 * Target: Active traders who want quick flips with minimal wait time
	 * Sorting: By profit-to-volume ratio (efficiency), then by absolute margin
	 * Key filters: Max 5min age, must have volume, affordable items
	 */
	/**
	 * Overnight Flipping Tab: Optimized for 6-12 hour flips with multiple buy limit cycles
	 * Strategy: Focus on high-volume items with consistent daily price fluctuations
	 * Target: Players who flip overnight and can hit buy limits 2-4 times
	 * Sorting: By total overnight profit potential (profit × limit × estimated cycles)
	 * Key filters: High volume (100+), reasonable margins, stable items
	 */
	public List<FlipOpportunity> calculateOvernightOpportunities(int minProfit, int minROI, int maxROI, int maxAgeMinutes, int budget)
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

			// Moderate age filter - overnight flips can use slightly older data
			int overnightMaxAge = Math.max(maxAgeMinutes, 30);

			if (buyAgeMinutes > overnightMaxAge || sellAgeMinutes > overnightMaxAge)
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

			// Check if affordable for at least one buy limit
			if (buyPrice * limit > budget)
			{
				continue;
			}

			// Require high volume for overnight flips (from 24h data)
			// High volume = more likely to hit multiple buy limits
			int totalVolume = priceData.getLowVolume() + priceData.getHighVolume();
			if (totalVolume < 100)
			{
				continue;
			}

			long latestTime = Math.max(buyTime, sellTime);
			int ageMinutes = (int) ((currentTime - latestTime) / 60);

			// Estimate number of buy limit cycles in 12 hours (overnight)
			// High volume items can hit 2-4 buy limits in 12 hours
			// Conservative estimate: 2 cycles for most items, 3 for very high volume
			int estimatedCycles = totalVolume > 500 ? 3 : 2;

			// Calculate total overnight profit potential
			int overnightProfit = profit * limit * estimatedCycles;

			FlipOpportunity opp = new FlipOpportunity(
				itemId,
				itemInfo.getName(),
				buyPrice,
				sellPrice,
				overnightProfit,  // Store overnight profit for sorting
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

		// Sort by overnight profit potential (profit × limit × cycles)
		// This shows items that will make the most GP over 6-12 hours
		opps.sort((a, b) -> Integer.compare(b.getProfit(), a.getProfit()));

		log.info("Found {} overnight flipping opportunities (min volume: 100)", opps.size());

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
