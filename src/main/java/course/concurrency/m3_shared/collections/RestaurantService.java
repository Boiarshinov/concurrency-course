package course.concurrency.m3_shared.collections;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class RestaurantService {

    private final Map<String, Restaurant> restaurantMap = new ConcurrentHashMap<>() {{
        put("A", new Restaurant("A"));
        put("B", new Restaurant("B"));
        put("C", new Restaurant("C"));
    }};

    private final Map<String, LongAdder> stat = new ConcurrentHashMap<>();
    {
        // restaurantMap never changes, so we can initialise stat map on service creation
        restaurantMap.keySet().forEach(k -> stat.put(k, new LongAdder()));
    }

    public Restaurant getByName(String restaurantName) {
        addToStat(restaurantName);
        return restaurantMap.get(restaurantName);
    }

    public void addToStat(String restaurantName) {
        stat.get(restaurantName).add(1);
    }

    public Set<String> printStat() {
        return stat.entrySet().stream()
                .map(e -> String.format("%s - %d", e.getKey(), e.getValue().intValue()))
                .collect(Collectors.toSet());
    }
}
