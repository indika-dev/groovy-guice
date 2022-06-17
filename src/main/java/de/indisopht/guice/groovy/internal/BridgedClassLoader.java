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

import org.osgi.framework.Bundle;

/**
 * Classloader, that bridges between Groovy
 * and other classloaders or class loading mechanisms.
 * 
 * @see org.osgi.framework.Bundle
 * 
 * @author Stefan Maassen
 * @since 0.3.0
 */
public class BridgedClassLoader extends ClassLoader {

    protected Bundle bundle;

    public BridgedClassLoader(ClassLoader parent) {
        this(parent, null);
    }
    
    public BridgedClassLoader(ClassLoader parent, Bundle b) {
        super(parent);
        this.bundle = b;
    }

    /**
     * @see java.lang.ClassLoader#findClass(java.lang.String)
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    /**
     * 
     * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
     */
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> result = findLoadedClass(name);
        if (result!=null) {
            if (resolve) {
                resolveClass(result);
            }
            return result;
        }
        if (bundle!=null) {
            result=bundle.loadClass(name);
            if (result!=null) {
                if (resolve) {
                    resolveClass(result);
                }
                return result;
            }
        }
        try {
            result = getParent().loadClass(name);
        } catch (ClassNotFoundException e) {
        }
        if (resolve && result != null) {
            resolveClass(result);
        }
        if (result==null) {
            throw new ClassNotFoundException(name);
        } else {
            return result;
        }
    }
}