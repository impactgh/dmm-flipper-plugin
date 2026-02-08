# Alternative: Develop with Auto-Build

Since running RuneLite directly from IntelliJ is complex, here's a simpler workflow:

## Setup: Auto-Build on Changes

1. **In IntelliJ**: Open your project
2. **File → Settings → Build, Execution, Deployment → Compiler**
3. Enable **"Build project automatically"**
4. Click **OK**

## Setup: Copy JAR Automatically

Add this to your `build.gradle` at the end:

```gradle
task copyToRuneLite(type: Copy) {
    from 'build/libs'
    into System.getProperty("user.home") + '/.runelite/externalplugins'
    include '*.jar'
}

build.finalizedBy(copyToRuneLite)
```

This automatically copies your JAR to RuneLite after building.

## Workflow

1. **Start RuneLite normally** (via Jagex Launcher)
2. **Make changes in IntelliJ**
3. **Build → Build Project** (Ctrl+F9)
4. **Restart RuneLite** to load the new version
5. Test your changes

## Even Faster: Use a Script

Create a file `rebuild-and-notify.sh` (Mac/Linux) or `rebuild.bat` (Windows):

**Windows (rebuild.bat):**
```batch
@echo off
cd /d %~dp0
call gradlew.bat build
echo Plugin rebuilt! Restart RuneLite to load changes.
pause
```

**Mac/Linux (rebuild.sh):**
```bash
#!/bin/bash
cd "$(dirname "$0")"
./gradlew build
echo "Plugin rebuilt! Restart RuneLite to load changes."
```

Make it executable (Mac/Linux):
```bash
chmod +x rebuild.sh
```

Then just double-click the script after making changes!

## Best Workflow for Development

1. **Keep RuneLite closed** while developing
2. **Make multiple changes** in IntelliJ
3. **Run the rebuild script** (or Ctrl+F9 in IntelliJ)
4. **Launch RuneLite** to test
5. Repeat

This is faster than trying to run RuneLite from IntelliJ.

## Debugging Without Running from IntelliJ

Use **logging** instead of breakpoints:

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyClass {
    public void myMethod() {
        log.info("Debug: value is {}", someVariable);
        log.debug("Detailed info: {}", detailedData);
    }
}
```

Then check RuneLite logs:
- **Help → Open logs folder**
- Open `client.log`
- Search for your log messages

## Why This Is Easier

- No complex classpath configuration
- No RuneLite source code needed
- Works with Jagex Accounts
- Simple build → test cycle
- Still get IntelliJ benefits (autocomplete, error checking, etc.)

## IntelliJ Benefits You Still Get

✅ Code completion (Ctrl+Space)
✅ Error detection as you type
✅ Refactoring tools
✅ Find usages (Alt+F7)
✅ Go to definition (Ctrl+B)
✅ Quick fixes (Alt+Enter)
✅ Code formatting (Ctrl+Alt+L)

You just don't get live debugging with breakpoints.

## If You Really Want Debugging

The proper way requires:
1. Clone RuneLite source: `git clone https://github.com/runelite/runelite.git`
2. Open RuneLite project in IntelliJ
3. Add your plugin as a module
4. Configure complex run settings

This is overkill for most plugin development. The build → test cycle is usually enough.
