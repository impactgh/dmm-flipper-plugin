# Quick Fix: Why Vesta's Longsword Isn't Showing

## TL;DR

**Vesta's longsword (ID: 22613) costs 2.2M GP but your default budget is 1M GP.**

## Solution

1. Open RuneLite
2. Go to plugin configuration (gear icon)
3. Find "DMM Flipper" plugin settings
4. Change **"Budget (GP)"** from `1000000` to `3000000` (or higher)
5. Click "Refresh" in the DMM Flipper panel

Vesta's longsword should now appear in your opportunities list!

## Why This Happens

The plugin filters out items you can't afford. The budget filter checks:
```
if (buyPrice > budget) → filter out
```

With default settings:
- Your budget: 1,000,000 GP
- Vesta's longsword buy price: 2,222,000 GP
- Result: **Filtered out by budget**

## Verify It's Working

After increasing your budget, check the logs for:

```
[PriceApiClient] FOUND Vesta's longsword from ...: Vesta's longsword (ID: 22613) - Buy: 2222000, Sell: 2455000
[PriceApiClient] Found Vesta item: Vesta's longsword (ID: 22613) - Buy: 2222000, Sell: 2455000
```

And in the opportunities list, you should see:
```
Vesta's longsword
Buy: 2,222,000 GP
Sell: 2,455,000 GP
Profit: ~208,000 GP
ROI: ~9.4%
```

## If It Still Doesn't Show

Check the logs for:
```
[PriceApiClient] High-profit item filtered by budget: Vesta's longsword (ID: 22613) - Profit: X, Buy: Y, Budget: Z
```

If you see this message, your budget is still too low. Increase it further.

## Other High-Value Items

Many profitable items in DMM cost more than 1M GP. Consider setting your budget to match your actual available GP for the best results.

Common high-value items that might be filtered:
- Vesta's equipment (2-3M GP)
- Statius's equipment (2-4M GP)
- Zuriel's equipment (1-3M GP)
- Morrigan's equipment (1-3M GP)
- Dragon claws (varies)
- Armadyl godsword (varies)

Set your budget to your actual GP amount to see all opportunities you can afford!
