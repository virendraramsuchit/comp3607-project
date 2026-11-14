package com.comp3607_project;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

// Helpers for finding BlueJ source files
public final class SourceFiles {

    private static final String JAVA = ".java";
    private static final String STRIDE = ".stride";

    private SourceFiles() { }

    
    public static String simpleName(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    
    public static String baseName(String fileName) {
        String base;
        if (fileName.endsWith(JAVA)) {
            base = fileName.substring(0, fileName.length() - JAVA.length());
        } else if (fileName.endsWith(STRIDE)) {
            base = fileName.substring(0, fileName.length() - STRIDE.length());
        } else {
            return null;
        }
        return isValidClassName(base) ? base : null;
    }

    public static boolean isValidClassName(String s) {
        if (s == null || s.isEmpty() || !Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // Maps each class name with a source file directly inside dir to a fingerprint of that file's contents. 
    public static Map<String, String> snapshot(Path dir) throws IOException {
        Map<String, String> result = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                String base = baseName(p.getFileName().toString());
                if (base != null && Files.isRegularFile(p)) {
                    String fingerprint;
                    try {
                        fingerprint = fingerprint(p);
                    } catch (IOException e) {
                        fingerprint = null;
                    }
                    result.put(base, fingerprint);
                }
            }
        }
        return result;
    }

    // SHA-256 of the file's bytes, as lowercase hex.
    public static String fingerprint(Path file) throws IOException {
        byte[] digest = sha256().digest(Files.readAllBytes(file));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }
}