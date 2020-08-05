package com.ripon.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    // 前缀树节点类
    private class TrieNode {
        // keyword结束标志
        private boolean isKeywordEnd = false;

        // 子节点集合
        private Map<Character, TrieNode> subNodes = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        public TrieNode getSubNode(Character c) {
            return subNodes.get(c);
        }

        public void addSubNode(Character c, TrieNode node) {
            subNodes.put(c, node);
        }
    }

    // 替换符
    private static final String REPLACEMENT = "***";

    // 根节点
    private TrieNode root = new TrieNode();

    // 初始化构建Trie树
    @PostConstruct
    public void init() {
        try (
                InputStream stream = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                InputStreamReader reader = new InputStreamReader(stream);
                BufferedReader bufferedReader = new BufferedReader(reader);
        ) {
            String keyWord;
            while ((keyWord = bufferedReader.readLine()) != null) {
                this.addKeyWord(keyWord);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 向Trie树添加敏感词
    private void addKeyWord(String keyWord) {
        TrieNode currentNode = root;
        for (int i = 0; i < keyWord.length(); i++) {
            Character ch = keyWord.charAt(i);
            TrieNode subNode = currentNode.getSubNode(ch);
            if (subNode == null) {
                subNode = new TrieNode();
                currentNode.addSubNode(ch, subNode);
            }
            currentNode = subNode;
            if (i == keyWord.length() - 1) {
                currentNode.setKeywordEnd(true);
            }
        }
    }

    /**
     * 过滤敏感词
     * @param str 待过滤的字符串
     * @return 过滤后的字串串
     */
    public String filter(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        int start = 0;
        int end = 0;
        TrieNode currentNode = root;
        StringBuilder sb = new StringBuilder();
        while (end != str.length()) {
            Character c = str.charAt(end);
            if (isSymbol(c)) {
                // 符号在开头，不替换
                if (currentNode == root) {
                    sb.append(c);
                    start++;
                }
                // 符号在中间，忽略并跳过
                end++;
                continue;
            }
            currentNode = currentNode.getSubNode(c);
            if (currentNode == null) {
                // 当前字符未匹配，则两指针都从start指针下一步开始走；
                sb.append(c);
                end = ++start;
                currentNode = root;
            } else if (currentNode.isKeywordEnd) {
                // 匹配，且为敏感词结尾字符，则两指针从end指针下一步开始走
                sb.append(REPLACEMENT);
                start = ++end;
                currentNode = root;
            } else {
                // 匹配，但不是敏感词结尾字符，则end指针继续往下走
                end++;
            }
        }
        // 复制末尾字符
        sb.append(str.substring(start));
        return sb.toString();
    }

    // 判断字符是否是符号
    private boolean isSymbol(Character c) {
        // 0x2E80~0x9FFF 是东亚文字范围
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }
}
