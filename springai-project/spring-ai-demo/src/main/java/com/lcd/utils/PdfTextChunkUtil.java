package com.lcd.utils;


import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


@Component
public class PdfTextChunkUtil {
    // 缩小单块上限，减少单次字符串体积
    private static final int CHUNK_MAX_SIZE = 500;
    private static final int CHUNK_OVERLAP = 100;
    // 过滤低于100字符无效碎片
    private static final int MIN_CHUNK_LEN = 100;

    public String cleanRawText(String rawText) {
        if (rawText == null || rawText.isBlank()) return "";
        // 流式处理，不一次性拼接超大字符串
        return rawText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .reduce("", (a, b) -> a + "\n" + b)
                .trim();
    }

    public List<String> splitTextChunk(String cleanText) {
        List<String> chunkList = new ArrayList<>();
        int start = 0;
        int textLen = cleanText.length();
        final int CHUNK_MAX_SIZE = 500;
        final int CHUNK_OVERLAP = 100;
        final int MIN_CHUNK_LEN = 100;
        // 循环次数上限，防止死循环
        int loopCount = 0;
        final int MAX_LOOP = 1000;

        while (start < textLen) {
            loopCount++;
            if (loopCount > MAX_LOOP) {
                break;
            }

            int end = Math.min(start + CHUNK_MAX_SIZE, textLen);
            String tempBlock = new String(cleanText.substring(start, end));

            int splitPos = findLastSentenceSplit(tempBlock);
            if (splitPos > CHUNK_MAX_SIZE * 0.5) {
                end = start + splitPos;
                tempBlock = new String(cleanText.substring(start, end));
            }

            String finalChunk = tempBlock.trim();
            if (finalChunk.length() >= MIN_CHUNK_LEN) {
                chunkList.add(finalChunk);
            }

            // 关键：强制窗口必须向前走，卡死死循环
            int nextStart = end - CHUNK_OVERLAP;
            if (nextStart <= start) {
                nextStart = start + 1;
            }
            start = nextStart;
        }
        cleanText = null;
        return chunkList;
    }


    private int findLastSentenceSplit(String text) {
        int d = text.lastIndexOf('。');
        int n = text.lastIndexOf('\n');
        int q = text.lastIndexOf('？');
        return Math.max(Math.max(d, n), q);
    }

    public List<String> parsePdfStream(MultipartFile file) throws Exception {
        List<String> result = new ArrayList<>();
        Tika tika = new Tika();
        Metadata meta = new Metadata();
        String fullText = "";

        // 全部文本处理逻辑缩进进try内部
        try (TikaInputStream tis = TikaInputStream.get(file.getInputStream());
             Reader reader = tika.parse(tis, meta)) {

            // 1. Reader转完整PDF文本
            StringBuilder sbRaw = new StringBuilder();
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                sbRaw.append(buf, 0, len);
            }
            fullText = sbRaw.toString();

            // 2. 切片逻辑放入try内部
            BufferedReader br = new BufferedReader(new StringReader(fullText));
            StringBuilder sb = new StringBuilder();
            String line;
            final int MAX_BLOCK = 500;
            final int OVERLAP = 100;

            while ((line = br.readLine()) != null) {
                String trimLine = line.trim();
                if (trimLine.isBlank()) continue;
                sb.append(trimLine).append("\n");
                if (sb.length() >= MAX_BLOCK) {
                    String block = sb.toString();
                    int splitIdx = Math.max(findLastSentenceSplit(block), (int)(MAX_BLOCK * 0.6));
                    String chunk = block.substring(0, splitIdx).trim();
                    if (chunk.length() > 100) {
                        result.add(chunk);
                    }
                    sb = new StringBuilder(block.substring(splitIdx - OVERLAP));
                }
            }
            // 末尾剩余文本
            String last = sb.toString().trim();
            if (last.length() > 100) {
                result.add(last);
            }
            br.close();
        }
        return result;
    }





}