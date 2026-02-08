# DMM Flipper Plugin - Testing Guide

## Quick Start

1. **Build the plugin:**
   ```
   gradlew build
   ```

2. **Install in RuneLite:**
   - Copy the JAR from `build/libs/` to your RuneLite plugins folder
   - Or use the sideload instructions in `SIDELOAD_INSTRUCTIONS.md`

3. **Start RuneLite and enable the plugin**

4. **Open the DMM Flipper panel** (look for the icon in the sidebar)

## What to Check

### 1. Plugin Startup
Check the RuneLite logs for:
```
[DMM Flipper] DMM Flipper started!
[PriceApiClient] Loaded X items
[PriceApiClient] Total items after fetching all endpoints: X
```

### 2. API Data Loading
Look for these log messages:
```
[PriceApiClient] Sample API response (first 500 chars): ...
[PriceApiClient] Sample item data for ID X: ...
[PriceApiClient] Loaded from https://prices.runescape.wiki/api/v1/dmm/latest: X new items, Y updated items, Z total
[PriceApiClient] Loaded from https://prices.runescape.wiki/api/v1/dmm/5m: X new items, Y updated items, Z total
```

### 3. High-Value Items
If high-value items exist in the API, you'll see:
```
[PriceApiClient] High-value item from ...: ItemName (ID: X) - Buy: Y, Sell: Z
```

### 4. Opportunity Calculation
When you click "Refresh" in the panel:
```
[PriceApiClient] Starting opportunity calculation with filters: minProfit=X, minROI=Y%, maxROI=Z%, maxAge=Wmin, budget=V
[PriceApiClient] Opportunity calculation: X total items, Y filtered (...), Z opportunities found
[PriceApiClient] Top 5 opportunities:
  1. ItemName - Profit: X, ROI: Y%, Buy: Z, Sell: W, Limit: V
  ...
```

## Troubleshooting

### No Opportunities Showing

**Check 1: Are items being loaded?**
- Look for "Total items after fetching all endpoints: X" in logs
- Should be 1500-2000+ items
- If 0 items, check your internet connection or API access

**Check 2: Are filters too strict?**
Try adjusting in the plugin config:
- Lower "Min Profit" to 100 GP
- Lower "Min ROI %" to 3%
- Increase "Max ROI %" to 200%
- Increase "Max Age" to 30-60 minutes
- Increase "Budget" to 10M GP

**Check 3: Is the price data too old?**
- DMM trading might be slow
- Increase "Max Age (minutes)" in config
- Check logs for "filtered by age" count

**Check 4: Check the filtering statistics**
Look for this log line:
```
Opportunity calculation: 1800 total items, 1750 filtered (no price: 50, age: 1200, profit: 300, ROI: 150, budget: 50), 50 opportunities found
```

This tells you why items are being filtered:
- **no price**: Item has no buy/sell price data
- **age**: Price data is older than maxAge setting
- **profit**: Profit is less than minProfit setting
- **ROI**: ROI is outside the minROI-maxROI range
- **budget**: Item costs more than your budget

### Specific Items Not Showing

Some items may not have recent trading data in DMM:
- Check if the item exists in the API by looking at logs
- The plugin can only show items that have recent trades
- Try searching for the item ID in the logs
- Example: Vesta's longsword is item ID 22613 and should appear if it has recent trades

### API Connection Issues

If you see errors like "Error fetching prices":
- Check your internet connection
- Verify the API is accessible: https://prices.runescape.wiki/api/v1/dmm/latest
- Check if you're being rate-limited
- Ensure User-Agent header is being sent correctly

## Default Configuration

The plugin starts with these defaults:
- **Min Profit**: 100 GP
- **Min ROI**: 3%
- **Max ROI**: 200%
- **Max Age**: 15 minutes
- **Budget**: 1,000,000 GP
- **Refresh Interval**: 60 seconds

## Expected Behavior

1. **On startup**: Plugin fetches item mapping and latest prices
2. **Every 60 seconds**: Prices are automatically refreshed
3. **When you click "Refresh"**: Opportunities are recalculated with current filters
4. **When you place a GE offer**: The plugin tracks it and shows progress
5. **When an offer completes**: It's added to your flip history

## Performance Notes

- Initial load may take 5-10 seconds
- Subsequent refreshes should be faster (1-2 seconds)
- The plugin fetches from 2 endpoints (/latest and /5m) for better coverage
- Opportunities are sorted by profit (highest first)

## Logging Levels

To see all debug logs, set your RuneLite log level to DEBUG or INFO.

## Common Issues

### "No opportunities found"
- Most likely: Filters are too strict or price data is too old
- Solution: Adjust config settings, especially maxAge

### "Loaded 0 items"
- Most likely: API connection issue
- Solution: Check internet connection and API accessibility

### "High-profit item filtered by ROI"
- This is normal - the plugin filters out suspiciously high ROI items
- These are often fake margins or stale data
- Adjust maxROI if you want to see them

### Items showing but can't be traded
- The API shows historical data
- Items might not be actively traded anymore
- Always verify in-game before placing offers
