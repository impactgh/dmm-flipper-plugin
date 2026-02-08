# OSRS/DMM Flipping Strategy Analysis & Optimizations

## Research Summary

Based on extensive research of OSRS flipping guides and strategies, I've identified the key principles that separate successful flippers from unsuccessful ones:

### Core Flipping Strategies

#### 1. High-Volume Flipping
- **Target**: Items with high GE limits (10,000+)
- **Margins**: Small (1-50 GP per item)
- **Risk**: Low
- **Speed**: Fast turnover
- **Examples**: Runes, ores, logs, potions
- **Key Metric**: Total profit = margin × volume × turnover rate

#### 2. Low-Volume/High-Margin Flipping
- **Target**: Expensive items with bigger margins
- **Margins**: Large (10,000-200,000 GP per item)
- **Risk**: High
- **Speed**: Slow turnover
- **Examples**: High-end gear, rare items
- **Key Metric**: ROI% and absolute profit per item

#### 3. Active Flipping
- **Target**: Cheap items (<25k) with very recent prices
- **Margins**: Any positive margin (even 1 GP acceptable)
- **Risk**: Very low
- **Speed**: Very fast (minutes to hours)
- **Examples**: Common consumables, frequently traded items
- **Key Metric**: Profit per hour = (margin × trades per hour)

#### 4. Bulk Trading
- **Target**: Items with high buy limits and confirmed volume
- **Margins**: Moderate
- **Risk**: Low-moderate
- **Speed**: Moderate (4-hour cycles)
- **Examples**: Skilling resources, consumables
- **Key Metric**: Total profit potential = margin × buy limit

## Tab Optimizations Implemented

### 1. Best Margin Tab
**Purpose**: High-ROI, high-margin flips for experienced traders

**Strategy Changes**:
- Strict age filter (respects user's max age setting)
- Sorts by absolute profit (margin) first, then ROI as tiebreaker
- No volume requirements (some high-margin items trade infrequently)
- Focus on maximum profit per individual flip

**Target User**: Experienced flippers with large banks looking for best profit per item

**Research Basis**: Low-volume/high-margin strategy from multiple guides

---

### 2. Bulk Volume Tab
**Purpose**: Maximize profit per 4-hour GE cycle

**Strategy Changes**:
- Minimum buy limit: 1000 (configurable)
- Minimum 24h volume: 50 (confirms item actually trades)
- Relaxed age filter: 60 minutes (bulk items trade less frequently)
- Sorts by **total profit potential** (profit × limit)
- Shows both total profit AND per-item profit in UI

**Key Innovation**: Displays total profit you can make if you buy the full limit, helping users understand the true profit potential per 4-hour cycle

**Target User**: Flippers who want to maximize GP per trading cycle with confirmed liquidity

**Research Basis**: High-volume flipping strategy emphasizing buy limits and total profit potential

---

### 3. Active Flipping Tab
**Purpose**: Fast-turnover, high-frequency trading

**Strategy Changes**:
- Maximum age: 5 minutes (very fresh data = active market)
- Maximum price: 25k (affordable, fast-moving items)
- Minimum profit: 1 GP (any positive margin acceptable)
- Must have volume data (filters out Vol: 0)
- **NEW SORTING**: Efficiency-based (profit × log(volume))
  - Prioritizes items with good margin AND high volume
  - Uses logarithmic scaling to balance margin vs volume
  - Tiebreaker: absolute margin

**Key Innovation**: Efficiency scoring that balances profit with volume, showing items most likely to flip quickly with good returns

**Target User**: Active traders who want quick flips with minimal wait time

**Research Basis**: Active flipping strategy from guides emphasizing speed, volume, and recent prices

---

## Sorting Algorithm Explanations

### Best Margin: Profit-First Sorting
```
Primary: Absolute profit (highest first)
Secondary: ROI% (tiebreaker)
```
**Rationale**: Shows items with best profit per flip, with ROI as quality indicator

### Bulk Volume: Total Profit Potential
```
Sort by: profit × limit (highest first)
```
**Rationale**: Shows which items will make you the most GP if you buy the full limit every 4 hours

### Active Flipping: Efficiency Scoring
```
Efficiency = profit × log(volume + 1)
Primary: Efficiency score (highest first)
Secondary: Absolute profit (tiebreaker)
```
**Rationale**: 
- Balances margin with volume using logarithmic scaling
- log(volume) prevents extremely high volume from dominating
- Favors items that will flip quickly (high volume) with decent profit
- Example: 10 GP margin with 1000 volume scores higher than 50 GP margin with 10 volume

---

## Key Research Sources

Content was synthesized and rephrased from multiple OSRS flipping guides:
- [Common Flipping Items in OSRS (2025)](https://osrsmoneymaking.guide/news/common-flipping-items-in-osrs-your-guide-to-profitable-trades-in-2025)
- [Best OSRS Items to Flip (2025)](https://runeflips.com/best-osrs-items-to-flip-for-consistent-gp-2025-complete-guide)
- Various GE-Tracker and community guides

**Note**: Content was rephrased for compliance with licensing restrictions.

---

## DMM-Specific Considerations

- **No GE Tax**: Unlike main game (1% tax), DMM has no tax, making all margins more profitable
- **Smaller Economy**: Fewer players = potentially different volume patterns
- **Seasonal Resets**: Item values may fluctuate more dramatically
- **PvP Focus**: Combat gear may have different demand patterns

---

## Future Optimization Ideas

1. **Volume Weighting**: Add user-configurable volume thresholds for each tab
2. **Historical Trends**: Track price movements over time to identify trending items
3. **Profit Per Hour Estimates**: Calculate estimated GP/hour based on volume and margin
4. **Risk Scoring**: Add risk indicators based on price volatility
5. **Favorite Items**: Allow users to pin specific items they prefer to flip
6. **Multi-Tab View**: Show top items from all tabs in a summary view

---

## Testing Recommendations

1. Monitor which tab users prefer over time
2. Track actual flip success rates vs predicted efficiency
3. Adjust volume thresholds based on DMM market data
4. Consider adding user feedback mechanism for tab effectiveness
