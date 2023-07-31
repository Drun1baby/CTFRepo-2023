package org.springframework.boot.loader.jarmode;

public interface JarMode {
  boolean accepts(String paramString);
  
  void run(String paramString, String[] paramArrayOfString);
}


/* Location:              G:\OneDrive备份\Repo\CTF-Repo-2023-Two\2023 巅峰极客\ctf-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jarmode\JarMode.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */