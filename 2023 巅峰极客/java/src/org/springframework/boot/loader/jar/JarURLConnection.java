/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.ByteArrayOutputStream;
/*     */ import java.io.FileNotFoundException;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import java.net.JarURLConnection;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.net.URLConnection;
/*     */ import java.net.URLEncoder;
/*     */ import java.net.URLStreamHandler;
/*     */ import java.security.Permission;
/*     */ import java.util.jar.JarEntry;
/*     */ import java.util.jar.JarFile;
/*     */ 
/*     */ final class JarURLConnection extends JarURLConnection {
/*  40 */   private static ThreadLocal<Boolean> useFastExceptions = new ThreadLocal<>();
/*     */   
/*  42 */   private static final FileNotFoundException FILE_NOT_FOUND_EXCEPTION = new FileNotFoundException("Jar file or entry not found");
/*     */   
/*  45 */   private static final IllegalStateException NOT_FOUND_CONNECTION_EXCEPTION = new IllegalStateException(FILE_NOT_FOUND_EXCEPTION);
/*     */   
/*     */   private static final String SEPARATOR = "!/";
/*     */   
/*     */   private static final URL EMPTY_JAR_URL;
/*     */   
/*     */   static {
/*     */     try {
/*  54 */       EMPTY_JAR_URL = new URL("jar:", null, 0, "file:!/", new URLStreamHandler() {
/*     */             protected URLConnection openConnection(URL u) throws IOException {
/*  59 */               return null;
/*     */             }
/*     */           });
/*  63 */     } catch (MalformedURLException ex) {
/*  64 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*  68 */   private static final JarEntryName EMPTY_JAR_ENTRY_NAME = new JarEntryName(new StringSequence(""));
/*     */   
/*  70 */   private static final JarURLConnection NOT_FOUND_CONNECTION = notFound();
/*     */   
/*     */   private final AbstractJarFile jarFile;
/*     */   
/*     */   private Permission permission;
/*     */   
/*     */   private URL jarFileUrl;
/*     */   
/*     */   private final JarEntryName jarEntryName;
/*     */   
/*     */   private JarEntry jarEntry;
/*     */   
/*     */   private JarURLConnection(URL url, AbstractJarFile jarFile, JarEntryName jarEntryName) throws IOException {
/*  84 */     super(EMPTY_JAR_URL);
/*  85 */     this.url = url;
/*  86 */     this.jarFile = jarFile;
/*  87 */     this.jarEntryName = jarEntryName;
/*     */   }
/*     */   
/*     */   public void connect() throws IOException {
/*  92 */     if (this.jarFile == null)
/*  93 */       throw FILE_NOT_FOUND_EXCEPTION; 
/*  95 */     if (!this.jarEntryName.isEmpty() && this.jarEntry == null) {
/*  96 */       this.jarEntry = this.jarFile.getJarEntry(getEntryName());
/*  97 */       if (this.jarEntry == null)
/*  98 */         throwFileNotFound(this.jarEntryName, this.jarFile); 
/*     */     } 
/* 101 */     this.connected = true;
/*     */   }
/*     */   
/*     */   public JarFile getJarFile() throws IOException {
/* 106 */     connect();
/* 107 */     return this.jarFile;
/*     */   }
/*     */   
/*     */   public URL getJarFileURL() {
/* 112 */     if (this.jarFile == null)
/* 113 */       throw NOT_FOUND_CONNECTION_EXCEPTION; 
/* 115 */     if (this.jarFileUrl == null)
/* 116 */       this.jarFileUrl = buildJarFileUrl(); 
/* 118 */     return this.jarFileUrl;
/*     */   }
/*     */   
/*     */   private URL buildJarFileUrl() {
/*     */     try {
/* 123 */       String spec = this.jarFile.getUrl().getFile();
/* 124 */       if (spec.endsWith("!/"))
/* 125 */         spec = spec.substring(0, spec.length() - "!/".length()); 
/* 127 */       if (!spec.contains("!/"))
/* 128 */         return new URL(spec); 
/* 130 */       return new URL("jar:" + spec);
/* 132 */     } catch (MalformedURLException ex) {
/* 133 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   public JarEntry getJarEntry() throws IOException {
/* 139 */     if (this.jarEntryName == null || this.jarEntryName.isEmpty())
/* 140 */       return null; 
/* 142 */     connect();
/* 143 */     return this.jarEntry;
/*     */   }
/*     */   
/*     */   public String getEntryName() {
/* 148 */     if (this.jarFile == null)
/* 149 */       throw NOT_FOUND_CONNECTION_EXCEPTION; 
/* 151 */     return this.jarEntryName.toString();
/*     */   }
/*     */   
/*     */   public InputStream getInputStream() throws IOException {
/* 156 */     if (this.jarFile == null)
/* 157 */       throw FILE_NOT_FOUND_EXCEPTION; 
/* 159 */     if (this.jarEntryName.isEmpty() && this.jarFile.getType() == AbstractJarFile.JarFileType.DIRECT)
/* 160 */       throw new IOException("no entry name specified"); 
/* 162 */     connect();
/* 164 */     InputStream inputStream = this.jarEntryName.isEmpty() ? this.jarFile.getInputStream() : this.jarFile.getInputStream(this.jarEntry);
/* 165 */     if (inputStream == null)
/* 166 */       throwFileNotFound(this.jarEntryName, this.jarFile); 
/* 168 */     return inputStream;
/*     */   }
/*     */   
/*     */   private void throwFileNotFound(Object entry, AbstractJarFile jarFile) throws FileNotFoundException {
/* 172 */     if (Boolean.TRUE.equals(useFastExceptions.get()))
/* 173 */       throw FILE_NOT_FOUND_EXCEPTION; 
/* 175 */     throw new FileNotFoundException("JAR entry " + entry + " not found in " + jarFile.getName());
/*     */   }
/*     */   
/*     */   public int getContentLength() {
/* 180 */     long length = getContentLengthLong();
/* 181 */     if (length > 2147483647L)
/* 182 */       return -1; 
/* 184 */     return (int)length;
/*     */   }
/*     */   
/*     */   public long getContentLengthLong() {
/* 189 */     if (this.jarFile == null)
/* 190 */       return -1L; 
/*     */     try {
/* 193 */       if (this.jarEntryName.isEmpty())
/* 194 */         return this.jarFile.size(); 
/* 196 */       JarEntry entry = getJarEntry();
/* 197 */       return (entry != null) ? (int)entry.getSize() : -1L;
/* 199 */     } catch (IOException ex) {
/* 200 */       return -1L;
/*     */     } 
/*     */   }
/*     */   
/*     */   public Object getContent() throws IOException {
/* 206 */     connect();
/* 207 */     return this.jarEntryName.isEmpty() ? this.jarFile : super.getContent();
/*     */   }
/*     */   
/*     */   public String getContentType() {
/* 212 */     return (this.jarEntryName != null) ? this.jarEntryName.getContentType() : null;
/*     */   }
/*     */   
/*     */   public Permission getPermission() throws IOException {
/* 217 */     if (this.jarFile == null)
/* 218 */       throw FILE_NOT_FOUND_EXCEPTION; 
/* 220 */     if (this.permission == null)
/* 221 */       this.permission = this.jarFile.getPermission(); 
/* 223 */     return this.permission;
/*     */   }
/*     */   
/*     */   public long getLastModified() {
/* 228 */     if (this.jarFile == null || this.jarEntryName.isEmpty())
/* 229 */       return 0L; 
/*     */     try {
/* 232 */       JarEntry entry = getJarEntry();
/* 233 */       return (entry != null) ? entry.getTime() : 0L;
/* 235 */     } catch (IOException ex) {
/* 236 */       return 0L;
/*     */     } 
/*     */   }
/*     */   
/*     */   static void setUseFastExceptions(boolean useFastExceptions) {
/* 241 */     JarURLConnection.useFastExceptions.set(Boolean.valueOf(useFastExceptions));
/*     */   }
/*     */   
/*     */   static JarURLConnection get(URL url, JarFile jarFile) throws IOException {
/* 245 */     StringSequence spec = new StringSequence(url.getFile());
/* 246 */     int index = indexOfRootSpec(spec, jarFile.getPathFromRoot());
/* 247 */     if (index == -1)
/* 248 */       return Boolean.TRUE.equals(useFastExceptions.get()) ? NOT_FOUND_CONNECTION : new JarURLConnection(url, null, EMPTY_JAR_ENTRY_NAME); 
/*     */     int separator;
/* 252 */     while ((separator = spec.indexOf("!/", index)) > 0) {
/* 253 */       JarEntryName entryName = JarEntryName.get(spec.subSequence(index, separator));
/* 254 */       JarEntry jarEntry = jarFile.getJarEntry(entryName.toCharSequence());
/* 255 */       if (jarEntry == null)
/* 256 */         return notFound(jarFile, entryName); 
/* 258 */       jarFile = jarFile.getNestedJarFile(jarEntry);
/* 259 */       index = separator + "!/".length();
/*     */     } 
/* 261 */     JarEntryName jarEntryName = JarEntryName.get(spec, index);
/* 262 */     if (Boolean.TRUE.equals(useFastExceptions.get()) && !jarEntryName.isEmpty() && 
/* 263 */       !jarFile.containsEntry(jarEntryName.toString()))
/* 264 */       return NOT_FOUND_CONNECTION; 
/* 266 */     return new JarURLConnection(url, jarFile.getWrapper(), jarEntryName);
/*     */   }
/*     */   
/*     */   private static int indexOfRootSpec(StringSequence file, String pathFromRoot) {
/* 270 */     int separatorIndex = file.indexOf("!/");
/* 271 */     if (separatorIndex < 0 || !file.startsWith(pathFromRoot, separatorIndex))
/* 272 */       return -1; 
/* 274 */     return separatorIndex + "!/".length() + pathFromRoot.length();
/*     */   }
/*     */   
/*     */   private static JarURLConnection notFound() {
/*     */     try {
/* 279 */       return notFound((JarFile)null, (JarEntryName)null);
/* 281 */     } catch (IOException ex) {
/* 282 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   private static JarURLConnection notFound(JarFile jarFile, JarEntryName jarEntryName) throws IOException {
/* 287 */     if (Boolean.TRUE.equals(useFastExceptions.get()))
/* 288 */       return NOT_FOUND_CONNECTION; 
/* 290 */     return new JarURLConnection(null, jarFile, jarEntryName);
/*     */   }
/*     */   
/*     */   static class JarEntryName {
/*     */     private final StringSequence name;
/*     */     
/*     */     private String contentType;
/*     */     
/*     */     JarEntryName(StringSequence spec) {
/* 303 */       this.name = decode(spec);
/*     */     }
/*     */     
/*     */     private StringSequence decode(StringSequence source) {
/* 307 */       if (source.isEmpty() || source.indexOf('%') < 0)
/* 308 */         return source; 
/* 310 */       ByteArrayOutputStream bos = new ByteArrayOutputStream(source.length());
/* 311 */       write(source.toString(), bos);
/* 313 */       return new StringSequence(AsciiBytes.toString(bos.toByteArray()));
/*     */     }
/*     */     
/*     */     private void write(String source, ByteArrayOutputStream outputStream) {
/* 317 */       int length = source.length();
/* 318 */       for (int i = 0; i < length; i++) {
/* 319 */         int c = source.charAt(i);
/* 320 */         if (c > 127) {
/*     */           try {
/* 322 */             String encoded = URLEncoder.encode(String.valueOf((char)c), "UTF-8");
/* 323 */             write(encoded, outputStream);
/* 325 */           } catch (UnsupportedEncodingException ex) {
/* 326 */             throw new IllegalStateException(ex);
/*     */           } 
/*     */         } else {
/* 330 */           if (c == 37) {
/* 331 */             if (i + 2 >= length)
/* 332 */               throw new IllegalArgumentException("Invalid encoded sequence \"" + source
/* 333 */                   .substring(i) + "\""); 
/* 335 */             c = decodeEscapeSequence(source, i);
/* 336 */             i += 2;
/*     */           } 
/* 338 */           outputStream.write(c);
/*     */         } 
/*     */       } 
/*     */     }
/*     */     
/*     */     private char decodeEscapeSequence(String source, int i) {
/* 344 */       int hi = Character.digit(source.charAt(i + 1), 16);
/* 345 */       int lo = Character.digit(source.charAt(i + 2), 16);
/* 346 */       if (hi == -1 || lo == -1)
/* 347 */         throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\""); 
/* 349 */       return (char)((hi << 4) + lo);
/*     */     }
/*     */     
/*     */     CharSequence toCharSequence() {
/* 353 */       return this.name;
/*     */     }
/*     */     
/*     */     public String toString() {
/* 358 */       return this.name.toString();
/*     */     }
/*     */     
/*     */     boolean isEmpty() {
/* 362 */       return this.name.isEmpty();
/*     */     }
/*     */     
/*     */     String getContentType() {
/* 366 */       if (this.contentType == null)
/* 367 */         this.contentType = deduceContentType(); 
/* 369 */       return this.contentType;
/*     */     }
/*     */     
/*     */     private String deduceContentType() {
/* 374 */       String type = isEmpty() ? "x-java/jar" : null;
/* 375 */       type = (type != null) ? type : URLConnection.guessContentTypeFromName(toString());
/* 376 */       type = (type != null) ? type : "content/unknown";
/* 377 */       return type;
/*     */     }
/*     */     
/*     */     static JarEntryName get(StringSequence spec) {
/* 381 */       return get(spec, 0);
/*     */     }
/*     */     
/*     */     static JarEntryName get(StringSequence spec, int beginIndex) {
/* 385 */       if (spec.length() <= beginIndex)
/* 386 */         return JarURLConnection.EMPTY_JAR_ENTRY_NAME; 
/* 388 */       return new JarEntryName(spec.subSequence(beginIndex));
/*     */     }
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\JarURLConnection.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */