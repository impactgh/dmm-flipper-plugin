# Smithing Level Export Feature

## Overview
The RuneLite plugin now exports the player's Smithing level in the `offers.json` file, allowing the webapp to calculate accurate Barrows armor repair costs.

## Changes Made

### OfferExporter.java
1. **Added Client dependency**: Now accepts `Client` in constructor to access player skills
2. **Added Smithing level export**: Retrieves and exports the player's real Smithing level
3. **Fallback handling**: Defaults to level 1 if player is not logged in

### DMMFlipperPlugin.java
1. **Updated OfferExporter initialization**: Passes `client` instance to the exporter

## Technical Details

### Smithing Level Retrieval
```java
int smithingLevel = client.getRealSkillLevel(Skill.SMITHING);
```

This uses the RuneLite API to get the player's **real** Smithing level (not boosted level), which is appropriate for calculating repair costs.

### Export Format
The `offers.json` file now includes:
```json
{
  "timestamp": 1234567890,
  "version": "1.0",
  "smithingLevel": 99,
  "offers": [...]
}
```

### When Smithing Level is Exported
- **Every time offers are exported** (when GE offers change)
- **Automatically updates** as the player levels up
- **Defaults to 1** if player is not logged in

## Building the Plugin

After making these changes, rebuild the plugin:

### Using Gradle (Command Line)
```bash
cd kiro_projects/dmm-flipper-plugin
./gradlew build
```

### Using IntelliJ IDEA
1. Open the project in IntelliJ
2. Click "Build" → "Build Project"
3. Or use the Gradle tool window → Tasks → build → build

### Output
The compiled plugin JAR will be in:
```
build/libs/dmm-flipper-1.0-SNAPSHOT.jar
```

## Installing the Updated Plugin

1. **Build the plugin** (see above)
2. **Copy the JAR** to your RuneLite plugins folder:
   - Windows: `%USERPROFILE%\.runelite\plugins\`
   - Linux/Mac: `~/.runelite/plugins/`
3. **Restart RuneLite**
4. **Enable the plugin** in the Plugin Hub

## Testing

### Verify Smithing Level Export
1. Start RuneLite with the updated plugin
2. Log into OSRS/DMM
3. Make a GE offer (or wait for existing offers to update)
4. Check the `offers.json` file:
   - Windows: `%USERPROFILE%\.runelite\dmm-flipper\offers.json`
   - Linux/Mac: `~/.runelite/dmm-flipper/offers.json`
5. Verify the file contains `"smithingLevel": <your_level>`

### Verify Webapp Integration
1. Start the webapp (OSRS or DMM)
2. Check the header stats area
3. Should display: `Smithing: <level> (<discount>% repair discount)`
4. Check Processing Profit strategy
5. Barrows repair costs should reflect your smithing level

## Example Output

### offers.json
```json
{
  "timestamp": 1707600000,
  "version": "1.0",
  "smithingLevel": 85,
  "offers": [
    {
      "itemId": 4708,
      "type": "BUY",
      "price": 1000000,
      "qty": 10,
      "filled": 5,
      "timestamp": 1707600000,
      "itemName": "Ahrim's hood"
    }
  ]
}
```

### Webapp Display
```
Smithing: 85 (42.5% repair discount)
```

### Repair Cost Calculation
- **Base cost** (Ahrim's staff): 100,000 gp
- **Smithing level**: 85
- **Discount**: 42.5% (85 / 2)
- **Adjusted cost**: 57,500 gp
- **Savings**: 42,500 gp per repair

## Compatibility

### RuneLite API
- Uses standard RuneLite API: `client.getRealSkillLevel(Skill.SMITHING)`
- Compatible with all RuneLite versions that support the Skill enum
- No external dependencies required

### Backward Compatibility
- If the plugin is not updated, webapp defaults to smithing level 1
- Existing functionality remains unchanged
- No breaking changes to the offers.json format

## Files Modified
- `src/main/java/com/dmmflipper/OfferExporter.java`
- `src/main/java/com/dmmflipper/DMMFlipperPlugin.java`

## Related Documentation
- See `kiro_projects/osrs-flipper/SMITHING_LEVEL_FEATURE.md` for webapp implementation details
- See `QUICKSTART.md` for general plugin setup instructions
