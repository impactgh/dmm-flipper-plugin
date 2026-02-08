# Quick Start: IntelliJ Setup (Easiest Method)

This is the simplest way to get your plugin running in IntelliJ.

## Step 1: Download IntelliJ IDEA

1. Go to https://www.jetbrains.com/idea/download/
2. Download **Community Edition** (free)
3. Install it

## Step 2: Open Your Project

1. Launch IntelliJ IDEA
2. Click **"Open"**
3. Navigate to `kiro_projects/dmm-flipper-plugin`
4. Click **OK**
5. Click **"Trust Project"** when asked
6. Wait for Gradle to sync (watch bottom-right corner)

## Step 3: Install RuneLite Plugin (in IntelliJ)

This makes development much easier:

1. Go to **File → Settings** (or IntelliJ IDEA → Preferences on Mac)
2. Go to **Plugins**
3. Search for **"RuneLite"**
4. Install the **RuneLite** plugin by RuneLite
5. Restart IntelliJ when prompted

## Step 4: Configure RuneLite Plugin

1. After restart, go to **File → Settings → Tools → RuneLite**
2. It will ask to download RuneLite - click **Yes**
3. Wait for it to download (this may take a minute)

## Step 5: Run Your Plugin

1. Look for a **green play button** (▶) in the top-right toolbar
2. Next to it should be a dropdown that says "RuneLite"
3. Click the **play button**
4. RuneLite will launch with your plugin automatically loaded!

## Step 6: Test It

1. In the launched RuneLite, log in to your account
2. Look for "DMM Flipper" in the plugin list (puzzle piece icon)
3. Enable it
4. Click the DMM Flipper icon in the sidebar
5. It should work!

## Making Changes

1. Edit your code in IntelliJ
2. Click the **play button** again
3. RuneLite restarts with your changes
4. That's it!

## If the RuneLite Plugin Doesn't Work

Use this manual method instead:

### Manual Run Configuration

1. **Run → Edit Configurations**
2. Click **+** → **Gradle**
3. Configure:
   - **Name**: `Run Plugin`
   - **Gradle project**: Select your project
   - **Tasks**: `runClient`
   - **Arguments**: `--console=plain`
4. Click **OK**
5. Click the **play button**

### If runClient task doesn't exist

Add this to your `build.gradle`:

```gradle
task runClient(type: JavaExec) {
    group = "runelite"
    description = "Run the RuneLite client with this plugin"
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'net.runelite.client.RuneLite'
    jvmArgs = ['-ea']
}
```

Then:
1. Click the **Gradle** tab on the right
2. Click **refresh** icon
3. Look for **Tasks → runelite → runClient**
4. Double-click it to run

## Debugging

To debug (see what's happening in your code):

1. Click in the left margin next to a line of code (red dot appears)
2. Click the **debug button** (🐛) instead of play
3. When code hits that line, it pauses
4. You can inspect variables and step through code

## Common Issues

### "Gradle sync failed"
- Check your internet connection
- Try: **File → Invalidate Caches → Invalidate and Restart**

### "Cannot find RuneLite classes"
- Make sure Gradle finished syncing
- Click **Gradle** tab → **refresh** icon

### "Plugin doesn't appear in RuneLite"
- Make sure you built the project first
- Try: **Build → Rebuild Project**

### "Java version error"
- **File → Project Structure → Project**
- Set SDK to Java 11 or higher

## Tips

- **Ctrl+Space**: Auto-complete (shows available methods)
- **Ctrl+B**: Jump to definition (click on a class/method)
- **Alt+Enter**: Quick fix (when you see a red underline)
- **Ctrl+Shift+F**: Search entire project

## What's Next?

Once it's running:
1. Make changes to the code
2. Click play to test
3. Use debugging to understand how it works
4. Check out other RuneLite plugins for examples

## Need Help?

- Check IntelliJ's **Event Log** (bottom-right) for errors
- Look at the **Build** output window for compilation errors
- Join RuneLite Discord: https://discord.gg/runelite
