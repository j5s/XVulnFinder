package com.emyiqing.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileUtil {
    public static String readFile(String filename) throws IOException {
        InputStream r = new FileInputStream(filename);
        ByteArrayOutputStream byteData = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        byte[] context;
        int i;
        while ((i = r.read(temp)) > 0) {
            byteData.write(temp, 0, i);
        }
        context = byteData.toByteArray();
        String str = new String(context, StandardCharsets.UTF_8);
        r.close();
        byteData.close();
        return str;
    }
}
