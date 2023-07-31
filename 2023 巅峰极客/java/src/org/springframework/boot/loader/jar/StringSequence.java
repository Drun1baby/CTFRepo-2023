/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.util.Objects;
/*     */ 
/*     */ final class StringSequence implements CharSequence {
/*     */   private final String source;
/*     */   
/*     */   private final int start;
/*     */   
/*     */   private final int end;
/*     */   
/*     */   private int hash;
/*     */   
/*     */   StringSequence(String source) {
/*  39 */     this(source, 0, (source != null) ? source.length() : -1);
/*     */   }
/*     */   
/*     */   StringSequence(String source, int start, int end) {
/*  43 */     Objects.requireNonNull(source, "Source must not be null");
/*  44 */     if (start < 0)
/*  45 */       throw new StringIndexOutOfBoundsException(start); 
/*  47 */     if (end > source.length())
/*  48 */       throw new StringIndexOutOfBoundsException(end); 
/*  50 */     this.source = source;
/*  51 */     this.start = start;
/*  52 */     this.end = end;
/*     */   }
/*     */   
/*     */   StringSequence subSequence(int start) {
/*  56 */     return subSequence(start, length());
/*     */   }
/*     */   
/*     */   public StringSequence subSequence(int start, int end) {
/*  61 */     int subSequenceStart = this.start + start;
/*  62 */     int subSequenceEnd = this.start + end;
/*  63 */     if (subSequenceStart > this.end)
/*  64 */       throw new StringIndexOutOfBoundsException(start); 
/*  66 */     if (subSequenceEnd > this.end)
/*  67 */       throw new StringIndexOutOfBoundsException(end); 
/*  69 */     if (start == 0 && subSequenceEnd == this.end)
/*  70 */       return this; 
/*  72 */     return new StringSequence(this.source, subSequenceStart, subSequenceEnd);
/*     */   }
/*     */   
/*     */   public boolean isEmpty() {
/*  80 */     return (length() == 0);
/*     */   }
/*     */   
/*     */   public int length() {
/*  85 */     return this.end - this.start;
/*     */   }
/*     */   
/*     */   public char charAt(int index) {
/*  90 */     return this.source.charAt(this.start + index);
/*     */   }
/*     */   
/*     */   int indexOf(char ch) {
/*  94 */     return this.source.indexOf(ch, this.start) - this.start;
/*     */   }
/*     */   
/*     */   int indexOf(String str) {
/*  98 */     return this.source.indexOf(str, this.start) - this.start;
/*     */   }
/*     */   
/*     */   int indexOf(String str, int fromIndex) {
/* 102 */     return this.source.indexOf(str, this.start + fromIndex) - this.start;
/*     */   }
/*     */   
/*     */   boolean startsWith(String prefix) {
/* 106 */     return startsWith(prefix, 0);
/*     */   }
/*     */   
/*     */   boolean startsWith(String prefix, int offset) {
/* 110 */     int prefixLength = prefix.length();
/* 111 */     int length = length();
/* 112 */     if (length - prefixLength - offset < 0)
/* 113 */       return false; 
/* 115 */     return this.source.startsWith(prefix, this.start + offset);
/*     */   }
/*     */   
/*     */   public boolean equals(Object obj) {
/* 120 */     if (this == obj)
/* 121 */       return true; 
/* 123 */     if (!(obj instanceof CharSequence))
/* 124 */       return false; 
/* 126 */     CharSequence other = (CharSequence)obj;
/* 127 */     int n = length();
/* 128 */     if (n != other.length())
/* 129 */       return false; 
/* 131 */     int i = 0;
/* 132 */     while (n-- != 0) {
/* 133 */       if (charAt(i) != other.charAt(i))
/* 134 */         return false; 
/* 136 */       i++;
/*     */     } 
/* 138 */     return true;
/*     */   }
/*     */   
/*     */   public int hashCode() {
/* 143 */     int hash = this.hash;
/* 144 */     if (hash == 0 && length() > 0) {
/* 145 */       for (int i = this.start; i < this.end; i++)
/* 146 */         hash = 31 * hash + this.source.charAt(i); 
/* 148 */       this.hash = hash;
/*     */     } 
/* 150 */     return hash;
/*     */   }
/*     */   
/*     */   public String toString() {
/* 155 */     return this.source.substring(this.start, this.end);
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\StringSequence.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */