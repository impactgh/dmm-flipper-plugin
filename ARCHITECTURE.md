# DMM Flipper Plugin - Architecture

## Overview

The DMM Flipper plugin is a RuneLite plugin that helps players find profitable flipping opportunities in Deadman Mode by analyzing real-time price data from the OSRS Wiki API.

## Component Architecture

```
DMMFlipperPlugin (Main)
├── PriceApiClient (API & Calculations)
│   ├── Fetches item mapping
│   ├── Fetches price data from /latest and /5m
│   └── Calculates flip opportunities
├── DMMFlipperPanel (UI)
│   ├── Displays opportunities
│   ├── Shows active offers
│   └── Displays flip history
├── GEOfferTracker (Offer Tracking)
│   └── Monitors Grand Exchange offers
├── FlipHistory (History)
│   └── Tracks completed flips
└── DMMFlipperConfig (Configuration)
    └── User settings for filters
```

## Data Flow

### 1. Startup Sequence

```
Plugin.startUp()
  → PriceApiClient.startPriceUpdates()
    → fetchItemMapping() [Background thread]
      → GET /api/v1/dmm/mapping
      → Store ItemInfo objects (id, name, limit, etc.)
    → fetchLatestPrices() [Background thread]
      → GET /api/v1/dmm/latest
      → GET /api/v1/dmm/5m
      → Merge price data (prefer newer timestamps)
      → Store PriceData objects (high, low, highTime, lowTime)
    → Schedule periodic updates (every 60 seconds)
```

### 2. Opportunity Calculation

```
User clicks "Refresh"
  → DMMFlipperPanel.refreshFlips()
    → PriceApiClient.calculateOpportunities(filters)
      → For each item with price data:
        1. Check if prices exist (high, low, highTime, lowTime)
        2. Check price age (must be < maxAge)
        3. Calculate profit = sellPrice - buyPrice - geTax
        4. Calculate ROI = (profit / buyPrice) * 100
        5. Apply filters (minProfit, minROI, maxROI, budget)
        6. Create FlipOpportunity object
      → Sort by profit (descending)
      → Return top opportunities
    → DMMFlipperPanel.updateOpportunitiesDisplay()
      → Create UI panels for each opportunity
      → Display in scrollable list
```

### 3. Offer Tracking

```
Player places GE offer
  → RuneLite fires GrandExchangeOfferChanged event
    → DMMFlipperPlugin.onGrandExchangeOfferChanged()
      → GEOfferTracker.updateOffer()
        → Store offer details (item, price, quantity, state)
      → DMMFlipperPanel.updateOfferDisplay()
        → Show offer progress in UI
      → DMMFlipperPanel.updateProfitLabels()
        → Calculate and display current profit

When offer completes:
  → GEOfferTracker detects completion
    → FlipHistory.addFlip()
      → Store completed flip details
    → DMMFlipperPanel.updateStatsDisplay()
      → Update total profit, flip count, etc.
```

## Key Classes

### PriceApiClient

**Responsibilities:**
- Fetch item mapping from API
- Fetch price data from multiple endpoints
- Calculate flip opportunities based on filters
- Provide price data to other components

**Key Methods:**
- `fetchItemMapping()`: Loads item names, limits, etc.
- `fetchLatestPrices()`: Fetches from /latest and /5m endpoints
- `calculateOpportunities()`: Applies filters and returns opportunities
- `getPriceData(itemId)`: Returns price data for specific item
- `getItemInfo(itemId)`: Returns item info for specific item

**Data Structures:**
- `Map<Integer, ItemInfo> itemMapping`: Item ID → Item details
- `Map<Integer, PriceData> latestPrices`: Item ID → Price data
- `List<FlipOpportunity> opportunities`: Calculated opportunities

### DMMFlipperPanel

**Responsibilities:**
- Display opportunities in scrollable list
- Show active GE offers with progress
- Display flip history and statistics
- Handle user interactions (refresh, config changes)

**Key Methods:**
- `refreshFlips()`: Triggers opportunity recalculation
- `updateOpportunitiesDisplay()`: Updates opportunity list UI
- `updateOfferDisplay()`: Updates active offers UI
- `updateStatsDisplay()`: Updates statistics UI
- `createOpportunityPanel()`: Creates UI for single opportunity

### GEOfferTracker

**Responsibilities:**
- Track active Grand Exchange offers
- Monitor offer progress (quantity filled)
- Detect offer completion
- Calculate profit for active offers

**Key Methods:**
- `updateOffer(event)`: Process GE offer change event
- `getActiveOffers()`: Returns list of active offers
- `getOfferProfit(offer)`: Calculates current profit for offer

