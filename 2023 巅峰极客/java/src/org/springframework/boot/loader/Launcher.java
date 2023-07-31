/*     */ package org.springframework.boot.loader;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.net.URI;
/*     */ import java.net.URL;
/*     */ import java.security.CodeSource;
/*     */ import java.security.ProtectionDomain;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import org.springframework.boot.loader.archive.Archive;
/*     */ import org.springframework.boot.loader.archive.ExplodedArchive;
/*     */ import org.springframework.boot.loader.archive.JarFileArchive;
/*     */ import org.springframework.boot.loader.jar.JarFile;
/*     */ 
/*     */ public abstract class Launcher {
/*     */   private static final String JAR_MODE_LAUNCHER = "org.springframework.boot.loader.jarmode.JarModeLauncher";
/*     */   
/*     */   protected void launch(String[] args) throws Exception {
/*  52 */     if (!isExploded())
/*  53 */       JarFile.registerUrlProtocolHandler(); 
/*  55 */     ClassLoader classLoader = createClassLoader(getClassPathArchivesIterator());
/*  56 */     String jarMode = System.getProperty("jarmode");
/*  57 */     String launchClass = (jarMode != null && !jarMode.isEmpty()) ? "org.springframework.boot.loader.jarmode.JarModeLauncher" : getMainClass();
/*  58 */     launch(args, launchClass, classLoader);
/*     */   }
/*     */   
/*     */   @Deprecated
/*     */   protected ClassLoader createClassLoader(List<Archive> archives) throws Exception {
/*  71 */     return createClassLoader(archives.iterator());
/*     */   }
/*     */   
/*     */   protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
/*  82 */     List<URL> urls = new ArrayList<>(50);
/*  83 */     while (archives.hasNext())
/*  84 */       urls.add(((Archive)archives.next()).getUrl()); 
/*  86 */     return createClassLoader(urls.<URL>toArray(new URL[0]));
/*     */   }
/*     */   
/*     */   protected ClassLoader createClassLoader(URL[] urls) throws Exception {
/*  96 */     return new LaunchedURLClassLoader(isExploded(), getArchive(), urls, getClass().getClassLoader());
/*     */   }
/*     */   
/*     */   protected void launch(String[] args, String launchClass, ClassLoader classLoader) throws Exception {
/* 107 */     Thread.currentThread().setContextClassLoader(classLoader);
/* 108 */     createMainMethodRunner(launchClass, args, classLoader).run();
/*     */   }
/*     */   
/*     */   protected MainMethodRunner createMainMethodRunner(String mainClass, String[] args, ClassLoader classLoader) {
/* 119 */     return new MainMethodRunner(mainClass, args);
/*     */   }
/*     */   
/*     */   protected abstract String getMainClass() throws Exception;
/*     */   
/*     */   protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
/* 136 */     return getClassPathArchives().iterator();
/*     */   }
/*     */   
/*     */   @Deprecated
/*     */   protected List<Archive> getClassPathArchives() throws Exception {
/* 148 */     throw new IllegalStateException("Unexpected call to getClassPathArchives()");
/*     */   }
/*     */   
/*     */   protected final Archive createArchive() throws Exception {
/* 152 */     ProtectionDomain protectionDomain = getClass().getProtectionDomain();
/* 153 */     CodeSource codeSource = protectionDomain.getCodeSource();
/* 154 */     URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
/* 155 */     String path = (location != null) ? location.getSchemeSpecificPart() : null;
/* 156 */     if (path == null)
/* 157 */       throw new IllegalStateException("Unable to determine code source archive"); 
/* 159 */     File root = new File(path);
/* 160 */     if (!root.exists())
/* 161 */       throw new IllegalStateException("Unable to determine code source archive from " + root); 
/* 163 */     return root.isDirectory() ? (Archive)new ExplodedArchive(root) : (Archive)new JarFileArchive(root);
/*     */   }
/*     */   
/*     */   protected boolean isExploded() {
/* 174 */     return false;
/*     */   }
/*     */   
/*     */   protected Archive getArchive() {
/* 183 */     return null;
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\Launcher.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */