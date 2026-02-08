# DMM Flipper RuneLite Plugin

A RuneLite plugin for Deadman Mode that provides real-time flipping suggestions and tracks your Grand Exchange offers.

## Features

- **Real-time Flip Suggestions**: Shows profitable flips based on live OSRS Wiki API data
- **GE Offer Tracking**: Monitors your active GE offers and alerts when they become stale
- **Slot Timers**: Shows how long each offer has been inactive
- **Profit Tracking**: Automatically tracks completed flips and calculates profit
- **Session Statistics**: View session profit, total profit, and flip history
- **Margin Check Detection**: Automatically detects when you do margin checks (buy 1, sell 1)
- **Smart Filtering**: Filters out fake margins and inactive items using historical data
- **Configurable**: Adjust profit margins, ROI thresholds, budget, and more
- **Three Tabs**:
  - **Flips**: Top 20 profitable flip opportunities
  - **Active Offers**: Your current GE offers with stale price warnings and slot timers
  - **Statistics**: Session/total profit, completed flip history

## Installation

### Method 1: Sideloading (Recommended for Development)

1. **Build the plugin**:
   ```bash
   cd kiro_projects/dmm-flipper-plugin
   ./gradlew build
   ```
   (On Windows use `gradlew.bat build`)

2. **Locate the JAR file**:
   - After building, find the JAR at: `build/libs/dmm-flipper-plugin-1.0.0.jar`

3. **Install in RuneLite**:
   - Open RuneLite
   - Go to Settings (wrench icon) → RuneLite → Enable "Allow loading external plugins"
   - Restart RuneLite
   - Click the plugin icon in the sidebar
   - Click "Open Plugin Hub"
   - Click the "+" button at the bottom
   - Select your JAR file

### Method 2: Development Mode

1. **Import into IntelliJ IDEA**:
   - File → Open → Select `dmm-flipper-plugin` folder
   - Wait for Gradle to sync

2. **Run RuneLite with your plugin**:
   - Add RuneLite as a dependency (see RuneLite plugin development guide)
   - Run RuneLite with your plugin in the classpath

## Configuration

Access plugin settings via the RuneLite configuration panel:

- **Min Profit**: Minimum profit per flip (default: 100 GP)
- **Min ROI %**: Minimum return on investment (default: 3%)
- **Max ROI %**: Maximum ROI to filter fake margins (default: 200%)
- **Max Age**: Maximum age of price data in minutes (default: 30)
- **Budget**: Your available GP for flipping (default: 1M)
- **Refresh Interval**: How often to refresh prices (default: 60 seconds)
- **Stale Offer Threshold**: Alert when offer differs by this % (default: 10%)

## Usage

1. **View Flip Suggestions**:
   - Click the DMM Flipper icon in the RuneLite sidebar
   - Browse the "Flips" tab for profitable opportunities
   - Click "Refresh" to update prices manually

2. **Monitor Your Offers**:
   - Place offers in the Grand Exchange
   - Switch to the "Active Offers" tab
   - Stale offers (price moved significantly) will be highlighted in red

3. **Adjust Settings**:
   - Right-click the plugin icon → Configure
   - Adjust your budget, profit thresholds, etc.

## How It Works

1. **Price Fetching**: Connects to OSRS Wiki API every 60 seconds
2. **Opportunity Calculation**: Filters items by age, ROI, profit, and budget
3. **GE Tracking**: Listens to GE events and compares your offers to current prices
4. **Stale Detection**: Alerts when your offer price differs by >10% from current market

## Troubleshooting

**No flips showing up?**
- Check your budget setting (might be too low)
- Increase Max Age to see older data
- Lower Min Profit or Min ROI thresholds

**Plugin not loading?**
- Ensure "Allow loading external plugins" is enabled
- Check RuneLite logs for errors
- Rebuild the plugin with `./gradlew clean build`

**Prices not updating?**
- Check your internet connection
- OSRS Wiki API might be down (check their Discord)
- Try clicking "Refresh" manually

## Development

Built with:
- Java 11
- Gradle
- RuneLite API
- OSRS Wiki Prices API
- Lombok for boilerplate reduction

## Credits

- Price data from [OSRS Wiki](https://prices.runescape.wiki/)
- Built for Deadman: Annihilation
