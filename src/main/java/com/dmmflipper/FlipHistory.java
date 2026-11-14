package com.dmmflipper;

import lombok.Data;
import javax.inject.Singleton;
import java.util.*;

@Singleton
@Data
public class FlipHistory
{
	private final Map<Integer, List<CompletedFlip>> completedFlips = new HashMap<>();
	private final Map<Integer, MarginCheck> marginChecks = new HashMap<>();
	private long sessionStartTime = System.currentTimeMillis();
	private long sessionProfit = 0;

	public void addCompletedFlip(CompletedFlip flip)
	{
		completedFlips.computeIfAbsent(flip.getItemId(), k -> new ArrayList<>()).add(flip);
		sessionProfit += flip.getProfit();
	}

	public void addMarginCheck(int itemId, long buyPrice, long sellPrice)
	{
		MarginCheck check = new MarginCheck(itemId, buyPrice, sellPrice, System.currentTimeMillis());
		marginChecks.put(itemId, check);
	}

	public MarginCheck getMarginCheck(int itemId)
	{
		return marginChecks.get(itemId);
	}

	public List<CompletedFlip> getFlipsForItem(int itemId)
	{
		return completedFlips.getOrDefault(itemId, new ArrayList<>());
	}

	public long getTotalProfit()
	{
		return completedFlips.values().stream()
			.flatMap(List::stream)
			.mapToLong(CompletedFlip::getProfit)
			.sum();
	}

	public long getSessionProfit()
	{
		return sessionProfit;
	}

	public int getTotalFlips()
	{
		return completedFlips.values().stream()
			.mapToInt(List::size)
			.sum();
	}

	public void resetSession()
	{
		sessionStartTime = System.currentTimeMillis();
		sessionProfit = 0;
	}

	@Data
	public static class CompletedFlip
	{
		private final int itemId;
		private final String itemName;
		private final long buyPrice;
		private final long sellPrice;
		private final int quantity;
		private final long profit;
		private final long timestamp;
		private final long geTax;

		public double getRoi()
		{
			return (profit / (double) (buyPrice * quantity)) * 100;
		}
	}

	@Data
	public static class MarginCheck
	{
		private final int itemId;
		private final long buyPrice;
		private final long sellPrice;
		private final long timestamp;

		public long getMargin()
		{
			return sellPrice - buyPrice;
		}

		public boolean isStale(int maxAgeMinutes)
		{
			long ageMinutes = (System.currentTimeMillis() - timestamp) / 60000;
			return ageMinutes > maxAgeMinutes;
		}
	}
}
