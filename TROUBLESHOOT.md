# Troubleshooting Plugin Loading

## The plugin isn't showing up at all

### Step 1: Verify Developer Mode is Enabled

RuneLite will NOT load external plugins unless developer mode is on:

1. Open RuneLite
2. Click **wrench icon** (Settings)
3. Search for "developer" in the search box
4. Look for **"Developer mode"** toggle
5. Turn it **ON**
6. **Restart RuneLite completely**

Without this, external plugins are completely ignored.

### Step 2: Use the Correct Directory

The directory changed in recent RuneLite versions. Try these in order:

**Option A: externalplugins (newer)**
- Windows: `C:\Users\<YourName>\.runelite\externalplugins\`
- Mac: `~/.runelite/externalplugins/`
- Linux: `~/.runelite/externalplugins/`

**Option B: sideloaded-plugins (older)**
- Windows: `C:\Users\<YourName>\.runelite\sideloaded-plugins\`
- Mac: `~/.runelite/sideloaded-plugins/`
- Linux: `~/.runelite/sideloaded-plugins/`

Create the folder if it doesn't exist!

### Step 3: Check the JAR File

Make sure:
- File is named: `dmm-flipper-plugin-1.0.0.jar`
- File size is around 66KB
- File is not corrupted (try re-copying it)

### Step 4: Check RuneLite Version

You need RuneLite **2.6.0 or newer** for external plugins.

To check version:
- Help → About
- Should show version number

### Step 5: Check Logs for Errors

1. Help → Open logs folder
2. Open `client.log`
3. Search for:
   - "external"
   - "sideload"
   - "plugin"
   - Any error messages

If you see "external plugins disabled" or similar, developer mode isn't on.

## Alternative: Use Plugin Hub Submission

If sideloading doesn't work, you can submit to Plugin Hub:

1. Create a GitHub repo with your plugin
2. Submit to RuneLite Plugin Hub
3. Once approved, install normally

## Quick Test: Try a Known Working Plugin

To verify your setup works:

1. Download a known working external plugin
2. Try loading it the same way
3. If that doesn't work, the issue is your RuneLite setup, not the plugin

## Common Issues

### "Developer mode option doesn't exist"
- Update RuneLite to latest version
- Some versions hide it - try searching "developer" in settings

### "Folder doesn't exist"
- Create it manually
- Make sure `.runelite` folder exists (it's hidden on Mac/Linux)

### "Plugin loads but crashes"
- Check logs for Java exceptions
- Plugin might be incompatible with your RuneLite version
- Try rebuilding with matching RuneLite version

### "Nothing in logs about the plugin"
- RuneLite isn't even trying to load it
- Developer mode is OFF
- Wrong directory
- JAR file is corrupted

## Last Resort: Manual Plugin Hub Method

If nothing works, you can:

1. Fork the RuneLite plugin-hub repo
2. Add your plugin to it
3. Build RuneLite from source with your plugin included

This is more complex but guaranteed to work.

## Need More Help?

Share:
1. Your RuneLite version
2. Which directory you used
3. Whether developer mode is enabled
4. Any errors from client.log
5. Your operating system
