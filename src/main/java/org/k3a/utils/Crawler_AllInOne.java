package org.k3a.utils;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by XPS15
 * on 2017/11/8  16:53
 * <p>
 * 从 中国统计局 中 获取 行政划分 数据
 * <p>
 * 单线程 爬取数据 并 放入 一个 文件 中
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class Crawler_AllInOne {
    private static final Logger LOGGER = Logger.getLogger(Crawler_AllInOne.class);
    //统计局 url
    private static final String BASEURi = "http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2016/index.html";
    //行政单位名的css 样式
    private static final String REG_CSS = "(province|city|county|town)tr";
    //寻找 中文
    private static final Pattern REG_CHINESE = Pattern.compile("[\\u4e00-\\u9fa5]+");
    //存储 的路径 和 文件全名
    private static final String FILEPATH = "/mapInfo/all_in_one/";

    public static void run(int maxLevel) {
        new Crawler_AllInOne().start(maxLevel);
    }

    private String file;

    private Crawler_AllInOne() {
        //create directory
        File file = new File(FILEPATH);
        if (!file.exists()) file.mkdirs();
        //避免覆盖 上一次 的文件
        this.file = FILEPATH + "all_in_one." + UUID.randomUUID() + ".txt";
    }

    /**
     * 单个线程 存入 单个文件中 , level -> 爬取 的最大层次
     */
    private void start(int maxLevel) {
        try {
            Document doc = Jsoup.parse(new URL(BASEURi).openStream(), "GBK", BASEURi);
            find(doc, 0, maxLevel);
            System.out.println("finished...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     */
    private void find(Document doc, int initLevel, int maxLevel) {
        Elements select = doc.select("tr[class~=" + REG_CSS + "]");
        for (Element next : select) {
            Elements a = next.select("a");
            for (Element next1 : a) {
                String href = next1.attr("href");
                String innerHtml = next1.html();
                Matcher matcher = REG_CHINESE.matcher(innerHtml);
                //地名 必然包含 中文
                if (matcher.find()) {
                    //记录 地名
                    String name = line(initLevel) + matcher.group();
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                        writer.write(name);
                        //compact or beautified
                        //writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //限制 层次
                    if (initLevel < maxLevel) {
                        String baseUri = doc.baseUri();
                        href = baseUri.substring(0, baseUri.lastIndexOf("/")) + "/" + href;
                        overAgain(href, initLevel, maxLevel, 4);
                    }
                }
            }
        }
    }

    /**
     * 失败 则 尝试 times 遍后 放弃
     */
    private void overAgain(String href, int level, int maxLevel, int times) {
        try {
            Document doc = Jsoup.parse(new URL(href).openStream(), "GBK", href);
            //避免网站挂掉
            Thread.sleep(100);
            find(doc, level + 1, maxLevel);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("<---------" + href + "-------->");
            while (times > 0) {
                times--;
                overAgain(href, level, maxLevel, times);
            }
        }
    }

    /**
     * 层级线
     */
    private static String line(int level) {
        StringBuilder sb = new StringBuilder("-");
        while (level > 0) {
            sb.append("-");
            level--;
        }
        return sb.toString();
    }

}
