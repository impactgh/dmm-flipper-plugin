# IntelliJ IDEA Setup for RuneLite Plugin Development

This guide will help you set up IntelliJ to develop and test your DMM Flipper plugin.

## Prerequisites

1. **IntelliJ IDEA** - Download from https://www.jetbrains.com/idea/download/
   - Community Edition (free) works fine
   - Ultimate Edition has more features but not required

2. **Java 11 JDK** - Should already be installed if you built the plugin
   - Verify: `java -version` (should show 11.x.x or higher)

3. **Git** (optional but recommended)

## Step 1: Install IntelliJ IDEA

1. Download and install IntelliJ IDEA Community Edition
2. Launch it and complete the initial setup wizard
3. Choose your theme and skip plugin recommendations for now

## Step 2: Import Your Plugin Project

1. **Open IntelliJ IDEA**
2. Click **"Open"** on the welcome screen (or File → Open if already in a project)
3. Navigate to and select: `kiro_projects/dmm-flipper-plugin`
4. Click **"OK"**
5. IntelliJ will detect it's a Gradle project and ask to import
6. Click **"Trust Project"** when prompted
7. Wait for Gradle to sync (this may take a few minutes)
   - You'll see progress in the bottom-right corner
   - It's downloading RuneLite dependencies

## Step 3: Configure JDK

1. Go to **File → Project Structure** (or press Ctrl+Alt+Shift+S / Cmd+; on Mac)
2. Under **Project Settings → Project**:
   - Set **SDK** to Java 11 (or higher)
   - Set **Language level** to 11
3. Click **OK**

## Step 4: Clone RuneLite (for running/debugging)

You need the RuneLite source code to run your plugin from IntelliJ:

```bash
# In your terminal (outside IntelliJ)
cd ~/projects  # or wherever you keep projects
git clone https://github.com/runelite/runelite.git
cd runelite
```

## Step 5: Create Run Configuration

### Option A: Run with RuneLite Source (Recommended)

1. In IntelliJ, go to **Run → Edit Configurations**
2. Click **+** (Add New Configuration) → **Application**
3. Configure:
   - **Name**: `RuneLite with DMM Flipper`
   - **Main class**: `net.runelite.client.RuneLite`
   - **VM options**: `-ea -Drunelite.pluginhub.skip=true`
   - **Working directory**: Path to your RuneLite clone (e.g., `~/projects/runelite`)
   - **Use classpath of module**: Select your plugin module
   - **Before launch**: Add "Build" task
4. Click **Modify options** → **Add dependencies with "provided" scope to classpath** (check this)
5. Click **OK**

### Option B: Run with Installed RuneLite (Simpler but less flexible)

1. **Run → Edit Configurations**
2. Click **+** → **JAR Application**
3. Configure:
   - **Name**: `RuneLite`
   - **Path to JAR**: Your installed RuneLite JAR (e.g., `~/.runelite/RuneLite.jar`)
   - **VM options**: `-ea --add-opens=java.desktop/sun.awt=ALL-UNNAMED`
   - **Before launch**: Add "Build" and "Copy JAR to externalplugins" tasks
4. Click **OK**

## Step 6: Add Your Plugin to RuneLite Classpath

### Method 1: Modify build.gradle (Easiest)

Your `build.gradle` already has the right dependencies. Just make sure Gradle synced properly.

### Method 2: Add as External Plugin Path

1. Create a file: `~/.runelite/developer-plugin-paths.txt`
2. Add this line: `/full/path/to/kiro_projects/dmm-flipper-plugin/build/libs`
3. Save the file

## Step 7: Run Your Plugin

1. Click the **green play button** (▶) in the toolbar
2. Or press **Shift+F10** (Windows/Linux) or **Ctrl+R** (Mac)
3. RuneLite should launch with your plugin loaded
4. Look for "DMM Flipper" in the plugin list

## Step 8: Verify It's Working

1. In the launched RuneLite, check the plugin list
2. Enable "DMM Flipper"
3. You should see the icon in the sidebar
4. Click it to open the panel

## Development Workflow

### Making Changes

1. Edit your code in IntelliJ
2. Click the **green play button** to rebuild and run
3. RuneLite restarts with your changes
4. Test your changes

### Debugging

1. Set breakpoints by clicking in the left margin (red dot appears)
2. Click the **debug button** (🐛) instead of run
3. When code hits a breakpoint, you can:
   - Inspect variables
   - Step through code line by line
   - Evaluate expressions

### Hot Reload (Advanced)

For faster iteration without restarting:
1. Make a small change
2. Press **Ctrl+F9** (Build Project)
3. Some changes will apply without restart

## Troubleshooting

### "Cannot resolve symbol RuneLite"

**Fix**: Gradle didn't sync properly
1. Click the **Gradle** tab on the right
2. Click the **refresh** icon (circular arrows)
3. Wait for sync to complete

### "Main class not found"

**Fix**: Wrong classpath configuration
1. Make sure you cloned RuneLite
2. Check your run configuration's working directory
3. Verify "Use classpath of module" is set correctly

### "Plugin doesn't appear in RuneLite"

**Fix**: Plugin not on classpath
1. Check `developer-plugin-paths.txt` exists and has correct path
2. Make sure you built the project (Build → Build Project)
3. Check RuneLite logs for errors

### "Gradle sync fails"

**Fix**: Network or dependency issue
1. Check your internet connection
2. Try: File → Invalidate Caches → Invalidate and Restart
3. Delete `~/.gradle/caches` and try again

### "Java version mismatch"

**Fix**: Wrong JDK configured
1. File → Project Structure → Project
2. Set SDK to Java 11
3. File → Settings → Build, Execution, Deployment → Build Tools → Gradle
4. Set "Gradle JVM" to Java 11

## Useful IntelliJ Shortcuts

- **Ctrl+Space**: Auto-complete
- **Ctrl+B**: Go to definition
- **Ctrl+Alt+L**: Format code
- **Shift+F10**: Run
- **Shift+F9**: Debug
- **Ctrl+F9**: Build project
- **Alt+Enter**: Show quick fixes
- **Ctrl+Shift+F**: Find in files

## Next Steps

Once you have it running:

1. **Explore the code** - Use Ctrl+Click to navigate
2. **Add features** - Make changes and test immediately
3. **Debug issues** - Set breakpoints to understand behavior
4. **Read RuneLite API docs** - https://static.runelite.net/api/runelite-api/

## Alternative: Simpler Setup with Example Plugin

If the above is too complex, you can:

1. Clone the RuneLite example plugin: https://github.com/runelite/example-plugin
2. Replace its code with your plugin code
3. Follow their README for setup
4. This gives you a pre-configured project

## Resources

- **RuneLite Plugin Development Guide**: https://github.com/runelite/runelite/wiki/Building-with-IntelliJ-IDEA
- **RuneLite API Docs**: https://static.runelite.net/api/runelite-api/
- **Example Plugins**: https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins
- **Plugin Hub Plugins**: https://github.com/runelite/plugin-hub (for reference)

## Getting Help

If you get stuck:
- RuneLite Discord: https://discord.gg/runelite (#plugin-dev channel)
- Share error messages from IntelliJ's "Event Log" (bottom-right)
- Check the "Build" output window for compilation errors
