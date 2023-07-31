/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import org.springframework.boot.loader.data.RandomAccessData;
/*     */ 
/*     */ class CentralDirectoryEndRecord {
/*     */   private static final int MINIMUM_SIZE = 22;
/*     */   
/*     */   private static final int MAXIMUM_COMMENT_LENGTH = 65535;
/*     */   
/*     */   private static final int MAXIMUM_SIZE = 65557;
/*     */   
/*     */   private static final int SIGNATURE = 101010256;
/*     */   
/*     */   private static final int COMMENT_LENGTH_OFFSET = 20;
/*     */   
/*     */   private static final int READ_BLOCK_SIZE = 256;
/*     */   
/*     */   private final Zip64End zip64End;
/*     */   
/*     */   private byte[] block;
/*     */   
/*     */   private int offset;
/*     */   
/*     */   private int size;
/*     */   
/*     */   CentralDirectoryEndRecord(RandomAccessData data) throws IOException {
/*  61 */     this.block = createBlockFromEndOfData(data, 256);
/*  62 */     this.size = 22;
/*  63 */     this.offset = this.block.length - this.size;
/*  64 */     while (!isValid()) {
/*  65 */       this.size++;
/*  66 */       if (this.size > this.block.length) {
/*  67 */         if (this.size >= 65557 || this.size > data.getSize())
/*  68 */           throw new IOException("Unable to find ZIP central directory records after reading " + this.size + " bytes"); 
/*  71 */         this.block = createBlockFromEndOfData(data, this.size + 256);
/*     */       } 
/*  73 */       this.offset = this.block.length - this.size;
/*     */     } 
/*  75 */     long startOfCentralDirectoryEndRecord = data.getSize() - this.size;
/*  76 */     Zip64Locator zip64Locator = Zip64Locator.find(data, startOfCentralDirectoryEndRecord);
/*  77 */     this.zip64End = (zip64Locator != null) ? new Zip64End(data, zip64Locator) : null;
/*     */   }
/*     */   
/*     */   private byte[] createBlockFromEndOfData(RandomAccessData data, int size) throws IOException {
/*  81 */     int length = (int)Math.min(data.getSize(), size);
/*  82 */     return data.read(data.getSize() - length, length);
/*     */   }
/*     */   
/*     */   private boolean isValid() {
/*  86 */     if (this.block.length < 22 || Bytes.littleEndianValue(this.block, this.offset + 0, 4) != 101010256L)
/*  87 */       return false; 
/*  90 */     long commentLength = Bytes.littleEndianValue(this.block, this.offset + 20, 2);
/*  91 */     return (this.size == 22L + commentLength);
/*     */   }
/*     */   
/*     */   long getStartOfArchive(RandomAccessData data) {
/* 102 */     long length = Bytes.littleEndianValue(this.block, this.offset + 12, 4);
/* 104 */     long specifiedOffset = (this.zip64End != null) ? this.zip64End.centralDirectoryOffset : Bytes.littleEndianValue(this.block, this.offset + 16, 4);
/* 105 */     long zip64EndSize = (this.zip64End != null) ? this.zip64End.getSize() : 0L;
/* 106 */     int zip64LocSize = (this.zip64End != null) ? 20 : 0;
/* 107 */     long actualOffset = data.getSize() - this.size - length - zip64EndSize - zip64LocSize;
/* 108 */     return actualOffset - specifiedOffset;
/*     */   }
/*     */   
/*     */   RandomAccessData getCentralDirectory(RandomAccessData data) {
/* 118 */     if (this.zip64End != null)
/* 119 */       return this.zip64End.getCentralDirectory(data); 
/* 121 */     long offset = Bytes.littleEndianValue(this.block, this.offset + 16, 4);
/* 122 */     long length = Bytes.littleEndianValue(this.block, this.offset + 12, 4);
/* 123 */     return data.getSubsection(offset, length);
/*     */   }
/*     */   
/*     */   int getNumberOfRecords() {
/* 131 */     if (this.zip64End != null)
/* 132 */       return this.zip64End.getNumberOfRecords(); 
/* 134 */     long numberOfRecords = Bytes.littleEndianValue(this.block, this.offset + 10, 2);
/* 135 */     return (int)numberOfRecords;
/*     */   }
/*     */   
/*     */   String getComment() {
/* 139 */     int commentLength = (int)Bytes.littleEndianValue(this.block, this.offset + 20, 2);
/* 140 */     AsciiBytes comment = new AsciiBytes(this.block, this.offset + 20 + 2, commentLength);
/* 141 */     return comment.toString();
/*     */   }
/*     */   
/*     */   boolean isZip64() {
/* 145 */     return (this.zip64End != null);
/*     */   }
/*     */   
/*     */   private static final class Zip64End {
/*     */     private static final int ZIP64_ENDTOT = 32;
/*     */     
/*     */     private static final int ZIP64_ENDSIZ = 40;
/*     */     
/*     */     private static final int ZIP64_ENDOFF = 48;
/*     */     
/*     */     private final CentralDirectoryEndRecord.Zip64Locator locator;
/*     */     
/*     */     private final long centralDirectoryOffset;
/*     */     
/*     */     private final long centralDirectoryLength;
/*     */     
/*     */     private final int numberOfRecords;
/*     */     
/*     */     private Zip64End(RandomAccessData data, CentralDirectoryEndRecord.Zip64Locator locator) throws IOException {
/* 171 */       this.locator = locator;
/* 172 */       byte[] block = data.read(locator.getZip64EndOffset(), 56L);
/* 173 */       this.centralDirectoryOffset = Bytes.littleEndianValue(block, 48, 8);
/* 174 */       this.centralDirectoryLength = Bytes.littleEndianValue(block, 40, 8);
/* 175 */       this.numberOfRecords = (int)Bytes.littleEndianValue(block, 32, 8);
/*     */     }
/*     */     
/*     */     private long getSize() {
/* 183 */       return this.locator.getZip64EndSize();
/*     */     }
/*     */     
/*     */     private RandomAccessData getCentralDirectory(RandomAccessData data) {
/* 193 */       return data.getSubsection(this.centralDirectoryOffset, this.centralDirectoryLength);
/*     */     }
/*     */     
/*     */     private int getNumberOfRecords() {
/* 201 */       return this.numberOfRecords;
/*     */     }
/*     */   }
/*     */   
/*     */   private static final class Zip64Locator {
/*     */     static final int SIGNATURE = 117853008;
/*     */     
/*     */     static final int ZIP64_LOCSIZE = 20;
/*     */     
/*     */     static final int ZIP64_LOCOFF = 8;
/*     */     
/*     */     private final long zip64EndOffset;
/*     */     
/*     */     private final long offset;
/*     */     
/*     */     private Zip64Locator(long offset, byte[] block) {
/* 225 */       this.offset = offset;
/* 226 */       this.zip64EndOffset = Bytes.littleEndianValue(block, 8, 8);
/*     */     }
/*     */     
/*     */     private long getZip64EndSize() {
/* 234 */       return this.offset - this.zip64EndOffset;
/*     */     }
/*     */     
/*     */     private long getZip64EndOffset() {
/* 242 */       return this.zip64EndOffset;
/*     */     }
/*     */     
/*     */     private static Zip64Locator find(RandomAccessData data, long centralDirectoryEndOffset) throws IOException {
/* 246 */       long offset = centralDirectoryEndOffset - 20L;
/* 247 */       if (offset >= 0L) {
/* 248 */         byte[] block = data.read(offset, 20L);
/* 249 */         if (Bytes.littleEndianValue(block, 0, 4) == 117853008L)
/* 250 */           return new Zip64Locator(offset, block); 
/*     */       } 
/* 253 */       return null;
/*     */     }
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\CentralDirectoryEndRecord.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */