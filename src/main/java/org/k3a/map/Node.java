package org.k3a.map;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by HQ.XPS15
 * on 2017/11/11  22:41
 * <p>
 * 行政划分 节点
 * 懒得写 getter/setter
 */
public class Node {

    public Set<String> collapsed = Collections.synchronizedSet(new HashSet<>());
    public String value;
    public Node parent;
    public Set<Node> child = Collections.synchronizedSet(new HashSet<>());

    public Node() {

    }

    public Node(String value) {
        this.value = value;
    }

    public Node(Node parent, Set<Node> child) {
        this.parent = parent;
        this.child = child;
    }
}
