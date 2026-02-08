# How to Sideload Your Custom Plugin

## Step-by-Step Instructions

### 1. Enable Developer Mode in RuneLite

1. Open RuneLite
2. Click the **wrench icon** (Settings) in the top-right
3. Scroll down to find the **"RuneLite"** section (not a plugin, it's the main RuneLite settings)
4. Look for **"Enable developer tools"** or **"Developer mode"**
5. Toggle it **ON**
6. You may need to restart RuneLite

### 2. Access the Plugin Sideloader

After enabling developer mode:

1. Click the **puzzle piece icon** (Plugin Hub) in the sidebar
2. At the bottom of the plugin list, you should now see a **"+"** button or **"Load external plugin"** option
3. Click it

### 3. Load Your Plugin

1. A file browser will open
2. Navigate to: `kiro_projects/dmm-flipper-plugin/build/libs/`
3. Select: `dmm-flipper-plugin-1.0.0.jar`
4. Click Open

### 4. Enable the Plugin

1. The plugin should now appear in your plugin list as "DMM Flipper"
2. Toggle it **ON**
3. Look for the DMM Flipper icon in the RuneLite sidebar (right side)

## Alternative Method: Manual Installation

If the above doesn't work, you can manually place the plugin:

1. Close RuneLite
2. Copy your JAR file to the RuneLite plugins directory:
   - **Windows**: `%USERPROFILE%\.runelite\sideloaded-plugins\`
   - **Mac**: `~/.runelite/sideloaded-plugins/`
   - **Linux**: `~/.runelite/sideloaded-plugins/`
3. Create the `sideloaded-plugins` folder if it doesn't exist
4. Restart RuneLite
5. The plugin should auto-load

## Troubleshooting

### "I don't see developer mode option"
- Make sure you're using RuneLite version 2.6.0 or newer
- Update RuneLite if needed

### "The + button doesn't appear"
- Make sure developer mode is enabled
- Try restarting RuneLite
- Check if you're looking at the Plugin Hub (puzzle piece icon)

### "Plugin won't load"
- Check RuneLite logs: Help → Open logs folder
- Look for errors related to your plugin
- Make sure the JAR file isn't corrupted

### "Plugin loads but doesn't work"
- Check the RuneLite console for errors
- Make sure you're in DMM mode (the plugin uses DMM API endpoints)
- Try clicking the refresh button in the plugin panel

## Verifying It Works

Once loaded:
1. Click the DMM Flipper icon in the sidebar
2. You should see three tabs: Flips, Active Offers, Statistics
3. Click "Refresh" to fetch prices
4. If you see flip opportunities, it's working!

## Updating the Plugin

When you make changes:
1. Rebuild: `./gradlew build`
2. In RuneLite, remove the old plugin
3. Load the new JAR file
4. Or just restart RuneLite if using manual installation

## Need Help?

If you're still stuck:
- Check RuneLite Discord #plugin-hub channel
- Share any error messages from the logs
- Verify the JAR file exists and is ~32KB in size
