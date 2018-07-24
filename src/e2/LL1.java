package e2;

import utils.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Vove.
 * Date: 2018/7/2
 */
public class LL1 {
    private File infile;//输入文件
    private Map<String, ArrayList<String>> grammerMap = new HashMap<>();//存储文法
    private String begin;//文法开始符号
    private Map<String, Set<Character>> firstSet = new HashMap<>();//FIRST集
    private Map<String, Set<Character>> followSet = new HashMap<>();//FOLLOW集
    private Set<Character> terminatorSet = new HashSet<>();//终结符集

    /**
     * 预测分析表
     * key: 非终结符
     * value:key :终结符
     * value:value: 产生式
     */
    private Map<String, Map<Character, String>> forecastAnalysisTable = new HashMap<>();


    public LL1() {
    }

    public LL1(File infile) {
        terminatorSet.add('#');
        this.infile = infile;
        try {//分析记录每个产生式
            Scanner scanner = new Scanner(infile);
            boolean f = true;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] ts = line.split("->");
                if (f) {
                    begin = ts[0];
                    f = false;
                }
                if (grammerMap.containsKey(ts[0])) {
                    grammerMap.get(ts[0]).addAll(t(ts[1]));
                } else {
                    grammerMap.put(ts[0], t(ts[1]));
                    firstSet.put(ts[0], new HashSet<>());
                    followSet.put(ts[0], new HashSet<>());
                }
                //获取所有终结符
                for (char c : ts[1].toCharArray()) {
                    if (isTerminator(c)) {
                        terminatorSet.add(c);
                    }
                }
            }

            calFirstSet();
            calFollowSet();
            buildForecastAnalysisTable();
            printForecastAnalysisTable();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private ArrayList<String> t(String text) {

        String[] ss = text.split("\\|");

        return new ArrayList<>(Arrays.asList(ss));
    }


    /**
     * 利用分析表 分析
     *
     * @see #forecastAnalysisTable
     */
    public boolean analysis(String expression) {
        Stack<String> stack = new Stack<>();
        stack.push("#");
        stack.push(begin);
        ArrayDeque<Character> characters = new ArrayDeque<>();
        for (char c : expression.toCharArray()) {
            characters.add(c);
        }
        logln(String.format("%-8s%-18s%-16s%-16s%-10s", "步骤", "分析栈", "剩余输入串", "所用产生式", "动作"));
        int step = 0;
        String l = "";
        String action = "初始化";
        while (!characters.isEmpty()) {
            log(String.format("%-10d", step++));
            log(String.format("%-20s", Util.stackElem(stack)));//分析栈
            log(String.format("%-20s", Util.queueElem(characters)));//剩余输入串
            String g = stack.pop();//出符号栈
            char q = characters.getFirst();

            if (g.equals(q + "")) {//相同
                characters.pop();//出队列
                log(String.format("%-20s", l));//产生式
                l = "";
                log(String.format("%-20s", action));
                action = "";
                System.out.println();
                continue;
            }
            log(String.format("%-20s", l));//产生式

            if (l.equals("") && action.equals("")) {
                action = "GETNEXT(I)";
            }
            log(String.format("%-20s", action));
            action = "";

            //产生式
            String production;
            try {
                production = forecastAnalysisTable.get(g).get(q);
            } catch (Exception e) {
                logln("\n失败");
                return false;
            }
            l = production;//下轮输出
            if (production != null) {
                production = production.split("->")[1];
            } else {//错误
                logln("\n失败");
                return false;
            }
            List<String> gs = transNonterminal(production);
            boolean p = false;
            StringBuilder ss = new StringBuilder();
            for (String s : gs) {
                if (!s.equals("ε")) {
                    p = true;
                    ss.append(s);
                    stack.push(s);
                }
            }
            action += "POP";

            if (p) {
                action += ",PUSH(" + ss.toString() + ")";
            }
            System.out.println();
        }
        System.out.println("解析成功");
        return true;
    }


    /**
     * α转产生式数组
     * ABcD'E  -> [E,D',cB,A]
     *
     * @param a 产生式右式
     * @return 逆序数组
     */
    private List<String> transNonterminal(String a) {
        List<String> list = new ArrayList<>();
        int l = a.length();
        int p = 0;
        while (p < l) {
            String s = nextNonterminal(a, p);
            p += s.length();
            list.add(s);
        }
        Collections.reverse(list);
        return list;
    }

    /**
     * 计算FIRST集
     * 第一轮 主要计算//X->a..| X->ε 从第二轮 不再计算
     * 此后主要计算包含关系X->Y... FIRST(Y)->FIRST(X)
     */
    public void calFirstSet() {
        int c = 0, old = -1;
        boolean first = true;
        while (old != c) {
            old = c;
            c = 0;
            for (String g : grammerMap.keySet()) {//推导式
                for (String gg : grammerMap.get(g)) {
                    char s = gg.charAt(0);
                    if (isTerminator(s)) {//X->a..|  X->ε
                        if (first) {
                            firstSet.get(g).add(s);//加g的first集
                        }
                    } else {//X->Y...
                        String f = nextNonterminal(gg, 0);
                        Set<Character> fset = new HashSet<>(firstSet.get(f));
                        fset.remove('ε');
                        firstSet.get(g).addAll(fset);
                    }
                }
                c += firstSet.get(g).size();
            }
            first = false;
        }

        for (String k : firstSet.keySet()) {
            System.out.println(String.format("FIRST(%s): %s", k, firstSet.get(k)));
        }
    }

    /**
     * 下一个非终结符
     * 区分A和A'
     *
     * @param gg    A->α 右式
     * @param start index
     * @return 下一个非终结符
     */
    private String nextNonterminal(String gg, int start) {
        return gg.substring(start, (gg.length() > start + 1) && gg.charAt(start + 1) == '\'' ? start + 2 : start + 1);
    }

    /**
     * 计算FOLLOW集
     * 第一轮 主要计算 A->αBβ 将FIRST(β)\ε -> FIRST(B) 从第二轮 不再计算
     * 此后主要计算包含关系A->αB FOLLOW(A)->FOLLOW(B)
     */
    public void calFollowSet() {
        //起始符
        int c = 0, old = -1, kase = 1;
        boolean first = true;
        followSet.get(begin).add('#');
        while (c != old) {
            old = c;
            c = 0;
            log("Case:" + kase++);
            for (String g : grammerMap.keySet()) {//推导式
                log("匹配 " + g);
                for (String gg : grammerMap.keySet()) {
                    for (String ggg : grammerMap.get(gg)) {//遍历所有右式  gg:A  g:B  ggg:αBβ
                        int p = matchAndNextPot(ggg, g);
                        if (p <= 0)
                            continue;
                        log("  " + gg + " -> " + ggg + "\t");
                        //匹配到
                        if (p >= ggg.length()) {//A->αB
                            if (g.equals(gg)) {//自身 A->aA
                                logln("自身");
                                continue;
                            }
                            logln(" FOLLOW(" + gg + ")->FOLLOW(" + g + ")");
                            followSet.get(g).addAll(followSet.get(gg));
                        } else {//A->αBβ
                            //循环
                            String next;
                            boolean allNull = true;//A->αBε
                            while (p < ggg.length()) {//FOLLOW(A)  S->ABCDEF
                                //β
                                next = nextNonterminal(ggg, p);
                                boolean br = true;
                                if (next.length() == 1 && isTerminator(next.charAt(0))) {//终结符
                                    logln(next + "->FOLLOW(" + g + ")");
                                    followSet.get(g).add(next.charAt(0));
                                    allNull = false;
                                    break;
                                }
                                //A->αBβ
                                if (first) {
                                    logln("FIRST(" + next + ")/ε->" + "FOLLOW(" + g + ")");
                                    Set<Character> fset = new HashSet<>(firstSet.get(next));
                                    fset.remove('ε');
                                    followSet.get(g).addAll(fset);
                                }
                                if (firstSet.get(next).contains('ε')) {//A->αBε
                                    logln("A->αBε 继续");
                                    br = false;//继续next
                                } else {
                                    allNull = false;
                                }
                                if (br)
                                    break;
                                else
                                    p += next.length();
                            }
                            if (allNull) {
                                logln(" FOLLOW(" + gg + ")->FOLLOW(" + g + ")");
                                followSet.get(g).addAll(followSet.get(gg));
                            }
                        }
                    }
                }
                c += followSet.get(g).size();
            }
            first = false;
        }
        for (String k : followSet.keySet()) {
            System.out.println(String.format("FOLLOW(%s): %s", k, followSet.get(k)));
        }
    }

