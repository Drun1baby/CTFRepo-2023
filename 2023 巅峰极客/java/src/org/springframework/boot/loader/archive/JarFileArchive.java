/*     */ package org.springframework.boot.loader.archive;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.OutputStream;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.nio.file.FileSystem;
/*     */ import java.nio.file.Files;
/*     */ import java.nio.file.OpenOption;
/*     */ import java.nio.file.Path;
/*     */ import java.nio.file.Paths;
/*     */ import java.nio.file.StandardOpenOption;
/*     */ import java.nio.file.attribute.FileAttribute;
/*     */ import java.nio.file.attribute.PosixFilePermission;
/*     */ import java.nio.file.attribute.PosixFilePermissions;
/*     */ import java.util.EnumSet;
/*     */ import java.util.Iterator;
/*     */ import java.util.UUID;
/*     */ import java.util.jar.JarEntry;
/*     */ import java.util.jar.Manifest;
/*     */ import org.springframework.boot.loader.jar.JarFile;
/*     */ 
/*     */ public class JarFileArchive implements Archive {
/*     */   private static final String UNPACK_MARKER = "UNPACK:";
/*     */   
/*     */   private static final int BUFFER_SIZE = 32768;
/*     */   
/*  54 */   private static final FileAttribute<?>[] NO_FILE_ATTRIBUTES = (FileAttribute<?>[])new FileAttribute[0];
/*     */   
/*  56 */   private static final EnumSet<PosixFilePermission> DIRECTORY_PERMISSIONS = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
/*     */   
/*  59 */   private static final EnumSet<PosixFilePermission> FILE_PERMISSIONS = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
/*     */   
/*     */   private final JarFile jarFile;
/*     */   
/*     */   private URL url;
/*     */   
/*     */   private Path tempUnpackDirectory;
/*     */   
/*     */   public JarFileArchive(File file) throws IOException {
/*  69 */     this(file, file.toURI().toURL());
/*     */   }
/*     */   
/*     */   public JarFileArchive(File file, URL url) throws IOException {
/*  73 */     this(new JarFile(file));
/*  74 */     this.url = url;
/*     */   }
/*     */   
/*     */   public JarFileArchive(JarFile jarFile) {
/*  78 */     this.jarFile = jarFile;
/*     */   }
/*     */   
/*     */   public URL getUrl() throws MalformedURLException {
/*  83 */     if (this.url != null)
/*  84 */       return this.url; 
/*  86 */     return this.jarFile.getUrl();
/*     */   }
/*     */   
/*     */   public Manifest getManifest() throws IOException {
/*  91 */     return this.jarFile.getManifest();
/*     */   }
/*     */   
/*     */   public Iterator<Archive> getNestedArchives(Archive.EntryFilter searchFilter, Archive.EntryFilter includeFilter) throws IOException {
/*  96 */     return new NestedArchiveIterator(this.jarFile.iterator(), searchFilter, includeFilter);
/*     */   }
/*     */   
/*     */   @Deprecated
/*     */   public Iterator<Archive.Entry> iterator() {
/* 102 */     return new EntryIterator(this.jarFile.iterator(), null, null);
/*     */   }
/*     */   
/*     */   public void close() throws IOException {
/* 107 */     this.jarFile.close();
/*     */   }
/*     */   
/*     */   protected Archive getNestedArchive(Archive.Entry entry) throws IOException {
/* 111 */     JarEntry jarEntry = ((JarFileEntry)entry).getJarEntry();
/* 112 */     if (jarEntry.getComment().startsWith("UNPACK:"))
/* 113 */       return getUnpackedNestedArchive(jarEntry); 
/*     */     try {
/* 116 */       JarFile jarFile = this.jarFile.getNestedJarFile(jarEntry);
/* 117 */       return new JarFileArchive(jarFile);
/* 119 */     } catch (Exception ex) {
/* 120 */       throw new IllegalStateException("Failed to get nested archive for entry " + entry.getName(), ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   private Archive getUnpackedNestedArchive(JarEntry jarEntry) throws IOException {
/* 125 */     String name = jarEntry.getName();
/* 126 */     if (name.lastIndexOf('/') != -1)
/* 127 */       name = name.substring(name.lastIndexOf('/') + 1); 
/* 129 */     Path path = getTempUnpackDirectory().resolve(name);
/* 130 */     if (!Files.exists(path, new java.nio.file.LinkOption[0]) || Files.size(path) != jarEntry.getSize())
/* 131 */       unpack(jarEntry, path); 
/* 133 */     return new JarFileArchive(path.toFile(), path.toUri().toURL());
/*     */   }
/*     */   
/*     */   private Path getTempUnpackDirectory() {
/* 137 */     if (this.tempUnpackDirectory == null) {
/* 138 */       Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"), new String[0]);
/* 139 */       this.tempUnpackDirectory = createUnpackDirectory(tempDirectory);
/*     */     } 
/* 141 */     return this.tempUnpackDirectory;
/*     */   }
/*     */   
/*     */   private Path createUnpackDirectory(Path parent) {
/* 145 */     int attempts = 0;
/* 146 */     while (attempts++ < 1000) {
/* 147 */       String fileName = Paths.get(this.jarFile.getName(), new String[0]).getFileName().toString();
/* 148 */       Path unpackDirectory = parent.resolve(fileName + "-spring-boot-libs-" + UUID.randomUUID());
/*     */       try {
/* 150 */         createDirectory(unpackDirectory);
/* 151 */         return unpackDirectory;
/* 153 */       } catch (IOException iOException) {}
/*     */     } 
/* 156 */     throw new IllegalStateException("Failed to create unpack directory in directory '" + parent + "'");
/*     */   }
/*     */   
/*     */   private void unpack(JarEntry entry, Path path) throws IOException {
/* 160 */     createFile(path);
/* 161 */     path.toFile().deleteOnExit();
/* 162 */     try(InputStream inputStream = this.jarFile.getInputStream(entry); 
/* 163 */         OutputStream outputStream = Files.newOutputStream(path, new OpenOption[] { StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING })) {
/* 165 */       byte[] buffer = new byte[32768];
/*     */       int bytesRead;
/* 167 */       while ((bytesRead = inputStream.read(buffer)) != -1)
/* 168 */         outputStream.write(buffer, 0, bytesRead); 
/* 170 */       outputStream.flush();
/*     */     } 
/*     */   }
/*     */   
/*     */   private void createDirectory(Path path) throws IOException {
/* 175 */     Files.createDirectory(path, getFileAttributes(path.getFileSystem(), DIRECTORY_PERMISSIONS));
/*     */   }
/*     */   
/*     */   private void createFile(Path path) throws IOException {
/* 179 */     Files.createFile(path, getFileAttributes(path.getFileSystem(), FILE_PERMISSIONS));
/*     */   }
/*     */   
/*     */   private FileAttribute<?>[] getFileAttributes(FileSystem fileSystem, EnumSet<PosixFilePermission> ownerReadWrite) {
/* 183 */     if (!fileSystem.supportedFileAttributeViews().contains("posix"))
/* 184 */       return NO_FILE_ATTRIBUTES; 
/* 186 */     return (FileAttribute<?>[])new FileAttribute[] { PosixFilePermissions.asFileAttribute(ownerReadWrite) };
/*     */   }
/*     */   
/*     */   public String toString() {
/*     */     try {
/* 192 */       return getUrl().toString();
/* 194 */     } catch (Exception ex) {
/* 195 */       return "jar archive";
/*     */     } 
/*     */   }
/*     */   
/*     */   private static abstract class AbstractIterator<T> implements Iterator<T> {
/*     */     private final Iterator<JarEntry> iterator;
/*     */     
/*     */     private final Archive.EntryFilter searchFilter;
/*     */     
/*     */     private final Archive.EntryFilter includeFilter;
/*     */     
/*     */     private Archive.Entry current;
/*     */     
/*     */     AbstractIterator(Iterator<JarEntry> iterator, Archive.EntryFilter searchFilter, Archive.EntryFilter includeFilter) {
/* 213 */       this.iterator = iterator;
/* 214 */       this.searchFilter = searchFilter;
/* 215 */       this.includeFilter = includeFilter;
/* 216 */       this.current = poll();
/*     */     }
/*     */     
/*     */     public boolean hasNext() {
/* 221 */       return (this.current != null);
/*     */     }
/*     */     
/*     */     public T next() {
/* 226 */       T result = adapt(this.current);
/* 227 */       this.current = poll();
/* 228 */       return result;
/*     */     }
/*     */     
/*     */     private Archive.Entry poll() {
/* 232 */       while (this.iterator.hasNext()) {
/* 233 */         JarFileArchive.JarFileEntry candidate = new JarFileArchive.JarFileEntry(this.iterator.next());
/* 234 */         if ((this.searchFilter == null || this.searchFilter.matches(candidate)) && (this.includeFilter == null || this.includeFilter
/* 235 */           .matches(candidate)))
/* 236 */           return candidate; 
/*     */       } 
/* 239 */       return null;
/*     */     }
/*     */     
/*     */     protected abstract T adapt(Archive.Entry param1Entry);
/*     */   }
/*     */   
/*     */   private static class EntryIterator extends AbstractIterator<Archive.Entry> {
/*     */     EntryIterator(Iterator<JarEntry> iterator, Archive.EntryFilter searchFilter, Archive.EntryFilter includeFilter) {
/* 252 */       super(iterator, searchFilter, includeFilter);
/*     */     }
/*     */     
/*     */     protected Archive.Entry adapt(Archive.Entry entry) {
/* 257 */       return entry;
/*     */     }
/*     */   }
/*     */   
/*     */   private class NestedArchiveIterator extends AbstractIterator<Archive> {
/*     */     NestedArchiveIterator(Iterator<JarEntry> iterator, Archive.EntryFilter searchFilter, Archive.EntryFilter includeFilter) {
/* 268 */       super(iterator, searchFilter, includeFilter);
/*     */     }
/*     */     
/*     */     protected Archive adapt(Archive.Entry entry) {
/*     */       try {
/* 274 */         return JarFileArchive.this.getNestedArchive(entry);
/* 276 */       } catch (IOException ex) {
/* 277 */         throw new IllegalStateException(ex);
/*     */       } 
/*     */     }
/*     */   }
/*     */   
/*     */   private static class JarFileEntry implements Archive.Entry {
/*     */     private final JarEntry jarEntry;
/*     */     
/*     */     JarFileEntry(JarEntry jarEntry) {
/* 291 */       this.jarEntry = jarEntry;
/*     */     }
/*     */     
/*     */     JarEntry getJarEntry() {
/* 295 */       return this.jarEntry;
/*     */     }
/*     */     
/*     */     public boolean isDirectory() {
/* 300 */       return this.jarEntry.isDirectory();
/*     */     }
/*     */     
/*     */     public String getName() {
/* 305 */       return this.jarEntry.getName();
/*     */     }
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\archive\JarFileArchive.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */