package com.lcd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rag")
public class ChunkProperties {
    // ====== 原有切片配置 ======
    private Integer maxSize = 500;
    private Integer overlap = 100;
    private Integer minLen = 100;

    // ====== 新增检索配置 ======
    private Integer vectorTopN = 8;
    private Double minSimilarity = 0.6;
    private Integer rerankTopN = 4;
    private Boolean enableRerank = true;

    // ====== getter / setter ======
    public Integer getMaxSize() {
        return maxSize;
    }
    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }
    public Integer getOverlap() {
        return overlap;
    }
    public void setOverlap(Integer overlap) {
        this.overlap = overlap;
    }
    public Integer getMinLen() {
        return minLen;
    }
    public void setMinLen(Integer minLen) {
        this.minLen = minLen;
    }

    public Integer getVectorTopN() {
        return vectorTopN;
    }
    public void setVectorTopN(Integer vectorTopN) {
        this.vectorTopN = vectorTopN;
    }
    public Double getMinSimilarity() {
        return minSimilarity;
    }
    public void setMinSimilarity(Double minSimilarity) {
        this.minSimilarity = minSimilarity;
    }
    public Integer getRerankTopN() {
        return rerankTopN;
    }
    public void setRerankTopN(Integer rerankTopN) {
        this.rerankTopN = rerankTopN;
    }
    public Boolean getEnableRerank() {
        return enableRerank;
    }
    public void setEnableRerank(Boolean enableRerank) {
        this.enableRerank = enableRerank;
    }
}