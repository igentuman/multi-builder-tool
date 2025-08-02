# Multi-Loader Minecraft Mod Development Rule

## Overview
This rule provides comprehensive guidance for developing Minecraft mods that support multiple mod loaders (Forge, Fabric, NeoForge) while maintaining code reusability and platform-specific optimizations.

## Project Structure

### Multi-Module Architecture
```
project-root/
├── settings.gradle                 # Root project configuration
├── gradle.properties              # Shared version properties
├── build.gradle                   # Root build configuration
├── common/                        # Platform-agnostic code
│   ├── build.gradle
│   └── src/main/
│       ├── java/                  # Shared business logic
│       └── resources/             # Shared assets
├── forge/                         # Forge-specific implementation
│   ├── build.gradle
│   └── src/main/
│       ├── java/                  # Forge platform services
│       └── resources/META-INF/mods.toml
├── fabric/                        # Fabric-specific implementation
│   ├── build.gradle
│   └── src/main/
│       ├── java/                  # Fabric platform services
│       └── resources/fabric.mod.json
└── neoforge/                      # NeoForge-specific (optional)
    ├── build.gradle
    └── src/main/
        ├── java/
        └── resources/META-INF/mods.toml
```

## Code Organization Principles

### Common Module (70% of codebase)
- **Business Logic**: Core mod functionality, algorithms, data structures
- **Abstractions**: Platform service interfaces, registry abstractions
- **Utilities**: Helper classes, constants, shared utilities
- **Data Models**: Item/block definitions, configuration models
- **Resources**: Textures, models, language files, recipes

### Platform Modules (30% of codebase)
- **Platform Services**: Concrete implementations of common interfaces
- **Registration**: Platform-specific registry handling
- **Event Handling**: Platform-specific event systems
- **Networking**: Platform-specific packet handling
- **Client Integration**: Platform-specific client setup

## Platform Service Pattern

### Core Service Interface (Common)
```java
// common/src/main/java/yourmod/platform/PlatformHelper.java
public interface PlatformHelper {
    <T> void registerItem(String name, Supplier<T> item);
    void sendPacketToServer(Object packet);
    void sendPacketToClient(ServerPlayer player, Object packet);
    boolean isModLoaded(String modId);
    Path getConfigDir();
}
```

### Platform Implementations
```java
// forge/src/main/java/yourmod/platform/ForgePlatformHelper.java
public class ForgePlatformHelper implements PlatformHelper {
    // Forge-specific implementations using DeferredRegister, etc.
}

// fabric/src/main/java/yourmod/platform/FabricPlatformHelper.java  
public class FabricPlatformHelper implements PlatformHelper {
    // Fabric-specific implementations using Registry, etc.
}
```

## Build Configuration

### Root settings.gradle
```gradle
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.minecraftforge.net/' }
        maven { url = 'https://maven.fabricmc.net/' }
        maven { url = 'https://maven.architectury.dev/' }
        maven { url = 'https://maven.neoforged.net/releases/' }
    }
}

include 'common'
include 'forge'  
include 'fabric'
// include 'neoforge'  // Optional
```

### Common Module build.gradle
```gradle
plugins {
    id 'java-library'
    id 'maven-publish'
}

dependencies {
    // Minecraft and mappings (compileOnly)
    compileOnly "net.minecraft:minecraft:${mc_version}"
    compileOnly "org.spongepowered:mixin:${mixin_version}"
    
    // Common dependencies that exist on all platforms
    // Use compileOnly for platform-specific APIs
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)
```

### Platform-Specific build.gradle Templates
```gradle
// forge/build.gradle
plugins {
    id 'net.minecraftforge.gradle'
    id 'org.parchmentmc.librarian.forgegradle'
}

dependencies {
    minecraft "net.minecraftforge:forge:${mc_version}-${forge_version}"
    implementation project(':common')
    
    // Forge-specific dependencies
}

// fabric/build.gradle  
plugins {
    id 'fabric-loom'
}

dependencies {
    minecraft "com.mojang:minecraft:${mc_version}"
    mappings "net.fabricmc:yarn:${yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${fabric_loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${fabric_version}"
    
    implementation project(':common')
    
    // Fabric-specific dependencies
}
```

## Registration Patterns

### Common Registration Interface
```java
// common/src/main/java/yourmod/registry/ModRegistry.java
public class ModRegistry {
    public static final PlatformHelper PLATFORM = ServiceLoader.load(PlatformHelper.class)
        .findFirst().orElseThrow();
    
    public static void registerItems() {
        PLATFORM.registerItem("your_item", () -> new YourItem(properties));
    }
}
```

### Platform-Specific Registration
```java
// forge/src/main/java/yourmod/ForgeModRegistry.java
public class ForgeModRegistry implements PlatformHelper {
    private static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    
    @Override
    public <T> void registerItem(String name, Supplier<T> item) {
        ITEMS.register(name, (Supplier<Item>) item);
    }
}

// fabric/src/main/java/yourmod/FabricModRegistry.java  
public class FabricModRegistry implements PlatformHelper {
    @Override
    public <T> void registerItem(String name, Supplier<T> item) {
        Registry.register(BuiltInRegistries.ITEM, 
            ResourceLocation.fromNamespaceAndPath(MODID, name), 
            (Item) item.get());
    }
}
```

## Networking Patterns

### Common Network Interface
```java
// common/src/main/java/yourmod/network/NetworkHandler.java
public class NetworkHandler {
    private static final PlatformHelper PLATFORM = ModRegistry.PLATFORM;
    
    public static void sendToServer(Object packet) {
        PLATFORM.sendPacketToServer(packet);
    }
    
    public static void sendToClient(ServerPlayer player, Object packet) {
        PLATFORM.sendPacketToClient(player, packet);
    }
}
```

### Platform-Specific Networking
```java
// forge/src/main/java/yourmod/network/ForgeNetworkHandler.java
public class ForgeNetworkHandler {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(/*...*/);
    
    public static void registerPackets() {
        CHANNEL.registerMessage(/*...*/);
    }
}

// fabric/src/main/java/yourmod/network/FabricNetworkHandler.java
public class FabricNetworkHandler {
    public static void registerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(/*...*/);
        ClientPlayNetworking.registerGlobalReceiver(/*...*/);
    }
}
```

## Configuration Management

### Common Config Interface
```java
// common/src/main/java/yourmod/config/ModConfig.java
public class ModConfig {
    public static boolean enableFeature = true;
    public static int maxValue = 100;
    
    public static void load() {
        ModRegistry.PLATFORM.loadConfig();
    }
}
```

### Platform-Specific Config
```java
// forge/src/main/java/yourmod/config/ForgeConfig.java
@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeConfig {
    private static final ForgeConfigSpec SPEC;
    
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        // Configure spec
        SPEC = builder.build();
    }
}

// fabric/src/main/java/yourmod/config/FabricConfig.java
public class FabricConfig {
    // Use AutoConfig or similar Fabric config library
}
```

## Mod Compatibility Handling

### Dependency Mapping
```java
// Common interface for mod compatibility
public class ModCompat {
    public static final Map<String, String> MOD_MAPPINGS = Map.of(
        "jei", "jei",           // Same on both platforms
        "rei", "roughlyenoughitems", // Fabric alternative to JEI
        "mekanism", "mekanism"  // Available on both
    );
    
    public static boolean isCompatModLoaded(String commonName) {
        String platformMod = MOD_MAPPINGS.get(commonName);
        return ModRegistry.PLATFORM.isModLoaded(platformMod);
    }
}
```

## Resource Management

### Shared Resources Structure
```
common/src/main/resources/
├── assets/yourmod/
│   ├── textures/
│   ├── models/
│   ├── lang/
│   └── sounds/
├── data/yourmod/
│   ├── recipes/
│   ├── loot_tables/
│   └── tags/
└── yourmod.mixins.json
```

### Platform-Specific Resources
```
forge/src/main/resources/
├── META-INF/
│   ├── mods.toml
│   └── accesstransformer.cfg
└── pack.mcmeta

fabric/src/main/resources/
├── fabric.mod.json
├── yourmod.mixins.json
└── assets/yourmod/icon.png
```

## Testing Strategy

### Multi-Platform Testing
```java
// common/src/test/java/yourmod/CommonTest.java
public class CommonTest {
    @Test
    public void testCommonLogic() {
        // Test platform-agnostic functionality
    }
}

// Platform-specific integration tests
// forge/src/test/java/yourmod/ForgeIntegrationTest.java
// fabric/src/test/java/yourmod/FabricIntegrationTest.java
```

## CI/CD Configuration

### GitHub Actions Multi-Platform Build
```yaml
# .github/workflows/build.yml
name: Build Multi-Platform
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        platform: [forge, fabric]
    
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: 17
      
      - name: Build ${{ matrix.platform }}
        run: ./gradlew :${{ matrix.platform }}:build
      
      - name: Upload artifacts
        uses: actions/upload-artifact@v3
        with:
          name: ${{ matrix.platform }}-build
          path: ${{ matrix.platform }}/build/libs/
```

## Migration Guidelines

### From Single-Platform to Multi-Platform

1. **Phase 1: Structure Setup**
   - Create multi-module structure
   - Move existing code to appropriate modules
   - Set up build configurations

2. **Phase 2: Abstraction Layer**
   - Identify platform-specific code
   - Create service interfaces
   - Implement platform services

3. **Phase 3: Code Migration**
   - Move common code to common module
   - Refactor platform-specific implementations
   - Update registration and networking

4. **Phase 4: Testing & Validation**
   - Test both platforms independently
   - Verify feature parity
   - Performance testing

## Best Practices

### Code Organization
- Keep business logic in common module
- Use service pattern for platform differences
- Minimize platform-specific code
- Share resources when possible

### Dependency Management
- Use compileOnly for platform APIs in common
- Map equivalent mods between platforms
- Handle missing dependencies gracefully

### Version Management
- Synchronize versions across platforms
- Use shared gradle.properties
- Maintain compatibility matrices

### Documentation
- Document platform differences
- Maintain migration guides
- Keep compatibility notes updated

## Common Pitfalls to Avoid

1. **Over-abstraction**: Don't abstract everything; some platform differences are acceptable
2. **Dependency Hell**: Carefully manage transitive dependencies
3. **Resource Duplication**: Share resources when possible, duplicate only when necessary
4. **Version Mismatches**: Keep platform versions synchronized
5. **Testing Gaps**: Test both platforms thoroughly, not just one

## Tools and Utilities

### Recommended Plugins
- **Architectury**: Provides multi-platform development tools
- **Loom**: Fabric development environment
- **ForgeGradle**: Forge development environment
- **ParchmentMC**: Improved mappings for development

### Development Environment
- Use IntelliJ IDEA with appropriate plugins
- Configure run configurations for each platform
- Set up debugging for both platforms
- Use version control effectively

This rule provides a comprehensive framework for developing multi-loader Minecraft mods while maintaining code quality, reusability, and platform-specific optimizations.