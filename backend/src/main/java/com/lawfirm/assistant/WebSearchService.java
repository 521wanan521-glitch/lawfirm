package com.lawfirm.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.common.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 免注册联网搜索：依次尝试 Sogou / Bing / DuckDuckGo 的公开结果页并解析。
 * 仅使用 JDK 内置 HttpClient，不引入额外依赖。作为 AI 助手 search_web 工具的数据源。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchService {

    private final ObjectMapper mapper;
    private final WebSearchProperties props;

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private static final Pattern A_TAG = Pattern.compile("<a[^>]+href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SNIPPET_SOGOU = Pattern.compile("<div class=\"(?:text-layout|space-txt|fz-mid)[^\"]*\"[^>]*>([\\s\\S]*?)</div>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SNIPPET_BING = Pattern.compile("<p[^>]*>([\\s\\S]*?)</p>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SNIPPET_DDG = Pattern.compile("class=\"result__snippet\"[^>]*>([\\s\\S]*?)</a>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_H = Pattern.compile("<h[123][^>]*>([\\s\\S]*?)</h[123]>", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * 执行一次联网搜索，返回 JSON：{engine, total, items:[{title,url,snippet}]} 或 {error}。
     */
    public String search(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BizException("缺少搜索关键词");
        }
        String q = query.trim();
        List<String> failures = new ArrayList<>();
        for (String engine : props.getEngines()) {
            String name = engine == null ? "" : engine.trim().toLowerCase();
            try {
                List<Map<String, String>> items = switch (name) {
                    case "sogou" -> searchSogou(q);
                    case "bing" -> searchBing(q);
                    case "duckduckgo", "ddg" -> searchDuckDuckGo(q);
                    default -> List.of();
                };
                if (items.isEmpty()) {
                    failures.add(name + "(无结果)");
                    continue;
                }
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("engine", name);
                out.put("total", items.size());
                out.put("items", items);
                return mapper.writeValueAsString(out);
            } catch (Exception e) {
                log.warn("搜索源 {} 失败：{}", name, e.getMessage());
                failures.add(name + "(" + e.getMessage() + ")");
            }
        }
        try {
            return mapper.writeValueAsString(Map.of("error",
                    "联网搜索失败：" + String.join("; ", failures) + "。请稍后重试或换一个关键词。"));
        } catch (Exception e) {
            return "{\"error\":\"联网搜索失败\"}";
        }
    }

    // ==================== 各搜索源实现 ====================

    private List<Map<String, String>> searchSogou(String query) throws Exception {
        String url = "https://www.sogou.com/web?query=" + enc(query)
                + "&num=" + Math.max(props.getMaxResults(), 10);
        String html = fetch(url, "https://www.sogou.com/");
        List<Map<String, String>> items = new ArrayList<>();
        String[] blocks = html.split("<div class=\"vrwrap\">");
        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            String title = null;
            String href = null;
            Matcher h = TITLE_H.matcher(block);
            if (h.find()) {
                Matcher a = A_TAG.matcher(h.group(1));
                if (a.find()) {
                    title = clean(a.group(2));
                    href = a.group(1);
                }
            }
            if (title == null || href == null || href.startsWith("javascript:")) {
                continue;
            }
            href = resolveSogouLink(href);
            Matcher s = SNIPPET_SOGOU.matcher(block);
            String snippet = s.find() ? clean(s.group(1)) : clean(block);
            items.add(item(title, href, snippet));
            if (items.size() >= props.getMaxResults()) {
                break;
            }
        }
        return items;
    }

    private List<Map<String, String>> searchBing(String query) throws Exception {
        String url = "https://www.bing.com/search?q=" + enc(query)
                + "&count=" + Math.max(props.getMaxResults(), 10) + "&mkt=zh-CN";
        String html = fetch(url, "https://www.bing.com/");
        List<Map<String, String>> items = new ArrayList<>();
        String[] blocks = html.split("<li class=\"b_algo\"");
        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            String title = null;
            String href = null;
            Matcher h = TITLE_H.matcher(block);
            if (h.find()) {
                Matcher a = A_TAG.matcher(h.group(1));
                if (a.find()) {
                    title = clean(a.group(2));
                    href = a.group(1);
                }
            }
            if (title == null || href == null || href.startsWith("javascript:")) {
                continue;
            }
            if (href.startsWith("/")) {
                href = "https://www.bing.com" + href;
            }
            Matcher s = SNIPPET_BING.matcher(block);
            String snippet = s.find() ? clean(s.group(1)) : clean(block);
            items.add(item(title, href, snippet));
            if (items.size() >= props.getMaxResults()) {
                break;
            }
        }
        return items;
    }

    private List<Map<String, String>> searchDuckDuckGo(String query) throws Exception {
        String url = "https://html.duckduckgo.com/html/?q=" + enc(query);
        String html = fetch(url, "https://html.duckduckgo.com/");
        List<Map<String, String>> items = new ArrayList<>();
        String[] blocks = html.split("class=\"result results_links");
        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            Matcher a = A_TAG.matcher(block);
            String title = null;
            String href = null;
            while (a.find()) {
                if (a.group(0).contains("result__a")) {
                    title = clean(a.group(2));
                    href = a.group(1);
                    break;
                }
            }
            if (title == null || href == null) {
                continue;
            }
            href = resolveDdgLink(href);
            Matcher s = SNIPPET_DDG.matcher(block);
            String snippet = s.find() ? clean(s.group(1)) : clean(block);
            items.add(item(title, href, snippet));
            if (items.size() >= props.getMaxResults()) {
                break;
            }
        }
        return items;
    }

    // ==================== 辅助方法 ====================

    /** Sogou 结果链接多为 /link?url=... 跳转，尝试解析为真实地址 */
    private String resolveSogouLink(String href) {
        if (!href.contains("/link?url=")) {
            return href;
        }
        String target = href.startsWith("http") ? href : "https://www.sogou.com" + href;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(target))
                    .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                    .header("User-Agent", UA)
                    .header("Referer", "https://www.sogou.com/")
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            String host = resp.uri().getHost();
            if (host != null && !host.contains("sogou.com")) {
                return resp.uri().toString();
            }
        } catch (Exception e) {
            log.debug("解析搜狗跳转链接失败：{}", e.getMessage());
        }
        return target;
    }

    /** DuckDuckGo 结果链接为 //duckduckgo.com/l/?uddg=... 跳转，取出真实地址 */
    private String resolveDdgLink(String href) {
        try {
            if (href.contains("uddg=")) {
                int s = href.indexOf("uddg=") + 5;
                int e = href.indexOf('&', s);
                String enc = e > 0 ? href.substring(s, e) : href.substring(s);
                String decoded = URLDecoder.decode(enc, StandardCharsets.UTF_8);
                if (decoded.startsWith("http")) {
                    return decoded;
                }
            }
        } catch (Exception ignored) {
        }
        return href.startsWith("//") ? "https:" + href : href;
    }

    private Map<String, String> item(String title, String url, String snippet) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("title", truncate(title, 120));
        m.put("url", decodeEntities(url));
        m.put("snippet", truncate(snippet, 300));
        return m;
    }

    private String fetch(String url, String referer) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .header("User-Agent", UA)
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Referer", referer)
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new BizException("HTTP " + resp.statusCode());
        }
        return resp.body();
    }

    private String clean(String html) {
        if (html == null) {
            return "";
        }
        String t = html.replaceAll("<[^>]*>", " ");
        t = decodeEntities(t);
        return t.replaceAll("\\s+", " ").trim();
    }

    /** 解码 HTML 实体（&amp;、&#数字;、&#x十六进制; 等） */
    private String decodeEntities(String t) {
        if (t == null) {
            return "";
        }
        t = t.replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
                .replace("&nbsp;", " ").replace("&ensp;", " ").replace("&emsp;", " ")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
        Matcher num = Pattern.compile("&#(\\d+);").matcher(t);
        StringBuilder sb = new StringBuilder();
        while (num.find()) {
            try {
                num.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(num.group(1)))));
            } catch (Exception e) {
                num.appendReplacement(sb, " ");
            }
        }
        num.appendTail(sb);
        t = sb.toString();
        Matcher hex = Pattern.compile("&#x([0-9a-fA-F]+);").matcher(t);
        sb = new StringBuilder();
        while (hex.find()) {
            try {
                hex.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(hex.group(1), 16))));
            } catch (Exception e) {
                hex.appendReplacement(sb, " ");
            }
        }
        hex.appendTail(sb);
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s == null ? "" : s;
        }
        return s.substring(0, max) + "…";
    }

    private String enc(String q) {
        return URLEncoder.encode(q, StandardCharsets.UTF_8);
    }
}
