/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.util.Arrays;
/*     */ import java.util.Collections;
/*     */ import java.util.Iterator;
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.Map;
/*     */ import java.util.NoSuchElementException;
/*     */ import java.util.jar.Attributes;
/*     */ import java.util.jar.JarEntry;
/*     */ import java.util.jar.JarInputStream;
/*     */ import java.util.jar.Manifest;
/*     */ import org.springframework.boot.loader.data.RandomAccessData;
/*     */ 
/*     */ class JarFileEntries implements CentralDirectoryVisitor, Iterable<JarEntry> {
/*     */   static {
/*     */     int version;
/*     */   }
/*     */   
/*     */   private static final Runnable NO_VALIDATION = () -> {
/*     */     
/*     */     };
/*     */   
/*     */   private static final String META_INF_PREFIX = "META-INF/";
/*     */   
/*  56 */   private static final Attributes.Name MULTI_RELEASE = new Attributes.Name("Multi-Release");
/*     */   
/*     */   private static final int BASE_VERSION = 8;
/*     */   
/*     */   private static final int RUNTIME_VERSION;
/*     */   
/*     */   private static final long LOCAL_FILE_HEADER_SIZE = 30L;
/*     */   
/*     */   private static final char SLASH = '/';
/*     */   
/*     */   private static final char NO_SUFFIX = '\000';
/*     */   
/*     */   protected static final int ENTRY_CACHE_SIZE = 25;
/*     */   
/*     */   private final JarFile jarFile;
/*     */   
/*     */   private final JarEntryFilter filter;
/*     */   
/*     */   private RandomAccessData centralDirectoryData;
/*     */   
/*     */   private int size;
/*     */   
/*     */   private int[] hashCodes;
/*     */   
/*     */   private Offsets centralDirectoryOffsets;
/*     */   
/*     */   private int[] positions;
/*     */   
/*     */   private Boolean multiReleaseJar;
/*     */   
/*     */   private JarEntryCertification[] certifications;
/*     */   
/*     */   static {
/*     */     try {
/*  65 */       Object runtimeVersion = Runtime.class.getMethod("version", new Class[0]).invoke(null, new Object[0]);
/*  66 */       version = ((Integer)runtimeVersion.getClass().getMethod("major", new Class[0]).invoke(runtimeVersion, new Object[0])).intValue();
/*  68 */     } catch (Throwable ex) {
/*  69 */       version = 8;
/*     */     } 
/*  71 */     RUNTIME_VERSION = version;
/*     */   }
/*     */   
/* 101 */   private final Map<Integer, FileHeader> entriesCache = Collections.synchronizedMap(new LinkedHashMap<Integer, FileHeader>(16, 0.75F, true) {
/*     */         protected boolean removeEldestEntry(Map.Entry<Integer, FileHeader> eldest) {
/* 105 */           return (size() >= 25);
/*     */         }
/*     */       });
/*     */   
/*     */   JarFileEntries(JarFile jarFile, JarEntryFilter filter) {
/* 111 */     this.jarFile = jarFile;
/* 112 */     this.filter = filter;
/* 113 */     if (RUNTIME_VERSION == 8)
/* 114 */       this.multiReleaseJar = Boolean.valueOf(false); 
/*     */   }
/*     */   
/*     */   public void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
/* 120 */     int maxSize = endRecord.getNumberOfRecords();
/* 121 */     this.centralDirectoryData = centralDirectoryData;
/* 122 */     this.hashCodes = new int[maxSize];
/* 123 */     this.centralDirectoryOffsets = Offsets.from(endRecord);
/* 124 */     this.positions = new int[maxSize];
/*     */   }
/*     */   
/*     */   public void visitFileHeader(CentralDirectoryFileHeader fileHeader, long dataOffset) {
/* 129 */     AsciiBytes name = applyFilter(fileHeader.getName());
/* 130 */     if (name != null)
/* 131 */       add(name, dataOffset); 
/*     */   }
/*     */   
/*     */   private void add(AsciiBytes name, long dataOffset) {
/* 136 */     this.hashCodes[this.size] = name.hashCode();
/* 137 */     this.centralDirectoryOffsets.set(this.size, dataOffset);
/* 138 */     this.positions[this.size] = this.size;
/* 139 */     this.size++;
/*     */   }
/*     */   
/*     */   public void visitEnd() {
/* 144 */     sort(0, this.size - 1);
/* 145 */     int[] positions = this.positions;
/* 146 */     this.positions = new int[positions.length];
/* 147 */     for (int i = 0; i < this.size; i++)
/* 148 */       this.positions[positions[i]] = i; 
/*     */   }
/*     */   
/*     */   int getSize() {
/* 153 */     return this.size;
/*     */   }
/*     */   
/*     */   private void sort(int left, int right) {
/* 158 */     if (left < right) {
/* 159 */       int pivot = this.hashCodes[left + (right - left) / 2];
/* 160 */       int i = left;
/* 161 */       int j = right;
/* 162 */       while (i <= j) {
/* 163 */         while (this.hashCodes[i] < pivot)
/* 164 */           i++; 
/* 166 */         while (this.hashCodes[j] > pivot)
/* 167 */           j--; 
/* 169 */         if (i <= j) {
/* 170 */           swap(i, j);
/* 171 */           i++;
/* 172 */           j--;
/*     */         } 
/*     */       } 
/* 175 */       if (left < j)
/* 176 */         sort(left, j); 
/* 178 */       if (right > i)
/* 179 */         sort(i, right); 
/*     */     } 
/*     */   }
/*     */   
/*     */   private void swap(int i, int j) {
/* 185 */     swap(this.hashCodes, i, j);
/* 186 */     this.centralDirectoryOffsets.swap(i, j);
/* 187 */     swap(this.positions, i, j);
/*     */   }
/*     */   
/*     */   public Iterator<JarEntry> iterator() {
/* 192 */     return new EntryIterator(NO_VALIDATION);
/*     */   }
/*     */   
/*     */   Iterator<JarEntry> iterator(Runnable validator) {
/* 196 */     return new EntryIterator(validator);
/*     */   }
/*     */   
/*     */   boolean containsEntry(CharSequence name) {
/* 200 */     return (getEntry(name, FileHeader.class, true) != null);
/*     */   }
/*     */   
/*     */   JarEntry getEntry(CharSequence name) {
/* 204 */     return getEntry(name, JarEntry.class, true);
/*     */   }
/*     */   
/*     */   InputStream getInputStream(String name) throws IOException {
/* 208 */     FileHeader entry = getEntry(name, FileHeader.class, false);
/* 209 */     return getInputStream(entry);
/*     */   }
/*     */   
/*     */   InputStream getInputStream(FileHeader entry) throws IOException {
/* 213 */     if (entry == null)
/* 214 */       return null; 
/* 216 */     InputStream inputStream = getEntryData(entry).getInputStream();
/* 217 */     if (entry.getMethod() == 8)
/* 218 */       inputStream = new ZipInflaterInputStream(inputStream, (int)entry.getSize()); 
/* 220 */     return inputStream;
/*     */   }
/*     */   
/*     */   RandomAccessData getEntryData(String name) throws IOException {
/* 224 */     FileHeader entry = getEntry(name, FileHeader.class, false);
/* 225 */     if (entry == null)
/* 226 */       return null; 
/* 228 */     return getEntryData(entry);
/*     */   }
/*     */   
/*     */   private RandomAccessData getEntryData(FileHeader entry) throws IOException {
/* 235 */     RandomAccessData data = this.jarFile.getData();
/* 236 */     byte[] localHeader = data.read(entry.getLocalHeaderOffset(), 30L);
/* 237 */     long nameLength = Bytes.littleEndianValue(localHeader, 26, 2);
/* 238 */     long extraLength = Bytes.littleEndianValue(localHeader, 28, 2);
/* 239 */     return data.getSubsection(entry.getLocalHeaderOffset() + 30L + nameLength + extraLength, entry
/* 240 */         .getCompressedSize());
/*     */   }
/*     */   
/*     */   private <T extends FileHeader> T getEntry(CharSequence name, Class<T> type, boolean cacheEntry) {
/* 244 */     T entry = doGetEntry(name, type, cacheEntry, null);
/* 245 */     if (!isMetaInfEntry(name) && isMultiReleaseJar()) {
/* 246 */       int version = RUNTIME_VERSION;
/* 248 */       AsciiBytes nameAlias = (entry instanceof JarEntry) ? ((JarEntry)entry).getAsciiBytesName() : new AsciiBytes(name.toString());
/* 249 */       while (version > 8) {
/* 250 */         T versionedEntry = doGetEntry("META-INF/versions/" + version + "/" + name, type, cacheEntry, nameAlias);
/* 251 */         if (versionedEntry != null)
/* 252 */           return versionedEntry; 
/* 254 */         version--;
/*     */       } 
/*     */     } 
/* 257 */     return entry;
/*     */   }
/*     */   
/*     */   private boolean isMetaInfEntry(CharSequence name) {
/* 261 */     return name.toString().startsWith("META-INF/");
/*     */   }
/*     */   
/*     */   private boolean isMultiReleaseJar() {
/* 265 */     Boolean multiRelease = this.multiReleaseJar;
/* 266 */     if (multiRelease != null)
/* 267 */       return multiRelease.booleanValue(); 
/*     */     try {
/* 270 */       Manifest manifest = this.jarFile.getManifest();
/* 271 */       if (manifest == null) {
/* 272 */         multiRelease = Boolean.valueOf(false);
/*     */       } else {
/* 275 */         Attributes attributes = manifest.getMainAttributes();
/* 276 */         multiRelease = Boolean.valueOf(attributes.containsKey(MULTI_RELEASE));
/*     */       } 
/* 279 */     } catch (IOException ex) {
/* 280 */       multiRelease = Boolean.valueOf(false);
/*     */     } 
/* 282 */     this.multiReleaseJar = multiRelease;
/* 283 */     return multiRelease.booleanValue();
/*     */   }
/*     */   
/*     */   private <T extends FileHeader> T doGetEntry(CharSequence name, Class<T> type, boolean cacheEntry, AsciiBytes nameAlias) {
/* 288 */     int hashCode = AsciiBytes.hashCode(name);
/* 289 */     T entry = getEntry(hashCode, name, false, type, cacheEntry, nameAlias);
/* 290 */     if (entry == null) {
/* 291 */       hashCode = AsciiBytes.hashCode(hashCode, '/');
/* 292 */       entry = getEntry(hashCode, name, '/', type, cacheEntry, nameAlias);
/*     */     } 
/* 294 */     return entry;
/*     */   }
/*     */   
/*     */   private <T extends FileHeader> T getEntry(int hashCode, CharSequence name, char suffix, Class<T> type, boolean cacheEntry, AsciiBytes nameAlias) {
/* 299 */     int index = getFirstIndex(hashCode);
/* 300 */     while (index >= 0 && index < this.size && this.hashCodes[index] == hashCode) {
/* 301 */       T entry = getEntry(index, type, cacheEntry, nameAlias);
/* 302 */       if (entry.hasName(name, suffix))
/* 303 */         return entry; 
/* 305 */       index++;
/*     */     } 
/* 307 */     return null;
/*     */   }
/*     */   
/*     */   private <T extends FileHeader> T getEntry(int index, Class<T> type, boolean cacheEntry, AsciiBytes nameAlias) {
/*     */     try {
/* 313 */       long offset = this.centralDirectoryOffsets.get(index);
/* 314 */       FileHeader cached = this.entriesCache.get(Integer.valueOf(index));
/* 316 */       FileHeader entry = (cached != null) ? cached : CentralDirectoryFileHeader.fromRandomAccessData(this.centralDirectoryData, offset, this.filter);
/* 317 */       if (CentralDirectoryFileHeader.class.equals(entry.getClass()) && type.equals(JarEntry.class))
/* 318 */         entry = new JarEntry(this.jarFile, index, (CentralDirectoryFileHeader)entry, nameAlias); 
/* 320 */       if (cacheEntry && cached != entry)
/* 321 */         this.entriesCache.put(Integer.valueOf(index), entry); 
/* 323 */       return (T)entry;
/* 325 */     } catch (IOException ex) {
/* 326 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   private int getFirstIndex(int hashCode) {
/* 331 */     int index = Arrays.binarySearch(this.hashCodes, 0, this.size, hashCode);
/* 332 */     if (index < 0)
/* 333 */       return -1; 
/* 335 */     while (index > 0 && this.hashCodes[index - 1] == hashCode)
/* 336 */       index--; 
/* 338 */     return index;
/*     */   }
/*     */   
/*     */   void clearCache() {
/* 342 */     this.entriesCache.clear();
/*     */   }
/*     */   
/*     */   private AsciiBytes applyFilter(AsciiBytes name) {
/* 346 */     return (this.filter != null) ? this.filter.apply(name) : name;
/*     */   }
/*     */   
/*     */   JarEntryCertification getCertification(JarEntry entry) throws IOException {
/* 350 */     JarEntryCertification[] certifications = this.certifications;
/* 351 */     if (certifications == null) {
/* 352 */       certifications = new JarEntryCertification[this.size];
/* 355 */       try (JarInputStream certifiedJarStream = new JarInputStream(this.jarFile.getData().getInputStream())) {
/* 356 */         JarEntry certifiedEntry = null;
/* 357 */         while ((certifiedEntry = certifiedJarStream.getNextJarEntry()) != null) {
/* 359 */           certifiedJarStream.closeEntry();
/* 360 */           int index = getEntryIndex(certifiedEntry.getName());
/* 361 */           if (index != -1)
/* 362 */             certifications[index] = JarEntryCertification.from(certifiedEntry); 
/*     */         } 
/*     */       } 
/* 366 */       this.certifications = certifications;
/*     */     } 
/* 368 */     JarEntryCertification certification = certifications[entry.getIndex()];
/* 369 */     return (certification != null) ? certification : JarEntryCertification.NONE;
/*     */   }
/*     */   
/*     */   private int getEntryIndex(CharSequence name) {
/* 373 */     int hashCode = AsciiBytes.hashCode(name);
/* 374 */     int index = getFirstIndex(hashCode);
/* 375 */     while (index >= 0 && index < this.size && this.hashCodes[index] == hashCode) {
/* 376 */       FileHeader candidate = getEntry(index, FileHeader.class, false, null);
/* 377 */       if (candidate.hasName(name, false))
/* 378 */         return index; 
/* 380 */       index++;
/*     */     } 
/* 382 */     return -1;
/*     */   }
/*     */   
/*     */   private static void swap(int[] array, int i, int j) {
/* 386 */     int temp = array[i];
/* 387 */     array[i] = array[j];
/* 388 */     array[j] = temp;
/*     */   }
/*     */   
/*     */   private static void swap(long[] array, int i, int j) {
/* 392 */     long temp = array[i];
/* 393 */     array[i] = array[j];
/* 394 */     array[j] = temp;
/*     */   }
/*     */   
/*     */   private final class EntryIterator implements Iterator<JarEntry> {
/*     */     private final Runnable validator;
/*     */     
/* 404 */     private int index = 0;
/*     */     
/*     */     private EntryIterator(Runnable validator) {
/* 407 */       this.validator = validator;
/* 408 */       validator.run();
/*     */     }
/*     */     
/*     */     public boolean hasNext() {
/* 413 */       this.validator.run();
/* 414 */       return (this.index < JarFileEntries.this.size);
/*     */     }
/*     */     
/*     */     public JarEntry next() {
/* 419 */       this.validator.run();
/* 420 */       if (!hasNext())
/* 421 */         throw new NoSuchElementException(); 
/* 423 */       int entryIndex = JarFileEntries.this.positions[this.index];
/* 424 */       this.index++;
/* 425 */       return (JarEntry)JarFileEntries.this.getEntry(entryIndex, (Class)JarEntry.class, false, null);
/*     */     }
/*     */   }
/*     */   
/*     */   private static interface Offsets {
/*     */     void set(int param1Int, long param1Long);
/*     */     
/*     */     long get(int param1Int);
/*     */     
/*     */     void swap(int param1Int1, int param1Int2);
/*     */     
/*     */     static Offsets from(CentralDirectoryEndRecord endRecord) {
/* 444 */       int size = endRecord.getNumberOfRecords();
/* 445 */       return endRecord.isZip64() ? new JarFileEntries.Zip64Offsets(size) : new JarFileEntries.ZipOffsets(size);
/*     */     }
/*     */   }
/*     */   
/*     */   private static final class ZipOffsets implements Offsets {
/*     */     private final int[] offsets;
/*     */     
/*     */     private ZipOffsets(int size) {
/* 458 */       this.offsets = new int[size];
/*     */     }
/*     */     
/*     */     public void swap(int i, int j) {
/* 463 */       JarFileEntries.swap(this.offsets, i, j);
/*     */     }
/*     */     
/*     */     public void set(int index, long value) {
/* 468 */       this.offsets[index] = (int)value;
/*     */     }
/*     */     
/*     */     public long get(int index) {
/* 473 */       return this.offsets[index];
/*     */     }
/*     */   }
/*     */   
/*     */   private static final class Zip64Offsets implements Offsets {
/*     */     private final long[] offsets;
/*     */     
/*     */     private Zip64Offsets(int size) {
/* 486 */       this.offsets = new long[size];
/*     */     }
/*     */     
/*     */     public void swap(int i, int j) {
/* 491 */       JarFileEntries.swap(this.offsets, i, j);
/*     */     }
/*     */     
/*     */     public void set(int index, long value) {
/* 496 */       this.offsets[index] = value;
/*     */     }
/*     */     
/*     */     public long get(int index) {
/* 501 */       return this.offsets[index];
/*     */     }
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\JarFileEntries.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */