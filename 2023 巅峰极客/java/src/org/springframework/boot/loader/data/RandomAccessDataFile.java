/*     */ package org.springframework.boot.loader.data;
/*     */ 
/*     */ import java.io.EOFException;
/*     */ import java.io.File;
/*     */ import java.io.FileNotFoundException;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.RandomAccessFile;
/*     */ 
/*     */ public class RandomAccessDataFile implements RandomAccessData {
/*     */   private final FileAccess fileAccess;
/*     */   
/*     */   private final long offset;
/*     */   
/*     */   private final long length;
/*     */   
/*     */   public RandomAccessDataFile(File file) {
/*  47 */     if (file == null)
/*  48 */       throw new IllegalArgumentException("File must not be null"); 
/*  50 */     this.fileAccess = new FileAccess(file);
/*  51 */     this.offset = 0L;
/*  52 */     this.length = file.length();
/*     */   }
/*     */   
/*     */   private RandomAccessDataFile(FileAccess fileAccess, long offset, long length) {
/*  62 */     this.fileAccess = fileAccess;
/*  63 */     this.offset = offset;
/*  64 */     this.length = length;
/*     */   }
/*     */   
/*     */   public File getFile() {
/*  72 */     return this.fileAccess.file;
/*     */   }
/*     */   
/*     */   public InputStream getInputStream() throws IOException {
/*  77 */     return new DataInputStream();
/*     */   }
/*     */   
/*     */   public RandomAccessData getSubsection(long offset, long length) {
/*  82 */     if (offset < 0L || length < 0L || offset + length > this.length)
/*  83 */       throw new IndexOutOfBoundsException(); 
/*  85 */     return new RandomAccessDataFile(this.fileAccess, this.offset + offset, length);
/*     */   }
/*     */   
/*     */   public byte[] read() throws IOException {
/*  90 */     return read(0L, this.length);
/*     */   }
/*     */   
/*     */   public byte[] read(long offset, long length) throws IOException {
/*  95 */     if (offset > this.length)
/*  96 */       throw new IndexOutOfBoundsException(); 
/*  98 */     if (offset + length > this.length)
/*  99 */       throw new EOFException(); 
/* 101 */     byte[] bytes = new byte[(int)length];
/* 102 */     read(bytes, offset, 0, bytes.length);
/* 103 */     return bytes;
/*     */   }
/*     */   
/*     */   private int readByte(long position) throws IOException {
/* 107 */     if (position >= this.length)
/* 108 */       return -1; 
/* 110 */     return this.fileAccess.readByte(this.offset + position);
/*     */   }
/*     */   
/*     */   private int read(byte[] bytes, long position, int offset, int length) throws IOException {
/* 114 */     if (position > this.length)
/* 115 */       return -1; 
/* 117 */     return this.fileAccess.read(bytes, this.offset + position, offset, length);
/*     */   }
/*     */   
/*     */   public long getSize() {
/* 122 */     return this.length;
/*     */   }
/*     */   
/*     */   public void close() throws IOException {
/* 126 */     this.fileAccess.close();
/*     */   }
/*     */   
/*     */   private class DataInputStream extends InputStream {
/*     */     private int position;
/*     */     
/*     */     private DataInputStream() {}
/*     */     
/*     */     public int read() throws IOException {
/* 138 */       int read = RandomAccessDataFile.this.readByte(this.position);
/* 139 */       if (read > -1)
/* 140 */         moveOn(1); 
/* 142 */       return read;
/*     */     }
/*     */     
/*     */     public int read(byte[] b) throws IOException {
/* 147 */       return read(b, 0, (b != null) ? b.length : 0);
/*     */     }
/*     */     
/*     */     public int read(byte[] b, int off, int len) throws IOException {
/* 152 */       if (b == null)
/* 153 */         throw new NullPointerException("Bytes must not be null"); 
/* 155 */       return doRead(b, off, len);
/*     */     }
/*     */     
/*     */     int doRead(byte[] b, int off, int len) throws IOException {
/* 168 */       if (len == 0)
/* 169 */         return 0; 
/* 171 */       int cappedLen = cap(len);
/* 172 */       if (cappedLen <= 0)
/* 173 */         return -1; 
/* 175 */       return (int)moveOn(RandomAccessDataFile.this.read(b, this.position, off, cappedLen));
/*     */     }
/*     */     
/*     */     public long skip(long n) throws IOException {
/* 180 */       return (n <= 0L) ? 0L : moveOn(cap(n));
/*     */     }
/*     */     
/*     */     public int available() throws IOException {
/* 185 */       return (int)RandomAccessDataFile.this.length - this.position;
/*     */     }
/*     */     
/*     */     private int cap(long n) {
/* 195 */       return (int)Math.min(RandomAccessDataFile.this.length - this.position, n);
/*     */     }
/*     */     
/*     */     private long moveOn(int amount) {
/* 204 */       this.position += amount;
/* 205 */       return amount;
/*     */     }
/*     */   }
/*     */   
/*     */   private static final class FileAccess {
/* 212 */     private final Object monitor = new Object();
/*     */     
/*     */     private final File file;
/*     */     
/*     */     private RandomAccessFile randomAccessFile;
/*     */     
/*     */     private FileAccess(File file) {
/* 219 */       this.file = file;
/* 220 */       openIfNecessary();
/*     */     }
/*     */     
/*     */     private int read(byte[] bytes, long position, int offset, int length) throws IOException {
/* 224 */       synchronized (this.monitor) {
/* 225 */         openIfNecessary();
/* 226 */         this.randomAccessFile.seek(position);
/* 227 */         return this.randomAccessFile.read(bytes, offset, length);
/*     */       } 
/*     */     }
/*     */     
/*     */     private void openIfNecessary() {
/* 232 */       if (this.randomAccessFile == null)
/*     */         try {
/* 234 */           this.randomAccessFile = new RandomAccessFile(this.file, "r");
/* 236 */         } catch (FileNotFoundException ex) {
/* 237 */           throw new IllegalArgumentException(
/* 238 */               String.format("File %s must exist", new Object[] { this.file.getAbsolutePath() }));
/*     */         }  
/*     */     }
/*     */     
/*     */     private void close() throws IOException {
/* 244 */       synchronized (this.monitor) {
/* 245 */         if (this.randomAccessFile != null) {
/* 246 */           this.randomAccessFile.close();
/* 247 */           this.randomAccessFile = null;
/*     */         } 
/*     */       } 
/*     */     }
/*     */     
/*     */     private int readByte(long position) throws IOException {
/* 253 */       synchronized (this.monitor) {
/* 254 */         openIfNecessary();
/* 255 */         this.randomAccessFile.seek(position);
/* 256 */         return this.randomAccessFile.read();
/*     */       } 
/*     */     }
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\data\RandomAccessDataFile.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */