package com.sincronizador.infrastructure.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public final class Md5Utils {

    private Md5Utils() {}

    public static String md5Hex(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            return toHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("Falha ao calcular MD5 do arquivo: " + file.getName(), e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
