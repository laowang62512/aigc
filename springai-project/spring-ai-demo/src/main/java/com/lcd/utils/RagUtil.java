package com.lcd.utils;

import com.lcd.config.ChunkProperties;
import lombok.AllArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class RagUtil {

    private VectorStore vectorStore;

    private EmbeddingModel embeddingModel;

    private ChunkProperties ragProp;

    /**
     * 混合检索：向量检索 + 关键词检索 + 内存元数据过滤 + 相似度阈值 + Rerank重排
     * @param query 用户查询文本
     * @param sourceFilter 过滤PDF文件名，传空不过滤
     * @param diseaseFilter 过滤疾病标签，传空不过滤
     * @return 过滤并重排后的知识库文档
     */
    public List<Document> hybridSearch(String query, String sourceFilter, String diseaseFilter) {
        // 1. 向量相似度检索（仅基础参数，不带底层filter）
        SearchRequest vectorReq = SearchRequest.builder()
                .query(query)
                .topK(ragProp.getVectorTopN())
                .similarityThreshold(ragProp.getMinSimilarity())
                .build();
        List<Document> vectorDocs = vectorStore.similaritySearch(vectorReq);
        // 内存过滤元数据
        vectorDocs = filterDocsByMeta(vectorDocs, sourceFilter, diseaseFilter);

        // 2. 关键词召回（纯文本匹配）
        SearchRequest keywordReq = SearchRequest.builder()
                .query(query)
                .topK(ragProp.getVectorTopN())
                .build();
        List<Document> keywordDocs = vectorStore.similaritySearch(keywordReq);
        keywordDocs = filterDocsByMeta(keywordDocs, sourceFilter, diseaseFilter);

        // 3. 两路结果合并去重
        Set<String> uniqueText = new HashSet<>();
        List<Document> mixResult = new ArrayList<>();
        for (Document doc : vectorDocs) {
            String text = doc.getText();
            if (!uniqueText.contains(text)) {
                uniqueText.add(text);
                mixResult.add(doc);
            }
        }
        for (Document doc : keywordDocs) {
            String text = doc.getText();
            if (!uniqueText.contains(text)) {
                uniqueText.add(text);
                mixResult.add(doc);
            }
        }

        // 4. 开启Rerank语义重排
        if (ragProp.getEnableRerank()) {
            mixResult = rerankDocs(query, mixResult);
        }

        // 5. 截断至配置的最终返回条数
        return mixResult.stream()
                .limit(ragProp.getRerankTopN())
                .collect(Collectors.toList());
    }

    /**
     * 内存过滤文档元数据，完全避开FilterExpressionBuilder、Op等兼容报错API
     */
    private List<Document> filterDocsByMeta(List<Document> docs, String sourceFilter, String diseaseFilter) {
        return docs.stream().filter(doc -> {
            boolean match = true;
            // 文件名校验
            if (sourceFilter != null && !sourceFilter.isBlank()) {
                String sourceVal = doc.getMetadata().getOrDefault("source", "").toString();
                match = sourceVal.equals(sourceFilter);
            }
            // 疾病标签校验（前面匹配成功才继续校验）
            if (match && diseaseFilter != null && !diseaseFilter.isBlank()) {
                String diseaseVal = doc.getMetadata().getOrDefault("disease", "").toString();
                match = diseaseVal.equals(diseaseFilter);
            }
            return match;
        }).collect(Collectors.toList());
    }

    /**
     * Rerank：计算query与文档向量欧式距离，距离越小相关性越高
     */
    private List<Document> rerankDocs(String query, List<Document> docs) {
        float[] queryEmb = embeddingModel.embed(query);
        record DocItem(Document doc, double distance) {}
        List<DocItem> scoreList = new ArrayList<>();

        for (Document doc : docs) {
            float[] docEmb = embeddingModel.embed(doc.getText());
            double dist = calcEuclidean(queryEmb, docEmb);
            scoreList.add(new DocItem(doc, dist));
        }
        // 距离升序，相关度高在前
        return scoreList.stream()
                .sorted(Comparator.comparingDouble(DocItem::distance))
                .map(DocItem::doc)
                .collect(Collectors.toList());
    }

    /**
     * 计算向量欧式距离
     */
    private double calcEuclidean(float[] vecA, float[] vecB) {
        double sumSquare = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            double diff = vecA[i] - vecB[i];
            sumSquare += diff * diff;
        }
        return Math.sqrt(sumSquare);
    }

    /**
     * 无过滤条件简化重载
     */
    public List<Document> hybridSearch(String query) {
        return hybridSearch(query, null, null);
    }
}