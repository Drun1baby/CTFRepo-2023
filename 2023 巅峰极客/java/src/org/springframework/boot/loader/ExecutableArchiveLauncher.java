/*     */ package org.springframework.boot.loader;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.net.URL;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import java.util.jar.Attributes;
/*     */ import java.util.jar.Manifest;
/*     */ import org.springframework.boot.loader.archive.Archive;
/*     */ 
/*     */ public abstract class ExecutableArchiveLauncher extends Launcher {
/*     */   private static final String START_CLASS_ATTRIBUTE = "Start-Class";
/*     */   
/*     */   protected static final String BOOT_CLASSPATH_INDEX_ATTRIBUTE = "Spring-Boot-Classpath-Index";
/*     */   
/*     */   protected static final String DEFAULT_CLASSPATH_INDEX_FILE_NAME = "classpath.idx";
/*     */   
/*     */   private final Archive archive;
/*     */   
/*     */   private final ClassPathIndexFile classPathIndex;
/*     */   
/*     */   public ExecutableArchiveLauncher() {
/*     */     try {
/*  53 */       this.archive = createArchive();
/*  54 */       this.classPathIndex = getClassPathIndex(this.archive);
/*  56 */     } catch (Exception ex) {
/*  57 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   protected ExecutableArchiveLauncher(Archive archive) {
/*     */     try {
/*  63 */       this.archive = archive;
/*  64 */       this.classPathIndex = getClassPathIndex(this.archive);
/*  66 */     } catch (Exception ex) {
/*  67 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   protected ClassPathIndexFile getClassPathIndex(Archive archive) throws IOException {
/*  73 */     if (archive instanceof org.springframework.boot.loader.archive.ExplodedArchive) {
/*  74 */       String location = getClassPathIndexFileLocation(archive);
/*  75 */       return ClassPathIndexFile.loadIfPossible(archive.getUrl(), location);
/*     */     } 
/*  77 */     return null;
/*     */   }
/*     */   
/*     */   private String getClassPathIndexFileLocation(Archive archive) throws IOException {
/*  81 */     Manifest manifest = archive.getManifest();
/*  82 */     Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
/*  83 */     String location = (attributes != null) ? attributes.getValue("Spring-Boot-Classpath-Index") : null;
/*  84 */     return (location != null) ? location : (getArchiveEntryPathPrefix() + "classpath.idx");
/*     */   }
/*     */   
/*     */   protected String getMainClass() throws Exception {
/*  89 */     Manifest manifest = this.archive.getManifest();
/*  90 */     String mainClass = null;
/*  91 */     if (manifest != null)
/*  92 */       mainClass = manifest.getMainAttributes().getValue("Start-Class"); 
/*  94 */     if (mainClass == null)
/*  95 */       throw new IllegalStateException("No 'Start-Class' manifest entry specified in " + this); 
/*  97 */     return mainClass;
/*     */   }
/*     */   
/*     */   protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
/* 102 */     List<URL> urls = new ArrayList<>(guessClassPathSize());
/* 103 */     while (archives.hasNext())
/* 104 */       urls.add(((Archive)archives.next()).getUrl()); 
/* 106 */     if (this.classPathIndex != null)
/* 107 */       urls.addAll(this.classPathIndex.getUrls()); 
/* 109 */     return createClassLoader(urls.<URL>toArray(new URL[0]));
/*     */   }
/*     */   
/*     */   private int guessClassPathSize() {
/* 113 */     if (this.classPathIndex != null)
/* 114 */       return this.classPathIndex.size() + 10; 
/* 116 */     return 50;
/*     */   }
/*     */   
/*     */   protected Iterator<Archive> getClassPathArchivesIterator() throws Exception {
/* 121 */     Archive.EntryFilter searchFilter = this::isSearchCandidate;
/* 122 */     Iterator<Archive> archives = this.archive.getNestedArchives(searchFilter, entry -> 
/* 123 */         (isNestedArchive(entry) && !isEntryIndexed(entry)));
/* 124 */     if (isPostProcessingClassPathArchives())
/* 125 */       archives = applyClassPathArchivePostProcessing(archives); 
/* 127 */     return archives;
/*     */   }
/*     */   
/*     */   private boolean isEntryIndexed(Archive.Entry entry) {
/* 131 */     if (this.classPathIndex != null)
/* 132 */       return this.classPathIndex.containsEntry(entry.getName()); 
/* 134 */     return false;
/*     */   }
/*     */   
/*     */   private Iterator<Archive> applyClassPathArchivePostProcessing(Iterator<Archive> archives) throws Exception {
/* 138 */     List<Archive> list = new ArrayList<>();
/* 139 */     while (archives.hasNext())
/* 140 */       list.add(archives.next()); 
/* 142 */     postProcessClassPathArchives(list);
/* 143 */     return list.iterator();
/*     */   }
/*     */   
/*     */   protected boolean isSearchCandidate(Archive.Entry entry) {
/* 153 */     if (getArchiveEntryPathPrefix() == null)
/* 154 */       return true; 
/* 156 */     return entry.getName().startsWith(getArchiveEntryPathPrefix());
/*     */   }
/*     */   
/*     */   protected boolean isPostProcessingClassPathArchives() {
/* 176 */     return true;
/*     */   }
/*     */   
/*     */   protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {}
/*     */   
/*     */   protected String getArchiveEntryPathPrefix() {
/* 194 */     return null;
/*     */   }
/*     */   
/*     */   protected boolean isExploded() {
/* 199 */     return this.archive.isExploded();
/*     */   }
/*     */   
/*     */   protected final Archive getArchive() {
/* 204 */     return this.archive;
/*     */   }
/*     */   
/*     */   protected abstract boolean isNestedArchive(Archive.Entry paramEntry);
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\ExecutableArchiveLauncher.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */