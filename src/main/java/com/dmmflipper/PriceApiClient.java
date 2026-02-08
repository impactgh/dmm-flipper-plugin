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
		
		fetchPricesFromEndpoint(API_BASE + "/latest");
		fetchPricesFromEndpoint(API_BASE + "/5m");
		fetchPricesFromEndpoint(API_BASE + "/1h");
		fetchPricesFromEndpoint(API_BASE + "/24h");
		
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

			// Strict age filter for Best Margin tab
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

		opps.sort((a, b) -> Integer.compare(b.getProfit(), a.getProfit()));

		log.info("Found {} best margin opportunities", opps.size());

		this.opportunities = opps;
		return opps;
	}
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

			// Relaxed age filter for bulk items (they trade less frequently)
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

			// Filter by minimum limit for bulk trading
			if (limit < minLimit)
			{
				continue;
			}

			// Require decent volume for bulk items (from 24h data)
			int totalVolume = priceData.getLowVolume() + priceData.getHighVolume();
			if (totalVolume < 100)
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
				totalProfit,
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

		// Sort by total profit (profit * limit)
		opps.sort((a, b) -> Integer.compare(b.getProfit(), a.getProfit()));

		log.info("Found {} bulk opportunities (min limit: {}, min volume: 100)", opps.size(), minLimit);

		return opps;
	}
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

			// Filter by max price (for active flipping, focus on cheaper items)
			if (buyPrice > maxPrice)
			{
				continue;
			}

			// Filter out items with no volume data
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

		// Sort by volume and recency (items that trade fast)
		opps.sort((a, b) -> {
			int volumeA = a.getBuyVolume() + a.getSellVolume();
			int volumeB = b.getBuyVolume() + b.getSellVolume();

			// If volumes are similar, prefer items with better margins
			if (Math.abs(volumeA - volumeB) < 100)
			{
				return Integer.compare(b.getProfit(), a.getProfit());
			}

			return Integer.compare(volumeB, volumeA);
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
