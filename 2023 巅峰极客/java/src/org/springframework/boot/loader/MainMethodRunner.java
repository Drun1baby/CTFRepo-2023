/*    */ package org.springframework.boot.loader;
/*    */ 
/*    */ import java.lang.reflect.Method;
/*    */ 
/*    */ public class MainMethodRunner {
/*    */   private final String mainClassName;
/*    */   
/*    */   private final String[] args;
/*    */   
/*    */   public MainMethodRunner(String mainClass, String[] args) {
/* 41 */     this.mainClassName = mainClass;
/* 42 */     this.args = (args != null) ? (String[])args.clone() : null;
/*    */   }
/*    */   
/*    */   public void run() throws Exception {
/* 46 */     Class<?> mainClass = Class.forName(this.mainClassName, false, Thread.currentThread().getContextClassLoader());
/* 47 */     Method mainMethod = mainClass.getDeclaredMethod("main", new Class[] { String[].class });
/* 48 */     mainMethod.setAccessible(true);
/* 49 */     mainMethod.invoke((Object)null, new Object[] { this.args });
/*    */   }
/*    */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\MainMethodRunner.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */