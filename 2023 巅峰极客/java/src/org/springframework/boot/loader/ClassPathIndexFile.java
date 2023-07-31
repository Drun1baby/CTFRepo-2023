/*     */ package org.springframework.boot.loader;
/*     */ 
/*     */ import java.io.BufferedReader;
/*     */ import java.io.File;
/*     */ import java.io.FileInputStream;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.InputStreamReader;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URISyntaxException;
/*     */ import java.net.URL;
/*     */ import java.nio.charset.StandardCharsets;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.List;
/*     */ import java.util.stream.Collectors;
/*     */ 
/*     */ final class ClassPathIndexFile {
/*     */   private final File root;
/*     */   
/*     */   private final List<String> lines;
/*     */   
/*     */   private ClassPathIndexFile(File root, List<String> lines) {
/*  47 */     this.root = root;
/*  48 */     this.lines = (List<String>)lines.stream().map(this::extractName).collect(Collectors.toList());
/*     */   }
/*     */   
/*     */   private String extractName(String line) {
/*  52 */     if (line.startsWith("- \"") && line.endsWith("\""))
/*  53 */       return line.substring(3, line.length() - 1); 
/*  55 */     throw new IllegalStateException("Malformed classpath index line [" + line + "]");
/*     */   }
/*     */   
/*     */   int size() {
/*  59 */     return this.lines.size();
/*     */   }
/*     */   
/*     */   boolean containsEntry(String name) {
/*  63 */     if (name == null || name.isEmpty())
/*  64 */       return false; 
/*  66 */     return this.lines.contains(name);
/*     */   }
/*     */   
/*     */   List<URL> getUrls() {
/*  70 */     return Collections.unmodifiableList((List<? extends URL>)this.lines.stream().map(this::asUrl).collect(Collectors.toList()));
/*     */   }
/*     */   
/*     */   private URL asUrl(String line) {
/*     */     try {
/*  75 */       return (new File(this.root, line)).toURI().toURL();
/*  77 */     } catch (MalformedURLException ex) {
/*  78 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   static ClassPathIndexFile loadIfPossible(URL root, String location) throws IOException {
/*  83 */     return loadIfPossible(asFile(root), location);
/*     */   }
/*     */   
/*     */   private static ClassPathIndexFile loadIfPossible(File root, String location) throws IOException {
/*  87 */     return loadIfPossible(root, new File(root, location));
/*     */   }
/*     */   
/*     */   private static ClassPathIndexFile loadIfPossible(File root, File indexFile) throws IOException {
/*  91 */     if (indexFile.exists() && indexFile.isFile())
/*  92 */       try (InputStream inputStream = new FileInputStream(indexFile)) {
/*  93 */         return new ClassPathIndexFile(root, loadLines(inputStream));
/*     */       }  
/*  96 */     return null;
/*     */   }
/*     */   
/*     */   private static List<String> loadLines(InputStream inputStream) throws IOException {
/* 100 */     List<String> lines = new ArrayList<>();
/* 101 */     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
/* 102 */     String line = reader.readLine();
/* 103 */     while (line != null) {
/* 104 */       if (!line.trim().isEmpty())
/* 105 */         lines.add(line); 
/* 107 */       line = reader.readLine();
/*     */     } 
/* 109 */     return Collections.unmodifiableList(lines);
/*     */   }
/*     */   
/*     */   private static File asFile(URL url) {
/* 113 */     if (!"file".equals(url.getProtocol()))
/* 114 */       throw new IllegalArgumentException("URL does not reference a file"); 
/*     */     try {
/* 117 */       return new File(url.toURI());
/* 119 */     } catch (URISyntaxException ex) {
/* 120 */       return new File(url.getPath());
/*     */     } 
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\ClassPathIndexFile.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */