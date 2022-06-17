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

package de.indisopht.guice.groovy.internal.annotations;

import java.lang.annotation.Annotation;

/**
 * Factory class for creating instances of {@link de.indisopht.guice.groovy.internal.annotations.GroovyGuiceInternal}
 * 
 * @author Stefan Maassen
 * @since 0.3.0
 */
public class GroovyGuiceInternalFactory {

    public static GroovyGuiceInternal create() {
        return new GroovyGuiceInternal() {

            /**
             * @see java.lang.annotation.Annotation#annotationType()
             */
            @Override
            public Class<? extends Annotation> annotationType() {
                return GroovyGuiceInternal.class;
            }

            /**
             * @see java.lang.Object#toString()
             */
            @Override
            public String toString() {
                return "@" + GroovyGuiceInternal.class.getName();
            }

            /**
             * @see java.lang.Object#equals(java.lang.Object)
             */
            @Override
            public boolean equals(Object o) {
                return o instanceof GroovyGuiceInternal;
            }

            /**
             * @see java.lang.Object#hashCode()
             */
            @Override
            public int hashCode() {
                return (127 * "value".hashCode()) ^ 17;
            }
        };
    }
    
}
