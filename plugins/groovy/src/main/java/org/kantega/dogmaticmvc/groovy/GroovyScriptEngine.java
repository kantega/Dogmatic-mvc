package org.kantega.dogmaticmvc.groovy;

import groovy.lang.*;
import groovy.util.ResourceConnector;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.tools.gse.DependencyTracker;
import org.codehaus.groovy.tools.gse.StringSetMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Specific script engine able to reload modified scripts as well as dealing properly
 * with dependent scripts.
 *
 * @author sam
 * @author Marc Palmer
 * @author Guillaume Laforge
 * @author Jochen Theodorou
 */
public class GroovyScriptEngine implements ResourceConnector {

    private static final ClassLoader CL_STUB = new ClassLoader(){};

    private static WeakReference<ThreadLocal<StringSetMap>> dependencyCache = new WeakReference<ThreadLocal<StringSetMap>>(null);
    private synchronized static ThreadLocal<StringSetMap> getDepCache() {
        ThreadLocal<StringSetMap> local = dependencyCache.get();
        if (local!=null) return local;
        local = new ThreadLocal<StringSetMap>() {
            @Override
            protected StringSetMap initialValue() {
                return new StringSetMap();
            }
        };
        dependencyCache = new WeakReference<ThreadLocal<StringSetMap>>(local);
        return local;
    }

    private static WeakReference<ThreadLocal<CompilationUnit>> localCu = new WeakReference<ThreadLocal<CompilationUnit>>(null);
    private synchronized static ThreadLocal<CompilationUnit> getLocalCompilationUnit() {
        ThreadLocal<CompilationUnit> local = localCu.get();
        if (local!=null) return local;
        local = new ThreadLocal<CompilationUnit>();
        localCu = new WeakReference<ThreadLocal<CompilationUnit>>(local);
        return local;
    }

    private URL[] roots;
    private ResourceConnector rc;
    private final ClassLoader parentLoader;
    private final GroovyClassLoader groovyLoader;
    private final Map<String, ScriptCacheEntry> scriptCache = new ConcurrentHashMap<String, ScriptCacheEntry>();
    private CompilerConfiguration config = new CompilerConfiguration(CompilerConfiguration.DEFAULT);

    //TODO: more finals?
    protected static class ScriptCacheEntry {
        final private Class scriptClass;
        final private long lastModified;
        final private Set<String> dependencies;

        public ScriptCacheEntry(Class clazz, long modified, Set<String> depend) {
            this.scriptClass = clazz;
            this.lastModified = modified;
            this.dependencies = depend;
        }
    }

   private class ScriptClassLoader extends GroovyClassLoader {
       public ScriptClassLoader(GroovyClassLoader loader) {
           super(loader);
           setResLoader();
       }

       public ScriptClassLoader(ClassLoader loader) {
           super(loader);
           setResLoader();
       }

       private void setResLoader(){
            final GroovyResourceLoader rl = getResourceLoader();
            setResourceLoader(new GroovyResourceLoader(){
                public URL loadGroovySource(String className) throws MalformedURLException {
                    String filename =   className.replace('.', File.separatorChar) +
                                        config.getDefaultScriptExtension();
                    try {
                        URLConnection dependentScriptConn = rc.getResourceConnection(filename);
                        return dependentScriptConn.getURL();
                    } catch (ResourceException e) {
                        //TODO: maybe do something here?
                    }
                    return rl.loadGroovySource(className);
                }
            });
       }

        @Override
        protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource source) {
            CompilationUnit cu = super.createCompilationUnit(config, source);
            getLocalCompilationUnit().set(cu);
            final StringSetMap cache = getDepCache().get();

            // "." is used to transfer compilation dependencies, which will be
            // recollected later during compilation
            for (String depSourcePath : cache.get(".")) {
                try {
                    cu.addSource(new URL("file","",depSourcePath));
                } catch (MalformedURLException e) {}
            }

            // remove all old entries including the "." entry
            cache.clear();
            cu.addPhaseOperation(new CompilationUnit.PrimaryClassNodeOperation() {
                @Override
                public void call(final SourceUnit source, GeneratorContext context, ClassNode classNode)
                    throws CompilationFailedException
                {
                    DependencyTracker dt = new DependencyTracker(source,cache);
                    dt.visitClass(classNode);
                }
            }, Phases.CLASS_GENERATION);

            customizeCompilationUnit(cu);
            return cu;
        }

        @Override
        public Class parseClass(GroovyCodeSource codeSource, boolean shouldCacheSource) throws CompilationFailedException {
            // local is kept as hard reference to avoid garbage collection
            ThreadLocal<CompilationUnit> localCu = getLocalCompilationUnit();
            ThreadLocal<StringSetMap> localCache = getDepCache();

            // we put the old dependencies into local cache so createCompilationUnit
            // can pick it up. We put that entry under the name "."
            ScriptCacheEntry origEntry = scriptCache.get(codeSource.getName());
            Set<String> origDep = null;
            if (origEntry != null) origDep = origEntry.dependencies;
            if (origDep != null) localCache.get().put(".",origDep);

            Class answer = super.parseClass(codeSource, false);

            StringSetMap cache = localCache.get();
            cache.makeTransitiveHull();
            long time = System.currentTimeMillis();
            for (Map.Entry<String,Set<String>> entry: cache.entrySet()) {
                String className = entry.getKey();
                Class clazz = getClassCacheEntry(className);
                if (clazz==null) continue;

                String entryName = getPath(clazz);
                Set<String> value = convertToPaths(entry.getValue());
                ScriptCacheEntry cacheEntry = new ScriptCacheEntry(clazz,time,value);
                scriptCache.put(entryName,cacheEntry);
            }
            cache.clear();
            localCu.set(null);
            return answer;
        }


        private String getPath(Class clazz) {
            ThreadLocal<CompilationUnit> localCu = getLocalCompilationUnit();

            ClassNode classNode = localCu.get().getClassNode(clazz.getCanonicalName());
            String entryName = classNode.getModule().getContext().getName();
            return entryName;
        }

        private Set<String> convertToPaths(Set<String> orig) {
            Set<String> ret = new HashSet<String>();
            for (String className : orig) {
                Class clazz = getClassCacheEntry(className);
                if (clazz==null) continue;
                ret.add(getPath(clazz));
            }
            return ret;
        }
   }

    protected void customizeCompilationUnit(CompilationUnit cu) {

    }

    /**
    * Simple testing harness for the GSE. Enter script roots as arguments and
    * then input script names to run them.
    *
    * @param urls an array of URLs
    * @throws Exception if something goes wrong
    */
   public static void main(String[] urls) throws Exception {
       GroovyScriptEngine gse = new GroovyScriptEngine(urls);
       BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
       String line;
       while (true) {
           System.out.print("groovy> ");
           if ((line = br.readLine()) == null || line.equals("quit"))
               break;
           try {
               System.out.println(gse.run(line, new Binding()));
           } catch (Exception e) {
               e.printStackTrace();
           }
       }
   }

    /**
     * Initialize a new GroovyClassLoader with a default or
     * constructor-supplied parentClassLoader.
     *
     * @return the parent classloader used to load scripts
     */
    private GroovyClassLoader initGroovyLoader() {
        return (GroovyClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                if (parentLoader instanceof GroovyClassLoader) {
                    return new ScriptClassLoader((GroovyClassLoader)parentLoader);
                } else {
                    return new ScriptClassLoader(parentLoader);
                }
            }
        });
    }

    /**
     * Get a resource connection as a <code>URLConnection</code> to retrieve a script
     * from the <code>ResourceConnector</code>.
     *
     * @param resourceName name of the resource to be retrieved
     * @return a URLConnection to the resource
     * @throws ResourceException
     */
    public URLConnection getResourceConnection(String resourceName) throws ResourceException {
        // Get the URLConnection
        URLConnection groovyScriptConn = null;

        ResourceException se = null;
        for (URL root : roots) {
            URL scriptURL = null;
            try {
                scriptURL = new URL(root, resourceName);
                groovyScriptConn = scriptURL.openConnection();

                // Make sure we can open it, if we can't it doesn't exist.
                // Could be very slow if there are any non-file:// URLs in there
                groovyScriptConn.getInputStream();

                break; // Now this is a bit unusual

            } catch (MalformedURLException e) {
                String message = "Malformed URL: " + root + ", " + resourceName;
                if (se == null) {
                    se = new ResourceException(message);
                } else {
                    se = new ResourceException(message, se);
                }
            } catch (IOException e1) {
                groovyScriptConn = null;
                String message = "Cannot open URL: " + scriptURL;
                groovyScriptConn = null;
                if (se == null) {
                    se = new ResourceException(message);
                } else {
                    se = new ResourceException(message, se);
                }
            }
        }

        if(se == null) se = new ResourceException("No resource for " + resourceName + " was found");

        // If we didn't find anything, report on all the exceptions that occurred.
        if (groovyScriptConn == null) throw se;
        return groovyScriptConn;
    }

    /**
     * The groovy script engine will run groovy scripts and reload them and
     * their dependencies when they are modified. This is useful for embedding
     * groovy in other containers like games and application servers.
     *
     * @param roots This an array of URLs where Groovy scripts will be stored. They should
     *              be laid out using their package structure like Java classes
     */
    private GroovyScriptEngine(URL[] roots, ClassLoader parent, ResourceConnector rc) {
        if (roots==null) roots = new URL[0];
        this.roots = roots;
        if (rc==null) rc = this;
        this.rc = rc;
        if (parent==CL_STUB) parent = this.getClass().getClassLoader();
        this.parentLoader = parent;
        this.groovyLoader = initGroovyLoader();
        for (URL root: roots) this.groovyLoader.addURL(root);
    }

    public GroovyScriptEngine(URL[] roots) {
        this(roots,CL_STUB,null);
    }

    public GroovyScriptEngine(URL[] roots, ClassLoader parentClassLoader) {
        this(roots,parentClassLoader,null);
    }

    public GroovyScriptEngine(String[] urls) throws IOException {
        this(createRoots(urls),CL_STUB, null);
    }

    private static URL[] createRoots(String[] urls) throws MalformedURLException {
        if (urls==null) return null;
        URL[] roots = new URL[urls.length];
        for (int i = 0; i < roots.length; i++) {
            if(urls[i].indexOf("://") != -1) {
                roots[i] = new URL(urls[i]);
            } else {
                roots[i] = new File(urls[i]).toURI().toURL();
            }
        }
        return roots;
    }

    public GroovyScriptEngine(String[] urls, ClassLoader parentClassLoader) throws IOException {
        this(createRoots(urls),parentClassLoader, null);
    }

    public GroovyScriptEngine(String url) throws IOException {
        this(new String[]{url});
    }

    public GroovyScriptEngine(String url, ClassLoader parentClassLoader) throws IOException {
        this(new String[]{url},parentClassLoader);
    }

    public GroovyScriptEngine(ResourceConnector rc) {
        this(null,CL_STUB, rc);
    }

    public GroovyScriptEngine(ResourceConnector rc, ClassLoader parentClassLoader) {
        this(null,parentClassLoader, rc);
    }

    /**
     * Get the <code>ClassLoader</code> that will serve as the parent ClassLoader of the
     * {@link GroovyClassLoader} in which scripts will be executed. By default, this is the
     * ClassLoader that loaded the <code>GroovyScriptEngine</code> class.
     *
     * @return the parent classloader used to load scripts
     */
    public ClassLoader getParentClassLoader() {
        return parentLoader;
    }

    /**
     * @param parentClassLoader ClassLoader to be used as the parent ClassLoader
     *        for scripts executed by the engine
     * @deprecated
     */
    public void setParentClassLoader(ClassLoader parentClassLoader) {
        throw new DeprecationException(
                "The method GroovyScriptEngine#setParentClassLoader(ClassLoader) " +
                "is no longer supported. Specify a parentLoader in the constructor instead."
        );
    }

    /**
     * Get the class of the scriptName in question, so that you can instantiate
     * Groovy objects with caching and reloading.
     *
     * @param scriptName resource name pointing to the script
     * @return the loaded scriptName as a compiled class
     * @throws ResourceException if there is a problem accessing the script
     * @throws groovy.util.ScriptException if there is a problem parsing the script
     */
    public Class loadScriptByName(String scriptName) throws ResourceException, ScriptException {
        URLConnection conn = rc.getResourceConnection(scriptName);
        String path = conn.getURL().getPath();
        ScriptCacheEntry entry = scriptCache.get(path);
        Class clazz = null;
        if (entry!=null) clazz=entry.scriptClass;
        if (isSourceNewer(entry)) {
            try {
                String encoding = conn.getContentEncoding() != null ? conn.getContentEncoding() : "UTF-8";
                clazz = groovyLoader.parseClass(DefaultGroovyMethods.getText(conn.getInputStream(), encoding), conn.getURL().getPath());
            } catch (IOException e) {
                throw new ResourceException(e);
            }
        }
        return clazz;
    }

    /**
     * Get the class of the scriptName in question, so that you can instantiate
     * Groovy objects with caching and reloading.
     *
     * @param scriptName resource name pointing to the script
     * @param parentClassLoader the class loader to use when loading the script
     * @return the loaded scriptName as a compiled class
     * @throws ResourceException if there is a problem accessing the script
     * @throws ScriptException if there is a problem parsing the script
     * @deprecated
     */
    public Class loadScriptByName(String scriptName, ClassLoader parentClassLoader)
            throws ResourceException, ScriptException {
        throw new DeprecationException(
                "The method GroovyScriptEngine#loadScriptByName(String,ClassLoader) "+
                "is no longer supported. Use GroovyScriptEngine#loadScriptByName(String) instead."
        );
    }

    /**
     * Run a script identified by name with a single argument.
     *
     * @param scriptName name of the script to run
     * @param argument   a single argument passed as a variable named <code>arg</code> in the binding
     * @return a <code>toString()</code> representation of the result of the execution of the script
     * @throws ResourceException if there is a problem accessing the script
     * @throws ScriptException if there is a problem parsing the script
     */
    public String run(String scriptName, String argument) throws ResourceException, ScriptException {
        Binding binding = new Binding();
        binding.setVariable("arg", argument);
        Object result = run(scriptName, binding);
        return result == null ? "" : result.toString();
    }

    /**
     * Run a script identified by name with a given binding.
     *
     * @param scriptName name of the script to run
     * @param binding    the binding to pass to the script
     * @return an object
     * @throws ResourceException if there is a problem accessing the script
     * @throws ScriptException if there is a problem parsing the script
     */
    public Object run(String scriptName, Binding binding) throws ResourceException, ScriptException {
        return createScript(scriptName, binding).run();
    }

    /**
     * Creates a Script with a given scriptName and binding.
     *
     * @param scriptName name of the script to run
     * @param binding    the binding to pass to the script
     * @return the script object
     * @throws ResourceException if there is a problem accessing the script
     * @throws ScriptException if there is a problem parsing the script
     */
    public Script createScript(String scriptName, Binding binding) throws ResourceException, ScriptException {
        return InvokerHelper.createScript(loadScriptByName(scriptName), binding);
    }

    protected boolean isSourceNewer(ScriptCacheEntry entry) throws ResourceException  {
        if (entry==null) return true;
        long time = System.currentTimeMillis();

        for (String scriptName:entry.dependencies) {
            ScriptCacheEntry depEntry = scriptCache.get(scriptName);
            long entryChangeTime = depEntry.lastModified + config.getMinimumRecompilationInterval();
            if (entryChangeTime>time) continue;

            URLConnection conn = rc.getResourceConnection(scriptName);
            URL source = conn.getURL();
            String path = source.getPath().replace('/', File.separatorChar).replace('|', ':');
            File file = new File(path);
            long lastMod = file.lastModified();

            if (entryChangeTime > lastMod) {
                ScriptCacheEntry newEntry = new ScriptCacheEntry(depEntry.scriptClass,time,depEntry.dependencies);
                scriptCache.put(scriptName,newEntry);
                continue;
            }
            return true;
        }

        return false;
    }

    /**
     * Returns the GroovyClassLoader associated with this script engine instance.
     * Useful if you need to pass the class loader to another library.
     *
     * @return the GroovyClassLoader
     */
    public GroovyClassLoader getGroovyClassLoader() {
        return groovyLoader;
    }

    /**
     * @return a non null compiler configuration
     */
    public CompilerConfiguration getConfig() {
        return config;
    }

    /**
     * sets a compiler configuration
     * @param config - the compiler configuration
     * @throws NullPointerException if config is null
     */
    public void setConfig(CompilerConfiguration config) {
        if (config==null) throw new NullPointerException("configuration cannot be null");
        this.config = config;
    }
}

