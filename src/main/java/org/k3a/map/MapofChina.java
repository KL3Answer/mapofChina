package org.k3a.map;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by XPS15
 * on 2017/11/9  14:36
 */
@SuppressWarnings({"SpellCheckingInspection", "ArraysAsListWithZeroOrOneArgument", "WeakerAccess", "unused"})
public class MapofChina implements Serializable {
    private static final String REGIONALISM;
    private static final Pattern REGIONALISM_REG;
    private static final Pattern REGIONALISM_WITH_LEVEL_REG;
    private static final File REGIONDATA;
    private static final List<String> AMBIGUOUS;

    private static MapofChina instance;

    static {
        //行政划分 正则
        REGIONALISM = "[\\u4e00-\\u9fa5（）()]+";
        REGIONALISM_REG = Pattern.compile(REGIONALISM);
        //带 级别 数字的
        REGIONALISM_WITH_LEVEL_REG = Pattern.compile("[0-9][\\u4e00-\\u9fa5（）()]+");
        //town 级别 数据 单文件
        REGIONDATA = new File(MapofChina.class.getClassLoader().getResource("").getPath() + "all_in_one/all_in_one_town_compact.txt");
        //遇到这些词时，需要再向上一级来寻找通俗意义上的上一级
        AMBIGUOUS = Arrays.asList("市辖区");
    }

    public static MapofChina getInstance() throws IOException {
        if (instance == null) {
            instance = new MapofChina();
        }
        return instance;
    }

    private String map;

