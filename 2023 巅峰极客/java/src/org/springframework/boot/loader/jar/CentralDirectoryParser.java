/*    */ package org.springframework.boot.loader.jar;
/*    */ 
/*    */ import java.io.IOException;
/*    */ import java.util.ArrayList;
/*    */ import java.util.List;
/*    */ import org.springframework.boot.loader.data.RandomAccessData;
/*    */ 
/*    */ class CentralDirectoryParser {
/*    */   private static final int CENTRAL_DIRECTORY_HEADER_BASE_SIZE = 46;
/*    */   
/* 36 */   private final List<CentralDirectoryVisitor> visitors = new ArrayList<>();
/*    */   
/*    */   <T extends CentralDirectoryVisitor> T addVisitor(T visitor) {
/* 39 */     this.visitors.add((CentralDirectoryVisitor)visitor);
/* 40 */     return visitor;
/*    */   }
/*    */   
/*    */   RandomAccessData parse(RandomAccessData data, boolean skipPrefixBytes) throws IOException {
/* 51 */     CentralDirectoryEndRecord endRecord = new CentralDirectoryEndRecord(data);
/* 52 */     if (skipPrefixBytes)
/* 53 */       data = getArchiveData(endRecord, data); 
/* 55 */     RandomAccessData centralDirectoryData = endRecord.getCentralDirectory(data);
/* 56 */     visitStart(endRecord, centralDirectoryData);
/* 57 */     parseEntries(endRecord, centralDirectoryData);
/* 58 */     visitEnd();
/* 59 */     return data;
/*    */   }
/*    */   
/*    */   private void parseEntries(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) throws IOException {
/* 64 */     byte[] bytes = centralDirectoryData.read(0L, centralDirectoryData.getSize());
/* 65 */     CentralDirectoryFileHeader fileHeader = new CentralDirectoryFileHeader();
/* 66 */     int dataOffset = 0;
/* 67 */     for (int i = 0; i < endRecord.getNumberOfRecords(); i++) {
/* 68 */       fileHeader.load(bytes, dataOffset, null, 0L, null);
/* 69 */       visitFileHeader(dataOffset, fileHeader);
/* 70 */       dataOffset += 46 + fileHeader.getName().length() + fileHeader
/* 71 */         .getComment().length() + (fileHeader.getExtra()).length;
/*    */     } 
/*    */   }
/*    */   
/*    */   private RandomAccessData getArchiveData(CentralDirectoryEndRecord endRecord, RandomAccessData data) {
/* 76 */     long offset = endRecord.getStartOfArchive(data);
/* 77 */     if (offset == 0L)
/* 78 */       return data; 
/* 80 */     return data.getSubsection(offset, data.getSize() - offset);
/*    */   }
/*    */   
/*    */   private void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
/* 84 */     for (CentralDirectoryVisitor visitor : this.visitors)
/* 85 */       visitor.visitStart(endRecord, centralDirectoryData); 
/*    */   }
/*    */   
/*    */   private void visitFileHeader(long dataOffset, CentralDirectoryFileHeader fileHeader) {
/* 90 */     for (CentralDirectoryVisitor visitor : this.visitors)
/* 91 */       visitor.visitFileHeader(fileHeader, dataOffset); 
/*    */   }
/*    */   
/*    */   private void visitEnd() {
/* 96 */     for (CentralDirectoryVisitor visitor : this.visitors)
/* 97 */       visitor.visitEnd(); 
/*    */   }
/*    */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\CentralDirectoryParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */