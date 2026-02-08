# Simple IntelliJ Setup (No Extra Plugins Needed)

## Step 1: Install IntelliJ IDEA

1. Download from https://www.jetbrains.com/idea/download/
2. Install **Community Edition** (free)
3. Launch it

## Step 2: Open Your Project

1. Click **"Open"** on the welcome screen
2. Navigate to `kiro_projects/dmm-flipper-plugin`
3. Click **OK**
4. Click **"Trust Project"**
5. Wait for Gradle to sync (progress bar in bottom-right)
   - This downloads RuneLite dependencies
   - May take 2-5 minutes first time

## Step 3: Add a Run Task to build.gradle

We need to add a task that runs RuneLite with your plugin.

1. In IntelliJ, open `build.gradle`
2. Add this at the end of the file:

```gradle
task runClient(type: JavaExec) {
    group = "runelite"
    description = "Run RuneLite with this plugin"
    
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'net.runelite.client.RuneLite'
    
    jvmArgs = [
        '-ea',
        '-Xmx512m',
        '-Xss2m',
        '-Drunelite.pluginhub.skip=true'
    ]
    
    // Point to your plugin
    systemProperty 'runelite.externalplugins', project.buildDir.toString() + '/libs'
}
```

3. Save the file
4. Click the **Gradle** tab on the right side
5. Click the **refresh** icon (circular arrows)

## Step 4: Run Your Plugin

1. In the **Gradle** tab (right side), expand:
   - **dmm-flipper-plugin**
   - **Tasks**
   - **runelite**
2. Double-click **runClient**
3. RuneLite should launch with your plugin!

## Alternative: Create Run Configuration

If the Gradle task doesn't work:

1. **Run → Edit Configurations**
2. Click **+** → **Application**
3. Fill in:
   - **Name**: `RuneLite`
   - **Main class**: `net.runelite.client.RuneLite`
   - **VM options**: `-ea -Xmx512m`
   - **Working directory**: `$ProjectFileDir$`
   - **Use classpath of module**: `dmm-flipper-plugin.main`
4. Click **OK**
5. Click the green **play button** in toolbar

## Step 5: Test Your Plugin

1. In the launched RuneLite, log in
2. Click the **puzzle piece** icon (plugins)
3. Look for "DMM Flipper"
4. Enable it
5. Click the DMM Flipper icon in the sidebar

## Making Changes

1. Edit your code
2. **Build → Build Project** (or Ctrl+F9)
3. Re-run using the play button
4. RuneLite restarts with changes

## Debugging

1. Click in the left margin next to code (red dot = breakpoint)
2. Click the **bug icon** (🐛) instead of play
3. Code pauses at breakpoint
4. Inspect variables in the bottom panel

## Troubleshooting

### "Cannot resolve symbol RuneLite"

Gradle didn't sync:
1. **File → Invalidate Caches → Invalidate and Restart**
2. After restart, click **Gradle** tab → **refresh**

### "Main class not found"

Wrong classpath:
1. **File → Project Structure → Modules**
2. Make sure `runelite-client` is in dependencies
3. If not, Gradle didn't sync properly

### "Plugin doesn't show up"

Build first:
1. **Build → Build Project**
2. Check `build/libs/` has the JAR
3. Try running again

### Gradle sync takes forever

1. Check internet connection
2. Try: **File → Settings → Build Tools → Gradle**
3. Change "Gradle JVM" to Java 11
4. Click **OK** and let it re-sync

## Simpler Alternative: Use Example Plugin Template

If this is too complex:

1. Clone the example plugin:
```bash
git clone https://github.com/runelite/example-plugin.git
cd example-plugin
```

2. Copy your source files into it:
```bash
cp -r kiro_projects/dmm-flipper-plugin/src/main/java/com/dmmflipper/* example-plugin/src/main/java/com/example/
```

3. Update package names and follow their README

The example plugin has everything pre-configured.

## What You Get with IntelliJ

✅ Instant code completion
✅ See errors as you type
✅ Click to jump to definitions
✅ Debugging with breakpoints
✅ No manual JAR copying
✅ Fast iteration cycle

## Resources

- **RuneLite API Docs**: https://static.runelite.net/api/runelite-api/
- **Example Plugins**: https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins
- **Discord**: https://discord.gg/runelite (#plugin-dev channel)
