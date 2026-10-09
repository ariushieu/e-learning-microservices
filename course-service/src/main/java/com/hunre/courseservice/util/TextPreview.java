package com.hunre.courseservice.util;

/** Một dòng ngắn cho hộp thư thông báo. */
public final class TextPreview {
    public static final int LENGTH = 140;

    private TextPreview() {}

    /** Gộp khoảng trắng và xuống dòng, cắt ở {@value #LENGTH} ký tự, không cắt đôi emoji. */
    public static String of(String content) {
        String flat = content.strip().replaceAll("\\s+", " ");
        if (flat.length() <= LENGTH) return flat;
        int end = LENGTH;
        if (Character.isHighSurrogate(flat.charAt(end - 1))) end--;
        return flat.substring(0, end).stripTrailing() + "…";
    }
}
