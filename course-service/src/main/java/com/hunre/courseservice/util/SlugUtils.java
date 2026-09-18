package com.hunre.courseservice.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTI_HYPHEN = Pattern.compile("-+");

    private SlugUtils() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Thay chữ Đ/đ tiếng Việt sang d
        String normalized = input.replace('Đ', 'D').replace('đ', 'd');

        // Bỏ dấu tiếng Việt (Normalizer NFD)
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");

        String noWhitespace = WHITESPACE.matcher(normalized.trim()).replaceAll("-");
        String normalizedLatin = NON_LATIN.matcher(noWhitespace).replaceAll("");
        String slug = MULTI_HYPHEN.matcher(normalizedLatin).replaceAll("-");

        return slug.toLowerCase(Locale.ENGLISH);
    }
}
