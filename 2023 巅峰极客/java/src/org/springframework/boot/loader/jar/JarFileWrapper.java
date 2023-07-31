/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.security.Permission;
/*     */ import java.util.Enumeration;
/*     */ import java.util.jar.JarEntry;
/*     */ import java.util.jar.JarFile;
/*     */ import java.util.jar.Manifest;
/*     */ import java.util.stream.Stream;
/*     */ import java.util.zip.ZipEntry;
/*     */ 
/*     */ class JarFileWrapper extends AbstractJarFile {
/*     */   private final JarFile parent;
/*     */   
/*     */   JarFileWrapper(JarFile parent) throws IOException {
/*  41 */     super(parent.getRootJarFile().getFile());
/*  42 */     this.parent = parent;
/*  43 */     if (System.getSecurityManager() == null)
/*  44 */       close(); 
/*     */   }
/*     */   
/*     */   URL getUrl() throws MalformedURLException {
/*  50 */     return this.parent.getUrl();
/*     */   }
/*     */   
/*     */   AbstractJarFile.JarFileType getType() {
/*  55 */     return this.parent.getType();
/*     */   }
/*     */   
/*     */   Permission getPermission() {
/*  60 */     return this.parent.getPermission();
/*     */   }
/*     */   
/*     */   public Manifest getManifest() throws IOException {
/*  65 */     return this.parent.getManifest();
/*     */   }
/*     */   
/*     */   public Enumeration<JarEntry> entries() {
/*  70 */     return this.parent.entries();
/*     */   }
/*     */   
/*     */   public Stream<JarEntry> stream() {
/*  75 */     return this.parent.stream();
/*     */   }
/*     */   
/*     */   public JarEntry getJarEntry(String name) {
/*  80 */     return this.parent.getJarEntry(name);
/*     */   }
/*     */   
/*     */   public ZipEntry getEntry(String name) {
/*  85 */     return this.parent.getEntry(name);
/*     */   }
/*     */   
/*     */   InputStream getInputStream() throws IOException {
/*  90 */     return this.parent.getInputStream();
/*     */   }
/*     */   
/*     */   public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
/*  95 */     return this.parent.getInputStream(ze);
/*     */   }
/*     */   
/*     */   public String getComment() {
/* 100 */     return this.parent.getComment();
/*     */   }
/*     */   
/*     */   public int size() {
/* 105 */     return this.parent.size();
/*     */   }
/*     */   
/*     */   public String toString() {
/* 110 */     return this.parent.toString();
/*     */   }
/*     */   
/*     */   public String getName() {
/* 115 */     return this.parent.getName();
/*     */   }
/*     */   
/*     */   static JarFile unwrap(JarFile jarFile) {
/* 119 */     if (jarFile instanceof JarFile)
/* 120 */       return (JarFile)jarFile; 
/* 122 */     if (jarFile instanceof JarFileWrapper)
/* 123 */       return unwrap(((JarFileWrapper)jarFile).parent); 
/* 125 */     throw new IllegalStateException("Not a JarFile or Wrapper");
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\JarFileWrapper.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */