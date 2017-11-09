package org.k3a.map;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by XPS15
 * on 2017/11/9  14:36
 */
@SuppressWarnings({"SpellCheckingInspection", "ArraysAsListWithZeroOrOneArgument"})
public class MapofChina {
    private static final String TOP;
    private static final String CHINESE_REG;
    private static final File COUNTYDATA;
    private static final List<String> AMBIGUOUS;
    private static final Pattern REDUPLICATION;

    static {
        TOP = "中国";
        //中文 正则
        CHINESE_REG = "[\\u4e00-\\u9fa5]+";
        //county 级别 数据 单文件
        COUNTYDATA = new File(MapofChina.class.getClassLoader().getResource("").getPath() + "all_in_one/all_in_one_county_compact.txt");
        //遇到这些词时，需要再向上一级来寻找通俗意义上的上一级
        AMBIGUOUS = Arrays.asList("市辖区");
        //去掉 重复 字段
        REDUPLICATION = Pattern.compile("([\\u4e00-\\u9fa5]{2,12})\\1");

    }

    public static void main(String[] args) {


        System.out.println("fullpath:\n" + fullPath("乌鲁木齐"));

    }

    public static String fullPath(String search) {
        StringBuilder path = new StringBuilder(search);
        String rs = "";
        do {
            rs = upperLevel(search);
            search = rs;
            path.insert(0, rs);
        } while (!"".equals(rs));

        String result = path.toString();
        //消除 术语
        result = result.replace("市辖区", "市");
        //消除 叠词
        Matcher matcher = REDUPLICATION.matcher(result);
        while (matcher.find()) {
            String group = matcher.group(1);
            result = result.replace(group + group, group);
        }
        return result;
    }


    /**
     * find upper level technically
     */
    public static String upperLevel(String search) {
        try {
            try (BufferedReader reader = new BufferedReader(new FileReader(COUNTYDATA))) {
                String map = reader.readLine();
                //0.0 return immediately  when not found
                if (!map.contains(search)) return "";

                //1.0 zoom in
                int i, level = 0;
                for (i = map.indexOf(search) - 1; i < map.length() && i > -1 && map.charAt(i) == '-'; i--) level++;

                //如果 是 次顶级 ，直接 返回 顶级 的 划分
                if (level == 1) {
                    return TOP;
                } else if (level > 1) {
                    map = map.substring(0, i + 1);
                    //1.1 concat reg of upperLevel
                    String reg = CHINESE_REG + line(level - 1) + "(" + CHINESE_REG + ")";
                    Pattern pattern = Pattern.compile(reg);
                    Matcher matcher = pattern.matcher(map);

                    //2.0 find upperLevel
                    String upperLevel = "";
                    while (matcher.find()) upperLevel = matcher.group(1);

                    //3.0 result
                    return upperLevel;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * build #{level} size line
     */
    private static String line(int level) {
        StringBuilder sb = new StringBuilder();
        while (level > 0) {
            sb.append("-");
            level--;
        }
        return sb.toString();
    }

}
