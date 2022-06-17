package de.indisopht.guice.groovy.utest.performance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class SimpleMethodHandler implements InvocationHandler {

    private final MyClassImpl impl=new MyClassImpl();
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(impl, args);
    }

}
