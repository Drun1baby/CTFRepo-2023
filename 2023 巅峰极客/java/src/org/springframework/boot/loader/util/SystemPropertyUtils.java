/*     */ package org.springframework.boot.loader.util;
/*     */ 
/*     */ import java.util.HashSet;
/*     */ import java.util.Locale;
/*     */ import java.util.Properties;
/*     */ import java.util.Set;
/*     */ 
/*     */ public abstract class SystemPropertyUtils {
/*     */   public static final String PLACEHOLDER_PREFIX = "${";
/*     */   
/*     */   public static final String PLACEHOLDER_SUFFIX = "}";
/*     */   
/*     */   public static final String VALUE_SEPARATOR = ":";
/*     */   
/*  56 */   private static final String SIMPLE_PREFIX = "${".substring(1);
/*     */   
/*     */   public static String resolvePlaceholders(String text) {
/*  68 */     if (text == null)
/*  69 */       return text; 
/*  71 */     return parseStringValue(null, text, text, new HashSet<>());
/*     */   }
/*     */   
/*     */   public static String resolvePlaceholders(Properties properties, String text) {
/*  85 */     if (text == null)
/*  86 */       return text; 
/*  88 */     return parseStringValue(properties, text, text, new HashSet<>());
/*     */   }
/*     */   
/*     */   private static String parseStringValue(Properties properties, String value, String current, Set<String> visitedPlaceholders) {
/*  94 */     StringBuilder buf = new StringBuilder(current);
/*  96 */     int startIndex = current.indexOf("${");
/*  97 */     while (startIndex != -1) {
/*  98 */       int endIndex = findPlaceholderEndIndex(buf, startIndex);
/*  99 */       if (endIndex != -1) {
/* 100 */         String placeholder = buf.substring(startIndex + "${".length(), endIndex);
/* 101 */         String originalPlaceholder = placeholder;
/* 102 */         if (!visitedPlaceholders.add(originalPlaceholder))
/* 103 */           throw new IllegalArgumentException("Circular placeholder reference '" + originalPlaceholder + "' in property definitions"); 
/* 109 */         placeholder = parseStringValue(properties, value, placeholder, visitedPlaceholders);
/* 111 */         String propVal = resolvePlaceholder(properties, value, placeholder);
/* 112 */         if (propVal == null) {
/* 113 */           int separatorIndex = placeholder.indexOf(":");
/* 114 */           if (separatorIndex != -1) {
/* 115 */             String actualPlaceholder = placeholder.substring(0, separatorIndex);
/* 116 */             String defaultValue = placeholder.substring(separatorIndex + ":".length());
/* 117 */             propVal = resolvePlaceholder(properties, value, actualPlaceholder);
/* 118 */             if (propVal == null)
/* 119 */               propVal = defaultValue; 
/*     */           } 
/*     */         } 
/* 123 */         if (propVal != null) {
/* 126 */           propVal = parseStringValue(properties, value, propVal, visitedPlaceholders);
/* 127 */           buf.replace(startIndex, endIndex + "}".length(), propVal);
/* 128 */           startIndex = buf.indexOf("${", startIndex + propVal.length());
/*     */         } else {
/* 132 */           startIndex = buf.indexOf("${", endIndex + "}".length());
/*     */         } 
/* 134 */         visitedPlaceholders.remove(originalPlaceholder);
/*     */         continue;
/*     */       } 
/* 137 */       startIndex = -1;
/*     */     } 
/* 141 */     return buf.toString();
/*     */   }
/*     */   
/*     */   private static String resolvePlaceholder(Properties properties, String text, String placeholderName) {
/* 145 */     String propVal = getProperty(placeholderName, null, text);
/* 146 */     if (propVal != null)
/* 147 */       return propVal; 
/* 149 */     return (properties != null) ? properties.getProperty(placeholderName) : null;
/*     */   }
/*     */   
/*     */   public static String getProperty(String key) {
/* 153 */     return getProperty(key, null, "");
/*     */   }
/*     */   
/*     */   public static String getProperty(String key, String defaultValue) {
/* 157 */     return getProperty(key, defaultValue, "");
/*     */   }
/*     */   
/*     */   public static String getProperty(String key, String defaultValue, String text) {
/*     */     try {
/* 172 */       String propVal = System.getProperty(key);
/* 173 */       if (propVal == null)
/* 175 */         propVal = System.getenv(key); 
/* 177 */       if (propVal == null) {
/* 179 */         String name = key.replace('.', '_');
/* 180 */         propVal = System.getenv(name);
/*     */       } 
/* 182 */       if (propVal == null) {
/* 184 */         String name = key.toUpperCase(Locale.ENGLISH).replace('.', '_');
/* 185 */         propVal = System.getenv(name);
/*     */       } 
/* 187 */       if (propVal != null)
/* 188 */         return propVal; 
/* 191 */     } catch (Throwable ex) {
/* 192 */       System.err.println("Could not resolve key '" + key + "' in '" + text + "' as system property or in environment: " + ex);
/*     */     } 
/* 195 */     return defaultValue;
/*     */   }
/*     */   
/*     */   private static int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
/* 199 */     int index = startIndex + "${".length();
/* 200 */     int withinNestedPlaceholder = 0;
/* 201 */     while (index < buf.length()) {
/* 202 */       if (substringMatch(buf, index, "}")) {
/* 203 */         if (withinNestedPlaceholder > 0) {
/* 204 */           withinNestedPlaceholder--;
/* 205 */           index += "}".length();
/*     */           continue;
/*     */         } 
/* 208 */         return index;
/*     */       } 
/* 211 */       if (substringMatch(buf, index, SIMPLE_PREFIX)) {
/* 212 */         withinNestedPlaceholder++;
/* 213 */         index += SIMPLE_PREFIX.length();
/*     */         continue;
/*     */       } 
/* 216 */       index++;
/*     */     } 
/* 219 */     return -1;
/*     */   }
/*     */   
/*     */   private static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
/* 223 */     for (int j = 0; j < substring.length(); j++) {
/* 224 */       int i = index + j;
/* 225 */       if (i >= str.length() || str.charAt(i) != substring.charAt(j))
/* 226 */         return false; 
/*     */     } 
/* 229 */     return true;
/*     */   }
/*     */ }


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loade\\util\SystemPropertyUtils.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */