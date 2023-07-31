/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.security.CodeSigner;
/*     */ import java.security.cert.Certificate;
/*     */ import java.util.jar.Attributes;
/*     */ import java.util.jar.JarEntry;
/*     */ import java.util.jar.Manifest;
/*     */ 
/*     */ class JarEntry extends JarEntry implements FileHeader {
/*     */   private final int index;
/*     */   
/*     */   private final AsciiBytes name;
/*     */   
/*     */   private final AsciiBytes headerName;
/*     */   
/*     */   private final JarFile jarFile;
/*     */   
/*     */   private long localHeaderOffset;
/*     */   
/*     */   private volatile JarEntryCertification certification;
/*     */   
/*     */   JarEntry(JarFile jarFile, int index, CentralDirectoryFileHeader header, AsciiBytes nameAlias) {
/*  48 */     super((nameAlias != null) ? nameAlias.toString() : header.getName().toString());
/*  49 */     this.index = index;
/*  50 */     this.name = (nameAlias != null) ? nameAlias : header.getName();
/*  51 */     this.headerName = header.getName();
/*  52 */     this.jarFile = jarFile;
/*  53 */     this.localHeaderOffset = header.getLocalHeaderOffset();
/*  54 */     setCompressedSize(header.getCompressedSize());
/*  55 */     setMethod(header.getMethod());
/*  56 */     setCrc(header.getCrc());
/*  57 */     setComment(header.getComment().toString());
/*  58 */     setSize(header.getSize());
/*  59 */     setTime(header.getTime());
/*  60 */     if (header.hasExtra())
/*  61 */       setExtra(header.getExtra()); 
/*     */   }
/*     */   
/*     */   int getIndex() {
/*  66 */     return this.index;
/*     */   }
/*     */   
/*     */   AsciiBytes getAsciiBytesName() {
/*  70 */     return this.name;
/*     */   }
/*     */   
/*     */   public boolean hasName(CharSequence name, char suffix) {
/*  75 */     return this.headerName.matches(name, suffix);
/*     */   }
/*     */   
/*     */   URL getUrl() throws MalformedURLException {
/*  84 */     return new URL(this.jarFile.getUrl(), getName());
/*     */   }
/*     */   
/*     */   public Attributes getAttributes() throws IOException {
/*  89 */     Manifest manifest = this.jarFile.getManifest();
/*  90 */     return (manifest != null) ? manifest.getAttributes(getName()) : null;
/*     */   }
/*     */   
/*     */   public Certificate[] getCertificates() {
/*  95 */     return getCertification().getCertificates();
/*     */   }
/*     */   
/*     */   public CodeSigner[] getCodeSigners() {
/* 100 */     return getCertification().getCodeSigners();
/*     */   }
/*     */   
/*     */   private JarEntryCertification getCertification() {
/* 104 */     if (!this.jarFile.isSigned())
/* 105 */       return JarEntryCertification.NONE; 
/* 107 */     JarEntryCertification certification = this.certification;
/* 108 */     if (certification == null) {
/* 109 */       certification = this.jarFile.getCertification(this);
/* 110 */       this.certification = certification;
/*     */     } 
/* 112 */     return certification;
/*     */   }
/*     */   
/*     */   public long getLocalHeaderOffset() {
/* 117 */     return this.localHeaderOffset;
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\JarEntry.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */