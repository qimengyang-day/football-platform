package com.football.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 拼音工具类
 */
public class PinyinUtil {
    
    /**
     * 将中文转换为拼音（小写，无音调）
     * @param chinese 中文字符串
     * @return 拼音字符串
     */
    public static String chineseToPinyin(String chinese) {
        if (chinese == null || chinese.trim().isEmpty()) {
            return "";
        }
        
        StringBuilder pinyin = new StringBuilder();
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        
        char[] chars = chinese.toCharArray();
        try {
            for (char c : chars) {
                // 如果是中文
                if (Character.toString(c).matches("[\\u4e00-\\u9fa5]")) {
                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null && pinyinArray.length > 0) {
                        pinyin.append(pinyinArray[0]);
                    } else {
                        // 如果转换失败，保留原字符
                        pinyin.append(c);
                    }
                } else {
                    // 非中文字符直接保留
                    pinyin.append(c);
                }
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            e.printStackTrace();
            return chinese; // 出错时返回原文
        }
        
        return pinyin.toString();
    }
    
    /**
     * 生成唯一的用户名（如果重复则添加数字后缀）
     * @param baseUsername 基础用户名
     * @param existingUsernames 已存在的用户名列表
     * @return 唯一的用户名
     */
    public static String generateUniqueUsername(String baseUsername, java.util.Set<String> existingUsernames) {
        if (!existingUsernames.contains(baseUsername.toLowerCase())) {
            return baseUsername.toLowerCase();
        }
        
        // 如果重复，从 1 开始添加数字后缀
        int counter = 1;
        String uniqueUsername;
        do {
            uniqueUsername = (baseUsername + counter).toLowerCase();
            counter++;
        } while (existingUsernames.contains(uniqueUsername));
        
        return uniqueUsername;
    }
    
    // 测试方法
    public static void main(String[] args) {
        System.out.println("韦世豪 -> " + chineseToPinyin("韦世豪"));
        System.out.println("武磊 -> " + chineseToPinyin("武磊"));
        System.out.println("张三 -> " + chineseToPinyin("张三"));
    }
}
