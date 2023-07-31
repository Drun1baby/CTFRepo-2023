/*     */ package org.springframework.boot.loader;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.FileInputStream;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import java.lang.reflect.Constructor;
/*     */ import java.net.HttpURLConnection;
/*     */ import java.net.URL;
/*     */ import java.net.URLConnection;
/*     */ import java.net.URLDecoder;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.Iterator;
/*     */ import java.util.LinkedHashSet;
/*     */ import java.util.List;
/*     */ import java.util.Locale;
/*     */ import java.util.Properties;
/*     */ import java.util.Set;
/*     */ import java.util.jar.Manifest;
/*     */ import java.util.regex.Matcher;
/*     */ import java.util.regex.Pattern;
/*     */ import org.springframework.boot.loader.archive.Archive;
/*     */ import org.springframework.boot.loader.archive.ExplodedArchive;
/*     */ import org.springframework.boot.loader.archive.JarFileArchive;
/*     */ import org.springframework.boot.loader.util.SystemPropertyUtils;
/*     */ 
/*     */ public class PropertiesLauncher extends Launcher {
/*  79 */   private static final Class<?>[] PARENT_ONLY_PARAMS = new Class[] { ClassLoader.class };
/*     */   
/*  81 */   private static final Class<?>[] URLS_AND_PARENT_PARAMS = new Class[] { URL[].class, ClassLoader.class };
/*     */   
/*  83 */   private static final Class<?>[] NO_PARAMS = new Class[0];
/*     */   
/*  85 */   private static final URL[] NO_URLS = new URL[0];
/*     */   
/*     */   private static final String DEBUG = "loader.debug";
/*     */   
/*     */   public static final String MAIN = "loader.main";
/*     */   
/*     */   public static final String PATH = "loader.path";
/*     */   
/*     */   public static final String HOME = "loader.home";
/*     */   
/*     */   public static final String ARGS = "loader.args";
/*     */   
/*     */   public static final String CONFIG_NAME = "loader.config.name";
/*     */   
/*     */   public static final String CONFIG_LOCATION = "loader.config.location";
/*     */   
/*     */   public static final String SET_SYSTEM_PROPERTIES = "loader.system";
/*     */   
/* 136 */   private static final Pattern WORD_SEPARATOR = Pattern.compile("\\W+");
/*     */   
/* 138 */   private static final String NESTED_ARCHIVE_SEPARATOR = "!" + File.separator;
/*     */   
/*     */   private final File home;
/*     */   
/* 142 */   private List<String> paths = new ArrayList<>();
/*     */   
/* 144 */   private final Properties properties = new Properties();
/*     */   
/*     */   private final Archive parent;
/*     */   
/*     */   private volatile ClassPathArchives classPathArchives;
/*     */   
/*     */   public PropertiesLauncher() {
/*     */     try {
/* 152 */       this.home = getHomeDirectory();
/* 153 */       initializeProperties();
/* 154 */       initializePaths();
/* 155 */       this.parent = createArchive();
/* 157 */     } catch (Exception ex) {
/* 158 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   protected File getHomeDirectory() {
/*     */     try {
/* 164 */       return new File(getPropertyWithDefault("loader.home", "${user.dir}"));
/* 166 */     } catch (Exception ex) {
/* 167 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void initializeProperties() throws Exception {
/* 172 */     List<String> configs = new ArrayList<>();
/* 173 */     if (getProperty("loader.config.location") != null) {
/* 174 */       configs.add(getProperty("loader.config.location"));
/*     */     } else {
/* 177 */       String[] names = getPropertyWithDefault("loader.config.name", "loader").split(",");
/* 178 */       for (String name : names) {
/* 179 */         configs.add("file:" + getHomeDirectory() + "/" + name + ".properties");
/* 180 */         configs.add("classpath:" + name + ".properties");
/* 181 */         configs.add("classpath:BOOT-INF/classes/" + name + ".properties");
/*     */       } 
/*     */     } 
/* 184 */     for (String config : configs) {
/* 185 */       try (InputStream resource = getResource(config)) {
/* 186 */         if (resource != null) {
/* 187 */           debug("Found: " + config);
/* 188 */           loadResource(resource);
/*     */           return;
/*     */         } 
/* 193 */         debug("Not found: " + config);
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   private void loadResource(InputStream resource) throws Exception {
/* 200 */     this.properties.load(resource);
/* 201 */     for (Object key : Collections.list(this.properties.propertyNames())) {
/* 202 */       String text = this.properties.getProperty((String)key);
/* 203 */       String value = SystemPropertyUtils.resolvePlaceholders(this.properties, text);
/* 204 */       if (value != null)
/* 205 */         this.properties.put(key, value); 
/*     */     } 
/* 208 */     if ("true".equals(getProperty("loader.system"))) {
/* 209 */       debug("Adding resolved properties to System properties");
/* 210 */       for (Object key : Collections.list(this.properties.propertyNames())) {
/* 211 */         String value = this.properties.getProperty((String)key);
/* 212 */         System.setProperty((String)key, value);
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   private InputStream getResource(String config) throws Exception {
/* 218 */     if (config.startsWith("classpath:"))
/* 219 */       return getClasspathResource(config.substring("classpath:".length())); 
/* 221 */     config = handleUrl(config);
/* 222 */     if (isUrl(config))
/* 223 */       return getURLResource(config); 
/* 225 */     return getFileResource(config);
/*     */   }
/*     */   
/*     */   private String handleUrl(String path) throws UnsupportedEncodingException {
/* 229 */     if (path.startsWith("jar:file:") || path.startsWith("file:")) {
/* 230 */       path = URLDecoder.decode(path, "UTF-8");
/* 231 */       if (path.startsWith("file:")) {
/* 232 */         path = path.substring("file:".length());
/* 233 */         if (path.startsWith("//"))
/* 234 */           path = path.substring(2); 
/*     */       } 
/*     */     } 
/* 238 */     return path;
/*     */   }
/*     */   
/*     */   private boolean isUrl(String config) {
/* 242 */     return config.contains("://");
/*     */   }
/*     */   
/*     */   private InputStream getClasspathResource(String config) {
/* 246 */     while (config.startsWith("/"))
/* 247 */       config = config.substring(1); 
/* 249 */     config = "/" + config;
/* 250 */     debug("Trying classpath: " + config);
/* 251 */     return getClass().getResourceAsStream(config);
/*     */   }
/*     */   
/*     */   private InputStream getFileResource(String config) throws Exception {
/* 255 */     File file = new File(config);
/* 256 */     debug("Trying file: " + config);
/* 257 */     if (file.canRead())
/* 258 */       return new FileInputStream(file); 
/* 260 */     return null;
/*     */   }
/*     */   
/*     */   private InputStream getURLResource(String config) throws Exception {
/* 264 */     URL url = new URL(config);
/* 265 */     if (exists(url)) {
/* 266 */       URLConnection con = url.openConnection();
/*     */       try {
/* 268 */         return con.getInputStream();
/* 270 */       } catch (IOException ex) {
/* 272 */         if (con instanceof HttpURLConnection)
/* 273 */           ((HttpURLConnection)con).disconnect(); 
/* 275 */         throw ex;
/*     */       } 
/*     */     } 
/* 278 */     return null;
/*     */   }
/*     */   
/*     */   private boolean exists(URL url) throws IOException {
/* 283 */     URLConnection connection = url.openConnection();
/*     */     try {
/* 285 */       connection.setUseCaches(connection.getClass().getSimpleName().startsWith("JNLP"));
/* 286 */       if (connection instanceof HttpURLConnection) {
/* 287 */         HttpURLConnection httpConnection = (HttpURLConnection)connection;
/* 288 */         httpConnection.setRequestMethod("HEAD");
/* 289 */         int responseCode = httpConnection.getResponseCode();
/* 290 */         if (responseCode == 200)
/* 291 */           return true; 
/* 293 */         if (responseCode == 404)
/* 294 */           return false; 
/*     */       } 
/* 297 */       return (connection.getContentLength() >= 0);
/*     */     } finally {
/* 300 */       if (connection instanceof HttpURLConnection)
/* 301 */         ((HttpURLConnection)connection).disconnect(); 
/*     */     } 
/*     */   }
/*     */   
/*     */   private void initializePaths() throws Exception {
/* 307 */     String path = getProperty("loader.path");
/* 308 */     if (path != null)
/* 309 */       this.paths = parsePathsProperty(path); 
/* 311 */     debug("Nested archive paths: " + this.paths);
/*     */   }
/*     */   
/*     */   private List<String> parsePathsProperty(String commaSeparatedPaths) {
/* 315 */     List<String> paths = new ArrayList<>();
/* 316 */     for (String path : commaSeparatedPaths.split(",")) {
/* 317 */       path = cleanupPath(path);
/* 319 */       path = (path == null || path.isEmpty()) ? "/" : path;
/* 320 */       paths.add(path);
/*     */     } 
/* 322 */     if (paths.isEmpty())
/* 323 */       paths.add("lib"); 
/* 325 */     return paths;
/*     */   }
/*     */   
/*     */   protected String[] getArgs(String... args) throws Exception {
/* 329 */     String loaderArgs = getProperty("loader.args");
/* 330 */     if (loaderArgs != null) {
/* 331 */       String[] defaultArgs = loaderArgs.split("\\s+");
/* 332 */       String[] additionalArgs = args;
/* 333 */       args = new String[defaultArgs.length + additionalArgs.length];
/* 334 */       System.arraycopy(defaultArgs, 0, args, 0, defaultArgs.length);
/* 335 */       System.arraycopy(additionalArgs, 0, args, defaultArgs.length, additionalArgs.length);
/*     */     } 
/* 337 */     return args;
/*     */   }
/*     */   
/*     */   protected String getMainClass() throws Exception {
/* 342 */     String mainClass = getProperty("loader.main", "Start-Class");
/* 343 */     if (mainClass == null)
/* 344 */       throw new IllegalStateException("No 'loader.main' or 'Start-Class' specified"); 
/* 346 */     return mainClass;
/*     */   }
/*     */   
/*     */   protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
/* 351 */     String customLoaderClassName = getProperty("loader.classLoader");
/* 352 */     if (customLoaderClassName == null)
/* 353 */       return super.createClassLoader(archives); 
/* 355 */     Set<URL> urls = new LinkedHashSet<>();
/* 356 */     while (archives.hasNext())
/* 357 */       urls.add(((Archive)archives.next()).getUrl()); 
/* 359 */     ClassLoader loader = new LaunchedURLClassLoader(urls.<URL>toArray(NO_URLS), getClass().getClassLoader());
/* 360 */     debug("Classpath for custom loader: " + urls);
/* 361 */     loader = wrapWithCustomClassLoader(loader, customLoaderClassName);
/* 362 */     debug("Using custom class loader: " + customLoaderClassName);
/* 363 */     return loader;
/*     */   }
/*     */   
/*     */   private ClassLoader wrapWithCustomClassLoader(ClassLoader parent, String className) throws Exception {
/* 368 */     Class<ClassLoader> type = (Class)Class.forName(className, true, parent);
/* 369 */     ClassLoader classLoader = newClassLoader(type, PARENT_ONLY_PARAMS, new Object[] { parent });
/* 370 */     if (classLoader == null)
/* 371 */       classLoader = newClassLoader(type, URLS_AND_PARENT_PARAMS, new Object[] { NO_URLS, parent }); 
/* 373 */     if (classLoader == null)
/* 374 */       classLoader = newClassLoader(type, NO_PARAMS, new Object[0]); 
/* 376 */     if (classLoader == null)
/* 377 */       throw new IllegalArgumentException("Unable to create class loader for " + className); 
/* 379 */     return classLoader;
/*     */   }
/*     */   
/*     */   private ClassLoader newClassLoader(Class<ClassLoader> loaderClass, Class<?>[] parameterTypes, Object... initargs) throws Exception {
/*     */     try {
/* 385 */       Constructor<ClassLoader> constructor = loaderClass.getDeclaredConstructor(parameterTypes);
/* 386 */       constructor.setAccessible(true);
/* 387 */       return constructor.newInstance(initargs);
/* 389 */     } catch (NoSuchMethodException ex) {
/* 390 */       return null;
/*     */     } 
/*     */   }
/*     */   
/*     */   private String getProperty(String propertyKey) throws Exception {
/* 395 */     return getProperty(propertyKey, (String)null, (String)null);
/*     */   }
/*     */   
/*     */   private String getProperty(String propertyKey, String manifestKey) throws Exception {
/* 399 */     return getProperty(propertyKey, manifestKey, (String)null);
/*     */   }
/*     */   
/*     */   private String getPropertyWithDefault(String propertyKey, String defaultValue) throws Exception {
/* 403 */     return getProperty(propertyKey, (String)null, defaultValue);
/*     */   }
/*     */   
/*     */   private String getProperty(String propertyKey, String manifestKey, String defaultValue) throws Exception {
/* 407 */     if (manifestKey == null) {
/* 408 */       manifestKey = propertyKey.replace('.', '-');
/* 409 */       manifestKey = toCamelCase(manifestKey);
/*     */     } 
/* 411 */     String property = SystemPropertyUtils.getProperty(propertyKey);
/* 412 */     if (property != null) {
/* 413 */       String value = SystemPropertyUtils.resolvePlaceholders(this.properties, property);
/* 414 */       debug("Property '" + propertyKey + "' from environment: " + value);
/* 415 */       return value;
/*     */     } 
/* 417 */     if (this.properties.containsKey(propertyKey)) {
/* 418 */       String value = SystemPropertyUtils.resolvePlaceholders(this.properties, this.properties
/* 419 */           .getProperty(propertyKey));
/* 420 */       debug("Property '" + propertyKey + "' from properties: " + value);
/* 421 */       return value;
/*     */     } 
/*     */     try {
/* 424 */       if (this.home != null)
/* 426 */         try (ExplodedArchive archive = new ExplodedArchive(this.home, false)) {
/* 427 */           Manifest manifest1 = archive.getManifest();
/* 428 */           if (manifest1 != null) {
/* 429 */             String value = manifest1.getMainAttributes().getValue(manifestKey);
/* 430 */             if (value != null) {
/* 431 */               debug("Property '" + manifestKey + "' from home directory manifest: " + value);
/* 432 */               return SystemPropertyUtils.resolvePlaceholders(this.properties, value);
/*     */             } 
/*     */           } 
/*     */         }  
/* 438 */     } catch (IllegalStateException illegalStateException) {}
/* 442 */     Manifest manifest = createArchive().getManifest();
/* 443 */     if (manifest != null) {
/* 444 */       String value = manifest.getMainAttributes().getValue(manifestKey);
/* 445 */       if (value != null) {
/* 446 */         debug("Property '" + manifestKey + "' from archive manifest: " + value);
/* 447 */         return SystemPropertyUtils.resolvePlaceholders(this.properties, value);
/*     */       } 
/*     */     } 
/* 450 */     return (defaultValue != null) ? SystemPropertyUtils.resolvePlaceholders(this.properties, defaultValue) : defaultValue;
/*     */   }
/*     */   
/*     */   protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
/* 456 */     ClassPathArchives classPathArchives = this.classPathArchives;
/* 457 */     if (classPathArchives == null) {
/* 458 */       classPathArchives = new ClassPathArchives();
/* 459 */       this.classPathArchives = classPathArchives;
/*     */     } 
/* 461 */     return classPathArchives.iterator();
/*     */   }
/*     */   
/*     */   public static void main(String[] args) throws Exception {
/* 465 */     PropertiesLauncher launcher = new PropertiesLauncher();
/* 466 */     args = launcher.getArgs(args);
/* 467 */     launcher.launch(args);
/*     */   }
/*     */   
/*     */   public static String toCamelCase(CharSequence string) {
/* 471 */     if (string == null)
/* 472 */       return null; 
/* 474 */     StringBuilder builder = new StringBuilder();
/* 475 */     Matcher matcher = WORD_SEPARATOR.matcher(string);
/* 476 */     int pos = 0;
/* 477 */     while (matcher.find()) {
/* 478 */       builder.append(capitalize(string.subSequence(pos, matcher.end()).toString()));
/* 479 */       pos = matcher.end();
/*     */     } 
/* 481 */     builder.append(capitalize(string.subSequence(pos, string.length()).toString()));
/* 482 */     return builder.toString();
/*     */   }
/*     */   
/*     */   private static String capitalize(String str) {
/* 486 */     return Character.toUpperCase(str.charAt(0)) + str.substring(1);
/*     */   }
/*     */   
/*     */   private void debug(String message) {
/* 490 */     if (Boolean.getBoolean("loader.debug"))
/* 491 */       System.out.println(message); 
/*     */   }
/*     */   
/*     */   private String cleanupPath(String path) {
/* 496 */     path = path.trim();
/* 498 */     if (path.startsWith("./"))
/* 499 */       path = path.substring(2); 
/* 501 */     String lowerCasePath = path.toLowerCase(Locale.ENGLISH);
/* 502 */     if (lowerCasePath.endsWith(".jar") || lowerCasePath.endsWith(".zip"))
/* 503 */       return path; 
/* 505 */     if (path.endsWith("/*")) {
/* 506 */       path = path.substring(0, path.length() - 1);
/* 510 */     } else if (!path.endsWith("/") && !path.equals(".")) {
/* 511 */       path = path + "/";
/*     */     } 
/* 514 */     return path;
/*     */   }
/*     */   
/*     */   void close() throws Exception {
/* 518 */     if (this.classPathArchives != null)
/* 519 */       this.classPathArchives.close(); 
/* 521 */     if (this.parent != null)
/* 522 */       this.parent.close(); 
/*     */   }
/*     */   
/*     */   private class ClassPathArchives implements Iterable<Archive> {
/*     */     private final List<Archive> classPathArchives;
/*     */     
/* 533 */     private final List<JarFileArchive> jarFileArchives = new ArrayList<>();
/*     */     
/*     */     ClassPathArchives() throws Exception {
/* 536 */       this.classPathArchives = new ArrayList<>();
/* 537 */       for (String path : PropertiesLauncher.this.paths) {
/* 538 */         for (Archive archive : getClassPathArchives(path))
/* 539 */           addClassPathArchive(archive); 
/*     */       } 
/* 542 */       addNestedEntries();
/*     */     }
/*     */     
/*     */     private void addClassPathArchive(Archive archive) throws IOException {
/* 546 */       if (!(archive instanceof ExplodedArchive)) {
/* 547 */         this.classPathArchives.add(archive);
/*     */         return;
/*     */       } 
/* 550 */       this.classPathArchives.add(archive);
/* 551 */       this.classPathArchives.addAll(asList(archive.getNestedArchives(null, new PropertiesLauncher.ArchiveEntryFilter())));
/*     */     }
/*     */     
/*     */     private List<Archive> getClassPathArchives(String path) throws Exception {
/* 555 */       String root = PropertiesLauncher.this.cleanupPath(PropertiesLauncher.this.handleUrl(path));
/* 556 */       List<Archive> lib = new ArrayList<>();
/* 557 */       File file = new File(root);
/* 558 */       if (!"/".equals(root)) {
/* 559 */         if (!isAbsolutePath(root))
/* 560 */           file = new File(PropertiesLauncher.this.home, root); 
/* 562 */         if (file.isDirectory()) {
/* 563 */           PropertiesLauncher.this.debug("Adding classpath entries from " + file);
/* 564 */           ExplodedArchive explodedArchive = new ExplodedArchive(file, false);
/* 565 */           lib.add(explodedArchive);
/*     */         } 
/*     */       } 
/* 568 */       Archive archive = getArchive(file);
/* 569 */       if (archive != null) {
/* 570 */         PropertiesLauncher.this.debug("Adding classpath entries from archive " + archive.getUrl() + root);
/* 571 */         lib.add(archive);
/*     */       } 
/* 573 */       List<Archive> nestedArchives = getNestedArchives(root);
/* 574 */       if (nestedArchives != null) {
/* 575 */         PropertiesLauncher.this.debug("Adding classpath entries from nested " + root);
/* 576 */         lib.addAll(nestedArchives);
/*     */       } 
/* 578 */       return lib;
/*     */     }
/*     */     
/*     */     private boolean isAbsolutePath(String root) {
/* 583 */       return (root.contains(":") || root.startsWith("/"));
/*     */     }
/*     */     
/*     */     private Archive getArchive(File file) throws IOException {
/* 587 */       if (isNestedArchivePath(file))
/* 588 */         return null; 
/* 590 */       String name = file.getName().toLowerCase(Locale.ENGLISH);
/* 591 */       if (name.endsWith(".jar") || name.endsWith(".zip"))
/* 592 */         return (Archive)getJarFileArchive(file); 
/* 594 */       return null;
/*     */     }
/*     */     
/*     */     private boolean isNestedArchivePath(File file) {
/* 598 */       return file.getPath().contains(PropertiesLauncher.NESTED_ARCHIVE_SEPARATOR);
/*     */     }
/*     */     
/*     */     private List<Archive> getNestedArchives(String path) throws Exception {
/*     */       JarFileArchive jarFileArchive;
/* 602 */       Archive parent = PropertiesLauncher.this.parent;
/* 603 */       String root = path;
/* 604 */       if ((!root.equals("/") && root.startsWith("/")) || parent
/* 605 */         .getUrl().toURI().equals(PropertiesLauncher.this.home.toURI()))
/* 607 */         return null; 
/* 609 */       int index = root.indexOf('!');
/* 610 */       if (index != -1) {
/* 611 */         File file = new File(PropertiesLauncher.this.home, root.substring(0, index));
/* 612 */         if (root.startsWith("jar:file:"))
/* 613 */           file = new File(root.substring("jar:file:".length(), index)); 
/* 615 */         jarFileArchive = getJarFileArchive(file);
/* 616 */         root = root.substring(index + 1);
/* 617 */         while (root.startsWith("/"))
/* 618 */           root = root.substring(1); 
/*     */       } 
/* 621 */       if (root.endsWith(".jar")) {
/* 622 */         File file = new File(PropertiesLauncher.this.home, root);
/* 623 */         if (file.exists()) {
/* 624 */           jarFileArchive = getJarFileArchive(file);
/* 625 */           root = "";
/*     */         } 
/*     */       } 
/* 628 */       if (root.equals("/") || root.equals("./") || root.equals("."))
/* 630 */         root = ""; 
/* 632 */       Archive.EntryFilter filter = new PropertiesLauncher.PrefixMatchingArchiveFilter(root);
/* 633 */       List<Archive> archives = asList(jarFileArchive.getNestedArchives(null, filter));
/* 634 */       if ((root == null || root.isEmpty() || ".".equals(root)) && !path.endsWith(".jar") && jarFileArchive != PropertiesLauncher.this
/* 635 */         .parent)
/* 638 */         archives.add(jarFileArchive); 
/* 640 */       return archives;
/*     */     }
/*     */     
/*     */     private void addNestedEntries() {
/*     */       try {
/* 648 */         Iterator<Archive> archives = PropertiesLauncher.this.parent.getNestedArchives(null, JarLauncher.NESTED_ARCHIVE_ENTRY_FILTER);
/* 650 */         while (archives.hasNext())
/* 651 */           this.classPathArchives.add(archives.next()); 
/* 654 */       } catch (IOException iOException) {}
/*     */     }
/*     */     
/*     */     private List<Archive> asList(Iterator<Archive> iterator) {
/* 660 */       List<Archive> list = new ArrayList<>();
/* 661 */       while (iterator.hasNext())
/* 662 */         list.add(iterator.next()); 
/* 664 */       return list;
/*     */     }
/*     */     
/*     */     private JarFileArchive getJarFileArchive(File file) throws IOException {
/* 668 */       JarFileArchive archive = new JarFileArchive(file);
/* 669 */       this.jarFileArchives.add(archive);
/* 670 */       return archive;
/*     */     }
/*     */     
/*     */     public Iterator<Archive> iterator() {
/* 675 */       return this.classPathArchives.iterator();
/*     */     }
/*     */     
/*     */     void close() throws IOException {
/* 679 */       for (JarFileArchive archive : this.jarFileArchives)
/* 680 */         archive.close(); 
/*     */     }
/*     */   }
/*     */   
/*     */   private static final class PrefixMatchingArchiveFilter implements Archive.EntryFilter {
/*     */     private final String prefix;
/*     */     
/* 694 */     private final PropertiesLauncher.ArchiveEntryFilter filter = new PropertiesLauncher.ArchiveEntryFilter();
/*     */     
/*     */     private PrefixMatchingArchiveFilter(String prefix) {
/* 697 */       this.prefix = prefix;
/*     */     }
/*     */     
/*     */     public boolean matches(Archive.Entry entry) {
/* 702 */       if (entry.isDirectory())
/* 703 */         return entry.getName().equals(this.prefix); 
/* 705 */       return (entry.getName().startsWith(this.prefix) && this.filter.matches(entry));
/*     */     }
/*     */   }
/*     */   
/*     */   private static final class ArchiveEntryFilter implements Archive.EntryFilter {
/*     */     private static final String DOT_JAR = ".jar";
/*     */     
/*     */     private static final String DOT_ZIP = ".zip";
/*     */     
/*     */     private ArchiveEntryFilter() {}
/*     */     
/*     */     public boolean matches(Archive.Entry entry) {
/* 722 */       return (entry.getName().endsWith(".jar") || entry.getName().endsWith(".zip"));
/*     */     }
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\PropertiesLauncher.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */