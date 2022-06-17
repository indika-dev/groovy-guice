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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.sf.cglib.proxy.Enhancer;

import org.apache.log4j.Logger;
import org.codehaus.groovy.control.CompilationFailedException;

import com.google.inject.CreationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.spi.Message;

import de.indisopht.guice.groovy.internal.GroovyClassloaderFactory;
import de.indisopht.guice.groovy.internal.RecompileConfiguration;
import de.indisopht.guice.groovy.internal.annotations.GroovyGuiceInternal;
import de.indisopht.guice.groovy.internal.interceptors.RecompilationInterceptor;

/**
 * {@link Provider} implementation for Groovy
 * integration
 * 
 * @author Stefan Maassen
 * @since 0.1.0 
 *
 * @param <T>   class to provide
 */
public abstract class GroovyProvider<T> implements Provider<T> {

    private static final Logger logger=Logger.getLogger(GroovyProvider.class);
    private static final String PROXYCLASSNAME=RecompilationInterceptor.class.toString().substring(6);
    
    @Inject
    private Injector injector;
    
    @Inject(optional=true) @GroovyGuiceInternal
    private GroovyClassLoader groovyLoader;
    
    @Inject(optional=true)
    private GroovyClassloaderFactory classloaderFactory;
    
    private Type bindingType;
    private String somethingGroovy;
    private boolean dynamicRecompilationEnabled=false;
    private boolean proxyCreated=false;
    private GroovyCodeSource sourceFile;
    private RecompileConfiguration recompileConfig = null;
    
    
    /**
     * constructs a new GroovyProvider
     * 
     * @param bindingType   type to provide
     * @param classloaderFactory    factory for {@link GroovyClassLoader}
     * @param somethingGroovy   a Script, a script file name or a fully qualified class name
     */
    public GroovyProvider(Type bindingType, GroovyClassloaderFactory classloaderFactory, String somethingGroovy) {
        super();
        this.bindingType = bindingType;
        this.classloaderFactory = classloaderFactory;
        setSomethingGroovy(somethingGroovy);
    }

    /**
     * constructs a new GroovyProvider
     * 
     * @param somethingGroovy   a Script, a script file name or a fully qualified class name
     */
    public GroovyProvider(String somethingGroovy) {
        setSomethingGroovy(somethingGroovy);
    }
    
    /**
     * constructs a new GroovyProvider
     * 
     * @param somethingGroovy   a Script, a script file name or a fully qualified class name
     * @param dynamicRecompilationEnabled   whether dynamic recompilation should be enabled or not
     */
    public GroovyProvider(String somethingGroovy, boolean dynamicRecompilationEnabled) {
        setSomethingGroovy(somethingGroovy);
        this.dynamicRecompilationEnabled=dynamicRecompilationEnabled;
    }
    
    
    /**
     * constructs a new GroovyProvider with dynamic recompilation enabled 
     * 
     * @param somethingGroovy   a Script, a script file name or a fully qualified class name
     * @param recompileInterval the time after class file should be checked for changes
     * @param tu    the time unit for recompileInterval
     */
    public GroovyProvider(String somethingGroovy, long recompileInterval, TimeUnit tu) {
        setSomethingGroovy(somethingGroovy);
        this.dynamicRecompilationEnabled=true;
        this.recompileConfig=new RecompileConfiguration(recompileInterval, tu);
    }

    /**
     * @see com.google.inject.Provider#get()
     */
    @SuppressWarnings("unchecked")
    @Override
    public T get() {
        if (proxyCreated && !classloaderFactory.isRecompile() && new RuntimeException().getStackTrace()[1].getClassName().equals(PROXYCLASSNAME)) {
            // return null, if classes shouldn't be recompiled for proxy
            return null;
        }
        if (classloaderFactory == null) {
            classloaderFactory = injector.getInstance(GroovyClassloaderFactory.class);
        }
        if (groovyLoader == null) {
            groovyLoader = classloaderFactory.createGroovyClassLoader();
        }
        if (sourceFile == null) {
            URL sourcefileURL;
            try {
                sourcefileURL = groovyLoader.getResourceLoader().loadGroovySource(somethingGroovy);
                if (sourcefileURL!=null) {
                    sourceFile=new GroovyCodeSource(new File(sourcefileURL.getFile()));
                }
            } catch (MalformedURLException e) {
                throw new CreationException(Arrays.asList(new Message(e, e.getMessage())));
            } catch (IOException e) {
                throw new CreationException(Arrays.asList(new Message(e, e.getMessage())));
            }
        }
        T result = injector.getInstance(loadSomethingGroovy());
        if (dynamicRecompilationEnabled && !proxyCreated) {
            logger.debug("creating recompiling proxy for "+somethingGroovy);
            try {
                if (recompileConfig==null) {
                    recompileConfig=classloaderFactory.getStdRecompileInterval();
                }
                if (bindingType==null) {
                	bindingType=((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0];
                }
                String clazz=bindingType.toString();
                clazz=clazz.substring(clazz.indexOf(" ")).trim();
                proxyCreated=true;
                return (T)Enhancer.create(Class.forName(clazz), new RecompilationInterceptor(recompileConfig, result, this));
            } catch (ClassNotFoundException e) {
                throw new CreationException(Arrays.asList(new Message("unknown class: "+e.getMessage())));
            }
        }
        return result;
    }

    /**
     * encapsulates how somethingGroovy is parsed
     * 
     * @return loaded class or a CreationException
     */
    @SuppressWarnings("unchecked")
    private Class<T> loadSomethingGroovy() {
        List<Message> exceptionMessages=new ArrayList<Message>();
        Class<T> classFromGroovy=null;
        try {
            try {
                if (sourceFile!=null) {
                    classFromGroovy = groovyLoader.parseClass(sourceFile, !classloaderFactory.isRecompile());
                }
            } catch (CompilationFailedException cfe) {
                if (logger.isDebugEnabled()) {
                    logger.debug("while compiling " + somethingGroovy + " :" + cfe.getMessage(), cfe);
                }
                exceptionMessages.add(new Message("while compiling " + somethingGroovy + " :" + cfe.getMessage()));
            }
            if (classFromGroovy == null) {
                if (classloaderFactory.getCodeBase()==null) {
                    classFromGroovy = groovyLoader.parseClass(somethingGroovy);
                } else {
                    classFromGroovy = groovyLoader.parseClass(new GroovyCodeSource(somethingGroovy, groovyLoader.generateScriptName(), classloaderFactory.getCodeBase()), true);
                }
            }
        } catch (ClassCastException cce) {
            exceptionMessages.add(new Message("while compiling " + somethingGroovy + " :" + cce.getMessage()));
        }
        if (classFromGroovy == null) {
            exceptionMessages.add(new Message("don't know what to do with: " + somethingGroovy));
            throw new CreationException(exceptionMessages);
        }
        return classFromGroovy;
    }

    public Type getBindingType() {
        return bindingType;
    }

    public GroovyProvider<T> setBindingType(Type classToBindTo) {
        this.bindingType = classToBindTo;
        return this;
    }

    public GroovyClassloaderFactory getGroovyClassloaderFactory() {
        return classloaderFactory;
    }

    public GroovyProvider<T> setGroovyClassloaderFactory(GroovyClassloaderFactory parameters) {
        this.classloaderFactory = parameters;
        return this;
    }

    public String getSomethingGroovy() {
        return somethingGroovy;
    }

    public GroovyProvider<T> setSomethingGroovy(String somethingGroovy) {
        this.somethingGroovy = somethingGroovy;
        return this;
    }

    /** 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getBindingType() + ": " + getSomethingGroovy() + "]";
    }
}