package com.lcd.controller;

import cn.hutool.core.collection.CollStreamUtil;
import com.lcd.utils.PdfTextChunkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/embedding")
@RequiredArgsConstructor
public class EmbeddingController {

    private final VectorStore vectorStore;
    // Tika单例解析PDF
    private final Tika tika = new Tika();
    private final PdfTextChunkUtil pdfTextChunkUtil;

    @PostMapping
    public void saveVectorStore(@RequestParam("messages") List<String> messages) {
        log.info("保存到向量数据库中，消息数据：{}", messages);
        //构建文档
        List<Document> documents = CollStreamUtil.toList(messages, message -> Document.builder()
                .text(message)
                .build());
        //存储到向量数据库中
        this.vectorStore.add(documents);
        log.info("保存到向量数据库成功, 数量：{}", messages.size());
    }
    @PostMapping("/pdf")
    public String uploadPdfToVector(@RequestParam("pdfFile") MultipartFile pdfFile) throws Exception {
        // 1. 文件基础校验
        String fileName = pdfFile.getOriginalFilename();
        if (!StringUtils.hasText(fileName)) {
            return "文件名称不能为空";
        }
        // 校验文件后缀，仅允许pdf
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!"pdf".equals(suffix)) {
            return "仅支持上传PDF格式文件";
        }
        // 校验文件大小（配合yml全局限制，双重兜底）
        long fileSize = pdfFile.getSize();
        long maxLimit = 10 * 1024 * 1024L;
        if (fileSize > maxLimit) {
            return "文件不能超过10MB";
        }

        // 2. 全部解析、切片逻辑统一复用工具类，无重复代码
        List<String> singleChunkList = pdfTextChunkUtil.parsePdfStream(pdfFile);
        log.info("文件【{}】切片总数：{}", fileName, singleChunkList.size());

        // 3. 批量写入向量库
        int batch = 10;
        List<Document> tempBatch = new ArrayList<>(batch);
        for (String chunk : singleChunkList) {
            tempBatch.add(Document.builder()
                    .text(chunk)
                    .metadata("source", fileName)
                    .build());
            if (tempBatch.size() >= batch) {
                vectorStore.add(tempBatch);
                tempBatch.clear();
                System.gc();
            }
        }
        if (!tempBatch.isEmpty()) {
            vectorStore.add(tempBatch);
            tempBatch.clear();
        }
        singleChunkList.clear();
        System.gc();
        return "上传完成";
    }


}