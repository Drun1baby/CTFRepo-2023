/*    */ package BOOT-INF.classes.com.yancao.ctf.util;
/*    */ 
/*    */ import java.io.IOException;
/*    */ import java.io.InputStream;
/*    */ import java.io.InvalidClassException;
/*    */ import java.io.ObjectInputStream;
/*    */ import java.io.ObjectStreamClass;
/*    */ 
/*    */ public class MyObjectInputStream extends ObjectInputStream {
/*    */   public MyObjectInputStream(InputStream in) throws IOException {
/*  8 */     super(in);
/*    */   }
/*    */   
/*    */   protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
/* 13 */     String className = desc.getName();
/* 14 */     String[] denyClasses = { "java.net.InetAddress", "org.apache.commons.collections.Transformer", "org.apache.commons.collections.functors", "com.yancao.ctf.bean.URLVisiter", "com.yancao.ctf.bean.URLHelper" };
/* 21 */     for (String denyClass : denyClasses) {
/* 22 */       if (className.startsWith(denyClass))
/* 23 */         throw new InvalidClassException("Unauthorized deserialization attempt", className); 
/*    */     } 
/* 26 */     return super.resolveClass(desc);
/*    */   }
/*    */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\yancao\ct\\util\MyObjectInputStream.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */