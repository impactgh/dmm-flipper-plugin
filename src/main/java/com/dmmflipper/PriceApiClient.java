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
		Request request = new Request.Builder()
			.url(API_BASE + "/latest")
			.header("User-Agent", USER_AGENT)
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			if (response.isSuccessful() && response.body() != null)
			{
				String json = response.body().string();
				JsonObject root = gson.fromJson(json, JsonObject.class);
				JsonObject data = root.getAsJsonObject("data");

				latestPrices.clear();
				for (String itemIdStr : data.keySet())
				{
					int itemId = Integer.parseInt(itemIdStr);
					JsonObject priceObj = data.getAsJsonObject(itemIdStr);
					
					PriceData priceData = new PriceData();
					priceData.setHigh(priceObj.has("high") ? priceObj.get("high").getAsInt() : 0);
					priceData.setLow(priceObj.has("low") ? priceObj.get("low").getAsInt() : 0);
					priceData.setHighTime(priceObj.has("highTime") ? priceObj.get("highTime").getAsLong() : 0);
					priceData.setLowTime(priceObj.has("lowTime") ? priceObj.get("lowTime").getAsLong() : 0);
					priceData.setHighVolume(priceObj.has("highPriceVolume") ? priceObj.get("highPriceVolume").getAsInt() : 0);
					priceData.setLowVolume(priceObj.has("lowPriceVolume") ? priceObj.get("lowPriceVolume").getAsInt() : 0);

					latestPrices.put(itemId, priceData);
				}

				log.info("Loaded prices for {} items", latestPrices.size());
			}
		}
		catch (IOException e)
		{
			log.error("Error fetching latest prices", e);
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

			// Skip if no prices
			if (priceData.getHigh() == 0 || priceData.getLow() == 0)
			{
				continue;
			}

			// Check age
			long latestTime = Math.max(priceData.getHighTime(), priceData.getLowTime());
			int ageMinutes = (int) ((currentTime - latestTime) / 60);
			
			if (ageMinutes > maxAgeMinutes)
			{
				continue;
			}

			// Get item info
			ItemInfo itemInfo = itemMapping.get(itemId);
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
			if (profit < minProfit || roi < minROI || roi > maxROI)
			{
				continue;
			}

			// Check if affordable
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

		// Sort by profit
		opps.sort((a, b) -> Integer.compare(b.getProfit(), a.getProfit()));

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
