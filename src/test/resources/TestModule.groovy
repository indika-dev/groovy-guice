import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import de.indisopht.guice.groovy.utest.TestInterface;

public class TestModule extends AbstractModule {
    protected void configure() {
        bind(TestInterface).to(TestClass);                   
    }
}