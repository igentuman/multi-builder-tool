package igentuman.mbtool.util;

import com.google.common.io.ByteStreams;
import igentuman.mbtool.Mbtool;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarExtract {

    public JarExtract() {
    }

    public static int copy(String src, File dst) {
        return extract(src, dst.getPath());
    }

    private static int extract(String src, String dst) {
        URL srcUrl = Mbtool.class.getResource("/" + src);
        if (srcUrl != null && srcUrl.getProtocol().equals("jar")) {
            int count = 0;

            try {
                JarURLConnection jarURLConnection = (JarURLConnection)srcUrl.openConnection();
                ZipFile zipFile = jarURLConnection.getJarFile();
                Enumeration zipEntries = zipFile.entries();

                while(zipEntries.hasMoreElements()) {
                    ZipEntry zipEntry = (ZipEntry)zipEntries.nextElement();
                    String zipName = zipEntry.getName();
                    if (zipName.startsWith(src)) {
                        File dstFile = new File(dst + File.separator + zipName.substring(src.length()));
                        if (zipEntry.isDirectory()) {
                            dstFile.mkdir();
                        } else {
                            InputStream inputStream = zipFile.getInputStream(zipEntry);
                            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(dstFile));
                            ByteStreams.copy(inputStream, outputStream);
                            inputStream.close();
                            outputStream.close();
                            ++count;
                        }
                    }
                }
            } catch (IOException var12) {
                var12.printStackTrace();
            }

            return count;
        } else {
            return 0;
        }
    }
}