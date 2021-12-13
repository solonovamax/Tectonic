package com.dfsek.tectonic.impl.loading.loaders.primitives;

import com.dfsek.tectonic.impl.loading.ConfigLoader;
import com.dfsek.tectonic.api.loader.TypeLoader;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AnnotatedType;

public class ByteLoader implements TypeLoader<Byte> {
    @Override
    public Byte load(@NotNull AnnotatedType t, @NotNull Object c, @NotNull ConfigLoader loader) {
        return ((Number) c).byteValue();
    }
}