# Quick Start Guide

## Prerequisites

1. **Java 11 or higher** - Check with: `java -version`
2. **RuneLite** - Download from https://runelite.net/

## Building the Plugin

```bash
cd kiro_projects/dmm-flipper-plugin
./gradlew build
```

The JAR file will be created at: `build/libs/dmm-flipper-plugin-1.0.0.jar`

## Installing in RuneLite

1. Open RuneLite
2. Click the wrench icon (Settings)
3. Scroll down to "RuneLite" section
4. Enable "Allow loading external plugins"
5. Restart RuneLite
6. Click the puzzle piece icon (Plugins)
7. Click the "+" button at the bottom
8. Navigate to and select: `build/libs/dmm-flipper-plugin-1.0.0.jar`
9. The plugin should now appear in your plugin list
10. Enable it by toggling the switch

## Using the Plugin

1. Look for the DMM Flipper icon in the RuneLite sidebar (right side)
2. Click it to open the panel
3. You'll see two tabs:
   - **Flips**: Shows profitable flip opportunities
   - **Active Offers**: Tracks your GE offers

## Configuring

Right-click the plugin in the plugin list and select "Configure" to adjust:
- Your budget
- Minimum profit/ROI
- How often to refresh prices
- Stale offer threshold

## Common Issues

**"Allow loading external plugins" option not showing?**
- Make sure you're using the latest RuneLite version

**Plugin not appearing after adding JAR?**
- Check RuneLite logs (Help → Open logs folder)
- Make sure the JAR was built successfully
- Try rebuilding: `./gradlew clean build`

**No flips showing?**
- Adjust your budget in settings (default is 1M)
- Lower the Min Profit or Min ROI thresholds
- Click "Refresh" to fetch latest prices

## Development Tips

If you want to modify the plugin:

1. Open the project in IntelliJ IDEA
2. Make your changes
3. Rebuild: `./gradlew build`
4. In RuneLite, remove the old plugin and add the new JAR
5. Or set up RuneLite development environment to run directly from IDE

## File Structure

```
dmm-flipper-plugin/
├── src/main/java/com/dmmflipper/
│   ├── DMMFlipperPlugin.java      # Main plugin class
│   ├── DMMFlipperConfig.java      # Configuration options
│   ├── DMMFlipperPanel.java       # UI panel
│   ├── PriceApiClient.java        # API client for OSRS Wiki
│   ├── GEOfferTracker.java        # Tracks your GE offers
│   ├── FlipOpportunity.java       # Data model for flips
│   ├── ItemInfo.java              # Item metadata
│   └── PriceData.java             # Price data model
├── build.gradle                    # Build configuration
└── README.md                       # Full documentation
```

## Next Steps

- Adjust settings to match your playstyle
- Monitor the "Active Offers" tab while flipping
- Use the Python script (`dmm_flipper.py`) for detailed analysis
- Report any bugs or suggestions!
