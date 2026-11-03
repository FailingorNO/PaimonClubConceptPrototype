package com.paimonclub.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 敏感词工具：DFA（字典树）实现，支持命中和替换。
 * 词典位于 classpath:/db/sensitive_words.txt，一行一个词，# 开头为注释。
 */
public final class SensitiveWordUtil {

    private static volatile Map<Character, Object> root;
    private static final List<String> WORDS = new CopyOnWriteArrayList<>();
    private static final char END = '\u0000';

    private SensitiveWordUtil() {
    }

    private static Map<Character, Object> tree() {
        Map<Character, Object> local = root;
        if (local == null) {
            synchronized (SensitiveWordUtil.class) {
                if (root == null) {
                    root = build(loadWords());
                }
                local = root;
            }
        }
        return local;
    }

    private static List<String> loadWords() {
        if (!WORDS.isEmpty()) {
            return WORDS;
        }
        try (InputStream in = SensitiveWordUtil.class.getResourceAsStream("/db/sensitive_words.txt")) {
            if (in == null) {
                return WORDS;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.trim();
                    if (!word.isEmpty() && !word.startsWith("#")) {
                        WORDS.add(word);
                    }
                }
            }
        } catch (Exception ignored) {
            // 词典缺失时降级为不过滤
        }
        return WORDS;
    }

    @SuppressWarnings("unchecked")
    private static Map<Character, Object> build(List<String> words) {
        Map<Character, Object> treeRoot = new HashMap<>();
        for (String word : words) {
            Map<Character, Object> node = treeRoot;
            for (char c : word.toCharArray()) {
                Object next = node.get(c);
                if (!(next instanceof Map)) {
                    next = new HashMap<Character, Object>();
                    node.put(c, next);
                }
                node = (Map<Character, Object>) next;
            }
            node.put(END, Boolean.TRUE);
        }
        return treeRoot;
    }

    /** 是否包含敏感词 */
    public static boolean contains(String text) {
        return firstHit(text) != null;
    }

    /** 返回第一个命中的敏感词，未命中返回 null */
    @SuppressWarnings("unchecked")
    public static String firstHit(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        Map<Character, Object> treeRoot = tree();
        if (treeRoot.isEmpty()) {
            return null;
        }
        for (int i = 0; i < text.length(); i++) {
            Map<Character, Object> node = treeRoot;
            int j = i;
            StringBuilder matched = new StringBuilder();
            while (j < text.length()) {
                Object next = node.get(text.charAt(j));
                if (!(next instanceof Map)) {
                    break;
                }
                node = (Map<Character, Object>) next;
                matched.append(text.charAt(j));
                j++;
                if (node.containsKey(END)) {
                    return matched.toString();
                }
            }
        }
        return null;
    }

    /** 将敏感词替换为等长星号 */
    @SuppressWarnings("unchecked")
    public static String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Map<Character, Object> treeRoot = tree();
        if (treeRoot.isEmpty()) {
            return text;
        }
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            Map<Character, Object> node = treeRoot;
            int j = i;
            int end = -1;
            while (j < chars.length) {
                Object next = node.get(chars[j]);
                if (!(next instanceof Map)) {
                    break;
                }
                node = (Map<Character, Object>) next;
                j++;
                if (node.containsKey(END)) {
                    end = j;
                }
            }
            if (end > i) {
                for (int k = i; k < end; k++) {
                    chars[k] = '*';
                }
                i = end - 1;
            }
        }
        return new String(chars);
    }

    /** 运行时追加敏感词（管理端新增后立即生效） */
    public static void addWord(String word) {
        if (word == null || word.isBlank() || WORDS.contains(word)) {
            return;
        }
        WORDS.add(word);
        synchronized (SensitiveWordUtil.class) {
            root = build(WORDS);
        }
    }
}
