/*    */ package org.springframework.boot.loader.jar;
/*    */ 
/*    */ import java.io.File;
/*    */ import java.io.IOException;
/*    */ import java.io.InputStream;
/*    */ import java.net.MalformedURLException;
/*    */ import java.net.URL;
/*    */ import java.security.Permission;
/*    */ import java.util.jar.JarFile;
/*    */ 
/*    */ abstract class AbstractJarFile extends JarFile {
/*    */   AbstractJarFile(File file) throws IOException {
/* 39 */     super(file);
/*    */   }
/*    */   
/*    */   abstract URL getUrl() throws MalformedURLException;
/*    */   
/*    */   abstract JarFileType getType();
/*    */   
/*    */   abstract Permission getPermission();
/*    */   
/*    */   abstract InputStream getInputStream() throws IOException;
/*    */   
/*    */   enum JarFileType {
/* 74 */     DIRECT, NESTED_DIRECTORY, NESTED_JAR;
/*    */   }
/*    */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\AbstractJarFile.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */