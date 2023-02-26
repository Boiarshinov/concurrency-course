package course.concurrency.m2_async.cf.min_price;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PriceAggregator {

    private static final long SHOP_REQUEST_TIMEOUT = 2950L; //Should be a bit less than 3s to pass tests

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        List<CompletableFuture<Double>> futures = shopIds.stream()
                .map(shopId -> CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId))
                        .orTimeout(SHOP_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS))
                .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .exceptionally(ignored -> null) //it is OK when some futures fall with TimeoutException
                .join();

        return futures.stream()
                .filter(future -> future.isDone() && !future.isCompletedExceptionally())
                .mapToDouble(CompletableFuture::join)
                .min()
                .orElse(Double.NaN);
    }
}
