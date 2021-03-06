package org.k3a.utils;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by XPS15
 * on 2017/11/8  16:53
 * <p>
 * 从 中国统计局 中 获取 行政划分 数据
 * <p>
 * 使用多个线程 并 按照 省份 为单位 分割 文件
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
public class Crawler_MultiThreads {
    private static final Logger LOGGER = Logger.getLogger(Crawler_MultiThreads.class.getName());
    //统计局 统计用区 划分 url
    private static final String BASE_URI = "http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/2016/index.html";
    //判断地名
    private static final Pattern CHINESE = Pattern.compile("[\\u4e00-\\u9fa5（）()]+");
    //后缀
    private static final String FILE_SUFFIX = ".txt";
    //当前线程文件名
    private static final ThreadLocal<String> FILENAME_POOL = new ThreadLocal<>();

    public static void run(int maxLevel) {
        new Crawler_MultiThreads().start(maxLevel);
    }

    //存储 文件夹
    private String filePath;

    private Crawler_MultiThreads() {
        //create directory
        filePath = "/mapInfo/sliced/" + UUID.randomUUID() + "/";
        File file = new File(filePath);
        if (!file.exists()) file.mkdirs();
    }

    /**
     * maxLevel -> 抓取的层次
     */
    private void start(int maxLevel) {
        ExecutorService pool = Executors.newCachedThreadPool();
        try {
            Document doc = Jsoup.parse(new URL(BASE_URI).openStream(), "GBK", BASE_URI);
            Elements select = doc.select("tr[class=provincetr]");
            //i7 6700k JDK 1.8 64位  跑了四个线程
            for (Element next : select) {
                pool.execute(() -> find(doc.baseUri(), next, 0, maxLevel));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pool.shutdown();
            while (!pool.isTerminated()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("finished..");
        }
    }

    /**
     *
     */
    private void find(String baseUri, Elements select, int initLevel, int maxLevel) {
        for (Element next : select) {
            find(baseUri, next, initLevel, maxLevel);
        }
    }

    /**
     *
     */
    private void find(String baseUri, Element next, int initLevel, int maxLevel) {
        //获取 a 标签
        Elements a = next.select("a");
        for (Element next1 : a) {
            String href = next1.attr("href");
            String innerHtml = next1.html();
            Matcher matcher = CHINESE.matcher(innerHtml);
            //地名 必然包含 中文
            if (matcher.find()) {
                //设置 文件名
                if (initLevel == 0) {
                    FILENAME_POOL.set(matcher.group());
                }
                //记录 地名
                String name = line(initLevel) + matcher.group();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.filePath + FILENAME_POOL.get() + FILE_SUFFIX, true))) {
                    writer.write(name);
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //限制 层次
                if (initLevel < maxLevel) {
                    href = baseUri.substring(0, baseUri.lastIndexOf("/")) + "/" + href;
                    overAgain(href, initLevel, maxLevel, 4);
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
            Elements select = doc.select("tr[class~=(city|county|town)tr]");
            //避免网站挂掉
            Thread.sleep(100);
            find(href, select, level + 1, maxLevel);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.log(Level.WARNING, "<---------failed on:" + href + "-------->");
            while (times > 0) {
                times--;
                overAgain(href, level, times, maxLevel);
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