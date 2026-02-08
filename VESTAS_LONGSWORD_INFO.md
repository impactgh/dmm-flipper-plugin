# Vesta's Longsword - Item Information

## Correct Item ID: 22613

**NOT 13899** - That's a different item!

## Current API Data (as of testing)

```json
{
  "high": 2455000,
  "highTime": 1770529614,
  "low": 2222000,
  "lowTime": 1770529289
}
```

## Flip Opportunity Analysis

- **Buy Price (instant buy)**: 2,222,000 GP
- **Sell Price (instant sell)**: 2,455,000 GP
- **Gross Profit**: 233,000 GP
- **GE Tax (1%)**: 24,550 GP
- **Net Profit**: 208,450 GP per flip
- **ROI**: ~9.4%
- **Price Data Age**: 6-11 minutes (very recent!)

## Why This Should Show Up

With default settings:
- ✅ **Min Profit (100 GP)**: 208,450 GP profit - PASSES
- ✅ **Min ROI (3%)**: 9.4% ROI - PASSES  
- ✅ **Max ROI (200%)**: 9.4% ROI - PASSES
- ✅ **Max Age (15 min)**: 6-11 minutes old - PASSES
- ❓ **Budget (1M GP)**: Costs 2.2M GP - FAILS if buy limit × price > budget

## Budget Issue - THIS IS WHY IT'S NOT SHOWING!

The default budget is 1,000,000 GP, but Vesta's longsword costs 2,222,000 GP to buy.

**The budget filter logic:**
```java
if (buyPrice * limit > budget && buyPrice > budget)
```

For Vesta's longsword:
- Buy price: 2,222,000 GP > 1,000,000 GP budget → **FILTERED OUT**

**Solution:**
**Increase your budget in the plugin config to at least 2,500,000 GP** to see Vesta's longsword in the opportunities list.

## Plugin Logging

When the plugin runs, you should see these log messages:

```
[PriceApiClient] High-value item from https://prices.runescape.wiki/api/v1/dmm/latest: Vesta's longsword (ID: 22613) - Buy: 2222000, Sell: 2455000
[PriceApiClient] FOUND Vesta's longsword from https://prices.runescape.wiki/api/v1/dmm/latest: Vesta's longsword (ID: 22613) - Buy: 2222000, Sell: 2455000, BuyTime: 1770529289, SellTime: 1770529614
[PriceApiClient] Found Vesta item: Vesta's longsword (ID: 22613) - Buy: 2222000, Sell: 2455000, BuyAge: 11m, SellAge: 6m, ...
```

If you see "Vesta item FILTERED by age" or it doesn't appear in the top opportunities, check:
1. Your budget setting
2. The buy limit for the item
3. Whether other items have higher profit

## Item Variations

There are multiple Vesta items in OSRS:
- **Vesta's longsword (22613)** - The main tradeable version
- Vesta's spear (22610)
- Vesta's chainbody (22611)
- Vesta's plateskirt (22612)
- Corrupted versions (different IDs)

Make sure you're looking for the correct item ID!

## Testing Steps

1. Build and run the plugin
2. Set budget to at least 3M GP in config
3. Click "Refresh" in the plugin panel
4. Check RuneLite logs for "Vesta" mentions
5. Look for Vesta's longsword in the opportunities list

If it doesn't appear:
- Check if it's being filtered by budget
- Verify the API still has recent data
- Check if other items have higher profit (it might be below the top 5)
