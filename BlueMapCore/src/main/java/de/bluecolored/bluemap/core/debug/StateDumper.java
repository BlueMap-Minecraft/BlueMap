/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.debug;

import de.bluecolored.bluemap.core.BlueMap;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

public class StateDumper {

    private static final StateDumper GLOBAL = new StateDumper();

    private final Set<Object> instances = Collections.newSetFromMap(new WeakHashMap<>());

    public void dump(Path file) throws IOException {
        GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                .path(file)
                .build();
        ConfigurationNode node = loader.createNode();

        collectSystemInfo(node.node("system-info"));

        Set<Object> alreadyDumped = Collections.newSetFromMap(new WeakHashMap<>());

        try {
            ConfigurationNode threadDump = node.node("threads");
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                dumpInstance(thread, loader.defaultOptions(), threadDump.appendListNode(), alreadyDumped);
            }
        } catch (SecurityException ex){
            node.node("threads").set(ex.toString());
        }

        ConfigurationNode dump = node.node("dump");
        for (Object instance : instances) {
            Class<?> type = instance.getClass();
            ConfigurationNode instanceDump = dump.node(type.getName()).appendListNode();
            dumpInstance(instance, loader.defaultOptions(), instanceDump, alreadyDumped);
        }

        loader.save(node);

    }

    private void dumpInstance(Object instance, ConfigurationOptions options, ConfigurationNode node, Set<Object> alreadyDumped) throws SerializationException {

        try {
            if (instance == null){
                node.raw(null);
                return;
            }

            Class<?> type = instance.getClass();

            if (!alreadyDumped.add(instance)) {
                node.set("<<" + instance + ">>");
                return;
            }

            if (instance instanceof Map) {
                int count = 0;
                Map<?, ?> map = (Map<?, ?>) instance;

                if (map.isEmpty()){
                    node.set(map.toString());
                    return;
                }

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (++count > 100) {
                        node.appendListNode().set("<<" + (map.size() - 100) + " more elements>>");
                        break;
                    }

                    ConfigurationNode entryNode = node.appendListNode();
                    dumpInstance(entry.getKey(), options, entryNode.node("key"), alreadyDumped);
                    dumpInstance(entry.getValue(), options, entryNode.node("value"), alreadyDumped);
                }
                return;
            }

            if (instance instanceof Collection) {
                if (((Collection<?>) instance).isEmpty()){
                    node.set(instance.toString());
                    return;
                }

                int count = 0;
                for (Object entry : (Collection<?>) instance) {
                    if (++count > 100) {
                        node.appendListNode().set("<<" + (((Collection<?>) instance).size() - 100) + " more elements>>");
                        break;
                    }

                    dumpInstance(entry, options, node.appendListNode(), alreadyDumped);
                }
                return;
            }

            if (instance instanceof Object[]) {
                if (((Object[]) instance).length == 0){
                    node.set(instance.toString());
                    return;
                }

                int count = 0;
                for (Object entry : (Object[]) instance) {
                    if (++count > 100) {
                        node.appendListNode().set("<<" + (((Object[]) instance).length - 100) + " more elements>>");
                        break;
                    }

                    dumpInstance(entry, options, node.appendListNode(), alreadyDumped);
                }
                return;
            }

            if (instance instanceof Thread) {
                Thread t = (Thread) instance;
                node.node("name").set(t.getName());
                node.node("state").set(t.getState().toString());
                node.node("priority").set(t.getPriority());
                node.node("alive").set(t.isAlive());
                node.node("id").set(t.getId());
                node.node("deamon").set(t.isDaemon());
                node.node("interrupted").set(t.isInterrupted());

                dumpInstance(t.getStackTrace(), options, node.node("stackTrace"), alreadyDumped);
                return;
            }

            boolean foundSomething = dumpAnnotatedInstance(type, instance, options, node, alreadyDumped);
            if (!foundSomething) {
                node.set(instance.toString());
            }

        } catch (Exception ex) {
            node.set("Error: " + ex);
        }
    }

    private boolean dumpAnnotatedInstance(Class<?> type, Object instance, ConfigurationOptions options, ConfigurationNode node, Set<Object> alreadyDumped) throws Exception {
        boolean foundSomething = false;
        boolean allFields = type.isAnnotationPresent(DebugDump.class);

        for (Field field : type.getDeclaredFields()) {
            DebugDump dd = field.getAnnotation(DebugDump.class);
            if (dd == null) {
                if (!allFields) continue;
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (Modifier.isTransient(field.getModifiers())) continue;
            }
            foundSomething = true;

            String key = "";
            if (dd != null) key = dd.value();
            if (key.isEmpty()) key = field.getName();

            field.setAccessible(true);
            if (options.acceptsType(field.getType())) {
                node.node(key).set(field.get(instance));
            } else {
                dumpInstance(field.get(instance), options, node.node(key), alreadyDumped);
            }
        }

        for (Method method : type.getDeclaredMethods()) {
            DebugDump dd = method.getAnnotation(DebugDump.class);
            if (dd == null) continue;
            foundSomething = true;

            String key = dd.value();
            if (key.isEmpty()) key = method.toGenericString().replace(' ', '_');

            if (options.acceptsType(method.getReturnType())) {
                method.setAccessible(true);
                node.node(key).set(method.invoke(instance));
            } else {
                method.setAccessible(true);
                dumpInstance(method.invoke(instance), options, node.node(key), alreadyDumped);
            }
        }

        for (Class<?> iface : type.getInterfaces()) {
            foundSomething |= dumpAnnotatedInstance(iface, instance, options, node, alreadyDumped);
        }

        Class<?> typeSuperclass = type.getSuperclass();
        if (typeSuperclass != null) {
            foundSomething |= dumpAnnotatedInstance(typeSuperclass, instance, options, node, alreadyDumped);
        }

        return foundSomething;
    }

    private void collectSystemInfo(ConfigurationNode node) throws SerializationException {
        node.node("bluemap-version").set(BlueMap.VERSION);
        node.node("git-hash").set(BlueMap.GIT_HASH);
        node.node("git-clean").set(BlueMap.GIT_CLEAN);

        String[] properties = new String[]{
                "java.runtime.name",
                "java.runtime.version",
                "java.vm.vendor",
                "java.vm.name",
                "os.name",
                "os.version",
                "user.dir",
                "java.home",
                "file.separator",
                "sun.io.unicode.encoding",
                "java.class.version"
        };
        Map<String, String> propMap = new HashMap<>();
        for (String key : properties) {
            propMap.put(key, System.getProperty(key));
        }
        node.node("system-properties").set(propMap);

        node.node("cores").set(Runtime.getRuntime().availableProcessors());
        node.node("max-memory").set(Runtime.getRuntime().maxMemory());
        node.node("total-memory").set(Runtime.getRuntime().totalMemory());
        node.node("free-memory").set(Runtime.getRuntime().freeMemory());

        node.node("timestamp").set(System.currentTimeMillis());
        node.node("time").set(LocalDateTime.now().toString());
    }

    public static StateDumper global() {
        return GLOBAL;
    }

    public synchronized void register(Object instance) {
        GLOBAL.instances.add(instance);
    }

    public synchronized void unregister(Object instance) {
        GLOBAL.instances.remove(instance);
    }

}
