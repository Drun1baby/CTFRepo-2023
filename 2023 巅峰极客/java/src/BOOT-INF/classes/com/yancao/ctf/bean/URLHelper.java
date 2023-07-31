/*    */ package BOOT-INF.classes.com.yancao.ctf.bean;
/*    */ 
/*    */ import com.yancao.ctf.bean.URLVisiter;
/*    */ import java.io.File;
/*    */ import java.io.FileOutputStream;
/*    */ import java.io.ObjectInputStream;
/*    */ import java.io.Serializable;
/*    */ 
/*    */ public class URLHelper implements Serializable {
/*    */   public String url;
/*    */   
/*  7 */   public URLVisiter visiter = null;
/*    */   
/*    */   private static final long serialVersionUID = 1L;
/*    */   
/*    */   public URLHelper(String url) {
/* 11 */     this.url = url;
/*    */   }
/*    */   
/*    */   private void readObject(ObjectInputStream in) throws Exception {
/* 15 */     in.defaultReadObject();
/* 16 */     if (this.visiter != null) {
/* 17 */       String result = this.visiter.visitUrl(this.url);
/* 18 */       File file = new File("/tmp/file");
/* 19 */       if (!file.exists())
/* 20 */         file.createNewFile(); 
/* 22 */       FileOutputStream fos = new FileOutputStream(file);
/* 23 */       fos.write(result.getBytes());
/* 24 */       fos.close();
/*    */     } 
/*    */   }
/*    */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\yancao\ctf\bean\URLHelper.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */