# groovy-guice
The groovy-guice project enables you to use Groovy script files and Groovy source files as binding targets in Guice. Ease of use and minimal amount of code for configuration were the design goals.

#summary howto use groovy-guice in your projects
#labels Featured

= Introduction =

This is a little guide on howto use groovy-guice in your project


= howto use at binding time =

*Binding a groovy class to a java class:*
{{{
bind(MyClass.class).toProvider(new GroovyProvider<MyClass>("com.mypackage.MyGroovyClass"){});
}}}

*Binding grovvy code inside a string to a java class*
{{{
bind(MyClass.class).toProvider(new GroovyProvider<MyClass>("import a.b.c; class B extends MyClass { }"){});
}}}

*Binding a grovvy script inside a string*
{{{
bind(Script.class).toProvider(new GroovyProvider<Script>("println('groovy hello'); 'groovy'"){});
}}}

*Binding a grovvy script from a file*
{{{
bind(Script.class).toProvider(new GroovyProvider<Script>("/path/to/TestScript.groovy"){});
}}}

*Binding a java interface to a groovy class with dynamic recompilation @ 5sec interval*
{{{
binder.bind(TestInterface.class).toProvider(new GroovyProvider<TestInterface>("TestClass", true){});
}}}

*Binding a java interface to a groovy class with dynamic recompilation using a custom interval*
{{{
binder.bind(TestInterface.class).toProvider(new GroovyProvider<TestInterface>("TestClass", 5, TimeUnit.MINUTES){});
}}}

Manual Recompilation will work automatically(if enabled at module installation time) each time, a new instance is returned by its Provider. E.g. explicitly calling `Provider.get()` or `Provider.get()` is called by Guice for provisioning another class.

*Please note: whether a groovy source file or not was changed, `Provider.get()` will always return a new instance!*
 
Perhaps, this will change in future releases. 

= howto use at module installation time =

*minimal configuration*
{{{
Module rewrittenModule = GroovyGuice
        .createModule()
        .build();
}}}

*minimal configuration, if only, at least one, script files will be used*
{{{
Module rewrittenModule = GroovyGuice
            .createModule()
            .bindScript("println('groovy hello'); 'groovy'")
            .build();
}}}

*minimal configuration, if deployed in an OSGi environment without peaberry*
{{{
Module groovyModule = GroovyGuice.createModule()
                .forBundle(context.getBundle())
                .addClasspath(System.getProperty("GroovyOsgiSrcPath") == null ? System.getenv("GroovyOsgiSrcPath") : System.getProperty("GroovyOsgiSrcPath"))
                .build();
}}}
If you use peaberry, calling `.forBundle(context.getBundle())` will be optional!

*minimal configuration with loading Modules written in Groovy*
{{{
Module groovyModule = GroovyGuice.createWithGroovyModules("org.somewhat.GroovyModule")
                .addClasspath(System.getProperty("GroovyOsgiSrcPath") == null ? System.getenv("GroovyOsgiSrcPath") : System.getProperty("GroovyOsgiSrcPath"))
                .build();
}}}

The given Modules must be on the given classpath.

=last, but not least=

  * You can find more examples at http://code.google.com/p/groovy-guice/source/browse/tags/groovy-guice-0.4.0/src/test/java/de/indisopht/guice/groovy/utest/spi/SPIIntegrationTest.java
  * The source code for a basic groovy-guice usage in an OSGi environment together with peaberry can be found at http://code.google.com/p/groovy-guice/source/browse/#svn/var/groovyOsgi
  * A complete OSGi installation together with a little example can be downloaded from the Downloads section
  * When you're running groovy-guice on OSGi platform, please don't use groovy 1.6.1 , 1.6.2 or 1.6.3, because of a bug in these versions.

#summary how to set up MANIFEST.MF for OSGi

= Introduction =

If you want to use groovy-guice in an OSGi environment, the
`META-INF/MANIFEST.MF` has to be setup correctly.


= MANIFEST.MF =

The following shows the `MANIFEST.MF` of the groovyGuice example:

{{{
Manifest-Version: 1.0
Export-Package: de.indisopht.serviceProvider.osgi;uses:="org.apache.lo
 g4j,de.indisopht.guice.groovy.spi,org.osgi.framework,org.ops4j.peaber
 ry,com.google.inject.name,com.google.inject.binder,de.indisopht.servi
 ceProvider,com.google.inject",de.indisopht.serviceProvider,de.indisop
 ht.groovy
Built-By: rapper
Tool: Bnd-0.0.255
Bundle-Name: ServiceProvider
Created-By: Apache Maven Bundle Plugin
DynamicImport-Package: de.indisopht.groovy.*,groovy.*,org.codehaus.*
Build-Jdk: 1.6.0_11
Bundle-Version: 1.0.0
Bnd-LastModified: 1229810649715
Bundle-Activator: de.indisopht.serviceProvider.osgi.Activator
Bundle-ManifestVersion: 2
Bundle-Description: provides groovy services
Bundle-SymbolicName: de.indisopht.serviceProvider
Import-Package: com.google.inject,com.google.inject.binder,com.google.
 inject.name,de.indisopht.guice.groovy.spi,de.indisopht.serviceProvide
 r,de.indisopht.serviceProvider.osgi,org.apache.log4j,org.ops4j.peaber
 ry,org.osgi.framework
Originally-Created-By: Apache Maven Bundle Plugin
}}}

The static part is as expected and all OSGi-dependencies for dynamically
loaded groovy source files must be configured in `DynamicImport-Package`.
If you add it to `Import-Package`, it won't work until the dependencies are
needed at bundle installation time.

#summary Example policy file
#labels Phase-Support

= Introduction =

This shows an example policy file, which is used for testing 
in this project. You can use this as a base for your
own policy files. For more informations, please have a look at http://docs.codehaus.org/display/GROOVY/Security


 Please note, that the policy provided by GroovyCodeBase
 only applies to Groovy scripts.
 = =
 Security rules are always applied to already compiled groovy files by Java(tm) runtime.


= Details =
Save the following in a file (e.g. called "groovy.policy"):
{{{
/* this codeBase will be applied to groovy scripts */
grant codeBase "file:/serverCodeBase/restrictedClient" {
  permission java.util.PropertyPermission "file.encoding", "read";
};

/* these permissions are applied to all class files and are needed */
/* for running TestCase in eclipse and maven2 */
/* these permissions are applied to all class files and are needed */
/* for running TestCase in eclipse and maven2 */
grant {
  permission java.lang.RuntimePermission "accessDeclaredMembers";
  permission java.io.FilePermission "<<ALL FILES>>", "read, write";
  permission java.util.logging.LoggingPermission "control";
  permission java.lang.RuntimePermission "setSecurityManager";
  permission java.lang.reflect.ReflectPermission "suppressAccessChecks";
  permission java.util.PropertyPermission "cglib.debugLocation", "read";
  permission java.lang.RuntimePermission "getProtectionDomain";
  permission java.util.PropertyPermission "guice.allow.nulls.bad.bad.bad", "read";
  permission java.lang.RuntimePermission "createClassLoader";
  permission groovy.security.GroovyCodeSourcePermission "/groovy/script";
  permission java.util.PropertyPermission "ANTLR_DO_NOT_EXIT", "read";
  permission java.util.PropertyPermission "ANTLR_USE_DIRECT_CLASS_LOADING", "read";
  permission java.util.PropertyPermission "groovyjarjarantlr.ast", "read";
  permission java.util.PropertyPermission "groovy.ast", "read";
  permission java.lang.RuntimePermission "setContextClassLoader";
};
}}}
Announce the codebase to groovy-guice:
{{{
GroovyGuice.....useCodeBase("/serverCodeBase/restrictedClient").....build();
}}}
Finally, you can enable SecurityManager at runtime:
{{{
System.setProperty("java.security.policy", "/path/to/groovy.policy");
System.setSecurityManager(new SecurityManager());
}}}
Or you use commandline parameters instead:
 * -Djava.security.Manager
 * -Djava.security.manager -Djava.security.policy=/path/to/groovy.policy
 * -Djava.security.manager -Djava.security.policy==/path/to/groovy.policy

#summary description of maven2Repository
#labels Featured

= Maven2 Repository =

All releases since version 0.3.0 can also be downloaded by
using the maven2 repository of groovy-guice.


= pom.xml =

add to your pom.xml the following lines:
{{{
<repositories>
.
.
.
  <repository>
    <id>groovy-guice-maven2-repository</id>
    <name>Groovy-Guice Maven2 Repository</name>
    <url>http://groovy-guice.googlecode.com/svn/maven2/repo/</url>
    <snapshots><enabled>false</enabled></snapshots>
    <releases><enabled>true</enabled></releases>
  </repository>
.
.
.
</repositories>
<dependencies>
.
.
.
  <dependency>
    <groupId>de.indisopht</groupId>
    <artifactId>groovy-guice</artifactId>
    <version>0.3.0</version>
  </dependency>
.
.
.
</dependencies>
}}}

#summary Frequently Asked Questions

= FAQ =

Add your content here.


= Frequently Asked Questions =

nothing so far
