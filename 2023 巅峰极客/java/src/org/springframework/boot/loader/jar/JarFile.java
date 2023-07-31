/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.FilePermission;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.lang.ref.SoftReference;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.security.Permission;
/*     */ import java.util.Enumeration;
/*     */ import java.util.Iterator;
/*     */ import java.util.Spliterator;
/*     */ import java.util.Spliterators;
/*     */ import java.util.function.Supplier;
/*     */ import java.util.jar.JarEntry;
/*     */ import java.util.jar.Manifest;
/*     */ import java.util.stream.Stream;
/*     */ import java.util.stream.StreamSupport;
/*     */ import java.util.zip.ZipEntry;
/*     */ import org.springframework.boot.loader.data.RandomAccessData;
/*     */ import org.springframework.boot.loader.data.RandomAccessDataFile;
/*     */ 
/*     */ public class JarFile extends AbstractJarFile implements Iterable<JarEntry> {
/*     */   private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
/*     */   
/*     */   private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";
/*     */   
/*     */   private static final String HANDLERS_PACKAGE = "org.springframework.boot.loader";
/*     */   
/*  64 */   private static final AsciiBytes META_INF = new AsciiBytes("META-INF/");
/*     */   
/*  66 */   private static final AsciiBytes SIGNATURE_FILE_EXTENSION = new AsciiBytes(".SF");
/*     */   
/*     */   private static final String READ_ACTION = "read";
/*     */   
/*     */   private final RandomAccessDataFile rootFile;
/*     */   
/*     */   private final String pathFromRoot;
/*     */   
/*     */   private final RandomAccessData data;
/*     */   
/*     */   private final AbstractJarFile.JarFileType type;
/*     */   
/*     */   private URL url;
/*     */   
/*     */   private String urlString;
/*     */   
/*     */   private JarFileEntries entries;
/*     */   
/*     */   private Supplier<Manifest> manifestSupplier;
/*     */   
/*     */   private SoftReference<Manifest> manifest;
/*     */   
/*     */   private boolean signed;
/*     */   
/*     */   private String comment;
/*     */   
/*     */   private volatile boolean closed;
/*     */   
/*     */   private volatile JarFileWrapper wrapper;
/*     */   
/*     */   public JarFile(File file) throws IOException {
/* 102 */     this(new RandomAccessDataFile(file));
/*     */   }
/*     */   
/*     */   JarFile(RandomAccessDataFile file) throws IOException {
/* 111 */     this(file, "", (RandomAccessData)file, AbstractJarFile.JarFileType.DIRECT);
/*     */   }
/*     */   
/*     */   private JarFile(RandomAccessDataFile rootFile, String pathFromRoot, RandomAccessData data, AbstractJarFile.JarFileType type) throws IOException {
/* 125 */     this(rootFile, pathFromRoot, data, null, type, null);
/*     */   }
/*     */   
/*     */   private JarFile(RandomAccessDataFile rootFile, String pathFromRoot, RandomAccessData data, JarEntryFilter filter, AbstractJarFile.JarFileType type, Supplier<Manifest> manifestSupplier) throws IOException {
/* 130 */     super(rootFile.getFile());
/* 131 */     if (System.getSecurityManager() == null)
/* 132 */       super.close(); 
/* 134 */     this.rootFile = rootFile;
/* 135 */     this.pathFromRoot = pathFromRoot;
/* 136 */     CentralDirectoryParser parser = new CentralDirectoryParser();
/* 137 */     this.entries = parser.<JarFileEntries>addVisitor(new JarFileEntries(this, filter));
/* 138 */     this.type = type;
/* 139 */     parser.addVisitor(centralDirectoryVisitor());
/*     */     try {
/* 141 */       this.data = parser.parse(data, (filter == null));
/* 143 */     } catch (RuntimeException ex) {
/*     */       try {
/* 145 */         this.rootFile.close();
/* 146 */         super.close();
/* 148 */       } catch (IOException iOException) {}
/* 150 */       throw ex;
/*     */     } 
/* 152 */     this.manifestSupplier = (manifestSupplier != null) ? manifestSupplier : (() -> {
/*     */         try (InputStream inputStream = getInputStream("META-INF/MANIFEST.MF")) {
/*     */           if (inputStream == null)
/*     */             return null; 
/*     */           return new Manifest(inputStream);
/* 159 */         } catch (IOException ex) {
/*     */           throw new RuntimeException(ex);
/*     */         } 
/*     */       });
/*     */   }
/*     */   
/*     */   private CentralDirectoryVisitor centralDirectoryVisitor() {
/* 166 */     return new CentralDirectoryVisitor() {
/*     */         public void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
/* 170 */           JarFile.this.comment = endRecord.getComment();
/*     */         }
/*     */         
/*     */         public void visitFileHeader(CentralDirectoryFileHeader fileHeader, long dataOffset) {
/* 175 */           AsciiBytes name = fileHeader.getName();
/* 176 */           if (name.startsWith(JarFile.META_INF) && name.endsWith(JarFile.SIGNATURE_FILE_EXTENSION))
/* 177 */             JarFile.this.signed = true; 
/*     */         }
/*     */         
/*     */         public void visitEnd() {}
/*     */       };
/*     */   }
/*     */   
/*     */   JarFileWrapper getWrapper() throws IOException {
/* 189 */     JarFileWrapper wrapper = this.wrapper;
/* 190 */     if (wrapper == null) {
/* 191 */       wrapper = new JarFileWrapper(this);
/* 192 */       this.wrapper = wrapper;
/*     */     } 
/* 194 */     return wrapper;
/*     */   }
/*     */   
/*     */   Permission getPermission() {
/* 199 */     return new FilePermission(this.rootFile.getFile().getPath(), "read");
/*     */   }
/*     */   
/*     */   protected final RandomAccessDataFile getRootJarFile() {
/* 203 */     return this.rootFile;
/*     */   }
/*     */   
/*     */   RandomAccessData getData() {
/* 207 */     return this.data;
/*     */   }
/*     */   
/*     */   public Manifest getManifest() throws IOException {
/* 212 */     Manifest manifest = (this.manifest != null) ? this.manifest.get() : null;
/* 213 */     if (manifest == null) {
/*     */       try {
/* 215 */         manifest = this.manifestSupplier.get();
/* 217 */       } catch (RuntimeException ex) {
/* 218 */         throw new IOException(ex);
/*     */       } 
/* 220 */       this.manifest = new SoftReference<>(manifest);
/*     */     } 
/* 222 */     return manifest;
/*     */   }
/*     */   
/*     */   public Enumeration<JarEntry> entries() {
/* 227 */     return new JarEntryEnumeration(this.entries.iterator());
/*     */   }
/*     */   
/*     */   public Stream<JarEntry> stream() {
/* 232 */     Spliterator<JarEntry> spliterator = Spliterators.spliterator(iterator(), size(), 1297);
/* 234 */     return StreamSupport.stream(spliterator, false);
/*     */   }
/*     */   
/*     */   public Iterator<JarEntry> iterator() {
/* 245 */     return (Iterator)this.entries.iterator(this::ensureOpen);
/*     */   }
/*     */   
/*     */   public JarEntry getJarEntry(CharSequence name) {
/* 249 */     return this.entries.getEntry(name);
/*     */   }
/*     */   
/*     */   public JarEntry getJarEntry(String name) {
/* 254 */     return (JarEntry)getEntry(name);
/*     */   }
/*     */   
/*     */   public boolean containsEntry(String name) {
/* 258 */     return this.entries.containsEntry(name);
/*     */   }
/*     */   
/*     */   public ZipEntry getEntry(String name) {
/* 263 */     ensureOpen();
/* 264 */     return this.entries.getEntry(name);
/*     */   }
/*     */   
/*     */   InputStream getInputStream() throws IOException {
/* 269 */     return this.data.getInputStream();
/*     */   }
/*     */   
/*     */   public synchronized InputStream getInputStream(ZipEntry entry) throws IOException {
/* 274 */     ensureOpen();
/* 275 */     if (entry instanceof JarEntry)
/* 276 */       return this.entries.getInputStream((JarEntry)entry); 
/* 278 */     return getInputStream((entry != null) ? entry.getName() : null);
/*     */   }
/*     */   
/*     */   InputStream getInputStream(String name) throws IOException {
/* 282 */     return this.entries.getInputStream(name);
/*     */   }
/*     */   
/*     */   public synchronized JarFile getNestedJarFile(ZipEntry entry) throws IOException {
/* 292 */     return getNestedJarFile((JarEntry)entry);
/*     */   }
/*     */   
/*     */   public synchronized JarFile getNestedJarFile(JarEntry entry) throws IOException {
/*     */     try {
/* 303 */       return createJarFileFromEntry(entry);
/* 305 */     } catch (Exception ex) {
/* 306 */       throw new IOException("Unable to open nested jar file '" + entry.getName() + "'", ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   private JarFile createJarFileFromEntry(JarEntry entry) throws IOException {
/* 311 */     if (entry.isDirectory())
/* 312 */       return createJarFileFromDirectoryEntry(entry); 
/* 314 */     return createJarFileFromFileEntry(entry);
/*     */   }
/*     */   
/*     */   private JarFile createJarFileFromDirectoryEntry(JarEntry entry) throws IOException {
/* 318 */     AsciiBytes name = entry.getAsciiBytesName();
/* 319 */     JarEntryFilter filter = candidate -> 
/* 320 */       (candidate.startsWith(name) && !candidate.equals(name)) ? candidate.substring(name.length()) : null;
/* 325 */     return new JarFile(this.rootFile, this.pathFromRoot + "!/" + entry.getName().substring(0, name.length() - 1), this.data, filter, AbstractJarFile.JarFileType.NESTED_DIRECTORY, this.manifestSupplier);
/*     */   }
/*     */   
/*     */   private JarFile createJarFileFromFileEntry(JarEntry entry) throws IOException {
/* 330 */     if (entry.getMethod() != 0)
/* 331 */       throw new IllegalStateException("Unable to open nested entry '" + entry
/* 332 */           .getName() + "'. It has been compressed and nested jar files must be stored without compression. Please check the mechanism used to create your executable jar file"); 
/* 336 */     RandomAccessData entryData = this.entries.getEntryData(entry.getName());
/* 337 */     return new JarFile(this.rootFile, this.pathFromRoot + "!/" + entry.getName(), entryData, AbstractJarFile.JarFileType.NESTED_JAR);
/*     */   }
/*     */   
/*     */   public String getComment() {
/* 343 */     ensureOpen();
/* 344 */     return this.comment;
/*     */   }
/*     */   
/*     */   public int size() {
/* 349 */     ensureOpen();
/* 350 */     return this.entries.getSize();
/*     */   }
/*     */   
/*     */   public void close() throws IOException {
/* 355 */     if (this.closed)
/*     */       return; 
/* 358 */     super.close();
/* 359 */     if (this.type == AbstractJarFile.JarFileType.DIRECT)
/* 360 */       this.rootFile.close(); 
/* 362 */     this.closed = true;
/*     */   }
/*     */   
/*     */   private void ensureOpen() {
/* 366 */     if (this.closed)
/* 367 */       throw new IllegalStateException("zip file closed"); 
/*     */   }
/*     */   
/*     */   boolean isClosed() {
/* 372 */     return this.closed;
/*     */   }
/*     */   
/*     */   String getUrlString() throws MalformedURLException {
/* 376 */     if (this.urlString == null)
/* 377 */       this.urlString = getUrl().toString(); 
/* 379 */     return this.urlString;
/*     */   }
/*     */   
/*     */   public URL getUrl() throws MalformedURLException {
/* 384 */     if (this.url == null) {
/* 385 */       String file = this.rootFile.getFile().toURI() + this.pathFromRoot + "!/";
/* 386 */       file = file.replace("file:////", "file://");
/* 387 */       this.url = new URL("jar", "", -1, file, new Handler(this));
/*     */     } 
/* 389 */     return this.url;
/*     */   }
/*     */   
/*     */   public String toString() {
/* 394 */     return getName();
/*     */   }
/*     */   
/*     */   public String getName() {
/* 399 */     return this.rootFile.getFile() + this.pathFromRoot;
/*     */   }
/*     */   
/*     */   boolean isSigned() {
/* 403 */     return this.signed;
/*     */   }
/*     */   
/*     */   JarEntryCertification getCertification(JarEntry entry) {
/*     */     try {
/* 408 */       return this.entries.getCertification(entry);
/* 410 */     } catch (IOException ex) {
/* 411 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   public void clearCache() {
/* 416 */     this.entries.clearCache();
/*     */   }
/*     */   
/*     */   protected String getPathFromRoot() {
/* 420 */     return this.pathFromRoot;
/*     */   }
/*     */   
/*     */   AbstractJarFile.JarFileType getType() {
/* 425 */     return this.type;
/*     */   }
/*     */   
/*     */   public static void registerUrlProtocolHandler() {
/* 433 */     Handler.captureJarContextUrl();
/* 434 */     String handlers = System.getProperty("java.protocol.handler.pkgs", "");
/* 435 */     System.setProperty("java.protocol.handler.pkgs", (handlers == null || handlers
/* 436 */         .isEmpty()) ? "org.springframework.boot.loader" : (handlers + "|" + "org.springframework.boot.loader"));
/* 437 */     resetCachedUrlHandlers();
/*     */   }
/*     */   
/*     */   private static void resetCachedUrlHandlers() {
/*     */     try {
/* 447 */       URL.setURLStreamHandlerFactory(null);
/* 449 */     } catch (Error error) {}
/*     */   }
/*     */   
/*     */   private static class JarEntryEnumeration implements Enumeration<JarEntry> {
/*     */     private final Iterator<JarEntry> iterator;
/*     */     
/*     */     JarEntryEnumeration(Iterator<JarEntry> iterator) {
/* 462 */       this.iterator = iterator;
/*     */     }
/*     */     
/*     */     public boolean hasMoreElements() {
/* 467 */       return this.iterator.hasNext();
/*     */     }
/*     */     
/*     */     public JarEntry nextElement() {
/* 472 */       return this.iterator.next();
/*     */     }
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\JarFile.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */