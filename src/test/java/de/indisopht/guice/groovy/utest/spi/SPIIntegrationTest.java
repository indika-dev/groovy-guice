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

package de.indisopht.guice.groovy.utest.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import groovy.lang.Script;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLClassLoader;
import java.security.AccessControlException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import de.indisopht.guice.groovy.GroovyGuice;
import de.indisopht.guice.groovy.GroovyProvider;
import de.indisopht.guice.groovy.utest.TestInterface;

public class SPIIntegrationTest {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SPIIntegrationTest.class);

    private static final String dynamicSource = "import de.indisopht.guice.groovy.utest.TestInterface; " + 
                                                "class TestClass implements TestInterface {" + 
                                                    "String getValue() {" + 
                                                        "'fromString'" + 
                                                    "}" + 
                                                "}";

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * TestClass.groovy is in the default package of the script root directory
     * ".../src/test/resources"
     */
    @Test
    public void loadFromFile() {
        final String classpath = new File(URLClassLoader.getSystemResource("TestClass.groovy").getPath()).getParent();

        Module groovyModule = new Module() {
            @Override
            public void configure(Binder binder) {
                binder.bind(TestInterface.class).toProvider(new GroovyProvider<TestInterface>("TestClass"){});
            }
        };

        Module groovyGuiceModule = GroovyGuice
            .createModule()
            .addClasspath(classpath)
            .enableRecompilation()
            .build();

        Injector injector = Guice.createInjector(groovyModule, groovyGuiceModule);
        assertEquals("fromFile", injector.getInstance(TestInterface.class).getValue());
    }

    @Test
    public void loadFromString() {
        Module groovyModule = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(TestInterface.class).toProvider(new GroovyProvider<TestInterface>(dynamicSource){});
            }
        };

        Module groovyGuiceModule= GroovyGuice
            .createModule()
            .build();

        assertEquals("fromString", Guice.createInjector(groovyModule, groovyGuiceModule).getInstance(TestInterface.class).getValue());
    }

    @Test
    public void loadScriptFromStringWithoutSecurity() {
        Module groovyGuiceModule= GroovyGuice
            .createModule()
            .bindScript("println('groovy hello'); 'groovy'")
            .build();
        assertEquals("groovy", Guice.createInjector(groovyGuiceModule).getInstance(Script.class).run());
    }
    
    @Test
    public void loadScriptFromStringWithExplicitBinding() {
        Module groovyModule = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(Script.class).toProvider(new GroovyProvider<Script>("println('groovy hello'); 'groovy'"){});
            }
        };

        Module groovyGuiceModule= GroovyGuice
            .createModule()
            .build();

        assertEquals("groovy", Guice.createInjector(groovyModule, groovyGuiceModule).getInstance(Script.class).run());
    }

    @Test
    public void loadScriptFromFile() {
        Module groovyGuiceModule= GroovyGuice
            .createModule()
            .addClasspath(URLClassLoader.getSystemResource("TestScript.groovy").getPath())
            .bindScript("TestScript")
            .build();
        assertEquals("fromFile", Guice.createInjector(groovyGuiceModule).getInstance(Script.class).run());
    }
    
    @Test
    public void loadScriptFromFileWithExplicitBinding() {
        Module groovyModule = new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(Script.class).toProvider(new GroovyProvider<Script>("TestScript"){});
            }
        };

        Module groovyGuiceModule= GroovyGuice
            .createModule()
            .addClasspath(URLClassLoader.getSystemResource("TestScript.groovy").getPath())
            .build();

        assertEquals("fromFile", Guice.createInjector(groovyModule, groovyGuiceModule).getInstance(Script.class).run());
    }

    @Test
    public void loadFromFileWithExternalDependency() {
        Module groovyModule = new Module() {
            @Override
            public void configure(Binder binder) {
                binder.bind(TestInterface.class).toProvider(new GroovyProvider<TestInterface>("TestClassWithGroovyDependency"){});
            }
        };
        Module groovyGuiceModule= GroovyGuice
            .createModule()
            .addClasspath(new File(URLClassLoader.getSystemResource("TestClassWithGroovyDependency.groovy").getPath()).getParent().toString())
            .enableRecompilation()
            .build();
        assertEquals("called successfully", Guice.createInjector(groovyModule, groovyGuiceModule).getInstance(TestInterface.class).getValue());
    }
    
    @Test
    public void testManualClassRecompilation() throws IOException {
        File f=null;
        FileWriter writer=null;
        Module groovyModule = new Module() {
            @Override
            public void configure(Binder binder) {
                binder.bind(TestInterface.class).toProvider(new GroovyProvider<TestInterface>("TestClass"){});
            }
        };
        Module groovyGuiceModule= GroovyGuice
            .createModule()
            .addClasspath(new File(URLClassLoader.getSystemResource("TestClass.groovy").getPath()).getParent().toString())
            .enableRecompilation()
            .build();
        Injector injector=Guice.createInjector(groovyModule, groovyGuiceModule);
        assertEquals("fromFile", injector.getInstance(TestInterface.class).getValue());
        try {
            f=new File(URLClassLoader.getSystemResource("TestClass.groovy").getPath());
            f.delete();
            f.createNewFile();
            writer=new FileWriter(f);
            writer.write("import de.indisopht.guice.groovy.utest.TestInterface\n"+
                        " class TestClass implements TestInterface {\n"+
                            "String getValue() {\n"+
                               "'again fromFile'\n"+
                            "}\n"+
                        "}");
            writer.close();
            assertEquals("again fromFile", injector.getInstance(TestInterface.class).getValue());
        } finally {
            if (f!=null) {
                f.delete();
                f.createNewFile();
                writer=new FileWriter(f);
                writer.write("import de.indisopht.guice.groovy.utest.TestInterface\n"+
                            " class TestClass implements TestInterface {\n"+
                                "String getValue() {\n"+
                                   "'fromFile'\n"+
                                "}\n"+
                            "}");
                writer.close();
            }
        }
    }
    
    @Test
    public void testDynamicClassRecompilation() throws IOException {
        File f=null;
        FileWriter writer=null;
        Module groovyModule = new AbstractModule() {
            @Override
            public void configure() {
                bind(TestInterface.class).toProvider(new GroovyProvider<TestInterface>("TestClass", true){});
            }
        };
        Module groovyGuiceModule = GroovyGuice
            .createModule()
            .addClasspath(new File(URLClassLoader.getSystemResource("TestClass.groovy").getPath()).getParent().toString())
            .enableRecompilation()
            .build();
        TestInterface ti=Guice.createInjector(groovyModule, groovyGuiceModule).getInstance(TestInterface.class);
        assertEquals("fromFile", ti.getValue());
        try {
            f=new File(URLClassLoader.getSystemResource("TestClass.groovy").getPath());
            f.delete();
            f.createNewFile();
            writer=new FileWriter(f);
            writer.write("import de.indisopht.guice.groovy.utest.TestInterface\n"+
                        " class TestClass implements TestInterface {\n"+
                            "String getValue() {\n"+
                               "'again fromFile'\n"+
                            "}\n"+
                        "}");
            writer.close();
            try {
                TimeUnit.MILLISECONDS.sleep(500l);
            } catch (InterruptedException e) {
            }
            assertEquals("fromFile", ti.getValue());
            try {
                TimeUnit.SECONDS.sleep(5l);
            } catch (InterruptedException e) {
            }
            assertEquals("again fromFile", ti.getValue());
        } finally {
            if (f!=null) {
                f.delete();
                f.createNewFile();
                writer=new FileWriter(f);
                writer.write("import de.indisopht.guice.groovy.utest.TestInterface\n"+
                            " class TestClass implements TestInterface {\n"+
                                "String getValue() {\n"+
                                   "'fromFile'\n"+
                                "}\n"+
                            "}");
                writer.close();
            }
        }
    }
    
    @Test
    public void testDynamicClassRecompilationDisabled() throws IOException {
        File f=null;
        FileWriter writer=null;
        Module groovyModule = new Module() {
            @Override
            public void configure(Binder binder) {
                binder.bind(TestInterface.class).toProvider(new GroovyProvider<TestInterface>("TestClass"){});
            }
        };
        Module groovyGuiceModule= GroovyGuice
            .createModule()
            .addClasspath(new File(URLClassLoader.getSystemResource("TestClass.groovy").getPath()).getParent().toString())
            .disableRecompilation()
            .build();
        Injector injector=Guice.createInjector(groovyModule, groovyGuiceModule);
        assertEquals("fromFile", injector.getInstance(TestInterface.class).getValue());
        try {
            f=new File(URLClassLoader.getSystemResource("TestClass.groovy").getPath());
            f.delete();
            f.createNewFile();
            writer=new FileWriter(f);
            writer.write("import de.indisopht.guice.groovy.utest.TestInterface\n"+
                        " class TestClass implements TestInterface {\n"+
                            "String getValue() {\n"+
                               "'again fromFile'\n"+
                            "}\n"+
                        "}");
            writer.close();
            assertEquals("fromFile", injector.getInstance(TestInterface.class).getValue());
        } finally {
            if (f!=null) {
                f.delete();
                f.createNewFile();
                writer=new FileWriter(f);
                writer.write("import de.indisopht.guice.groovy.utest.TestInterface\n"+
                            " class TestClass implements TestInterface {\n"+
                                "String getValue() {\n"+
                                   "'fromFile'\n"+
                                "}\n"+
                            "}");
                writer.close();
            }
        }
    }
    
    @Test
    public void testDynamicClassRecompilationWithoutChanges() throws IOException {
        Module groovyModule = new Module() {
            @Override
            public void configure(Binder binder) {
                binder.bind(TestInterface.class).toProvider(new GroovyProvider<TestInterface>("TestClass", true){});
            }
        };
        Module groovyGuiceModule= GroovyGuice.createModule()
            .addClasspath(new File(URLClassLoader.getSystemResource("TestClass.groovy").getPath()).getParent().toString())
            .disableRecompilation()
            .build();
        TestInterface ti = Guice.createInjector(groovyModule, groovyGuiceModule).getInstance(TestInterface.class);
        assertEquals("fromFile", ti.getValue());
        try {
            TimeUnit.MILLISECONDS.sleep(500l);
        } catch (InterruptedException e) {
        }
        assertEquals("fromFile", ti.getValue());
        try {
            TimeUnit.SECONDS.sleep(5l);
        } catch (InterruptedException e) {
        }
        assertEquals("fromFile", ti.getValue());
    }
    
    @Test
    public void testBasicLoadingModuleFromScript() {
        Module groovyGuiceModule= GroovyGuice.createWithGroovyModules("TestBasicSourceModule")
            .addClasspath(new File(URLClassLoader.getSystemResource("TestBasicSourceModule.groovy").getPath()).getParent().toString())
            .enableRecompilation()
            .build();
        assertEquals("Yes, it works", Guice.createInjector(groovyGuiceModule).getInstance(Key.<String>get(String.class, Names.named("testScript"))));
    }
    
    @Test
    public void testLoadingModuleFromScriptAndClasses() {
        Module groovyGuiceModule= GroovyGuice.createWithGroovyModules("TestModule")
            .addClasspath(new File(URLClassLoader.getSystemResource("TestModule.groovy").getPath()).getParent().toString())
            .enableRecompilation()
            .build();
        Injector injector=Guice.createInjector(groovyGuiceModule);
        assertNotNull(injector.getInstance(TestInterface.class));
        assertEquals("fromFile", injector.getInstance(TestInterface.class).getValue());
    }
    
    @Test
    public void testReloadingModule() throws IOException {
        Module groovyGuiceModule= GroovyGuice.createWithGroovyModules("TestModule")
            .addClasspath(new File(URLClassLoader.getSystemResource("TestModule.groovy").getPath()).getParent().toString())
            .enableRecompilation()
            .build();
        Injector injector=Guice.createInjector(groovyGuiceModule);
        assertNotNull(injector.getInstance(TestInterface.class));
        assertEquals("fromFile", injector.getInstance(TestInterface.class).getValue());
        File f=null;
        FileWriter writer;
        try {
            f=new File(URLClassLoader.getSystemResource("TestModule.groovy").getPath());
            f.delete();
            f.createNewFile();
            writer=new FileWriter(f);
            writer.write("");
            writer.close();
            try {
                TimeUnit.SECONDS.sleep(5l);
                TimeUnit.MILLISECONDS.sleep(100l);
            } catch (InterruptedException e) {
            }
            assertNotNull(injector.getInstance(TestInterface.class));
            assertEquals("fromFile", injector.getInstance(TestInterface.class).getValue());
        } finally {
            if (f!=null) {
                f.delete();
                f.createNewFile();
                writer=new FileWriter(f);
                writer.write("import com.google.inject.AbstractModule;\n"+
                             "import com.google.inject.name.Names;\n"+
                             "import de.indisopht.guice.groovy.utest.TestInterface;\n"+
                             "\n"+
                             "public class TestModule extends AbstractModule {\n"+
                             "  protected void configure() {\n"+
                             "      bind(TestInterface).to(TestClass);\n"+                   
                             "  }\n"+
                             "}");
                writer.close();
            }
        }
    }
    
    @Test(expected = AccessControlException.class)
    public void loadScriptFromStringWithSecurity() {
        System.setProperty("java.security.policy", URLClassLoader.getSystemResource("security" + File.separator + "groovy_guiceV2.policy").toExternalForm());
        System.setSecurityManager(new SecurityManager());
        Module groovyGuiceModule= GroovyGuice.createModule()
            .useCodeBase("/serverCodeBase/restrictedClient")
            .bindScript("System.setProperty('file.encoding', 'UTF-8')")
            .build();
        Guice.createInjector(groovyGuiceModule).getInstance(Script.class).run();
    }
    
    @SuppressWarnings("unchecked")
    @Test(expected = AccessControlException.class)
    public void loadScriptFromStringWithSecurity_pureGroovy() throws InstantiationException, IllegalAccessException {
        System.setProperty("java.security.policy", URLClassLoader.getSystemResource("security" + File.separator + "groovy_guiceV2.policy").toExternalForm());
        System.setSecurityManager(new SecurityManager());
        GroovyClassLoader groovyLoader=new GroovyClassLoader(Thread.currentThread().getContextClassLoader());
        Class<Script> script=(Class<Script>)groovyLoader.parseClass(new GroovyCodeSource("System.setProperty('file.encoding', 'UTF-8')", "SecurityTestScript", "/serverCodeBase/restrictedClient"), true);
        script.newInstance().run();
        
    }
}
