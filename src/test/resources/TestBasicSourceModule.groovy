import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class TestBasicSourceModule extends AbstractModule {
    protected void configure() {
        bindConstant().annotatedWith(Names.named('testScript')).to('Yes, it works');                    
    }
}
