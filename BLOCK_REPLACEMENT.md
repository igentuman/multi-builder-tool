# Block Replacement System

The Multi-Builder Tool now supports automatic block replacement during construction. This feature allows you to build structures even when you don't have the exact blocks specified in the structure file.

## How It Works

When building a multiblock structure, the tool will:

1. **Check for exact blocks first** - If you have the exact blocks required, they will be used
2. **Look for equivalent blocks** - If you don't have the exact block, the tool will check for equivalent blocks you've configured
3. **Use the replacement** - The tool will place the equivalent block instead, preserving as many block properties as possible (like rotation, facing direction, etc.)

## Configuration

Block equivalency sets are defined in the `mbtool-common.toml` config file under the `[blockReplacementSettings]` section.

### Format

Each equivalency set is a comma-separated list of block IDs that can be used interchangeably:

```toml
blockEquivalencySets = [
    "mekanism:basic_mechanical_pipe,mekanism:advanced_mechanical_pipe,mekanism:elite_mechanical_pipe,mekanism:ultimate_mechanical_pipe",
    "mekanism:basic_pressurized_tube,mekanism:advanced_pressurized_tube,mekanism:elite_pressurized_tube,mekanism:ultimate_pressurized_tube",
    "mekanism:basic_universal_cable,mekanism:advanced_universal_cable,mekanism:elite_universal_cable,mekanism:ultimate_universal_cable"
]
```

### Example Use Cases

1. **Mekanism Pipes**: If a structure requires Ultimate Mechanical Pipes but you only have Basic ones, the tool will use the Basic pipes instead
2. **Thermal Expansion Ducts**: You can define equivalency between different tiers of ducts
3. **Applied Energistics Cables**: Different cable types can be made equivalent
4. **Any modded blocks**: Any blocks can be made equivalent as long as they have the same basic function

### Adding Your Own Equivalency Sets

1. Open the `mbtool-common.toml` config file
2. Add a new line to the `blockEquivalencySets` array
3. List all equivalent blocks separated by commas
4. Use the full mod:block_name format (e.g., `minecraft:stone`, `mekanism:steel_casing`)
5. Restart the game or reload the config

### Example Configuration

```toml
[blockReplacementSettings]
blockEquivalencySets = [
    # Mekanism Mechanical Pipes (all tiers are equivalent)
    "mekanism:basic_mechanical_pipe,mekanism:advanced_mechanical_pipe,mekanism:elite_mechanical_pipe,mekanism:ultimate_mechanical_pipe",
    
    # Thermal Expansion Ducts
    "thermal:fluid_duct,thermal:fluid_duct_windowed",
    
    # Applied Energistics Cables
    "appliedenergistics2:fluix_glass_cable,appliedenergistics2:fluix_covered_cable,appliedenergistics2:fluix_smart_cable",
    
    # Custom equivalency - different stone types
    "minecraft:stone,minecraft:cobblestone,minecraft:stone_bricks"
]
```

## Important Notes

- **Property Preservation**: The system attempts to preserve block properties like rotation, facing direction, etc. when replacing blocks
- **Creative Mode**: Block replacement is disabled in creative mode - you'll always get the exact blocks from the structure
- **Inventory Check**: The tool will only use blocks you actually have in your inventory
- **Order Matters**: The tool will use the first available equivalent block it finds in your inventory
- **Config Reload**: Changes to the config require a game restart to take effect

## Troubleshooting

- **Invalid Block IDs**: If you specify an invalid block ID, it will be ignored and logged to the console
- **No Replacement Found**: If no equivalent blocks are available in your inventory, the build will fail with an "insufficient blocks" message
- **Property Mismatch**: If the replacement block doesn't support the same properties as the original, default values will be used