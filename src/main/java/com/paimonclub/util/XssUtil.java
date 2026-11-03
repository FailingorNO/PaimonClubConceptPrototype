package com.paimonclub.util;

/**
 * 输入净化工具。
 *
 * <p><b>职责边界：</b>后端只负责「把危险内容变成纯文本」，不做 HTML 实体转义；
 * 输出时的转义统一由前端 {@code PC.escapeHtml()} 负责。两端都转义会产生
 * {@code &amp;lt;} 这类展示乱码，这正是早期版本的问题。</p>
 *
 * <p><b>为什么不用正则黑名单做安全边界：</b>{@code <img onerror>}、SVG、Unicode 同形字都能绕过正则。
 * 这里的处理是「只保留纯文本」——把标签整体剥离，而不是挑几个危险标签删掉，
 * 因此不存在被绕过的标签白名单问题。真正的富文本场景应接入 OWASP Java HTML Sanitizer，
 * 本项目帖子正文按纯文本存储，不需要保留任何标签。</p>
 */
public final class XssUtil {

    private XssUtil() {
    }

    /**
     * 输入净化：剥离全部 HTML 标签、去除控制字符、裁剪首尾空白。
     * 用于昵称、签名、帖子正文、评论、私信等所有用户可写文本的入库前处理。
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String cleaned = input;
        // 1. 先整体移除「标签 + 内容」型危险块（script/style/iframe 等）
        cleaned = cleaned.replaceAll(
                "(?is)<\\s*(script|style|iframe|object|embed|svg|math|template|noscript)[^>]*>.*?<\\s*/\\s*\\1\\s*>",
                "");
        // 2. 移除残留的危险标签开闭标签
        cleaned = cleaned.replaceAll(
                "(?i)<\\s*/?\\s*(script|style|iframe|object|embed|svg|math|template|noscript)[^>]*>", "");
        // 3. 其余标签一律剥离，只保留标签内的文本（< 后必须紧跟字母或 / 才认定为标签）
        cleaned = cleaned.replaceAll("(?i)<\\s*/?\\s*[a-zA-Z][^>]*>", "");
        // 4. 去掉除换行/制表外的控制字符
        cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
        return cleaned.trim();
    }

    /**
     * HTML 实体转义。
     *
     * <p>仅在必须由后端直接输出 HTML 的场景使用（例如生成分享页）。
     * 普通 JSON 接口不要调用——前端 {@code escapeHtml()} 已经负责输出转义。</p>
     */
    public static String escape(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 帖子正文：按纯文本净化，保留换行 */
    public static String cleanContent(String input) {
        return sanitize(input);
    }

    /** 兼容旧调用名 */
    @Deprecated
    public static String stripScript(String input) {
        return sanitize(input);
    }
}
