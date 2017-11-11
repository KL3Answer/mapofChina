package org.k3a;

import org.k3a.map.MapofChina;

import java.util.Arrays;

/**
 * Created by HQ.XPS15
 * on 2017/11/10  9:34
 */
public class Demo01 {

    public static void main(String[] args) {
        System.out.println(MapofChina.superior("玄武区"));
        System.out.println(Arrays.toString(MapofChina.inferior("新疆")));//有一条街道叫新疆街道。。。
        System.out.println(Arrays.toString(MapofChina.lowerLevels("新疆维吾尔自治区")));

        int count=10;
        while(count>0){
            System.out.println(MapofChina.random("南京市"));
            count--;
        }
    }

}
