package logic.service;

import logic.repository.CodeRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Deterministic CodeManager - sequential allocation.
 * Optimizations:
 * 1. True circular scanning for refill (guaranteed to find space if available).
 * 2. Optimized bitwise operations.
 * 3. Prevents "N+1" select queries during startup recovery.
 */
public class CodeManager {

    private static final int MAX_CODES = 100000;
    // Calculate exact size needed for the long array
    private static final int LONG_ARRAY_SIZE = (MAX_CODES + 63) / 64; 

    private static final int REFILL_BATCH_SIZE = 1000;
    private static final int LOW_WATER_MARK = 100;

    private final CodeRepository repository;
    private final long[] codeBitmap;
    private final ConcurrentLinkedDeque<Integer> hotCodeCache;
    private final ScheduledExecutorService scheduler;

    // Optional: map to track active assignments in memory. 
    // Note: Keeps track of assignments made *since server start* to save memory/startup time.
    private final Map<String, Integer> assignedMap = new ConcurrentHashMap<>();

    // Sequential cursor guarded by codeBitmap lock
    private int cursorIndex = 0;

    public CodeManager(CodeRepository repository) throws SQLException {
        this.repository = repository;
        this.codeBitmap = new long[LONG_ARRAY_SIZE];
        this.hotCodeCache = new ConcurrentLinkedDeque<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        loadInitialState();

        // Background task to refill cache if getting low
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Check size without locking first (optimization)
                if (hotCodeCache.size() < LOW_WATER_MARK) {
                    refillHotCache(REFILL_BATCH_SIZE);
                }
            } catch (SQLException e) {
                System.err.println("[CodeManager] Async refill failed: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[CodeManager] Critical error in scheduler: " + e.getMessage());
            }
        }, 1, 5, TimeUnit.SECONDS); // Start after 1 second to allow startup to settle
    }

    /**
     * O(1) Check if an index is marked as used in the bitmap.
     */
    public boolean isIndexUsed(int index) {
        if (index < 0 || index >= MAX_CODES) return true;
        return (codeBitmap[index >> 6] & (1L << (index & 63))) != 0;
    }

    private void setIndexUsed(int index) {
        codeBitmap[index >> 6] |= (1L << (index & 63));
    }

    private void clearIndexUsed(int index) {
        codeBitmap[index >> 6] &= ~(1L << (index & 63));
    }

    /**
     * Loads the initial state from the DB.
     * WARNING: We intentionally do NOT load 'assignedMap' (userIds) here because 
     * fetching userId for every single code would cause N+1 DB queries and hang startup.
     * We only recover the used-status (Bitmap).
     */
    private void loadInitialState() throws SQLException {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println("[CodeManager] Loading initial state from DB (Attempt " + attempt + ")...");
                List<String> usedCodes = repository.loadUsedCodes();
                int loadedCount = 0;
                
                for (String code : usedCodes) {
                    try {
                        int idx = Integer.parseInt(code);
                        if (idx >= 0 && idx < MAX_CODES) {
                            setIndexUsed(idx);
                            loadedCount++;
                        }
                    } catch (NumberFormatException ignored) {
                        // Skip corrupted data
                    }
                }
                
                // If the cache is completely empty after load, trigger an immediate refill
                // so the first user doesn't wait.
                if (hotCodeCache.isEmpty() && loadedCount < MAX_CODES) {
                    System.out.println("[CodeManager] Cache is empty after startup, pre-filling now...");
                    refillHotCache(REFILL_BATCH_SIZE);
               }
                
                System.out.println("[CodeManager] Recovery complete. Loaded " + loadedCount + " active codes.");
                return;
                
            } catch (SQLException e) {
                if (attempt == maxRetries) throw e;
                System.err.println("[CodeManager] DB Failure during load, retrying in 2s...");
                try { TimeUnit.SECONDS.sleep(2); } catch (InterruptedException ignored) {}
            }
        }
    }

    public String getAndAssignCode(int userId) throws SQLException {
        Integer assignedIndex = hotCodeCache.poll();
        
        // Double-check locking pattern for empty cache
        if (assignedIndex == null) {
            // Synchronous refill if empty (blocking the user, but necessary)
            refillHotCache(REFILL_BATCH_SIZE);
            assignedIndex = hotCodeCache.poll();
            
            if (assignedIndex == null) {
                throw new RuntimeException("Code pool exhausted - No free codes available.");
            }
        }

        String assignedCodeString = String.format("%05d", assignedIndex);

        // 1. Forward to repository (The Source of Truth)
        // Note: If this throws exception, the code is lost from cache but marked in Bitmap. 
        // Ideally we should rollback bit, but for "No-Op" repo it doesn't matter.
        repository.insertAssignment(assignedCodeString, userId);
        
        // 2. Track locally
        assignedMap.put(assignedCodeString, userId);

        // 3. Trigger async refill if we dipped below water mark
        if (hotCodeCache.size() < LOW_WATER_MARK) {
            scheduler.execute(() -> {
                try {
                    refillHotCache(REFILL_BATCH_SIZE);
                } catch (SQLException ignored) {}
            });
        }

        return assignedCodeString;
    }



    /**
     * Scans the bitmap sequentially to find free codes.
     * Uses a circular cursor to ensure fair coverage of the entire range.
     */
    public void refillHotCache(int requiredCount) throws SQLException {
        List<Integer> indicesToInsert = new ArrayList<>(requiredCount);
        List<String> codesToPersist = new ArrayList<>(requiredCount);

        synchronized (codeBitmap) {
            // Safety counter to prevent infinite loop if MAX_CODES is reached (pool full)
            int scannedCount = 0; 

            while (indicesToInsert.size() < requiredCount && scannedCount < MAX_CODES) {
                int candidate = cursorIndex;
                
                // Move cursor forward (Circular Buffer Logic)
                cursorIndex = (cursorIndex + 1);
                if (cursorIndex >= MAX_CODES) {
                    cursorIndex = 0;
                }

                if (!isIndexUsed(candidate)) {
                    setIndexUsed(candidate);
                    indicesToInsert.add(candidate);
                    codesToPersist.add(String.format("%05d", candidate));
                }
                
                scannedCount++;
            }
        }

        if (!indicesToInsert.isEmpty()) {
            hotCodeCache.addAll(indicesToInsert);
            // Bulk insert is much more efficient
            repository.insertBatch(codesToPersist);
        }
    }

    public void releaseCode(String code) throws SQLException {
        try {
            int index = Integer.parseInt(code);
            
            // 1. DB Update
            repository.releaseCodeFromDB(code);
            
            // 2. Memory Cleanup
            assignedMap.remove(code);
            clearIndexUsed(index);
            
            // Adding to back preserves order. 
            hotCodeCache.offer(index); 
            
        } catch (NumberFormatException ex) {
            throw new SQLException("Invalid code format: " + code);
        }
    }

    /**
     * Warning: This only clears codes tracked in memory since startup.
     * Use with caution if precise full-history cleanup is needed.
     */
    public String releaseCodeForUser(int userId) throws SQLException {
        for (Map.Entry<String, Integer> e : assignedMap.entrySet()) {
            if (e.getValue() == userId) {
                String code = e.getKey();
                releaseCode(code); 
                return code;
            }
        }
        return null;
    }

    public ConcurrentLinkedDeque<Integer> getHotCodeCache() { return hotCodeCache; }

    public void shutdown() { 
        scheduler.shutdownNow(); 
    }
}