package com.dfsek.tectonic.api.loader;

import com.dfsek.tectonic.api.ObjectTemplate;
import com.dfsek.tectonic.api.TypeRegistry;
import com.dfsek.tectonic.api.config.Configuration;
import com.dfsek.tectonic.api.config.template.ConfigTemplate;
import com.dfsek.tectonic.api.config.template.ValidatedConfigTemplate;
import com.dfsek.tectonic.api.exception.ConfigException;
import com.dfsek.tectonic.api.exception.LoadException;
import com.dfsek.tectonic.api.exception.ValidationException;
import com.dfsek.tectonic.api.loader.type.TypeLoader;
import com.dfsek.tectonic.api.preprocessor.ValuePreprocessor;
import com.dfsek.tectonic.impl.loading.loaders.EnumLoader;
import com.dfsek.tectonic.impl.loading.loaders.StringLoader;
import com.dfsek.tectonic.impl.loading.loaders.generic.ArrayListLoader;
import com.dfsek.tectonic.impl.loading.loaders.generic.HashMapLoader;
import com.dfsek.tectonic.impl.loading.loaders.generic.HashSetLoader;
import com.dfsek.tectonic.impl.loading.loaders.other.DurationLoader;
import com.dfsek.tectonic.impl.loading.loaders.primitives.BooleanLoader;
import com.dfsek.tectonic.impl.loading.loaders.primitives.ByteLoader;
import com.dfsek.tectonic.impl.loading.loaders.primitives.CharLoader;
import com.dfsek.tectonic.impl.loading.loaders.primitives.DoubleLoader;
import com.dfsek.tectonic.impl.loading.loaders.primitives.FloatLoader;
import com.dfsek.tectonic.impl.loading.loaders.primitives.IntLoader;
import com.dfsek.tectonic.impl.loading.loaders.primitives.LongLoader;
import com.dfsek.tectonic.impl.loading.loaders.primitives.ShortLoader;
import com.dfsek.tectonic.impl.loading.object.ObjectTemplateLoader;
import com.dfsek.tectonic.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Class to load a config using reflection magic.
 */
public class ConfigLoader implements TypeRegistry {
    private static final EnumLoader ENUM_LOADER = new EnumLoader();
    private final Map<Type, TypeLoader<?>> loaders = new HashMap<>();
    private final Map<Class<? extends Annotation>, List<ValuePreprocessor<?>>> preprocessors = new HashMap<>();

    public ConfigLoader() {
        // Default primitive/wrapper loaders
        BooleanLoader booleanLoader = new BooleanLoader();
        registerLoader(boolean.class, booleanLoader);
        registerLoader(Boolean.class, booleanLoader);

        ByteLoader byteLoader = new ByteLoader();
        registerLoader(byte.class, byteLoader);
        registerLoader(Byte.class, byteLoader);

        ShortLoader shortLoader = new ShortLoader();
        registerLoader(short.class, shortLoader);
        registerLoader(Short.class, shortLoader);

        CharLoader charLoader = new CharLoader();
        registerLoader(char.class, charLoader);
        registerLoader(Character.class, charLoader);

        IntLoader intLoader = new IntLoader();
        registerLoader(int.class, intLoader);
        registerLoader(Integer.class, intLoader);

        LongLoader longLoader = new LongLoader();
        registerLoader(long.class, longLoader);
        registerLoader(Long.class, longLoader);

        FloatLoader floatLoader = new FloatLoader();
        registerLoader(float.class, floatLoader);
        registerLoader(Float.class, floatLoader);

        DoubleLoader doubleLoader = new DoubleLoader();
        registerLoader(double.class, doubleLoader);
        registerLoader(Double.class, doubleLoader);

        // Default class loaders
        registerLoader(String.class, new StringLoader());

        ArrayListLoader arrayListLoader = new ArrayListLoader();
        registerLoader(ArrayList.class, arrayListLoader);
        registerLoader(List.class, arrayListLoader); // Lists will default to ArrayList.

        HashMapLoader hashMapLoader = new HashMapLoader();
        registerLoader(HashMap.class, hashMapLoader);
        registerLoader(Map.class, hashMapLoader); // Maps will default to HashMap.

        HashSetLoader hashSetLoader = new HashSetLoader();
        registerLoader(HashSet.class, hashSetLoader);
        registerLoader(Set.class, hashSetLoader); // Sets will default to HashSet.

        registerLoader(Duration.class, new DurationLoader());

        registerLoader(Enum.class, ENUM_LOADER);
    }

