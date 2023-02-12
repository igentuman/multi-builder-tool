package igentuman.mbtool.util;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourceLoader {
    private final String runtimePathName;
    private final String assetPathName;
    private final Class jarClass;

    public ResourceLoader(Class jarClass, File runtimeFile, String assetPathName) {
        this.assetPathName = assetPathName;
        this.runtimePathName = runtimeFile.getAbsolutePath();
        this.jarClass = jarClass;
    }

    public ResourceLoader(Class jarClass, String runtimePathName, String assetPathName) {
        this.runtimePathName = runtimePathName;
        this.assetPathName = assetPathName;
        this.jarClass = jarClass;
    }

    public Map<String, InputStream> getResources() {
        Map<String, InputStream> result = new HashMap();
        File runtimePath = new File(this.runtimePathName);
        int var5;
        if (runtimePath.exists() && runtimePath.isDirectory()) {
            File[] var3 = runtimePath.listFiles();
            int var4 = var3.length;

            for(var5 = 0; var5 < var4; ++var5) {
                File file = var3[var5];

                try {
                    result.put(file.getName(), new FileInputStream(file));
                } catch (FileNotFoundException var11) {
                }
            }
        }
        URL srcUrl = this.jarClass.getResource("/" + this.assetPathName);
        if (srcUrl == null || !srcUrl.getProtocol().equals("jar")) {
            return null;
        }

        try {
            JarURLConnection jarURLConnection = (JarURLConnection)srcUrl.openConnection();
            ZipFile zipFile = jarURLConnection.getJarFile();
            Enumeration zipEntries = zipFile.entries();

            while(zipEntries.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry)zipEntries.nextElement();
                String zipName = zipEntry.getName();
                if (zipName.startsWith(this.assetPathName)) {
                    String filename = zipName.substring(this.assetPathName.length());
                    if (!result.keySet().contains(filename) && filename.length() != 0) {
                        result.put(filename, zipFile.getInputStream(zipEntry));
                    }
                }
            }
        } catch (IOException var12) {
            var12.printStackTrace();
        }

        return result;
    }
}
