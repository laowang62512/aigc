package com.lcd.tools;

import com.lcd.utils.RagUtil;
import lombok.AllArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@AllArgsConstructor
public class DiseaseStandardSearchTool {
    private RagUtil ragSearchUtil;

    @Tool(
            description="检索慢性病知识库，向量+关键词混合检索，自动过滤低相似度文档、语义重排；可指定PDF文件名、疾病分类过滤，用于查询诊断标准、用药方案、并发症、饮食运动建议"
    )
    public List<Document> searchDiseaseStandard(
            @ToolParam(description="用户提问内容，例如：糖尿病用药规范") String searchQuery,
            @ToolParam(description="可选：指定PDF文件名过滤，不需要填空", required=false) String sourceFileName,
            @ToolParam(description="可选：指定疾病过滤：2型糖尿病/原发性高血压/混合型高脂血症，不需要填空", required=false) String diseaseType
    ){
        // 调用混合检索
        return ragSearchUtil.hybridSearch(searchQuery, sourceFileName, diseaseType);
    }
}