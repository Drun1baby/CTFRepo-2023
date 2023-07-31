/*     */ package org.springframework.boot.loader.archive;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.FileInputStream;
/*     */ import java.io.IOException;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URI;
/*     */ import java.net.URL;
/*     */ import java.util.Arrays;
/*     */ import java.util.Collections;
/*     */ import java.util.Comparator;
/*     */ import java.util.Deque;
/*     */ import java.util.HashSet;
/*     */ import java.util.Iterator;
/*     */ import java.util.LinkedList;
/*     */ import java.util.NoSuchElementException;
/*     */ import java.util.Set;
/*     */ import java.util.jar.Manifest;
/*     */ 
/*     */ public class ExplodedArchive implements Archive {
/*  46 */   private static final Set<String> SKIPPED_NAMES = new HashSet<>(Arrays.asList(new String[] { ".", ".." }));
/*     */   
/*     */   private final File root;
/*     */   
/*     */   private final boolean recursive;
/*     */   
/*     */   private File manifestFile;
/*     */   
/*     */   private Manifest manifest;
/*     */   
/*     */   public ExplodedArchive(File root) {
/*  61 */     this(root, true);
/*     */   }
/*     */   
/*     */   public ExplodedArchive(File root, boolean recursive) {
/*  72 */     if (!root.exists() || !root.isDirectory())
/*  73 */       throw new IllegalArgumentException("Invalid source directory " + root); 
/*  75 */     this.root = root;
/*  76 */     this.recursive = recursive;
/*  77 */     this.manifestFile = getManifestFile(root);
/*     */   }
/*     */   
/*     */   private File getManifestFile(File root) {
/*  81 */     File metaInf = new File(root, "META-INF");
/*  82 */     return new File(metaInf, "MANIFEST.MF");
/*     */   }
/*     */   
/*     */   public URL getUrl() throws MalformedURLException {
/*  87 */     return this.root.toURI().toURL();
/*     */   }
/*     */   
/*     */   public Manifest getManifest() throws IOException {
/*  92 */     if (this.manifest == null && this.manifestFile.exists())
/*  93 */       try (FileInputStream inputStream = new FileInputStream(this.manifestFile)) {
/*  94 */         this.manifest = new Manifest(inputStream);
/*     */       }  
/*  97 */     return this.manifest;
/*     */   }
/*     */   
/*     */   public Iterator<Archive> getNestedArchives(Archive.EntryFilter searchFilter, Archive.EntryFilter includeFilter) throws IOException {
/* 102 */     return new ArchiveIterator(this.root, this.recursive, searchFilter, includeFilter);
/*     */   }
/*     */   
/*     */   @Deprecated
/*     */   public Iterator<Archive.Entry> iterator() {
/* 108 */     return new EntryIterator(this.root, this.recursive, null, null);
/*     */   }
/*     */   
/*     */   protected Archive getNestedArchive(Archive.Entry entry) throws IOException {
/* 112 */     File file = ((FileEntry)entry).getFile();
/* 113 */     return file.isDirectory() ? new ExplodedArchive(file) : new SimpleJarFileArchive((FileEntry)entry);
/*     */   }
/*     */   
/*     */   public boolean isExploded() {
/* 118 */     return true;
/*     */   }
/*     */   
/*     */   public String toString() {
/*     */     try {
/* 124 */       return getUrl().toString();
/* 126 */     } catch (Exception ex) {
/* 127 */       return "exploded archive";
/*     */     } 
/*     */   }
/*     */   
/*     */   private static abstract class AbstractIterator<T> implements Iterator<T> {
/* 136 */     private static final Comparator<File> entryComparator = Comparator.comparing(File::getAbsolutePath);
/*     */     
/*     */     private final File root;
/*     */     
/*     */     private final boolean recursive;
/*     */     
/*     */     private final Archive.EntryFilter searchFilter;
/*     */     
/*     */     private final Archive.EntryFilter includeFilter;
/*     */     
/* 146 */     private final Deque<Iterator<File>> stack = new LinkedList<>();
/*     */     
/*     */     private ExplodedArchive.FileEntry current;
/*     */     
/*     */     private String rootUrl;
/*     */     
/*     */     AbstractIterator(File root, boolean recursive, Archive.EntryFilter searchFilter, Archive.EntryFilter includeFilter) {
/* 153 */       this.root = root;
/* 154 */       this.rootUrl = this.root.toURI().getPath();
/* 155 */       this.recursive = recursive;
/* 156 */       this.searchFilter = searchFilter;
/* 157 */       this.includeFilter = includeFilter;
/* 158 */       this.stack.add(listFiles(root));
/* 159 */       this.current = poll();
/*     */     }
/*     */     
/*     */     public boolean hasNext() {
/* 164 */       return (this.current != null);
/*     */     }
/*     */     
/*     */     public T next() {
/* 169 */       ExplodedArchive.FileEntry entry = this.current;
/* 170 */       if (entry == null)
/* 171 */         throw new NoSuchElementException(); 
/* 173 */       this.current = poll();
/* 174 */       return adapt(entry);
/*     */     }
/*     */     
/*     */     private ExplodedArchive.FileEntry poll() {
/* 178 */       while (!this.stack.isEmpty()) {
/* 179 */         while (((Iterator)this.stack.peek()).hasNext()) {
/* 180 */           File file = ((Iterator<File>)this.stack.peek()).next();
/* 181 */           if (ExplodedArchive.SKIPPED_NAMES.contains(file.getName()))
/*     */             continue; 
/* 184 */           ExplodedArchive.FileEntry entry = getFileEntry(file);
/* 185 */           if (isListable(entry))
/* 186 */             this.stack.addFirst(listFiles(file)); 
/* 188 */           if (this.includeFilter == null || this.includeFilter.matches(entry))
/* 189 */             return entry; 
/*     */         } 
/* 192 */         this.stack.poll();
/*     */       } 
/* 194 */       return null;
/*     */     }
/*     */     
/*     */     private ExplodedArchive.FileEntry getFileEntry(File file) {
/* 198 */       URI uri = file.toURI();
/* 199 */       String name = uri.getPath().substring(this.rootUrl.length());
/*     */       try {
/* 201 */         return new ExplodedArchive.FileEntry(name, file, uri.toURL());
/* 203 */       } catch (MalformedURLException ex) {
/* 204 */         throw new IllegalStateException(ex);
/*     */       } 
/*     */     }
/*     */     
/*     */     private boolean isListable(ExplodedArchive.FileEntry entry) {
/* 209 */       return (entry.isDirectory() && (this.recursive || entry.getFile().getParentFile().equals(this.root)) && (this.searchFilter == null || this.searchFilter
/* 210 */         .matches(entry)) && (this.includeFilter == null || 
/* 211 */         !this.includeFilter.matches(entry)));
/*     */     }
/*     */     
/*     */     private Iterator<File> listFiles(File file) {
/* 215 */       File[] files = file.listFiles();
/* 216 */       if (files == null)
/* 217 */         return Collections.emptyIterator(); 
/* 219 */       Arrays.sort(files, entryComparator);
/* 220 */       return Arrays.<File>asList(files).iterator();
/*     */     }
/*     */     
/*     */     public void remove() {
/* 225 */       throw new UnsupportedOperationException("remove");
/*     */     }
/*     */     
/*     */     protected abstract T adapt(ExplodedArchive.FileEntry param1FileEntry);
/*     */   }
/*     */   
/*     */   private static class EntryIterator extends AbstractIterator<Archive.Entry> {
/*     */     EntryIterator(File root, boolean recursive, Archive.EntryFilter searchFilter, Archive.EntryFilter includeFilter) {
/* 235 */       super(root, recursive, searchFilter, includeFilter);
/*     */     }
/*     */     
/*     */     protected Archive.Entry adapt(ExplodedArchive.FileEntry entry) {
/* 240 */       return entry;
/*     */     }
/*     */   }
/*     */   
/*     */   private static class ArchiveIterator extends AbstractIterator<Archive> {
/*     */     ArchiveIterator(File root, boolean recursive, Archive.EntryFilter searchFilter, Archive.EntryFilter includeFilter) {
/* 248 */       super(root, recursive, searchFilter, includeFilter);
/*     */     }
/*     */     
/*     */     protected Archive adapt(ExplodedArchive.FileEntry entry) {
/* 253 */       File file = entry.getFile();
/* 254 */       return file.isDirectory() ? new ExplodedArchive(file) : new ExplodedArchive.SimpleJarFileArchive(entry);
/*     */     }
/*     */   }
/*     */   
/*     */   private static class FileEntry implements Archive.Entry {
/*     */     private final String name;
/*     */     
/*     */     private final File file;
/*     */     
/*     */     private final URL url;
/*     */     
/*     */     FileEntry(String name, File file, URL url) {
/* 271 */       this.name = name;
/* 272 */       this.file = file;
/* 273 */       this.url = url;
/*     */     }
/*     */     
/*     */     File getFile() {
/* 277 */       return this.file;
/*     */     }
/*     */     
/*     */     public boolean isDirectory() {
/* 282 */       return this.file.isDirectory();
/*     */     }
/*     */     
/*     */     public String getName() {
/* 287 */       return this.name;
/*     */     }
/*     */     
/*     */     URL getUrl() {
/* 291 */       return this.url;
/*     */     }
/*     */   }
/*     */   
/*     */   private static class SimpleJarFileArchive implements Archive {
/*     */     private final URL url;
/*     */     
/*     */     SimpleJarFileArchive(ExplodedArchive.FileEntry file) {
/* 305 */       this.url = file.getUrl();
/*     */     }
/*     */     
/*     */     public URL getUrl() throws MalformedURLException {
/* 310 */       return this.url;
/*     */     }
/*     */     
/*     */     public Manifest getManifest() throws IOException {
/* 315 */       return null;
/*     */     }
/*     */     
/*     */     public Iterator<Archive> getNestedArchives(Archive.EntryFilter searchFilter, Archive.EntryFilter includeFilter) throws IOException {
/* 321 */       return Collections.emptyIterator();
/*     */     }
/*     */     
/*     */     @Deprecated
/*     */     public Iterator<Archive.Entry> iterator() {
/* 327 */       return Collections.emptyIterator();
/*     */     }
/*     */     
/*     */     public String toString() {
/*     */       try {
/* 333 */         return getUrl().toString();
/* 335 */       } catch (Exception ex) {
/* 336 */         return "jar archive";
/*     */       } 
/*     */     }
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\archive\ExplodedArchive.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */