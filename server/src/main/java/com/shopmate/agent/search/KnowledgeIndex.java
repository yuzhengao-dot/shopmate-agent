package com.shopmate.agent.search;

import com.shopmate.agent.model.KnowledgeArticle;
import com.shopmate.agent.model.KnowledgeHit;
import com.shopmate.agent.repository.KnowledgeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量知识检索：BM25 打分 + 中文 bigram 分词。
 *
 * <p>不依赖向量库和 embedding 模型，启动即建索引、离线可跑，演示模式下同样有效。
 * 中文没有空格，这里用字符 bigram（「退货」「运费」）做主要特征，再用单字特征兜底，
 * 但要给单字降权，避免「的 / 了 / 吗」这类高频字拉高无关文档的分数。
 * 需要换成语义检索时，只要替换本类，对上层工具无影响。</p>
 */
@Component
public class KnowledgeIndex {

    private static final Pattern LATIN_WORD = Pattern.compile("[a-z0-9]+");
    private static final Pattern HAN_RUN = Pattern.compile("[\\u4e00-\\u9fa5]+");

    /** BM25 参数：词频饱和系数与长度归一化系数，取常见经验值 */
    private static final double K1 = 1.2;
    private static final double B = 0.75;
    private static final double UNIGRAM_WEIGHT = 0.35;
    /** 相对最高分低于该值的视为噪声，直接丢弃，避免把无关规则塞给模型 */
    private static final double MIN_MATCH = 0.45;

    /**
     * 口语里的通用词，几乎每篇规则都会出现，只会把无关文档拉进结果；
     * 只在查询侧剔除，不影响文档索引。
     */
    private static final Set<String> STOP_TERMS = Set.of(
            "b:多久", "b:什么", "b:怎么", "b:多少", "b:可以", "b:请问", "b:我想", "b:帮我",
            "b:一下", "b:我的", "b:你们", "b:能否", "b:是否", "b:有没", "b:没有", "b:这个", "b:那个");
    private static final int SNIPPET_LENGTH = 96;

    private record Doc(KnowledgeArticle article, Map<String, Integer> freq, int length) {
    }

    private final KnowledgeRepository repository;
    private final List<Doc> docs = new ArrayList<>();
    private final Map<String, Integer> docFreq = new HashMap<>();
    private double avgLength = 1;

    public KnowledgeIndex(KnowledgeRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void build() {
        docs.clear();
        docFreq.clear();
        long totalLength = 0;
        for (KnowledgeArticle article : repository.findAll()) {
            List<String> terms = tokenize(article.title() + " " + article.content());
            Map<String, Integer> freq = new HashMap<>();
            for (String term : terms) {
                freq.merge(term, 1, Integer::sum);
            }
            for (String term : freq.keySet()) {
                docFreq.merge(term, 1, Integer::sum);
            }
            docs.add(new Doc(article, freq, terms.size()));
            totalLength += terms.size();
        }
        avgLength = docs.isEmpty() ? 1 : (double) totalLength / docs.size();
    }

    public List<KnowledgeHit> search(String query, int topK) {
        if (query == null || query.isBlank() || docs.isEmpty()) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>(tokenize(query));
        terms.removeAll(STOP_TERMS);
        if (terms.isEmpty()) {
            return List.of();
        }

        List<Map.Entry<Doc, Double>> scored = new ArrayList<>();
        for (Doc doc : docs) {
            double score = 0;
            for (String term : terms) {
                int tf = doc.freq().getOrDefault(term, 0);
                if (tf == 0) {
                    continue;
                }
                int df = docFreq.getOrDefault(term, 0);
                double idf = Math.log(1 + (docs.size() - df + 0.5) / (df + 0.5));
                double norm = tf * (K1 + 1) / (tf + K1 * (1 - B + B * doc.length() / avgLength));
                score += idf * norm * (term.startsWith("u:") ? UNIGRAM_WEIGHT : 1.0);
            }
            if (score > 0) {
                scored.add(Map.entry(doc, score));
            }
        }
        if (scored.isEmpty()) {
            return List.of();
        }

        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        double best = scored.get(0).getValue();
        int limit = Math.max(1, Math.min(topK, scored.size()));
        List<KnowledgeHit> hits = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            double match = Math.round(scored.get(i).getValue() / best * 100) / 100.0;
            if (i > 0 && match < MIN_MATCH) {
                break;
            }
            Doc doc = scored.get(i).getKey();
            hits.add(new KnowledgeHit(doc.article().id(), doc.article().title(), doc.article().category(),
                    doc.article().content(), snippet(doc.article().content(), terms), match));
        }
        return hits;
    }

    /** 中文切 bigram + 单字，英文与数字按整词切；统一带前缀，避免两类特征混淆 */
    static List<String> tokenize(String text) {
        List<String> terms = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return terms;
        }
        String lower = text.toLowerCase(Locale.ROOT);

        Matcher latin = LATIN_WORD.matcher(lower);
        while (latin.find()) {
            terms.add("w:" + latin.group());
        }

        Matcher han = HAN_RUN.matcher(lower);
        while (han.find()) {
            String run = han.group();
            for (int i = 0; i < run.length(); i++) {
                terms.add("u:" + run.charAt(i));
                if (i + 1 < run.length()) {
                    terms.add("b:" + run.substring(i, i + 2));
                }
            }
        }
        return terms;
    }

    /** 摘录时尽量从命中的词开始截取，让用户一眼看到匹配位置 */
    private static String snippet(String content, Set<String> terms) {
        if (content == null || content.isBlank()) {
            return "";
        }
        if (content.length() <= SNIPPET_LENGTH) {
            return content;
        }
        int anchor = -1;
        for (String term : terms) {
            if (!term.startsWith("b:")) {
                continue;
            }
            int idx = content.indexOf(term.substring(2));
            if (idx >= 0 && (anchor < 0 || idx < anchor)) {
                anchor = idx;
            }
        }
        if (anchor < 0) {
            return content.substring(0, SNIPPET_LENGTH) + "…";
        }
        int start = Math.max(0, anchor - 24);
        int end = Math.min(content.length(), start + SNIPPET_LENGTH);
        return (start > 0 ? "…" : "") + content.substring(start, end)
                + (end < content.length() ? "…" : "");
    }
}