    private MapofChina() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(REGIONDATA))) {
            this.map = reader.readLine();
        }
    }

    private Object readResolve() {
        return instance;
    }

    /**
     * get positions of search in map
     */
    private List<Integer> getPositions(String search, int maxNum) {
        int length = search.length();
        String _map = map;
        List<Integer> positions = Collections.synchronizedList(new ArrayList<>());
        int offset = 0;
        for (int i = _map.indexOf(search); i > -1; i = _map.indexOf(search)) {
            if (positions.size() < maxNum) {
                positions.add(i + offset);
                offset = i + offset + length;
                _map = _map.substring(i + length, _map.length());
            } else break;
        }
        return positions;
    }

    /**
     * 获取 search的 带级别数字的 全名
     */
    public List<String> getRegionWithLV(String search) {
        List<Integer> positions = getPositions(search, 20);
        List<String> fullName = Collections.synchronizedList(new ArrayList<>());
        positions.parallelStream().forEach(position -> {
            //to avoid side effect
            String _map = map;
            //find the level number of search
            int numPos = position - 1;
            while (_map.charAt(numPos) > 57) {
                //in case of too much loop which should not happen
                if (position - --numPos > 25) {
                    return;
                }
            }
            //upper Level
            int iUpperLV = _map.charAt(numPos) - 49;
            if (iUpperLV >= 0) {
                _map = _map.substring(numPos, _map.length());
                Matcher matcher = REGIONALISM_WITH_LEVEL_REG.matcher(_map);
                if (matcher.find()) {
                    fullName.add(matcher.group());
                }
            }
        });
        return fullName;
    }

    /**
     * get full upper levels
     */
    public Set<String> fullPath(String search) {
        //1.0 build tree
        Node node = buildNodes(new Node(search), 5);
        //concat values of nodes
        return collapse(node).parallelStream().map(e -> e.replace(">", "")).collect(Collectors.toSet());
    }

    /**
     * concat node value
     */
    public Set<String> collapse(Node node) {
        //0.0 collpase childs
        node.child.forEach(this::collapse);
        //1.0 collpase this with childs
        node.child.forEach(e -> {
            if (e.collapsed.size() == 0) {
                e.collapsed.add(e.value);
            }
            node.collapsed.addAll(e.collapsed.stream().map(s -> s + node.value).collect(Collectors.toSet()));
        });
        //2.0 result
        return node.collapsed;
    }


    /**
     * build nodes ,#{max} level at most
     */
    private Node buildNodes(Node root, int upperThan) {
        Set<String> superior = superior(root.value, upperThan - 1);
        superior.parallelStream().forEach(e -> {
            if (!"".equals(e)) {
                Node node = new Node(e);
                root.child.add(node);
                node.parent = root;
            }
        });
        root.child.parallelStream().forEach(e -> {
            //to be more precise
            e.value = e.value + ">";
            buildNodes(e, upperThan - 1);
        });
        return root;
    }

    /**
     * find upper level with defualt min level
     */
    public Set<String> upperLevel(String search) {
        return upperLevel(search, 20, 4);
    }

    /**
     * find upper level ,technically
     */
    public Set<String> upperLevel(String search, int max, int notLowerThan) {
        Set<String> upperLevels = Collections.synchronizedSet(new HashSet<>());
        try {
            //get 20 positions at most
            List<Integer> positions = getPositions(search, max);
            positions.parallelStream().forEach(position -> {
                //to avoid side effect
                String _map = map;
                //the level number of search
                int numPos = position - 1;
                while (_map.charAt(numPos) > 57) {
                    //in case of too much loop which should not happen
                    if (position - --numPos > 25) {
                        return;
                    }
                }
                //upper Level
                int iUpperLV = _map.charAt(numPos) - 49;
                if (iUpperLV <= notLowerThan && iUpperLV > -1) {
                    //upper level position
                    int iUpperPos = (_map = _map.substring(0, position)).lastIndexOf(String.valueOf(iUpperLV));
                    //
                    Matcher matcher = REGIONALISM_REG.matcher(_map.substring(iUpperPos, _map.length()));
                    if (matcher.find()) upperLevels.add(matcher.group());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return upperLevels;
    }

    /**
     * find superior with default min level
     */
    public Set<String> superior(String search) {
        return superior(search, 4);
    }

    /**
     * get conventional superior level(ignore AMBIGUOUS words)
     */
    public Set<String> superior(String search, int notLowerThan) {
        Set<String> superiors = Collections.synchronizedSet(new HashSet<>());
        Set<String> trash = Collections.synchronizedSet(new HashSet<>());
        //
        Set<String> set = upperLevel(search, 20, notLowerThan - 1);
        set.parallelStream().forEach(result -> {
            for (String e : AMBIGUOUS) {
                if (result.contains(e)) {
                    trash.add(result);
                    superiors.addAll(superior(result, notLowerThan));
                }
            }
        });
        //remove trash
        set.removeAll(trash);
        //results
        superiors.addAll(set);
        return superiors;
    }

    /**
     * find next lower levels with default max level
     */
    public Set<List<String>> lowerLevels(String search) {
        return lowerLevels(search, 10, 0);
    }

    /**
     * find next lower levels ,technically
     */
    public Set<List<String>> lowerLevels(String search, int max, int notUpperThan) {
        Set<List<String>> lowerLevels = Collections.synchronizedSet(new HashSet<>());
        try {
            List<Integer> positions = getPositions(search, max);
            positions.parallelStream().forEach(position -> {
                List<String> list = new ArrayList<>();
                //to avoid side effect
                String _map = map;
                //the level number of search
                int numPos = position - 1;
                while (_map.charAt(numPos) > 57) {
                    //in case of too much loop which should not happen
                    if (position - --numPos > 25) {
                        return;
                    }
                }
                //lower Level
                int iLowerLV = _map.charAt(numPos) - 47;
                if (iLowerLV >= notUpperThan && iLowerLV > 0) {
                    _map = _map.substring(position, _map.length());
                    int end = _map.indexOf(String.valueOf(iLowerLV - 1)) > 0 ? _map.indexOf(String.valueOf(iLowerLV - 1)) : _map.length();
                    Matcher matcher = Pattern.compile(iLowerLV + "(" + REGIONALISM + ")").matcher(_map.substring(0, end));
                    while (matcher.find()) list.add(matcher.group(1));
                    if (list.size() > 0) lowerLevels.add(list);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lowerLevels;
    }

    /**
     * find superior with default max level
     */
    public Set<List<String>> inferior(String search) {
        return inferior(search, 0);
    }

    /**
     * find conventional inferior level(ignore AMBIGUOUS words)
     */
    public Set<List<String>> inferior(String search, int notUpperThan) {
        Set<List<String>> inferior = Collections.synchronizedSet(new HashSet<>());
        Set<String> trash = Collections.synchronizedSet(new HashSet<>());
        //
        Set<List<String>> set = lowerLevels(search, 20, notUpperThan + 1);
        set.parallelStream().forEach(result -> {
            for (String e : AMBIGUOUS) {
                result.forEach(lower -> {
                    if (lower.contains(e)) {
                        trash.add(lower);
                        inferior.addAll(inferior(lower, notUpperThan));
                    }
                });

            }
        });
        //remove trash
        Iterator<List<String>> iterator = set.iterator();
        while (iterator.hasNext()) {
            List<String> next = iterator.next();
            if (next == null || next.size() == 0) {
                iterator.remove();
            } else {
                next.forEach(str -> {
                    if (trash.contains(str)) {
                        iterator.remove();
                    }
                });
            }
        }
        //results
        inferior.addAll(set);
        return inferior;
    }

    /**
     * get random address from search
     */
    public String random(String search) {
        Set<List<String>> set = this.lowerLevels(search);
        List<String>[] arr = set.toArray(new List[set.size()]);
        if (arr.length > 0) {
            Random random = new Random();
            int i = random.nextInt(arr.length);
            List<String> strings = arr[i];
            if (strings.size() == 0) {
                return search;
            }
            String next = strings.get(random.nextInt(strings.size()));
            search = search + random(next);
        }
        return search;
    }

}
