package com.dfsek.tectonic.loading.loaders.primitives;

import com.dfsek.tectonic.loading.ConfigLoader;
import com.dfsek.tectonic.loading.TypeLoader;

import java.lang.reflect.Type;

public class LongLoader implements TypeLoader<Long> {
    @Override
    public Long load(Type t, Object c, ConfigLoader loader) {
        return ((Number) c).longValue();
    }
}
