package de.indisopht.guice.groovy.utest.performance;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class SimpleMethodInterceptor implements MethodInterceptor {

    private final MyClassImpl impl=new MyClassImpl();
    
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        return proxy.invoke(impl, args);
    }

}
