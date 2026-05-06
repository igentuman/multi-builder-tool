package igentuman.mbtool.util;

import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Server-side singleton that tracks which blocks have been requested for autocrafting per player.
 * Stores pending crafting plan futures so that the server thread is never blocked.
 */
public class AutocraftTracker {

    public enum RequestStatus { CALCULATING, SUBMITTED, FAILED }

    /**
     * Holds the state of a single autocrafting request.
     * When status is CALCULATING, the planFuture is non-null and being computed off-thread.
     */
    public static class CraftRequest {
        public RequestStatus status;
        public Future<?> planFuture; // Future<ICraftingPlan>, stored as raw Future to avoid AE2 import
        public int amount;

        public CraftRequest(RequestStatus status, Future<?> planFuture, int amount) {
            this.status = status;
            this.planFuture = planFuture;
            this.amount = amount;
        }
    }

    // Map<PlayerUUID, Map<Block, CraftRequest>>
    public static final Map<UUID, Map<Block, CraftRequest>> activeRequests = new ConcurrentHashMap<>();

    public static CraftRequest getRequest(UUID playerUUID, Block block) {
        Map<Block, CraftRequest> playerRequests = activeRequests.get(playerUUID);
        if (playerRequests == null) return null;
        return playerRequests.get(block);
    }

    public static boolean hasActiveRequest(UUID playerUUID, Block block) {
        CraftRequest request = getRequest(playerUUID, block);
        return request != null && (request.status == RequestStatus.CALCULATING || request.status == RequestStatus.SUBMITTED);
    }

    public static void trackCalculation(UUID playerUUID, Block block, Future<?> planFuture, int amount) {
        activeRequests.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .put(block, new CraftRequest(RequestStatus.CALCULATING, planFuture, amount));
    }

    public static void markSubmitted(UUID playerUUID, Block block) {
        CraftRequest request = getRequest(playerUUID, block);
        if (request != null) {
            request.status = RequestStatus.SUBMITTED;
            request.planFuture = null;
        }
    }

    public static void markFailed(UUID playerUUID, Block block) {
        Map<Block, CraftRequest> playerRequests = activeRequests.get(playerUUID);
        if (playerRequests != null) {
            playerRequests.remove(block);
        }
    }

    public static void clearPlayer(UUID playerUUID) {
        activeRequests.remove(playerUUID);
    }

    public static void clearAll() {
        activeRequests.clear();
    }
}
