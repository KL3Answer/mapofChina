package org.k3a;

import org.k3a.map.MapofChina;

/**
 * Created by HQ.XPS15
 * on 2017/11/10  9:34
 */
public class Demo01 {

    public static void main(String[] args) {

        try {

            MapofChina instance = MapofChina.getInstance();
            System.out.println(instance.superior("孙塬镇"));
            System.out.println(instance.lowerLevels("越秀区"));
            System.out.println(instance.inferior("北京"));
            int i = 100;
            while (i > 0) {
                System.out.println(instance.random("中华人民共和国"));
                i--;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
