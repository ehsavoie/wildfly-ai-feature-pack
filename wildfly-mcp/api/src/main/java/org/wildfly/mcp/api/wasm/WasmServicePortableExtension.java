/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.mcp.api.wasm;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import java.util.HashSet;
import java.util.Set;

public class WasmServicePortableExtension implements Extension {
    private static final Set<Class<?>> detectedWasmServicesDeclaredInterfaces = new HashSet<>();
    private final ClassLoader deploymentClassLoader;

    public WasmServicePortableExtension(ClassLoader deploymentClassLoader) {
        this.deploymentClassLoader = deploymentClassLoader;
    }

    public static Set<Class<?>> getDetectedAIServicesDeclaredInterfaces() {
        return detectedWasmServicesDeclaredInterfaces;
    }

    <T> void processAnnotatedType(@Observes @WithAnnotations({WasmToolService.class}) ProcessAnnotatedType<T> pat) {
        if (pat.getAnnotatedType().getJavaClass().isInterface()) {
            System.out.println("processAnnotatedType register " + pat.getAnnotatedType().getJavaClass().getName());
            detectedWasmServicesDeclaredInterfaces.add(pat.getAnnotatedType().getJavaClass());
        } else {
            System.out.println("processAnnotatedType reject " + pat.getAnnotatedType().getJavaClass().getName()
                    + " which is not an interface");
            pat.veto();
        }
    }

    public void atd(@Observes AfterBeanDiscovery atd) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        for (Class<?> wasmToolServiceClass : detectedWasmServicesDeclaredInterfaces) {
            System.out.println("afterBeanDiscovery create synthetic:  " + wasmToolServiceClass.getName() + " " + wasmToolServiceClass.getClassLoader());
            atd.addBean()
                    .scope(ApplicationScoped.class)
                    .addQualifier(Default.Literal.INSTANCE)
                    .beanClass(wasmToolServiceClass)
                    .types(wasmToolServiceClass)
                    .produceWith(lookup -> {
                        System.out.println("Producing synthetic:  " + wasmToolServiceClass.getName());
                        String invokerName = wasmToolServiceClass.getAnnotation(WasmToolService.class).wasmToolConfigurationName();
                        WasmInvoker invoker = lookup.select(WasmInvoker.class, NamedLiteral.of(invokerName)).get();
                        return WasmTools.create(wasmToolServiceClass, invoker);
                    });
        }
    }
}
