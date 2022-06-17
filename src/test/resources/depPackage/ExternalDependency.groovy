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
package depPackage

public class ExternalDependency{

     private final Date date=new Date(System.currentTimeMillis());
     
    public boolean callMe() {
        println "${this} was called successfully"
        return date;
    }
    
    @Override
    String toString() {
        return "${this.class.simpleName}@$date"
    }
    
}
