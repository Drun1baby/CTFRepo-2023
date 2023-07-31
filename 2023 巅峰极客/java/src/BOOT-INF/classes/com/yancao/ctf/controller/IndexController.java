/*    */ package BOOT-INF.classes.com.yancao.ctf.controller;
/*    */ 
/*    */ import com.yancao.ctf.bean.URLHelper;
/*    */ import com.yancao.ctf.util.MyObjectInputStream;
/*    */ import java.io.ByteArrayInputStream;
/*    */ import java.io.File;
/*    */ import java.io.FileInputStream;
/*    */ import java.io.IOException;
/*    */ import java.nio.charset.StandardCharsets;
/*    */ import java.util.Base64;
/*    */ import org.springframework.stereotype.Controller;
/*    */ import org.springframework.web.bind.annotation.GetMapping;
/*    */ import org.springframework.web.bind.annotation.RequestMapping;
/*    */ import org.springframework.web.bind.annotation.RequestParam;
/*    */ import org.springframework.web.bind.annotation.ResponseBody;
/*    */ 
/*    */ @Controller
/*    */ public class IndexController {
/*    */   @RequestMapping({"/"})
/*    */   @ResponseBody
/*    */   public String index() {
/* 18 */     return "Hello World";
/*    */   }
/*    */   
/*    */   @GetMapping({"/hack"})
/*    */   @ResponseBody
/*    */   public String hack(@RequestParam String payload) {
/* 24 */     byte[] bytes = Base64.getDecoder().decode(payload.getBytes(StandardCharsets.UTF_8));
/* 25 */     ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
/*    */     try {
/* 27 */       MyObjectInputStream myObjectInputStream = new MyObjectInputStream(byteArrayInputStream);
/* 28 */       URLHelper o = (URLHelper)myObjectInputStream.readObject();
/* 29 */       System.out.println(o);
/* 30 */       System.out.println(o.url);
/* 31 */     } catch (Exception e) {
/* 32 */       e.printStackTrace();
/* 33 */       return e.toString();
/*    */     } 
/* 35 */     return "ok!";
/*    */   }
/*    */   
/*    */   @RequestMapping({"/file"})
/*    */   @ResponseBody
/*    */   public String file() throws IOException {
/* 41 */     File file = new File("/tmp/file");
/* 42 */     if (!file.exists())
/* 43 */       file.createNewFile(); 
/* 45 */     FileInputStream fis = new FileInputStream(file);
/* 46 */     byte[] bytes = new byte[1024];
/* 47 */     fis.read(bytes);
/* 48 */     return new String(bytes);
/*    */   }
/*    */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\yancao\ctf\controller\IndexController.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */