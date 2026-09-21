package com.sja.system.service.fee;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 根据作品类型定位唯一持久化适配器。
 */
@Component
// 202609201644 - 完成了稿费作品类型与持久化适配器的集中注册。
public class FeeWorkAdapterRegistry {

    private final Map<FeeWorkType, FeeWorkAdapter> adapters;

    public FeeWorkAdapterRegistry(List<FeeWorkAdapter> adapters) {
        Map<FeeWorkType, FeeWorkAdapter> registered = new EnumMap<>(FeeWorkType.class);
        for (FeeWorkAdapter adapter : adapters) {
            FeeWorkAdapter previous = registered.put(adapter.supports(), adapter);
            if (previous != null) {
                throw new IllegalStateException("作品类型存在重复适配器: " + adapter.supports().code());
            }
        }
        this.adapters = Map.copyOf(registered);
    }

    public FeeWorkAdapter require(String workType) {
        FeeWorkType type = FeeWorkType.require(workType);
        FeeWorkAdapter adapter = adapters.get(type);
        if (adapter == null) throw new IllegalStateException("作品类型缺少适配器: " + type.code());
        return adapter;
    }
}
