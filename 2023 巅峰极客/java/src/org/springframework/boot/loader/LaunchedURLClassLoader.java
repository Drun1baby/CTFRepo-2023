/*     */ package org.springframework.boot.loader;
/*     */ 
/*     */ import java.io.ByteArrayOutputStream;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.net.JarURLConnection;
/*     */ import java.net.URL;
/*     */ import java.net.URLClassLoader;
/*     */ import java.net.URLConnection;
/*     */ import java.security.AccessController;
/*     */ import java.security.PrivilegedActionException;
/*     */ import java.util.Enumeration;
/*     */ import java.util.function.Supplier;
/*     */ import java.util.jar.JarFile;
/*     */ import java.util.jar.Manifest;
/*     */ import org.springframework.boot.loader.archive.Archive;
/*     */ import org.springframework.boot.loader.jar.Handler;
/*     */ import org.springframework.boot.loader.jar.JarFile;
/*     */ 
/*     */ public class LaunchedURLClassLoader extends URLClassLoader {
/*     */   private static final int BUFFER_SIZE = 4096;
/*     */   
/*     */   private final boolean exploded;
/*     */   
/*     */   private final Archive rootArchive;
/*     */   
/*     */   static {
/*  49 */     ClassLoader.registerAsParallelCapable();
/*     */   }
/*     */   
/*  56 */   private final Object packageLock = new Object();
/*     */   
/*     */   private volatile DefinePackageCallType definePackageCallType;
/*     */   
/*     */   public LaunchedURLClassLoader(URL[] urls, ClassLoader parent) {
/*  66 */     this(false, urls, parent);
/*     */   }
/*     */   
/*     */   public LaunchedURLClassLoader(boolean exploded, URL[] urls, ClassLoader parent) {
/*  76 */     this(exploded, (Archive)null, urls, parent);
/*     */   }
/*     */   
/*     */   public LaunchedURLClassLoader(boolean exploded, Archive rootArchive, URL[] urls, ClassLoader parent) {
/*  88 */     super(urls, parent);
/*  89 */     this.exploded = exploded;
/*  90 */     this.rootArchive = rootArchive;
/*     */   }
/*     */   
/*     */   public URL findResource(String name) {
/*  95 */     if (this.exploded)
/*  96 */       return super.findResource(name); 
/*  98 */     Handler.setUseFastConnectionExceptions(true);
/*     */     try {
/* 100 */       return super.findResource(name);
/*     */     } finally {
/* 103 */       Handler.setUseFastConnectionExceptions(false);
/*     */     } 
/*     */   }
/*     */   
/*     */   public Enumeration<URL> findResources(String name) throws IOException {
/* 109 */     if (this.exploded)
/* 110 */       return super.findResources(name); 
/* 112 */     Handler.setUseFastConnectionExceptions(true);
/*     */     try {
/* 114 */       return new UseFastConnectionExceptionsEnumeration(super.findResources(name));
/*     */     } finally {
/* 117 */       Handler.setUseFastConnectionExceptions(false);
/*     */     } 
/*     */   }
/*     */   
/*     */   protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
/* 123 */     if (name.startsWith("org.springframework.boot.loader.jarmode."))
/*     */       try {
/* 125 */         Class<?> result = loadClassInLaunchedClassLoader(name);
/* 126 */         if (resolve)
/* 127 */           resolveClass(result); 
/* 129 */         return result;
/* 131 */       } catch (ClassNotFoundException classNotFoundException) {} 
/* 134 */     if (this.exploded)
/* 135 */       return super.loadClass(name, resolve); 
/* 137 */     Handler.setUseFastConnectionExceptions(true);
/*     */     try {
/*     */       try {
/* 140 */         definePackageIfNecessary(name);
/* 142 */       } catch (IllegalArgumentException ex) {
/* 144 */         if (getPackage(name) == null)
/* 148 */           throw new AssertionError("Package " + name + " has already been defined but it could not be found"); 
/*     */       } 
/* 151 */       return super.loadClass(name, resolve);
/*     */     } finally {
/* 154 */       Handler.setUseFastConnectionExceptions(false);
/*     */     } 
/*     */   }
/*     */   
/*     */   private Class<?> loadClassInLaunchedClassLoader(String name) throws ClassNotFoundException {
/* 159 */     String internalName = name.replace('.', '/') + ".class";
/* 160 */     InputStream inputStream = getParent().getResourceAsStream(internalName);
/* 161 */     if (inputStream == null)
/* 162 */       throw new ClassNotFoundException(name); 
/*     */     try {
/*     */       try {
/* 166 */         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
/* 167 */         byte[] buffer = new byte[4096];
/* 168 */         int bytesRead = -1;
/* 169 */         while ((bytesRead = inputStream.read(buffer)) != -1)
/* 170 */           outputStream.write(buffer, 0, bytesRead); 
/* 172 */         inputStream.close();
/* 173 */         byte[] bytes = outputStream.toByteArray();
/* 174 */         Class<?> definedClass = defineClass(name, bytes, 0, bytes.length);
/* 175 */         definePackageIfNecessary(name);
/* 176 */         return definedClass;
/*     */       } finally {
/* 179 */         inputStream.close();
/*     */       } 
/* 182 */     } catch (IOException ex) {
/* 183 */       throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void definePackageIfNecessary(String className) {
/* 194 */     int lastDot = className.lastIndexOf('.');
/* 195 */     if (lastDot >= 0) {
/* 196 */       String packageName = className.substring(0, lastDot);
/* 197 */       if (getPackage(packageName) == null)
/*     */         try {
/* 199 */           definePackage(className, packageName);
/* 201 */         } catch (IllegalArgumentException ex) {
/* 203 */           if (getPackage(packageName) == null)
/* 207 */             throw new AssertionError("Package " + packageName + " has already been defined but it could not be found"); 
/*     */         }  
/*     */     } 
/*     */   }
/*     */   
/*     */   private void definePackage(String className, String packageName) {
/*     */     try {
/* 217 */       AccessController.doPrivileged(() -> {
/*     */             String packageEntryName = packageName.replace('.', '/') + "/";
/*     */             String classEntryName = className.replace('.', '/') + ".class";
/*     */             for (URL url : getURLs()) {
/*     */               try {
/*     */                 URLConnection connection = url.openConnection();
/*     */                 if (connection instanceof JarURLConnection) {
/*     */                   JarFile jarFile = ((JarURLConnection)connection).getJarFile();
/*     */                   if (jarFile.getEntry(classEntryName) != null && jarFile.getEntry(packageEntryName) != null && jarFile.getManifest() != null) {
/*     */                     definePackage(packageName, jarFile.getManifest(), url);
/*     */                     return null;
/*     */                   } 
/*     */                 } 
/* 232 */               } catch (IOException iOException) {}
/*     */             } 
/*     */             return null;
/* 237 */           }AccessController.getContext());
/* 239 */     } catch (PrivilegedActionException privilegedActionException) {}
/*     */   }
/*     */   
/*     */   protected Package definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
/* 246 */     if (!this.exploded)
/* 247 */       return super.definePackage(name, man, url); 
/* 249 */     synchronized (this.packageLock) {
/* 250 */       return doDefinePackage(DefinePackageCallType.MANIFEST, () -> super.definePackage(name, man, url));
/*     */     } 
/*     */   }
/*     */   
/*     */   protected Package definePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
/* 257 */     if (!this.exploded)
/* 258 */       return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase); 
/* 261 */     synchronized (this.packageLock) {
/* 262 */       if (this.definePackageCallType == null) {
/* 266 */         Manifest manifest = getManifest(this.rootArchive);
/* 267 */         if (manifest != null)
/* 268 */           return definePackage(name, manifest, sealBase); 
/*     */       } 
/* 271 */       return doDefinePackage(DefinePackageCallType.ATTRIBUTES, () -> super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase));
/*     */     } 
/*     */   }
/*     */   
/*     */   private Manifest getManifest(Archive archive) {
/*     */     try {
/* 278 */       return (archive != null) ? archive.getManifest() : null;
/* 280 */     } catch (IOException ex) {
/* 281 */       return null;
/*     */     } 
/*     */   }
/*     */   
/*     */   private <T> T doDefinePackage(DefinePackageCallType type, Supplier<T> call) {
/* 286 */     DefinePackageCallType existingType = this.definePackageCallType;
/*     */     try {
/* 288 */       this.definePackageCallType = type;
/* 289 */       return call.get();
/*     */     } finally {
/* 292 */       this.definePackageCallType = existingType;
/*     */     } 
/*     */   }
/*     */   
/*     */   public void clearCache() {
/* 300 */     if (this.exploded)
/*     */       return; 
/* 303 */     for (URL url : getURLs()) {
/*     */       try {
/* 305 */         URLConnection connection = url.openConnection();
/* 306 */         if (connection instanceof JarURLConnection)
/* 307 */           clearCache(connection); 
/* 310 */       } catch (IOException iOException) {}
/*     */     } 
/*     */   }
/*     */   
/*     */   private void clearCache(URLConnection connection) throws IOException {
/* 318 */     Object jarFile = ((JarURLConnection)connection).getJarFile();
/* 319 */     if (jarFile instanceof JarFile)
/* 320 */       ((JarFile)jarFile).clearCache(); 
/*     */   }
/*     */   
/*     */   private static class UseFastConnectionExceptionsEnumeration implements Enumeration<URL> {
/*     */     private final Enumeration<URL> delegate;
/*     */     
/*     */     UseFastConnectionExceptionsEnumeration(Enumeration<URL> delegate) {
/* 329 */       this.delegate = delegate;
/*     */     }
/*     */     
/*     */     public boolean hasMoreElements() {
/* 334 */       Handler.setUseFastConnectionExceptions(true);
/*     */       try {
/* 336 */         return this.delegate.hasMoreElements();
/*     */       } finally {
/* 339 */         Handler.setUseFastConnectionExceptions(false);
/*     */       } 
/*     */     }
/*     */     
/*     */     public URL nextElement() {
/* 346 */       Handler.setUseFastConnectionExceptions(true);
/*     */       try {
/* 348 */         return this.delegate.nextElement();
/*     */       } finally {
/* 351 */         Handler.setUseFastConnectionExceptions(false);
/*     */       } 
/*     */     }
/*     */   }
/*     */   
/*     */   private enum DefinePackageCallType {
/* 366 */     MANIFEST, ATTRIBUTES;
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\LaunchedURLClassLoader.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */