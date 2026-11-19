package com.comp3607_project;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Static helpers for recognising BlueJ source files ({@code .java} and {@code .stride}) and
 * fingerprinting their content. It cannot be instantiated.
 */
// Helpers for finding BlueJ source files
public final class SourceFiles {

        /**
     * {@code ".java"}
     */
    private static final String JAVA = ".java";

        /**
     * {@code ".stride"}
     */
    private static final String STRIDE = ".stride";

    private SourceFiles() { }

        /**
     * {@code "pkg.sub.Foo"} -> {@code "Foo"}; a name without dots is returned unchanged.
     *
     * @param name a class name, possibly fully qualified
     * @return the name after the last dot
     */
    public static String simpleName(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

        /**
     * {@code "Foo.java"} or {@code "Foo.stride"} -> {@code "Foo"}; returns {@code null} for other
     * files or invalid class names.
     *
     * @param fileName the source file name, or null
     * @return the class name, or {@code null} if the file is not a valid source file
     */
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

        /**
     * Returns {@code true} if the string is a valid Java identifier.
     *
     * @param s the text to process
     * @return {@code true} if the string is a valid Java identifier
     */
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

        /**
     * Maps each class with a source file directly in {@code dir} to its content fingerprint; the
     * fingerprint is {@code null} if the file could not be read.
     *
     * @param dir a package or project folder
     * @return class name to fingerprint for every source file in the folder
     * @throws IOException if the folder cannot be listed
     */
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

        /**
     * Returns the SHA-256 of the file's bytes as lowercase hex.
     *
     * @param file the source file to fingerprint
     * @return the SHA-256 of the file's bytes as lowercase hex
     * @throws IOException if the file cannot be read
     */
    // SHA-256 of the file's bytes, as lowercase hex.
    public static String fingerprint(Path file) throws IOException {
        byte[] digest = sha256().digest(Files.readAllBytes(file));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

        /**
     * Returns a SHA-256 digest.
     * @return a SHA-256 digest
     */
    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }
}
