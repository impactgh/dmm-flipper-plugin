# Setting Up RuneLite with Jagex Accounts for Plugin Development

If you've converted to a Jagex Account, you need to extract credentials to test your plugin.

## Steps

### 1. Update RuneLite Launcher
Make sure your RuneLite launcher is **version 2.6.3 or newer**.

### 2. Configure RuneLite to Save Credentials

**On Windows:**
- Run "RuneLite (configure)" from the Start menu

**On Mac:**
```bash
/Applications/RuneLite.app/Contents/MacOS/RuneLite --configure
```

**On Linux:**
```bash
/path/to/RuneLite --configure
```

### 3. Add Client Argument
In the configuration window:
- Find the "Client arguments" input box
- Add: `--insecure-write-credentials`
- Click "Save"

### 4. Launch via Jagex Launcher
- Launch RuneLite through the **Jagex Launcher** (not directly)
- RuneLite will write credentials to: `~/.runelite/credentials.properties`
- **IMPORTANT**: These credentials bypass your password - keep them secure!

### 5. Test Your Plugin
Now you can:
- Launch RuneLite directly (not through Jagex Launcher)
- It will use the saved credentials automatically
- Load your plugin JAR as described in QUICKSTART.md

## For Sideloading Your Plugin

Since you're just sideloading a plugin (not doing IDE development), you can skip the credential setup and just:

1. Launch RuneLite normally through Jagex Launcher
2. Enable "Allow loading external plugins" in settings
3. Load your plugin JAR
4. The plugin will work while you're logged in

**You only need the credential setup if you're:**
- Running RuneLite from an IDE for development
- Testing without the Jagex Launcher

## Security Notes

- `credentials.properties` contains sensitive data - don't share it
- Delete it when you're done developing: `rm ~/.runelite/credentials.properties`
- To invalidate credentials: Use "End sessions" button at runescape.com account settings

## For Your Use Case

Since you're just installing a plugin (not developing in an IDE), you can:

1. Launch RuneLite via Jagex Launcher normally
2. Install the plugin JAR using the steps in QUICKSTART.md
3. Play as normal - no credential extraction needed!

The credential setup is only needed if you want to run RuneLite directly without the Jagex Launcher.
