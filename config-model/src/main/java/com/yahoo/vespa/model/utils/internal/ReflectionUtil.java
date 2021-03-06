// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.model.utils.internal;

import com.yahoo.config.ChangesRequiringRestart;
import com.yahoo.config.ConfigInstance;
import com.yahoo.vespa.config.ConfigKey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Utility class containing static methods for retrievinig information about the config producer tree.
 *
 * @author lulf
 * @author bjorncs
 * @since 5.1
 */
public final class ReflectionUtil {

    private ReflectionUtil() {
    }

    /**
     * Returns a set of all the configs produced by a given producer.
     *
     * @param iface The config producer or interface to check for producers.
     * @param configId The config id to use when creating keys.
     * @return A set of config keys.
     */
    public static Set<ConfigKey<?>> configsProducedByInterface(Class<?> iface, String configId) {
        Set<ConfigKey<?>> ret = new LinkedHashSet<>();
        if (isConcreteProducer(iface)) {
            ret.add(createConfigKeyFromInstance(iface.getEnclosingClass(), configId));
        }
        for (Class<?> parentIface : iface.getInterfaces()) {
            ret.addAll(configsProducedByInterface(parentIface, configId));
        }
        return ret;
    }

    /**
     * Determines if the config class contains the methods required for detecting config value changes
     * between two config instances.
     */
    public static boolean hasRestartMethods(Class<? extends ConfigInstance> configClass) {
        try {
            configClass.getDeclaredMethod("containsFieldsFlaggedWithRestart");
            configClass.getDeclaredMethod("getChangesRequiringRestart", configClass);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Determines if the config definition for the given config class contains key-values flagged with restart.
     */
    public static boolean containsFieldsFlaggedWithRestart(Class<? extends ConfigInstance> configClass)  {
        try {
            Method m = configClass.getDeclaredMethod("containsFieldsFlaggedWithRestart");
            m.setAccessible(true);
            return (boolean) m.invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compares the config instances and lists any differences that will require service restart.
     * @param from The previous config.
     * @param to The new config.
     * @return An object describing the difference.
     */
    public static ChangesRequiringRestart getChangesRequiringRestart(ConfigInstance from, ConfigInstance to) {
        Class<?> clazz = from.getClass();
        if (!clazz.equals(to.getClass())) {
            throw new IllegalArgumentException(String.format("%s != %s", clazz, to.getClass()));
        }
        try {
            Method m = clazz.getDeclaredMethod("getChangesRequiringRestart", clazz);
            m.setAccessible(true);
            return (ChangesRequiringRestart) m.invoke(from, to);
        }  catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static ConfigKey<?> createConfigKeyFromInstance(Class<?> configInstClass, String configId) {
        try {
            String defName = ConfigInstance.getDefName(configInstClass);
            String defNamespace = ConfigInstance.getDefNamespace(configInstClass);
            return new ConfigKey<>(defName, configId, defNamespace);
        } catch (IllegalArgumentException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean classIsConfigInstanceProducer(Class<?> clazz) {
        return clazz.getName().equals(ConfigInstance.Producer.class.getName());
    }

    private static boolean isConcreteProducer(Class<?> producerInterface) {
        boolean parentIsConfigInstance = false;
        for (Class<?> ifaceParent : producerInterface.getInterfaces()) {
            if (classIsConfigInstanceProducer(ifaceParent)) {
                parentIsConfigInstance = true;
            }
        }
        return (ConfigInstance.Producer.class.isAssignableFrom(producerInterface) && parentIsConfigInstance && !classIsConfigInstanceProducer(producerInterface));
    }

}
