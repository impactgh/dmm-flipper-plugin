package com.dmmflipper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GrandExchangeOfferChanged;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Slf4j
@Singleton
public class GEOfferTracker
{
	@Inject
	private PriceApiClient priceApiClient;

	@Inject
	private FlipHistory flipHistory;

	private final Map<Integer, TrackedOffer> activeOffers = new HashMap<>();
	private final Map<Integer, List<TrackedOffer>> completedBuys = new HashMap<>();
	private final Map<Integer, List<TrackedOffer>> completedSells = new HashMap<>();

	public void updateOffer(GrandExchangeOfferChanged event)
	{
		GrandExchangeOffer offer = event.getOffer();
		int slot = event.getSlot();

		// Handle completed offers
		if (offer.getState() == GrandExchangeOfferState.BOUGHT ||
			offer.getState() == GrandExchangeOfferState.SOLD)
		{
			TrackedOffer tracked = activeOffers.remove(slot);
			if (tracked != null)
			{
				tracked.setQuantityFilled(offer.getQuantitySold());
				
				// Store completed offer
				if (offer.getState() == GrandExchangeOfferState.BOUGHT)
				{
					completedBuys.computeIfAbsent(offer.getItemId(), k -> new ArrayList<>()).add(tracked);
					log.debug("Completed buy: {} x{} @ {}gp", offer.getItemId(), tracked.getQuantityFilled(), tracked.getPrice());
				}
				else
				{
					completedSells.computeIfAbsent(offer.getItemId(), k -> new ArrayList<>()).add(tracked);
					log.debug("Completed sell: {} x{} @ {}gp", offer.getItemId(), tracked.getQuantityFilled(), tracked.getPrice());
					
					// Try to match with a buy to create a completed flip
					matchFlip(offer.getItemId(), tracked);
				}
				
				// Check if this was a margin check (quantity = 1)
				if (tracked.getQuantityFilled() == 1)
				{
					checkForMarginCheck(offer.getItemId());
				}
			}
			return;
		}

		// Remove cancelled offers
		if (offer.getState() == GrandExchangeOfferState.CANCELLED_BUY ||
			offer.getState() == GrandExchangeOfferState.CANCELLED_SELL)
		{
			activeOffers.remove(slot);
			return;
		}

		// Track active offers
		if (offer.getState() == GrandExchangeOfferState.BUYING ||
			offer.getState() == GrandExchangeOfferState.SELLING)
		{
			TrackedOffer tracked = new TrackedOffer();
			tracked.setSlot(slot);
			tracked.setItemId(offer.getItemId());
			tracked.setQuantity(offer.getTotalQuantity());
			tracked.setQuantityFilled(offer.getQuantitySold());
			tracked.setPrice(offer.getPrice());
			tracked.setBuying(offer.getState() == GrandExchangeOfferState.BUYING);
			tracked.setTimestamp(System.currentTimeMillis());

			activeOffers.put(slot, tracked);
			
			log.debug("Tracking offer: {} x{} @ {}gp", 
				offer.getItemId(), 
				offer.getTotalQuantity(), 
				offer.getPrice());
		}
	}

	private void matchFlip(int itemId, TrackedOffer sell)
	{
		List<TrackedOffer> buys = completedBuys.get(itemId);
		if (buys == null || buys.isEmpty())
		{
			return;
		}

		// Match with most recent buy
		TrackedOffer buy = buys.get(buys.size() - 1);
		
		// Calculate profit
		int quantity = Math.min(buy.getQuantityFilled(), sell.getQuantityFilled());
		int revenue = sell.getPrice() * quantity;
		int cost = buy.getPrice() * quantity;
		int geTax = Math.min((int)(revenue * 0.01), 5_000_000);
		int profit = revenue - cost - geTax;

		// Get item name
		ItemInfo itemInfo = priceApiClient.getItemInfo(itemId);
		String itemName = itemInfo != null ? itemInfo.getName() : "Item #" + itemId;

		// Create completed flip
		FlipHistory.CompletedFlip flip = new FlipHistory.CompletedFlip(
			itemId,
			itemName,
			buy.getPrice(),
			sell.getPrice(),
			quantity,
			profit,
			System.currentTimeMillis(),
			geTax
		);

		flipHistory.addCompletedFlip(flip);
		log.info("Completed flip: {} - Profit: {}gp", itemName, profit);

		// Remove matched buy
		buys.remove(buy);
	}

	private void checkForMarginCheck(int itemId)
	{
		List<TrackedOffer> buys = completedBuys.get(itemId);
		List<TrackedOffer> sells = completedSells.get(itemId);

		if (buys != null && !buys.isEmpty() && sells != null && !sells.isEmpty())
		{
			TrackedOffer lastBuy = buys.get(buys.size() - 1);
			TrackedOffer lastSell = sells.get(sells.size() - 1);

			// If both are quantity 1 and recent, it's likely a margin check
			if (lastBuy.getQuantityFilled() == 1 && lastSell.getQuantityFilled() == 1)
			{
				long timeDiff = Math.abs(lastBuy.getTimestamp() - lastSell.getTimestamp());
				if (timeDiff < 300000) // Within 5 minutes
				{
					flipHistory.addMarginCheck(itemId, lastBuy.getPrice(), lastSell.getPrice());
					log.info("Margin check detected for item {}: buy={}, sell={}", 
						itemId, lastBuy.getPrice(), lastSell.getPrice());
				}
			}
		}
	}

	public List<TrackedOffer> getActiveOffers()
	{
		return new ArrayList<>(activeOffers.values());
	}

	public boolean isOfferStale(TrackedOffer offer, int thresholdPercent)
	{
		PriceData currentPrice = priceApiClient.getPriceData(offer.getItemId());
		if (currentPrice == null)
		{
			return false;
		}

		int relevantPrice = offer.isBuying() ? currentPrice.getLow() : currentPrice.getHigh();
		if (relevantPrice == 0)
		{
			return false;
		}

		double percentDiff = Math.abs((offer.getPrice() - relevantPrice) / (double) relevantPrice * 100);
		return percentDiff > thresholdPercent;
	}

	@Data
	public static class TrackedOffer
	{
		private int slot;
		private int itemId;
		private int quantity;
		private int quantityFilled;
		private int price;
		private boolean buying;
		private long timestamp;

		public long getInactiveTimeMinutes()
		{
			return (System.currentTimeMillis() - timestamp) / 60000;
		}

		public boolean isStale(int thresholdMinutes)
		{
			return getInactiveTimeMinutes() > thresholdMinutes;
		}
	}
}
