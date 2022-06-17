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

import java.util.concurrent.TimeUnit;

/**
 * Container class for configuration of recompilation
 * source files
 * 
 * @author rapper
 * @since 0.3.0
 */
public final class RecompileConfiguration {
    
    private final long timeInterval;
    private final TimeUnit timeUnit;
    
    /**
     * @param timeInterval  the amount of time
     * @param timeUnit  the unit of time
     */
    public RecompileConfiguration(long timeInterval, TimeUnit timeUnit) {
        super();
        this.timeInterval = timeInterval;
        this.timeUnit = timeUnit;
    }

    public long getIntervalIn(TimeUnit target) {
        return target.convert(timeInterval, timeUnit);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (getIntervalIn(TimeUnit.MILLISECONDS) ^ (getIntervalIn(TimeUnit.MILLISECONDS) >>> 32));
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RecompileConfiguration other = (RecompileConfiguration) obj;
        return getIntervalIn(TimeUnit.MILLISECONDS)==other.getIntervalIn(TimeUnit.MILLISECONDS);
    }
}
