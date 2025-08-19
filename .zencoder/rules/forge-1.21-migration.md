---
description: "Forge 1.21 Migration Guidelines"
globs: ["*.java", "*.gradle", "*.properties", "*.toml", "*.json"]
alwaysApply: true
---

# Forge 1.21 Migration Guidelines

## API Changes
- Use `net.minecraft.world.item.Item.Properties` builder pattern for item properties
- Registry system now uses `DeferredRegister` for all registrations
- Event handling may require updated event classes and parameters
- Check for renamed or moved packages in both Minecraft and Forge

## Resource Updates
- Ensure all JSON files follow the 1.21 format specifications
- Update model files if needed
- Check for changes in resource paths

## Dependencies
- Update all dependencies to their 1.21 versions
- For dependencies not yet updated:
    - Consider temporary workarounds
    - Look for alternative implementations
    - Isolate dependency code to make future updates easier

## Testing Guidelines
- Test each feature individually
- Verify multiblock structure detection and building
- Check compatibility with major mods (Mekanism, GTCEu, etc.)
- Test performance with large multiblock structures

## Common Migration Issues
- Watch for changes in rendering systems
- Check for updates to capability system
- Network packet handling may have changed
- Resource loading and data generation might use new patterns

## Best Practices
- Use the latest Forge documentation as reference
- Join the Forge Discord for migration assistance
- Check GitHub issues of similar mods for common problems
- Maintain backward compatibility where possible