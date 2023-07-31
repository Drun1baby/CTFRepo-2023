/*    */ package org.springframework.boot.loader;
/*    */ 
/*    */ import org.springframework.boot.loader.archive.Archive;
/*    */ 
/*    */ public class JarLauncher extends ExecutableArchiveLauncher {
/*    */   static final Archive.EntryFilter NESTED_ARCHIVE_ENTRY_FILTER;
/*    */   
/*    */   static {
/* 35 */     NESTED_ARCHIVE_ENTRY_FILTER = (entry -> entry.isDirectory() ? entry.getName().equals("BOOT-INF/classes/") : entry.getName().startsWith("BOOT-INF/lib/"));
/*    */   }
/*    */   
/*    */   public JarLauncher() {}
/*    */   
/*    */   protected JarLauncher(Archive archive) {
/* 46 */     super(archive);
/*    */   }
/*    */   
/*    */   protected boolean isPostProcessingClassPathArchives() {
/* 51 */     return false;
/*    */   }
/*    */   
/*    */   protected boolean isNestedArchive(Archive.Entry entry) {
/* 56 */     return NESTED_ARCHIVE_ENTRY_FILTER.matches(entry);
/*    */   }
/*    */   
/*    */   protected String getArchiveEntryPathPrefix() {
/* 61 */     return "BOOT-INF/";
/*    */   }
/*    */   
/*    */   public static void main(String[] args) throws Exception {
/* 65 */     (new JarLauncher()).launch(args);
/*    */   }
/*    */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\JarLauncher.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */