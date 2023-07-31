/*    */ package BOOT-INF.classes.com.yancao.ctf.bean;
/*    */ 
/*    */ import java.io.BufferedReader;
/*    */ import java.io.InputStreamReader;
/*    */ import java.io.Serializable;
/*    */ import java.net.URL;
/*    */ 
/*    */ public class URLVisiter implements Serializable {
/*    */   public String visitUrl(String myurl) {
/* 13 */     if (myurl.startsWith("file"))
/* 14 */       return "file protocol is not allowed"; 
/* 16 */     URL url = null;
/*    */     try {
/* 18 */       url = new URL(myurl);
/* 19 */       BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
/* 21 */       StringBuilder sb = new StringBuilder();
/*    */       String inputLine;
/* 22 */       while ((inputLine = in.readLine()) != null)
/* 23 */         sb.append(inputLine); 
/* 25 */       in.close();
/* 26 */       return sb.toString();
/* 27 */     } catch (Exception e) {
/* 28 */       return e.toString();
/*    */     } 
/*    */   }
/*    */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\yancao\ctf\bean\URLVisiter.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */