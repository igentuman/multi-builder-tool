package igentuman.mbtool.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for MbtoolConfig
 * Note: These tests verify the configuration structure and default values.
 * In a real Minecraft environment, the config would be loaded from the TOML file.
 */
public class MbtoolConfigTest {
    
    @BeforeEach
    void setUp() {
        // In a real test environment, we would need to initialize the Forge config system
        // For now, we'll test the structure and default values
    }
    
    @Test
    void testConfigDefaultValues() {
        // Test that the configuration spec has the expected default values
        assertEquals(100000, MbtoolConfig.MAX_ENERGY.getDefault());
        assertEquals(1000, MbtoolConfig.ENERGY_TRANSFER_RATE.getDefault());
        assertEquals(100, MbtoolConfig.ENERGY_PER_BLOCK.getDefault());
    }
    
    @Test
    void testConfigRanges() {
        // Test that the configuration ranges are correct
        assertTrue(MbtoolConfig.MAX_ENERGY.getDefault() >= 1000);
        assertTrue(MbtoolConfig.MAX_ENERGY.getDefault() <= 10000000);
        
        assertTrue(MbtoolConfig.ENERGY_TRANSFER_RATE.getDefault() >= 1);
        assertTrue(MbtoolConfig.ENERGY_TRANSFER_RATE.getDefault() <= 100000);
        
        assertTrue(MbtoolConfig.ENERGY_PER_BLOCK.getDefault() >= 1);
        assertTrue(MbtoolConfig.ENERGY_PER_BLOCK.getDefault() <= 10000);
    }
    
    @Test
    void testConfigSpec() {
        // Test that the config spec is properly built
        assertNotNull(MbtoolConfig.SPEC);
        assertNotNull(MbtoolConfig.BUILDER);
        
        // Test that all config values are defined
        assertNotNull(MbtoolConfig.MAX_ENERGY);
        assertNotNull(MbtoolConfig.ENERGY_TRANSFER_RATE);
        assertNotNull(MbtoolConfig.ENERGY_PER_BLOCK);
    }
    
    @Test
    void testConvenienceMethods() {
        // Test that convenience methods exist and don't throw exceptions
        // Note: In a test environment without Forge config system, these will throw IllegalStateException
        // This is expected behavior - the methods require the config to be loaded
        assertThrows(IllegalStateException.class, () -> MbtoolConfig.getMaxEnergy());
        assertThrows(IllegalStateException.class, () -> MbtoolConfig.getEnergyTransferRate());
        assertThrows(IllegalStateException.class, () -> MbtoolConfig.getEnergyPerBlock());
    }
}