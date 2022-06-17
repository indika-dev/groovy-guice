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

package de.indisopht.guice.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.Script;

import java.io.Reader;
import java.lang.annotation.Annotation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.ConfigurationException;
import org.osgi.framework.Bundle;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;

import de.indisopht.guice.groovy.internal.BridgedClassLoader;
import de.indisopht.guice.groovy.internal.GroovyClassloaderFactory;
import de.indisopht.guice.groovy.internal.annotations.GroovyGuiceInternal;

/**
 * Fluent API for configuring the environment
 * of {@link Module}s with bindings to Groovy source
 * files 
 * 
 * <br>Basic Usage:<br>
 * <code><pre>
 * Module groovyGuiceModule = GroovyGuice
 *   .create()
 *   .addClasspath(classpath)
 *   .build();</pre>
 * Guice.createInjector(myModule, groovyGuiceModule);<br>
 * </code>
 * 
 * <br>Example bindings:<br>
 * 
 * <pre><b>Binding a groovy class to a java interface</b><code>
 *  bind(TestInterface.class).toProvider(new GroovyProvider&lt;TestInterface&gt;("TestClass"){});
 * </code>
 * <b>Binding grovvy code inside a string to a java class</b><code>
 *  bind(MyClass.class).to(new GroovyProvider&lt;MyClass&gt;(&quot;import a.b.c; class B extends MyClass { }&quot;){});
 * </code>
 * <b>Binding a grovvy script inside a string</b><code>
 *  bind(Script.class).toProvider(new GroovyProvider&lt;Script&gt;(&quot;println('groovy hello'); 'groovy'&quot;){});
 * </code>
 * <b>Binding a grovvy script from a file</b><code>
 *  bind(Script.class).toProvider(new GroovyProvider&lt;Script&gt;(&quot;TestScript&quot;){});
 * </code>
 * 
 * @author Stefan Maassen
 * @since 0.3.0
 *
 */
public class GroovyGuice {

    private static final Logger logger=Logger.getLogger(GroovyGuice.class);
    
    /**
     * start configuration based on the
     * given modules
     * 
     * @param modules the modues to configure
     * @return {@link GroovyModuleBuilderInstance}
     * 
     * @deprecated  method is obsolete and will be removed in future release
     */
    @Deprecated
    public static GroovyModuleBuilderInstance create(Module... modules) {
        return new GroovyModuleBuilderInstance();
    }
    
    /**
     * the given {@link Module}s (as Groovy source files) will be installed additionally
     *  while creating the {@link Injector}. All given {@link Module}s must have a standard 
     *  constructor without any parameters.
     *  </br></br>Please note, that the given Modules won't be recompiled due to unknown 
     *  side-effects on internal Guice behaviour.
     * 
     * @param sourceModules the {@link Module}s (as Groovy source files) to install
     * @return {@link GroovyModuleBuilderInstance}
     */
    public static GroovyModuleBuilderInstance createWithGroovyModules(String... sourceModules)   {
        GroovyModuleBuilderInstance result = new GroovyModuleBuilderInstance();
        Collections.addAll(result.groovySourceModules, sourceModules);
        return result;
    }
    
    /**
     * start configuration with no module
     * 
     * @return {@link GroovyModuleBuilderInstance}
     * 
     * @deprecated use {@link GroovyGuice#createModule()} instead
     */
    public static GroovyModuleBuilderInstance createWithEmptyModule() {
        return  new GroovyModuleBuilderInstance();
    }
    
    /**
     * start configuration with no module
     * 
     * @return {@link GroovyModuleBuilderInstance}
     */
    public static GroovyModuleBuilderInstance createModule() {
        return new GroovyModuleBuilderInstance();
    }

    /**
     * Building Block of Fluent API
     * 
     * @author Stefan Maassen
     * @since 0.3.0
     */
    public static class GroovyModuleBuilderInstance {
        
        /**
         * factory class for {@link GroovyClassLoader}
         */
        private GroovyClassloaderFactory classloaderFactory = new GroovyClassloaderFactory();
        
        /**
         * {@link Script}s to bind
         */
        Map<Key<Script>, String> scripts = new HashMap<Key<Script>, String>();
        
        /**
         * used for OSGi configuration
         */
        private Bundle bundle;
        
        /**
         * a list of all {@link Module} files to be installed additionally and given as Groovy
         * source files
         */
        private List<String> groovySourceModules = new ArrayList<String>();
        
        /**
         * add path to current Classpath for
         * Groovy
         * 
         * @param path classpath
         * @return {@link GroovyModuleBuilderInstance}
         */
        public GroovyModuleBuilderInstance addClasspath(String path) {
            classloaderFactory.getClassPath().addClasspath(path);
            return this;
        }

        /**
         * please note:
         * if peaberry is used for integrating OSGi, configuring the bundle is optional
         * 
         * @param b the bundle
         * @return {@link GroovyModuleBuilderInstance}
         */
        public GroovyModuleBuilderInstance forBundle(Bundle b) {
            this.bundle=b;
            return this;
        }
        
        /**
         * whether loaded classes should be resolved or
         * not
         * default: {@link GroovyClassloaderFactory#isResolve()}
         * 
         * @param b boolean value
         * @return {@link GroovyModuleBuilderInstance}
         */
        public GroovyModuleBuilderInstance resolveClasses(Boolean b) {
            classloaderFactory.setResolve(b);
            return this;
        }

        /**
         * alternative method for {@link #resolveClasses(Boolean)}
         * 
         * @param b string representing a boolean value
         * @return {@link GroovyModuleBuilderInstance}
         */
        public GroovyModuleBuilderInstance resolveClasses(String b) {
            classloaderFactory.setResolve(Boolean.parseBoolean(b));
            return this;
        }

        /**
         *  security configuration for the configured {@link Script}s
         * 
         *  @see SecurityManager
         *  @see GroovyCodeSource#GroovyCodeSource(Reader, String, String)
         *  @see <a href="http://docs.codehaus.org/display/GROOVY/Security">Groovy Security</a>
         * 
         * @param base the code base to use
         * @return {@link GroovyModuleBuilderInstance}
         */
        public GroovyModuleBuilderInstance useCodeBase(String base) {
            classloaderFactory.setCodeBase(base);
            return this;
        }
        
        /**
         * enables recompilation of Groovy source files
         * 
         * @return {@link GroovyModuleBuilderInstance}
         */
        public GroovyModuleBuilderInstance enableRecompilation() {
            classloaderFactory.setRecompile(true);
            return this;
        }
        
        /**
         * disables recompilation of Groovy source files
         * 
         * @return {@link GroovyModuleBuilderInstance}
         */
        public GroovyModuleBuilderInstance disableRecompilation() {
            classloaderFactory.setRecompile(false);
            return this;
        }

        /**
         * starts a subpart of this Fluent API
         * for configuring scripts
         * 
         * @param script the script
         * @return {@link ScriptBindConfig}
         */
        public ScriptBindConfig script(String script) {
            return new ScriptBindConfig(this, script);
        }
        
        /**
         * convinience method for configuring scripts
         * 
         * @param script the script
         * @return {@link ScriptBindConfig}
         */
        public GroovyModuleBuilderInstance bindScript(String script) {
            return new ScriptBindConfig(this, script).bind();
        }
        
        /**
         * convinience method for configuring scripts with
         * an annotation
         * 
         * @param script the script
         * @return {@link ScriptBindConfig}
         */
        public GroovyModuleBuilderInstance bindScript(String script, Annotation annotation) {
            return new ScriptBindConfig(this, script).bindWithAnnotation(annotation);
        }
        
        /**
         * finally builds some modules ready
         * for injection.
         * Please note, that given {@link Module}s
         * at {@link GroovyGuice#create(Module...)} must be given 
         * to {@link Guice#createInjector(Module...)} as parameter
         * 
         * @return {@link Collection} of {@link Module}s
         */
        public Module build() {
            return new AbstractModule() {

                @Override
                protected void configure() {
                    classloaderFactory.setBridgedClassloader(AccessController.doPrivileged(new PrivilegedAction<BridgedClassLoader>() {
                        public BridgedClassLoader run() {
                            return new BridgedClassLoader(Thread.currentThread().getContextClassLoader(), bundle);
                        }
                    }));
                    bind(GroovyClassLoader.class).annotatedWith(GroovyGuiceInternal.class).toInstance(classloaderFactory.createGroovyClassLoader());
                    bind(GroovyClassloaderFactory.class).toInstance(classloaderFactory);
                    for (final Entry<Key<Script>, String> currentEntry : scripts.entrySet()) {
                        AccessController.doPrivileged(new PrivilegedAction<Void>() {
                            @Override
                            public Void run() {
                                bind(currentEntry.getKey()).toProvider(new GroovyProvider<Script>(currentEntry.getValue()){});
                                return null;
                            }
                            
                        });
                    }
                    GroovyClassLoader gcl=classloaderFactory.createGroovyClassLoader();
                    for (String currentSourceModule : groovySourceModules) {
                        try {
                            logger.info("installing Groovy module ["+currentSourceModule+"] ");
                            install((Module)gcl.loadClass(currentSourceModule).newInstance());
                            logger.debug("installed successfully Groovy module ["+currentSourceModule+"] ");
                        } catch (InstantiationException e) {
                            throw new ConfigurationException(e);
                        } catch (IllegalAccessException e) {
                            throw new ConfigurationException(e);
                        } catch (CompilationFailedException e) {
                            throw new ConfigurationException(e);
                        } catch (ClassNotFoundException e) {
                            throw new ConfigurationException(e);
                        }
                    }
                }
            };
        }
    }
    
    /**
     * Building Block of Fluent API
     * for binding {@link Script}s on the fly.
     * 
     * @author Stefan Maassn
     *@since 0.3.0
     *
     */
    public static class ScriptBindConfig  {
        
        private final GroovyModuleBuilderInstance parent;
        private final String script;
        private Annotation keyAnnotation=null;
        
        public ScriptBindConfig(GroovyModuleBuilderInstance parent, String script) {
            this.parent=parent;
            this.script=script;
        }
        
        /**
         * configure an {@link Annotation} for this script
         * 
         * @param annotation
         * @return {@link ScriptBindConfig}
         * 
         * @deprecated will be removed in future release; use {@link ScriptBindConfig#bindWithAnnotation(Annotation)} instead
         */
        public ScriptBindConfig withAnnotation(Annotation annotation) {
            this.keyAnnotation=annotation;
            return this;
        }
        
        /**
         * final step for configuring a {@link Script} on
         * the fly
         * 
         * @return {@link GroovyModuleBuilderInstance}
         */
        public GroovyModuleBuilderInstance bind() {
            if (keyAnnotation!=null) {
                parent.scripts.put(Key.get(Script.class, keyAnnotation), script);
            } else {
                parent.scripts.put(Key.get(Script.class), script);
            }
            return parent;
        }
        
        /**
         * final step for configuring a {@link Script} on
         * the fly with the given annotation
         * 
         * @return {@link GroovyModuleBuilderInstance}
         */
        public GroovyModuleBuilderInstance bindWithAnnotation(Annotation annotation) {
            if (annotation!=null) {
                parent.scripts.put(Key.get(Script.class, annotation), script);
            } else {
                parent.scripts.put(Key.get(Script.class), script);
            }
            return parent;
        }
    }
}
