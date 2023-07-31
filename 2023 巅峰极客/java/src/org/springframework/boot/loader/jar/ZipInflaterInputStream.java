/*    */ package org.springframework.boot.loader.jar;
/*    */ 
/*    */ import java.io.EOFException;
/*    */ import java.io.IOException;
/*    */ import java.io.InputStream;
/*    */ import java.util.zip.Inflater;
/*    */ import java.util.zip.InflaterInputStream;
/*    */ 
/*    */ class ZipInflaterInputStream extends InflaterInputStream {
/*    */   private int available;
/*    */   
/*    */   private boolean extraBytesWritten;
/*    */   
/*    */   ZipInflaterInputStream(InputStream inputStream, int size) {
/* 38 */     super(inputStream, new Inflater(true), getInflaterBufferSize(size));
/* 39 */     this.available = size;
/*    */   }
/*    */   
/*    */   public int available() throws IOException {
/* 44 */     if (this.available < 0)
/* 45 */       return super.available(); 
/* 47 */     return this.available;
/*    */   }
/*    */   
/*    */   public int read(byte[] b, int off, int len) throws IOException {
/* 52 */     int result = super.read(b, off, len);
/* 53 */     if (result != -1)
/* 54 */       this.available -= result; 
/* 56 */     return result;
/*    */   }
/*    */   
/*    */   public void close() throws IOException {
/* 61 */     super.close();
/* 62 */     this.inf.end();
/*    */   }
/*    */   
/*    */   protected void fill() throws IOException {
/*    */     try {
/* 68 */       super.fill();
/* 70 */     } catch (EOFException ex) {
/* 71 */       if (this.extraBytesWritten)
/* 72 */         throw ex; 
/* 74 */       this.len = 1;
/* 75 */       this.buf[0] = 0;
/* 76 */       this.extraBytesWritten = true;
/* 77 */       this.inf.setInput(this.buf, 0, this.len);
/*    */     } 
/*    */   }
/*    */   
/*    */   private static int getInflaterBufferSize(long size) {
/* 82 */     size += 2L;
/* 83 */     size = (size > 65536L) ? 8192L : size;
/* 84 */     size = (size <= 0L) ? 4096L : size;
/* 85 */     return (int)size;
/*    */   }
/*    */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\ZipInflaterInputStream.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */