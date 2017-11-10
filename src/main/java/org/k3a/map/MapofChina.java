package org.k3a.map;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by XPS15
 * on 2017/11/9  14:36
 */
@SuppressWarnings({"SpellCheckingInspection", "ArraysAsListWithZeroOrOneArgument", "WeakerAccess"})
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

    /**
     * 获取 全路径
     */
    public static String fullPath(String search) {
        StringBuilder path = new StringBuilder(search);
        do {
            search = upperLevel(search);
            boolean isAmbiguous = false;
            for (String e : AMBIGUOUS) {
                if (search.contains(e)) {
                    isAmbiguous = true;
                    break;
                }
            }
            if (!isAmbiguous) path.insert(0, search);
        } while (!"".equals(search));

        return path.toString();
    }

    /**
     * find upper level ,technically
     */
    public static String upperLevel(String search) {
        try (BufferedReader reader = new BufferedReader(new FileReader(COUNTYDATA))) {
            String map = reader.readLine();
            //0.0 return immediately  when not found
            if (!map.contains(search)) return "";

            //1.0 zoom out
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * get conventional superior level(ignore AMBIGUOUS words)
     */
    public static String superior(String search) {
        String result = upperLevel(search);
        for (String e : AMBIGUOUS) {
            if (result.contains(e)) {
                result = upperLevel(result);
            }
        }
        return result;
    }

    /**
     * find next lower levels ,technically
     */
    public static String[] lowerLevels(String search) {
        try (BufferedReader reader = new BufferedReader(new FileReader(COUNTYDATA))) {
            String map = reader.readLine();
            int i, level = 0;
            //
            if (!TOP.equals(search)) {
                //0.0 return immediately  when not found
                if (!map.contains(search)) return null;

                //1.0 set start
                int beginnen = map.indexOf(search);
                //1.1 zoom in
                for (i = beginnen - 1; i < map.length() && i > -1 && map.charAt(i) == '-'; i--) level++;
                map = map.substring(beginnen, map.length());

                //2.0 set end
                int ende = map.length();
                String haltReg = CHINESE_REG + line(level) + "(" + CHINESE_REG + ")";
                Matcher haltMachter = Pattern.compile(haltReg).matcher(map);
                if (haltMachter.find()) {
                    ende = map.indexOf(haltMachter.group(1));
                }
                //2.2 truncate
                map = map.substring(0, ende);
            }

            //3.0
            String targetReg = line(level + 1) + "(" + CHINESE_REG + ")";
            Matcher matcher = Pattern.compile(targetReg).matcher(map);
            ArrayList<String> list = new ArrayList<>();
            while (matcher.find()) {
                String target = matcher.group(0);
                if (map.charAt(map.indexOf(target) - 1) != '-') {
                    list.add(matcher.group(1));
                }
            }

            return list.toArray(new String[list.size()]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * find conventional inferior level(ignore AMBIGUOUS words)
     */
    public static String[] inferior(String search) {
        String[] result = lowerLevels(search);
        for (String e : AMBIGUOUS) {
            if (result != null) {
                for (String s : result) {
                    if (s.contains(e)) {
                        result = lowerLevels((result != null ? result.length : 0) > 0 ? result[0] : "");
                    }
                }
            }
        }
        return result;
    }

    /**
     * get random address from search
     */
    public static String random(String search) {
        String[] arr = MapofChina.lowerLevels(search);
        if (arr != null && arr.length > 0) {
            String next = arr[new Random().nextInt(arr.length)];
            search = search + random(next);
        }
        return search;
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
