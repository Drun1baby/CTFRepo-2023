/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.time.ZoneId;
/*     */ import java.time.ZonedDateTime;
/*     */ import java.time.temporal.ChronoField;
/*     */ import java.time.temporal.ChronoUnit;
/*     */ import java.time.temporal.ValueRange;
/*     */ import org.springframework.boot.loader.data.RandomAccessData;
/*     */ 
/*     */ final class CentralDirectoryFileHeader implements FileHeader {
/*  39 */   private static final AsciiBytes SLASH = new AsciiBytes("/");
/*     */   
/*  41 */   private static final byte[] NO_EXTRA = new byte[0];
/*     */   
/*  43 */   private static final AsciiBytes NO_COMMENT = new AsciiBytes("");
/*     */   
/*     */   private byte[] header;
/*     */   
/*     */   private int headerOffset;
/*     */   
/*     */   private AsciiBytes name;
/*     */   
/*     */   private byte[] extra;
/*     */   
/*     */   private AsciiBytes comment;
/*     */   
/*     */   private long localHeaderOffset;
/*     */   
/*     */   CentralDirectoryFileHeader() {}
/*     */   
/*     */   CentralDirectoryFileHeader(byte[] header, int headerOffset, AsciiBytes name, byte[] extra, AsciiBytes comment, long localHeaderOffset) {
/*  62 */     this.header = header;
/*  63 */     this.headerOffset = headerOffset;
/*  64 */     this.name = name;
/*  65 */     this.extra = extra;
/*  66 */     this.comment = comment;
/*  67 */     this.localHeaderOffset = localHeaderOffset;
/*     */   }
/*     */   
/*     */   void load(byte[] data, int dataOffset, RandomAccessData variableData, long variableOffset, JarEntryFilter filter) throws IOException {
/*  73 */     this.header = data;
/*  74 */     this.headerOffset = dataOffset;
/*  75 */     long compressedSize = Bytes.littleEndianValue(data, dataOffset + 20, 4);
/*  76 */     long uncompressedSize = Bytes.littleEndianValue(data, dataOffset + 24, 4);
/*  77 */     long nameLength = Bytes.littleEndianValue(data, dataOffset + 28, 2);
/*  78 */     long extraLength = Bytes.littleEndianValue(data, dataOffset + 30, 2);
/*  79 */     long commentLength = Bytes.littleEndianValue(data, dataOffset + 32, 2);
/*  80 */     long localHeaderOffset = Bytes.littleEndianValue(data, dataOffset + 42, 4);
/*  82 */     dataOffset += 46;
/*  83 */     if (variableData != null) {
/*  84 */       data = variableData.read(variableOffset + 46L, nameLength + extraLength + commentLength);
/*  85 */       dataOffset = 0;
/*     */     } 
/*  87 */     this.name = new AsciiBytes(data, dataOffset, (int)nameLength);
/*  88 */     if (filter != null)
/*  89 */       this.name = filter.apply(this.name); 
/*  91 */     this.extra = NO_EXTRA;
/*  92 */     this.comment = NO_COMMENT;
/*  93 */     if (extraLength > 0L) {
/*  94 */       this.extra = new byte[(int)extraLength];
/*  95 */       System.arraycopy(data, (int)(dataOffset + nameLength), this.extra, 0, this.extra.length);
/*     */     } 
/*  97 */     this.localHeaderOffset = getLocalHeaderOffset(compressedSize, uncompressedSize, localHeaderOffset, this.extra);
/*  98 */     if (commentLength > 0L)
/*  99 */       this.comment = new AsciiBytes(data, (int)(dataOffset + nameLength + extraLength), (int)commentLength); 
/*     */   }
/*     */   
/*     */   private long getLocalHeaderOffset(long compressedSize, long uncompressedSize, long localHeaderOffset, byte[] extra) throws IOException {
/* 105 */     if (localHeaderOffset != 4294967295L)
/* 106 */       return localHeaderOffset; 
/* 108 */     int extraOffset = 0;
/* 109 */     while (extraOffset < extra.length - 2) {
/* 110 */       int id = (int)Bytes.littleEndianValue(extra, extraOffset, 2);
/* 111 */       int length = (int)Bytes.littleEndianValue(extra, extraOffset, 2);
/* 112 */       extraOffset += 4;
/* 113 */       if (id == 1) {
/* 114 */         int localHeaderExtraOffset = 0;
/* 115 */         if (compressedSize == 4294967295L)
/* 116 */           localHeaderExtraOffset += 4; 
/* 118 */         if (uncompressedSize == 4294967295L)
/* 119 */           localHeaderExtraOffset += 4; 
/* 121 */         return Bytes.littleEndianValue(extra, extraOffset + localHeaderExtraOffset, 8);
/*     */       } 
/* 123 */       extraOffset += length;
/*     */     } 
/* 125 */     throw new IOException("Zip64 Extended Information Extra Field not found");
/*     */   }
/*     */   
/*     */   AsciiBytes getName() {
/* 129 */     return this.name;
/*     */   }
/*     */   
/*     */   public boolean hasName(CharSequence name, char suffix) {
/* 134 */     return this.name.matches(name, suffix);
/*     */   }
/*     */   
/*     */   boolean isDirectory() {
/* 138 */     return this.name.endsWith(SLASH);
/*     */   }
/*     */   
/*     */   public int getMethod() {
/* 143 */     return (int)Bytes.littleEndianValue(this.header, this.headerOffset + 10, 2);
/*     */   }
/*     */   
/*     */   long getTime() {
/* 147 */     long datetime = Bytes.littleEndianValue(this.header, this.headerOffset + 12, 4);
/* 148 */     return decodeMsDosFormatDateTime(datetime);
/*     */   }
/*     */   
/*     */   private long decodeMsDosFormatDateTime(long datetime) {
/* 159 */     int year = getChronoValue((datetime >> 25L & 0x7FL) + 1980L, ChronoField.YEAR);
/* 160 */     int month = getChronoValue(datetime >> 21L & 0xFL, ChronoField.MONTH_OF_YEAR);
/* 161 */     int day = getChronoValue(datetime >> 16L & 0x1FL, ChronoField.DAY_OF_MONTH);
/* 162 */     int hour = getChronoValue(datetime >> 11L & 0x1FL, ChronoField.HOUR_OF_DAY);
/* 163 */     int minute = getChronoValue(datetime >> 5L & 0x3FL, ChronoField.MINUTE_OF_HOUR);
/* 164 */     int second = getChronoValue(datetime << 1L & 0x3EL, ChronoField.SECOND_OF_MINUTE);
/* 165 */     return ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.systemDefault()).toInstant()
/* 166 */       .truncatedTo(ChronoUnit.SECONDS).toEpochMilli();
/*     */   }
/*     */   
/*     */   long getCrc() {
/* 170 */     return Bytes.littleEndianValue(this.header, this.headerOffset + 16, 4);
/*     */   }
/*     */   
/*     */   public long getCompressedSize() {
/* 175 */     return Bytes.littleEndianValue(this.header, this.headerOffset + 20, 4);
/*     */   }
/*     */   
/*     */   public long getSize() {
/* 180 */     return Bytes.littleEndianValue(this.header, this.headerOffset + 24, 4);
/*     */   }
/*     */   
/*     */   byte[] getExtra() {
/* 184 */     return this.extra;
/*     */   }
/*     */   
/*     */   boolean hasExtra() {
/* 188 */     return (this.extra.length > 0);
/*     */   }
/*     */   
/*     */   AsciiBytes getComment() {
/* 192 */     return this.comment;
/*     */   }
/*     */   
/*     */   public long getLocalHeaderOffset() {
/* 197 */     return this.localHeaderOffset;
/*     */   }
/*     */   
/*     */   public CentralDirectoryFileHeader clone() {
/* 202 */     byte[] header = new byte[46];
/* 203 */     System.arraycopy(this.header, this.headerOffset, header, 0, header.length);
/* 204 */     return new CentralDirectoryFileHeader(header, 0, this.name, header, this.comment, this.localHeaderOffset);
/*     */   }
/*     */   
/*     */   static CentralDirectoryFileHeader fromRandomAccessData(RandomAccessData data, long offset, JarEntryFilter filter) throws IOException {
/* 209 */     CentralDirectoryFileHeader fileHeader = new CentralDirectoryFileHeader();
/* 210 */     byte[] bytes = data.read(offset, 46L);
/* 211 */     fileHeader.load(bytes, 0, data, offset, filter);
/* 212 */     return fileHeader;
/*     */   }
/*     */   
/*     */   private static int getChronoValue(long value, ChronoField field) {
/* 216 */     ValueRange range = field.range();
/* 217 */     return Math.toIntExact(Math.min(Math.max(value, range.getMinimum()), range.getMaximum()));
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\CentralDirectoryFileHeader.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */