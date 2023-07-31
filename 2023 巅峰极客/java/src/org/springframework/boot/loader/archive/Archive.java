/*     */ package org.springframework.boot.loader.archive;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import java.util.Objects;
/*     */ import java.util.Spliterator;
/*     */ import java.util.Spliterators;
/*     */ import java.util.function.Consumer;
/*     */ import java.util.jar.Manifest;
/*     */ 
/*     */ public interface Archive extends Iterable<Archive.Entry>, AutoCloseable {
/*     */   default Iterator<Archive> getNestedArchives(EntryFilter searchFilter, EntryFilter includeFilter) throws IOException {
/*  67 */     EntryFilter combinedFilter = entry -> ((searchFilter == null || searchFilter.matches(entry)) && (includeFilter == null || includeFilter.matches(entry)));
/*  69 */     List<Archive> nestedArchives = getNestedArchives(combinedFilter);
/*  70 */     return nestedArchives.iterator();
/*     */   }
/*     */   
/*     */   @Deprecated
/*     */   default List<Archive> getNestedArchives(EntryFilter filter) throws IOException {
/*  83 */     throw new IllegalStateException("Unexpected call to getNestedArchives(filter)");
/*     */   }
/*     */   
/*     */   @Deprecated
/*     */   default void forEach(Consumer<? super Entry> action) {
/* 108 */     Objects.requireNonNull(action);
/* 109 */     for (Entry entry : this)
/* 110 */       action.accept(entry); 
/*     */   }
/*     */   
/*     */   @Deprecated
/*     */   default Spliterator<Entry> spliterator() {
/* 124 */     return Spliterators.spliteratorUnknownSize(iterator(), 0);
/*     */   }
/*     */   
/*     */   default boolean isExploded() {
/* 133 */     return false;
/*     */   }
/*     */   
/*     */   default void close() throws Exception {}
/*     */   
/*     */   URL getUrl() throws MalformedURLException;
/*     */   
/*     */   Manifest getManifest() throws IOException;
/*     */   
/*     */   @Deprecated
/*     */   Iterator<Entry> iterator();
/*     */   
/*     */   @FunctionalInterface
/*     */   public static interface EntryFilter {
/*     */     boolean matches(Archive.Entry param1Entry);
/*     */   }
/*     */   
/*     */   public static interface Entry {
/*     */     boolean isDirectory();
/*     */     
/*     */     String getName();
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\archive\Archive.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */