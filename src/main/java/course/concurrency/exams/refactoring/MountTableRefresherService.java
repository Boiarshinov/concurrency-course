package course.concurrency.exams.refactoring;

import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class MountTableRefresherService {

    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    private Function<String, Others.MountTableManager> managerFactory = Others.MountTableManager::new;

    public void serviceInit()  {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<String, Others.RouterClient>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread();
                t.setName("MountTableRefresh_ClientsCacheCleaner");
                t.setDaemon(true);
                return t;
            }
        };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh()  {
        List<String> addresses = getAddresses();
        ExecutorService executor = prepareExecutor(addresses.size());

        Map<String, Future<Boolean>> futureByAddress = addresses.stream()
                .collect(Collectors.toMap(a -> a,
                        a -> {
                            String address = isLocalAdmin(a) ? "local" : a;
                            Others.MountTableManager manager = managerFactory.apply(address);
                            return executor.submit(mapToCallable(manager, address));
                        }));

        awaitCompletion(executor);

        List<Result> results = futureByAddress.entrySet().stream()
                .map(e -> new Result(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        results.stream()
                .filter(result -> !result.success)
                .forEach(result -> removeFromCache(result.address));

        logResults(results);
    }

    private List<String> getAddresses() {
        return routerStore.getCachedRecords().stream()
                .map(Others.RouterState::getAdminAddress)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private ExecutorService prepareExecutor(int addressesCount) {
        return new ThreadPoolExecutor(addressesCount, addressesCount,
                0, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>());
    }

    private void awaitCompletion(ExecutorService executor) {
        try {
            executor.shutdown();
            boolean allReqCompleted = executor.awaitTermination(cacheUpdateTimeout, TimeUnit.MILLISECONDS);
            if (!allReqCompleted) {
                executor.shutdownNow();
                log("Not all router admins updated their cache");
            }
        } catch (InterruptedException e) {
            log("Mount table cache refresher was interrupted.");
        }
    }

    private void logResults(List<Result> results) {
        Map<Boolean, List<Result>> resultsBySuccess = results.stream()
                .collect(Collectors.groupingBy(result -> result.success));

        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                resultsBySuccess.getOrDefault(true, Collections.emptyList()).size(),
                resultsBySuccess.getOrDefault(false, Collections.emptyList()).size()));
    }

    private static class Result {
        final String address;
        final boolean success;

        public Result(String address, Future<Boolean> future) {
            this.address = address;
            boolean executionResult;
            try {
                executionResult = future.get();
            } catch (CancellationException | InterruptedException | ExecutionException e) {
                executionResult = false;
            }
            this.success = executionResult;
        }
    }

    private Callable<Boolean> mapToCallable(Others.MountTableManager manager, String adminAddress) {
        return () -> manager.refresh();
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }
    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }

    void setManagerFactory(Function<String, Others.MountTableManager> managerFactory) {
        this.managerFactory = managerFactory;
    }
}