### FlipHistory

**Responsibilities:**
- Store completed flips
- Calculate total profit
- Provide flip statistics

**Key Methods:**
- `addFlip(flip)`: Add completed flip to history
- `getTotalProfit()`: Returns sum of all flip profits
- `getFlipCount()`: Returns number of completed flips
- `getRecentFlips()`: Returns recent flip history

## Data Models

### ItemInfo
```java
{
  int id;           // Item ID
  String name;      // Item name
  int limit;        // GE buy limit
  int value;        // High alch value
  boolean members;  // Members item?
}
```

### PriceData
```java
{
  int high;         // Instant sell price (GP)
  int low;          // Instant buy price (GP)
  long highTime;    // Unix timestamp of high price
  long lowTime;     // Unix timestamp of low price
  int highVolume;   // Volume at high price
  int lowVolume;    // Volume at low price
}
```

### FlipOpportunity
```java
{
  int itemId;       // Item ID
  String itemName;  // Item name
  int buyPrice;     // Price to buy at (low)
  int sellPrice;    // Price to sell at (high)
  int profit;       // Profit per item (after GE tax)
  double roi;       // Return on investment %
  int geTax;        // Grand Exchange tax (1%, max 5M)
  int limit;        // GE buy limit
  int ageMinutes;   // Age of price data
  int buyVolume;    // Buy volume
  int sellVolume;   // Sell volume
}
```

## API Integration

### Endpoints Used

1. **Item Mapping**: `GET /api/v1/dmm/mapping`
   - Returns array of all items with metadata
   - Fetched once on startup
   - Used to get item names and buy limits

2. **Latest Prices**: `GET /api/v1/dmm/latest`
   - Returns most recent price for each item
   - Fetched every 60 seconds
   - Provides instant buy/sell prices

3. **5-Minute Prices**: `GET /api/v1/dmm/5m`
   - Returns 5-minute average prices
   - Fetched every 60 seconds
   - Provides additional coverage for less-traded items

### API Response Format

```json
{
  "data": {
    "2": {
      "high": 60,
      "highTime": 1770529556,
      "low": 42,
      "lowTime": 1770529637
    },
    "4": {
      "high": 120,
      "highTime": 1770529500,
      "low": 100,
      "lowTime": 1770529600
    }
  }
}
```

### Data Merging Strategy

When fetching from multiple endpoints:
1. Fetch from /latest first
2. Fetch from /5m second
3. For each item:
   - If only in one endpoint, use that data
   - If in both endpoints, use the one with newer timestamps
   - Prefer /latest over /5m when timestamps are equal

## Filtering Logic

Opportunities are filtered in this order:

1. **Has Prices**: Both high and low prices must exist
2. **Has Timestamps**: Both highTime and lowTime must exist
3. **Price Age**: `max(currentTime - buyTime, currentTime - sellTime) <= maxAge`
4. **Profit**: `profit >= minProfit`
5. **ROI**: `minROI <= roi <= maxROI`
6. **Budget**: `buyPrice * limit <= budget` (or `buyPrice <= budget` if no limit)

## GE Tax Calculation

Grand Exchange tax is 1% of the sell price, capped at 5M GP:
```java
int geTax = Math.min((int)(sellPrice * 0.01), 5_000_000);
int profit = sellPrice - buyPrice - geTax;
```

## Threading Model

- **Main Thread**: UI updates, event handling
- **Background Thread**: API calls, opportunity calculation
- **Scheduled Thread**: Periodic price updates (every 60 seconds)

All API calls and calculations happen in background threads to avoid blocking the UI.

## Configuration

User-configurable settings:
- **minProfit**: Minimum profit per flip (GP)
- **minROI**: Minimum ROI percentage
- **maxROI**: Maximum ROI percentage (filters fake margins)
- **maxAge**: Maximum age of price data (minutes)
- **budget**: Available GP for flipping
- **refreshInterval**: How often to refresh prices (seconds)
- **staleOfferThreshold**: Alert threshold for stale offers (%)

## Error Handling

- API connection errors are logged but don't crash the plugin
- Failed API calls return empty data sets
- UI gracefully handles empty opportunity lists
- Invalid price data is filtered out during calculation

## Performance Considerations

- Item mapping is fetched once on startup (not every refresh)
- Price data is cached and only refreshed periodically
- Opportunity calculation is done in background thread
- UI updates are batched to avoid flickering
- Only top opportunities are displayed (sorted by profit)

## Future Enhancements

Possible improvements:
- Historical price charts
- Profit tracking over time
- Item watchlist
- Price alerts
- Margin checking (buy +1, sell -1)
- Volume analysis
- Trend detection