    /**
     * Register a custom class loader for a type
     *
     * @param t      Type
     * @param loader Loader to load type with
     * @return This config loader
     */
    public @NotNull ConfigLoader registerLoader(@NotNull Type t, @NotNull TypeLoader<?> loader) {
        loaders.put(t, loader);
        return this;
    }

    public <T> @NotNull ConfigLoader registerLoader(@NotNull Type t, @NotNull Supplier<ObjectTemplate<T>> provider) {
        loaders.put(t, new ObjectTemplateLoader<>(provider));
        return this;
    }

    public <T extends Annotation> ConfigLoader registerPreprocessor(Class<? extends T> clazz, ValuePreprocessor<T> processor) {
        preprocessors.computeIfAbsent(clazz, c -> new ArrayList<>()).add(processor);
        return this;
    }

    /**
     * Load a {@link Configuration} to a ConfigTemplate object.
     *
     * @param config        ConfigTemplate to put config on.
     * @param configuration Configuration to load from.
     * @return The loaded configuration. <b>not</b> a new instance.
     * @throws ConfigException If config cannot be loaded.
     */
    public <T extends ConfigTemplate> T load(T config, Configuration configuration) throws ConfigException {
        T result = config.loader().load(config, configuration, this::loadType);
        if(result instanceof ValidatedConfigTemplate
                && !((ValidatedConfigTemplate) result).validate()) {
            throw new ValidationException("Failed to validate config. Reason unspecified:" + configuration.getName());
        }
        return result;
    }

    /**
     * Load an Object using the {@link TypeLoader}s registered with this ConfigTemplate.
     *
     * @param t Type of object to load
     * @param o Object to pass to TypeLoader
     * @return Loaded object.
     * @throws LoadException If object could not be loaded.
     */
    @SuppressWarnings("unchecked")
    public Object loadType(AnnotatedType t, Object o) throws LoadException {
        for(Annotation annotation : t.getAnnotations()) {
            if(preprocessors.containsKey(annotation.annotationType())) {
                for(ValuePreprocessor<?> preprocessor : preprocessors.get(annotation.annotationType())) {
                    o = ((ValuePreprocessor<Annotation>) preprocessor).process(t, o, this, annotation).apply(o);
                }
            }
        }
        return getObject(t, o);
    }

    private Object getObject(AnnotatedType t, Object o) throws LoadException {
        try {
            Type raw = t.getType();
            if(loaders.containsKey(raw)) return loaders.get(raw).load(t, o, this);
            if(t instanceof AnnotatedParameterizedType) {
                raw = ((ParameterizedType) t.getType()).getRawType();
                if(loaders.containsKey(raw)) return loaders.get(raw).load(t, o, this);
            }

            if(raw instanceof Class && ((Class<?>) raw).isEnum()) {
                return ENUM_LOADER.load(t, o, this); // Special enum loader if enum doesn't already have loader defined.
            }

            throw new LoadException("No loaders are registered for type " + t.getType().getTypeName());
        } catch(LoadException e) { // Rethrow LoadExceptions.
            throw e;
        } catch(Exception e) { // Catch, wrap, and rethrow exception.
            throw new LoadException("Unexpected exception thrown during type loading: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T loadType(Class<T> clazz, Object o) throws LoadException {
        try {
            if(loaders.containsKey(clazz))
                return ReflectionUtil.cast(clazz, ((TypeLoader<Object>) loaders.get(clazz)).load((Class<Object>) clazz, o, this));
            else return ReflectionUtil.cast(clazz, o);
        } catch(LoadException e) { // Rethrow LoadExceptions.
            throw e;
        } catch(Exception e) { // Catch, wrap, and rethrow exception.
            throw new LoadException("Unexpected exception thrown during type loading: " + e.getMessage(), e);
        }
    }
}