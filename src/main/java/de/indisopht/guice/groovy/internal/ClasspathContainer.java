/**
 * Copyright (C) 2008 Stefan Maassen
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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.inject.Inject;

/**
 * Container for classpath components. Currently
 * supported component types are directories and URLs.
 * 
 * @author Stefan Maassen
 */
public class ClasspathContainer implements Iterable<String>{
    
    private final Set<String> classpath=new HashSet<String>();
    
    @Inject(optional=true)
    public void setClasspath(String... classpath) {
        this.classpath.addAll(Arrays.asList(classpath));
    }
    
    @Inject(optional=true)
    public void addClasspath(String classpath) {
        this.classpath.addAll(Arrays.asList(classpath.split(File.pathSeparator)));
    }
    
    @Inject(optional=true)
    public void addClasspath(Collection<String> classpath) {
        this.classpath.addAll(classpath);
    }
    
    public Set<String> getClasspath() {
        return classpath;
    }

    @Override
    public Iterator<String> iterator() {
        return getClasspath().iterator();
    }

    @Override
    public String toString() {
        StringBuffer result=new StringBuffer();
        for (String currentPath : classpath) {
            result.append(currentPath).append(File.pathSeparator);
        }
        return result.substring(0, result.length()-1);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((classpath == null) ? 0 : classpath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ClasspathContainer other = (ClasspathContainer) obj;
        if (classpath == null) {
            if (other.classpath != null)
                return false;
        } else if (!classpath.equals(other.classpath))
            return false;
        return true;
    }
    
    
}
