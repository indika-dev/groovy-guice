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

package de.indisopht.guice.groovy.internal;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.google.inject.Inject;

import de.indisopht.guice.groovy.GroovyGuice;

/**
 * Configures and creates a suitable GroovyClassLoader
 * 
 * @author Stefan Maassen
 * @since 0.3.0
 */
public class GroovyClassloaderFactory {
    
    private static final Logger logger=Logger.getLogger(GroovyClassloaderFactory.class);
    private static final RecompileConfiguration STDRecompileInterval = new RecompileConfiguration(5l, TimeUnit.SECONDS);

    private boolean resolve = true;
    private String codeBase = null;
    private Boolean recompile = true;
    private ClasspathContainer classPath = new ClasspathContainer();
    private BridgedClassLoader bridgedClassloader = null;
    private BundleContext bundleContext = null;
    private GroovyClassLoader groovyClassLoader = null;

    public GroovyClassloaderFactory() {
    }

    /**
     * @param resolve   whether loaded classes should be resolved
     * @param codeBase  the code base to be used for {@link java.lang.SecurityManager}
     * @param recompile whether recompilation should be enabled
     * @param classPath classpath to be used
     * @param classLoader   preconfigured classloader
     * @param bridgedLoader the bridge to be used for loading classes
     */
    public GroovyClassloaderFactory(boolean resolve, String codeBase, boolean recompile, ClasspathContainer classPath, GroovyClassLoader classLoader, BridgedClassLoader bridgedLoader) {
        super();
        this.resolve = resolve;
        this.codeBase = codeBase;
        this.recompile = recompile;
        this.classPath = classPath;
        this.bridgedClassloader = bridgedLoader;
    }

    /**
     * default: true
     * 
     * @return true, if loaded classes are resolved
     */
    public boolean isResolve() {
        return resolve;
    }

    /**
     * 
     * @param resolve true, if loaded classes should be resolved
     */
    public void setResolve(boolean resolve) {
        this.resolve = resolve;
    }

    /**
     * @return the code base for security
     * 
     * @see java.lang.SecurityManager
     * @see groovy.lang.GroovyCodeSource
     */
    public String getCodeBase() {
        return codeBase;
    }

    /**
     * @param codeBase the code base used by {@link java.lang.SecurityManager}
     */
    public void setCodeBase(String codeBase) {
        this.codeBase = codeBase;
    }

    /**
     * 
     * @return true => recompilation is enabled,<br/> 
     *         false => recompilation is disabled,<br/> 
     *         null => default configuration of {@link groovy.lang.GroovyClassLoader} is used
     */
    public Boolean isRecompile() {
        return recompile;
    }

    /**
     * 
     * @param recompile <br/>
     *         true => recompilation is enabled,<br/> 
     *         false => recompilation is disabled,<br/> 
     *         null => default configuration of {@link groovy.lang.GroovyClassLoader} is used
     */
    public void setRecompile(Boolean recompile) {
        this.recompile = recompile;
    }

    /**
     * 
     * @return container for managing classpath
     * 
     * @see de.indisopht.guice.groovy.internal.ClasspathContainer
     */
    public ClasspathContainer getClassPath() {
        return classPath;
    }

    /**
     * @param classPath the classpath container to be used
     * 
     * @see de.indisopht.guice.groovy.internal.ClasspathContainer
     */
    public void setClassPath(ClasspathContainer classPath) {
        this.classPath = classPath;
    }

    /**
     * @return the bundleContext
     */
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * automatically injected, if peaberry is used for OSGi integration
     * 
     * @param bundleContext the bundleContext to set
     */
    @Inject(optional=true)
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    /**
     * @return the bridgedClassloader
     */
    public BridgedClassLoader getBridgedClassloader() {
        return bridgedClassloader;
    }

    /**
     * @param bridgedClassloader the bridgedClassloader to set
     */
    public void setBridgedClassloader(BridgedClassLoader bridgedClassloader) {
        this.bridgedClassloader = bridgedClassloader;
    }

    /**
     * @return the standard recompilation interval
     */
    public RecompileConfiguration getStdRecompileInterval() {
        return STDRecompileInterval;
    }

    /**
     * @return preconfigured classloader suitable for the configured environment
     */
    public GroovyClassLoader createGroovyClassLoader() {
        if (groovyClassLoader==null) {
            if (bridgedClassloader == null) {
                bridgedClassloader = AccessController.doPrivileged(new PrivilegedAction<BridgedClassLoader>() {
                    public BridgedClassLoader run() {
                        return new BridgedClassLoader(Thread.currentThread().getContextClassLoader(), bundleContext == null ? null : bundleContext.getBundle());
                    }
                });
            }
            groovyClassLoader = AccessController.doPrivileged(new PrivilegedAction<GroovyClassLoader>() {
                public GroovyClassLoader run() {
                    return new GroovyClassLoader(bridgedClassloader);
                }
            });
            groovyClassLoader.setShouldRecompile(isRecompile());
            for (String currentPath : getClassPath()) {
                if (currentPath.contains(":/")) {
                    try {
                        groovyClassLoader.addURL(new URL(currentPath));
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("can't add " + currentPath + " to classpath due to " + e.getMessage());
                    }
                } else {
                    groovyClassLoader.addClasspath(currentPath);
                }
            }
            if (logger.isInfoEnabled()) {
                StringBuilder finalClasspath=new StringBuilder();
                for (URL currentURL : groovyClassLoader.getURLs()) {
                    finalClasspath.append(currentURL.toExternalForm()).append(File.pathSeparator);
                }
                logger.info(GroovyGuice.class.getSimpleName()+" classpath: "+finalClasspath.toString());
            }
        }
        return groovyClassLoader;
    }
}