    /**
     * 匹配右式
     *
     * @param text  α of A->α
     * @param match 非终结符
     * @return -1 匹配失败 else 下一下标
     */
    private static int matchAndNextPot(String text, String match) {
        Pattern pattern = Pattern.compile(match);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            int end = matcher.end();
            if (end < text.length()) {
                if (text.charAt(end) == '\'') {
                    return -1;
                }
            }
            return end;
        }
        return -1;
    }

    /**
     * 构建预测分析表
     */
    public void buildForecastAnalysisTable() {
        for (String A : grammerMap.keySet()) {
            for (char a : firstSet.get(A)) {
                if (a != 'ε') {//A->α a∈FIRST(α) A->α -> [A,a]
                    String g = A + "->" + getProduction(A, a);
                    push2Table(A, a, g);
                } else {//b∈FOLLOW(A) A->α ->[A,b]
                    for (char c : followSet.get(A))
                        push2Table(A, c, A + "->ε");
                }
            }
        }
    }

    /**
     * 输出分析表
     */
    public void printForecastAnalysisTable() {
        System.out.println("预测分析表：");
        boolean first = true;
        StringBuilder header = new StringBuilder("\t");
        for (String g : forecastAnalysisTable.keySet()) {

            Map<Character, String> m = forecastAnalysisTable.get(g);
            StringBuilder builder = new StringBuilder(g).append('\t');
            for (char c : terminatorSet) {
                if (first) {
                    header.append(String.format("%-12s", c + ""));
                }
                builder.append(String.format("%-12s", (m.get(c))));
            }
            if (first) {
                System.out.println(header.toString());
                first = false;
            }
            System.out.println(builder.toString());
        }
    }

    /**
     * 放入分析表
     * @param A
     * @param c
     * @param s
     */
    private void push2Table(String A, char c, String s) {
        if (forecastAnalysisTable.containsKey(A)) {
            forecastAnalysisTable.get(A).put(c, s);
        } else {
            HashMap<Character, String> m = new HashMap<>();
            m.put(c, s);
            forecastAnalysisTable.put(A, m);
        }
    }

    /**
     * 获取产生式
     *
     * @param k A
     * @param a ∈FIRST
     * @return a在FIRST(A)中的产生式
     */
    private String getProduction(String k, char a) {
        String g = nextNonterminal(k, 0);
        for (String s : grammerMap.get(k)) {
            if (isTerminator(s.charAt(0))) {//终结符
                if (a == s.charAt(0)) {
                    return s;
                }
            }
            //非终结符
            else if (firstSet.get(g).contains(a)) {//在FIRST
                return s;
            } else if (firstSet.get(g).contains('ε')) {
                // A->BCDE|XYZ (B->ε,C->ε,D->a)?   计算B.FOLLOW .contains(a)
                if (followSet.get(g).contains(a)) {
                    return s;
                }
            }
        }

        return null;
    }


    /**
     * 是否为终结符
     *
     * @param c c
     * @return is:true ,else false
     */
    public boolean isTerminator(char c) {
        return !(c >= 'A' && c <= 'Z');
    }

    boolean out = true;

    private void log(String msg) {
        if (out)
            System.out.print(msg);
    }

    private void logln(String msg) {
        if (out)
            System.out.println(msg);
    }
}
