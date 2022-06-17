/**
 * Copyright (C) 2009 Stefan Maassen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.indisopht.guice.groovy.internal.interceptors;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.log4j.Logger;

import de.indisopht.guice.groovy.GroovyProvider;
import de.indisopht.guice.groovy.internal.RecompileConfiguration;

/**
 * Interceptor for triggering automatic recompilation of changed 
 * groovy source files without the need to change references to the
 * recompiled class.
 * 
 * @author Stefan Maassen
 * @since 0.3.0
 */
@SuppressWarnings("unchecked")
public class RecompilationInterceptor implements MethodInterceptor {
    
    private static final Logger logger=Logger.getLogger(RecompilationInterceptor.class);

    private final RecompileConfiguration recompileConfig;
    private final GroovyProvider provider;
    
    private Object delegate;
    private long lastRecompilation=0;
    
    /**
     * @param recompileConfig   configuration used for recompilation
     * @param delegate  the object to be proxied
     * @param provider  the provider for delegate
     */
    public RecompilationInterceptor(RecompileConfiguration recompileConfig, Object delegate, GroovyProvider provider) {
        super();
        this.recompileConfig = recompileConfig;
        this.delegate = delegate;
        this.provider = provider;
        this.lastRecompilation=System.currentTimeMillis();
    }

    
    /**
     * @see net.sf.cglib.proxy.MethodInterceptor#intercept(java.lang.Object, java.lang.reflect.Method, java.lang.Object[], net.sf.cglib.proxy.MethodProxy)
     */
    @Override
    public Object intercept(Object object, Method method, Object[] params, MethodProxy methodProxy) throws Throwable {
        if (shouldRecompile()) {
            Object tmp=provider.get();
            if (tmp!=null) {
                logger.debug("recompiling "+provider.getSomethingGroovy());
                delegate=tmp;
            }
            this.lastRecompilation=System.currentTimeMillis();
        }
        return methodProxy.invoke(delegate, params);
    }
    
    /**
     * determines whether it is time for a recompilation check or not
     * 
     * @return true, if recompilation should be done
     */
    private boolean shouldRecompile() {
        return System.currentTimeMillis()>=lastRecompilation+recompileConfig.getIntervalIn(TimeUnit.MILLISECONDS);
    }
}