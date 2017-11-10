package org.k3a.map;

/**
 * Created by HQ.XPS15
 * on 2017/11/10  9:34
 */
public class Demo01 {

    public static void main(String[] args) {
        System.out.println("fullpath:\n" + MapofChina.fullPath("朝阳区"));
        System.out.println("fullpath:\n" + MapofChina.fullPath("玄武区"));
        System.out.println("fullpath:\n" + MapofChina.upperLevel("朝阳区"));
        System.out.println("fullpath:\n" + MapofChina.superior("朝阳区"));


    }


}
