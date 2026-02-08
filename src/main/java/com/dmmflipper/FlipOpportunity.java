package com.dmmflipper;

import lombok.Data;

@Data
public class FlipOpportunity
{
	private final int itemId;
	private final String itemName;
	private final int buyPrice;
	private final int sellPrice;
	private final int profit;
	private final double roi;
	private final int geTax;
	private final int limit;
	private final int ageMinutes;
	private final int buyVolume;
	private final int sellVolume;
	private final String trend;
	private final boolean isReal;

	public int getTotalCost(int quantity)
	{
		return buyPrice * quantity;
	}

	public int getTotalProfit(int quantity)
	{
		return profit * quantity;
	}

	public int getMaxQuantity(int budget)
	{
		if (limit <= 0)
		{
			return budget / buyPrice;
		}
		return Math.min(limit, budget / buyPrice);
	}
}
