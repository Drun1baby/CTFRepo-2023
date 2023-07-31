/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.nio.charset.StandardCharsets;
/*     */ 
/*     */ final class AsciiBytes {
/*     */   private static final String EMPTY_STRING = "";
/*     */   
/*  32 */   private static final int[] INITIAL_BYTE_BITMASK = new int[] { 127, 31, 15, 7 };
/*     */   
/*     */   private static final int SUBSEQUENT_BYTE_BITMASK = 63;
/*     */   
/*     */   private final byte[] bytes;
/*     */   
/*     */   private final int offset;
/*     */   
/*     */   private final int length;
/*     */   
/*     */   private String string;
/*     */   
/*     */   private int hash;
/*     */   
/*     */   AsciiBytes(String string) {
/*  51 */     this(string.getBytes(StandardCharsets.UTF_8));
/*  52 */     this.string = string;
/*     */   }
/*     */   
/*     */   AsciiBytes(byte[] bytes) {
/*  61 */     this(bytes, 0, bytes.length);
/*     */   }
/*     */   
/*     */   AsciiBytes(byte[] bytes, int offset, int length) {
/*  72 */     if (offset < 0 || length < 0 || offset + length > bytes.length)
/*  73 */       throw new IndexOutOfBoundsException(); 
/*  75 */     this.bytes = bytes;
/*  76 */     this.offset = offset;
/*  77 */     this.length = length;
/*     */   }
/*     */   
/*     */   int length() {
/*  81 */     return this.length;
/*     */   }
/*     */   
/*     */   boolean startsWith(AsciiBytes prefix) {
/*  85 */     if (this == prefix)
/*  86 */       return true; 
/*  88 */     if (prefix.length > this.length)
/*  89 */       return false; 
/*  91 */     for (int i = 0; i < prefix.length; i++) {
/*  92 */       if (this.bytes[i + this.offset] != prefix.bytes[i + prefix.offset])
/*  93 */         return false; 
/*     */     } 
/*  96 */     return true;
/*     */   }
/*     */   
/*     */   boolean endsWith(AsciiBytes postfix) {
/* 100 */     if (this == postfix)
/* 101 */       return true; 
/* 103 */     if (postfix.length > this.length)
/* 104 */       return false; 
/* 106 */     for (int i = 0; i < postfix.length; i++) {
/* 107 */       if (this.bytes[this.offset + this.length - 1 - i] != postfix.bytes[postfix.offset + postfix.length - 1 - i])
/* 109 */         return false; 
/*     */     } 
/* 112 */     return true;
/*     */   }
/*     */   
/*     */   AsciiBytes substring(int beginIndex) {
/* 116 */     return substring(beginIndex, this.length);
/*     */   }
/*     */   
/*     */   AsciiBytes substring(int beginIndex, int endIndex) {
/* 120 */     int length = endIndex - beginIndex;
/* 121 */     if (this.offset + length > this.bytes.length)
/* 122 */       throw new IndexOutOfBoundsException(); 
/* 124 */     return new AsciiBytes(this.bytes, this.offset + beginIndex, length);
/*     */   }
/*     */   
/*     */   boolean matches(CharSequence name, char suffix) {
/* 128 */     int charIndex = 0;
/* 129 */     int nameLen = name.length();
/* 130 */     int totalLen = nameLen + ((suffix != '\000') ? 1 : 0);
/* 131 */     for (int i = this.offset; i < this.offset + this.length; i++) {
/* 132 */       int b = this.bytes[i];
/* 133 */       int remainingUtfBytes = getNumberOfUtfBytes(b) - 1;
/* 134 */       b &= INITIAL_BYTE_BITMASK[remainingUtfBytes];
/* 135 */       for (int j = 0; j < remainingUtfBytes; j++)
/* 136 */         b = (b << 6) + (this.bytes[++i] & 0x3F); 
/* 138 */       char c = getChar(name, suffix, charIndex++);
/* 139 */       if (b <= 65535) {
/* 140 */         if (c != b)
/* 141 */           return false; 
/*     */       } else {
/* 145 */         if (c != (b >> 10) + 55232)
/* 146 */           return false; 
/* 148 */         c = getChar(name, suffix, charIndex++);
/* 149 */         if (c != (b & 0x3FF) + 56320)
/* 150 */           return false; 
/*     */       } 
/*     */     } 
/* 154 */     return (charIndex == totalLen);
/*     */   }
/*     */   
/*     */   private char getChar(CharSequence name, char suffix, int index) {
/* 158 */     if (index < name.length())
/* 159 */       return name.charAt(index); 
/* 161 */     if (index == name.length())
/* 162 */       return suffix; 
/* 164 */     return Character.MIN_VALUE;
/*     */   }
/*     */   
/*     */   private int getNumberOfUtfBytes(int b) {
/* 168 */     if ((b & 0x80) == 0)
/* 169 */       return 1; 
/* 171 */     int numberOfUtfBytes = 0;
/* 172 */     while ((b & 0x80) != 0) {
/* 173 */       b <<= 1;
/* 174 */       numberOfUtfBytes++;
/*     */     } 
/* 176 */     return numberOfUtfBytes;
/*     */   }
/*     */   
/*     */   public boolean equals(Object obj) {
/* 181 */     if (obj == null)
/* 182 */       return false; 
/* 184 */     if (this == obj)
/* 185 */       return true; 
/* 187 */     if (obj.getClass() == AsciiBytes.class) {
/* 188 */       AsciiBytes other = (AsciiBytes)obj;
/* 189 */       if (this.length == other.length) {
/* 190 */         for (int i = 0; i < this.length; i++) {
/* 191 */           if (this.bytes[this.offset + i] != other.bytes[other.offset + i])
/* 192 */             return false; 
/*     */         } 
/* 195 */         return true;
/*     */       } 
/*     */     } 
/* 198 */     return false;
/*     */   }
/*     */   
/*     */   public int hashCode() {
/* 203 */     int hash = this.hash;
/* 204 */     if (hash == 0 && this.bytes.length > 0) {
/* 205 */       for (int i = this.offset; i < this.offset + this.length; i++) {
/* 206 */         int b = this.bytes[i];
/* 207 */         int remainingUtfBytes = getNumberOfUtfBytes(b) - 1;
/* 208 */         b &= INITIAL_BYTE_BITMASK[remainingUtfBytes];
/* 209 */         for (int j = 0; j < remainingUtfBytes; j++)
/* 210 */           b = (b << 6) + (this.bytes[++i] & 0x3F); 
/* 212 */         if (b <= 65535) {
/* 213 */           hash = 31 * hash + b;
/*     */         } else {
/* 216 */           hash = 31 * hash + (b >> 10) + 55232;
/* 217 */           hash = 31 * hash + (b & 0x3FF) + 56320;
/*     */         } 
/*     */       } 
/* 220 */       this.hash = hash;
/*     */     } 
/* 222 */     return hash;
/*     */   }
/*     */   
/*     */   public String toString() {
/* 227 */     if (this.string == null)
/* 228 */       if (this.length == 0) {
/* 229 */         this.string = "";
/*     */       } else {
/* 232 */         this.string = new String(this.bytes, this.offset, this.length, StandardCharsets.UTF_8);
/*     */       }  
/* 235 */     return this.string;
/*     */   }
/*     */   
/*     */   static String toString(byte[] bytes) {
/* 239 */     return new String(bytes, StandardCharsets.UTF_8);
/*     */   }
/*     */   
/*     */   static int hashCode(CharSequence charSequence) {
/* 244 */     if (charSequence instanceof StringSequence)
/* 246 */       return charSequence.hashCode(); 
/* 248 */     return charSequence.toString().hashCode();
/*     */   }
/*     */   
/*     */   static int hashCode(int hash, char suffix) {
/* 252 */     return (suffix != '\000') ? (31 * hash + suffix) : hash;
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\AsciiBytes.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */