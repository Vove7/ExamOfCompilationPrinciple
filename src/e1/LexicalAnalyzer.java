package e1;

import utils.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * 词法分析器
 * Created by IntelliJ IDEA.
 * User: Vove
 * Date: 2018/7/2
 * Time: 13:06
 */
public class LexicalAnalyzer {
    private List<Word> wordList;//

    //当前行字符列下标
    private int currentCol = 0;
    //当前行字符数组
    private char[] lineChars;
    //当前行
    private int row = 1;

    //单词列,当前行单词序号
    private int wordCol = 1;

    /**
     * 进行分析
     *
     * @param file 输入文件
     */
    public void analysis(File file) {
        wordList = new ArrayList<>();
        Scanner scanner;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.out.println("文件未找到！");
            return;
        }
        row = 1;
        while (scanner.hasNextLine()) {//分析行
            String line = scanner.nextLine();
            lineChars = line.toCharArray();
            currentCol = 0;
            wordCol = 1;
            while (currentCol != lineChars.length) {
                begin();//0节点
            }
            row++;
        }
    }

    /**
     * 起始节点
     */
    private void begin() {
        while (currentCol < lineChars.length &&
                lineChars[currentCol] == ' ') {
            currentCol++;
        }
        char c = lineChars[currentCol];
        if (Util.isLetter(c)) {//分析字母关键字
            getWordOrKey();
        } else if (Util.isNumber(c)) {//分析数字
            getNumber();
        } else if (Util.isArithmeticWord(lineChars[currentCol])) {//算数运算
            getArithmeticWord();
        } else if (Util.isRelationOperator(c)) {//关系运算
            getRelationOperator();
        } else if (delimiters.containsValue(c + "")) {
            wordList.add(new Word(c + "", 2, c + "", row, wordCol++));
            currentCol++;
        } else {//ERROR
            wordList.add(new Word(c + "", 5, c + "", row, wordCol++, true));
            currentCol++;
        }
    }

    /**
     * 获取算数运算符
     */
    private void getArithmeticWord() {//算数运算
        StringBuilder numBuilder = new StringBuilder();
        while (currentCol < lineChars.length &&
                Util.isArithmeticWord(lineChars[currentCol])) {
            numBuilder.append(lineChars[currentCol]);
            currentCol++;
        }
        String num = numBuilder.toString();
        if (arithmeticWords.containsValue(num)) {
            wordList.add(new Word(num, 3, num, row, wordCol++));
        } else {//ERROR
            wordList.add(new Word(num, 3, num, row, wordCol++, true));
        }
    }

    /**
     * 获取关系运算符
     */
    private void getRelationOperator() {
        StringBuilder sBuilder = new StringBuilder();
        while (Util.isRelationOperator(lineChars[currentCol])) {
            sBuilder.append(lineChars[currentCol]);
            currentCol++;
        }

        String o = sBuilder.toString();
        if (relationOperators.containsValue(o)) {
            wordList.add(new Word(o, 4, o, row, wordCol++));
        } else {//ERROR
            wordList.add(new Word(o, 4, o, row, wordCol++, true));
        }
    }

    /**
     * 获取数字
     */
    private void getNumber() {
        StringBuilder numBuilder = new StringBuilder();
        while (currentCol < lineChars.length &&
                (Util.isNumber(lineChars[currentCol]) ||
                Util.isLetter(lineChars[currentCol]) || lineChars[currentCol] == '.')) {
            numBuilder.append(lineChars[currentCol]);
            currentCol++;
        }
        String num = numBuilder.toString();
        try {
            Integer.parseInt(num);
            wordList.add(new Word(num, 5, num, row, wordCol++));
        } catch (NumberFormatException e) {//ERROR
            //e.printStackTrace();
            wordList.add(new Word(num, 5, num, row, wordCol++, true));
        }
    }


    /**
     * 获取标识符或关键字
     */
    private void getWordOrKey() {
        StringBuilder wordBuilder = new StringBuilder();
        while (currentCol < lineChars.length && (Util.isNumber(lineChars[currentCol]) ||
                Util.isLetter(lineChars[currentCol]))) {
            wordBuilder.append(lineChars[currentCol]);
            currentCol++;
        }
        String word = wordBuilder.toString();
        //单词构造完毕
        //关键字、符号
        if (keyWords.containsValue(word)) {//关键字
            wordList.add(new Word(word, 1, word, row, wordCol++));
        } else {//符号
            wordList.add(new Word(word, 6, word, row, wordCol++));
        }
    }


    /**
     * 输出字符表
     */
    public void printWords() {
        System.out.println(String.format("%-10s%-10s%-10s%10s", "单词", "二元序列", "类 型", "位置（行，列）"));
        for (Word w : wordList) {
            System.out.println(w);
        }
    }

    //分界符
    private static final Map<Integer, String> delimiters
            = new MapBuilder<Integer, String>()
            .add(0, ",")
            .add(1, ";")
            .add(2, "(")
            .add(3, ")")
            .add(4, "[")
            .add(5, "]")
            .map();

    //关键字
    private static final Map<Integer, String> keyWords
            = new MapBuilder<Integer, String>()
            .add(0, "do")
            .add(1, "end")
            .add(2, "for")
            .add(3, "if")
            .add(4, "printf")
            .add(5, "scanf")
            .add(6, "then")
            .add(7, "while")
            .map();
    //算数运算符
    private static final Map<Integer, String> arithmeticWords
            = new MapBuilder<Integer, String>()
            .add(0x10, "+")
            //.add(0x10, "++")
            .add(0x11, "-")
            //.add(0x11, "--")
            .add(0x20, "*")
            .add(0x21, "/")
            .map();
    //关系运算符
    private static final Map<Integer, String> relationOperators
            = new MapBuilder<Integer, String>()
            .add(0x0, "<")
            .add(0x1, "<=")
            .add(0x2, "=")
            .add(0x3, ">")
            .add(0x4, ">+")
            .add(0x5, "<>")
            .map();
}

class MapBuilder<KeyType, DataType> {
    private Map<KeyType, DataType> map = new HashMap<>();

    MapBuilder<KeyType, DataType> add(KeyType k, DataType value) {
        map.put(k, value);
        return this;
    }

    Map<KeyType, DataType> map() {
        return map;
    }
}

/**
 * 单词
 */
class Word {
    private String word;
    /**
     * wordType：
     * 1：关键字
     * 2：分界符
     * 3：算数运算符
     * 4：关系运算符
     * 5：常数
     * 6：标识符
     */
    private int wordType;//单词种别
    private String wordAttr;//单词属性
    private String type;//类型
    private int row;//行
    private int col;//列
    private boolean error;

    @Override
    public String toString() {
        String t;
        if (!error)
            t = String.format("(%d,%s)", wordType, wordAttr);
        else
            t = "ERROR";
        return String.format("%-10s%-15s%-10s (%d,%d)", word, t, type, row, col);
    }

    private static final String[] types = new String[]{
            "", "关键字", "分界符", "算数运算符", "关系运算符", "常数", "标识符"
    };

    public Word(String word, int wordType, String wordAttr, int row, int col, boolean error) {
        this.word = word;
        this.wordType = wordType;
        this.wordAttr = wordAttr;
        this.type = types[wordType];
        this.row = row;
        this.col = col;
        this.error = error;
        if (error) {
            type = "ERROR\t";
        }
    }

    public Word(String word, int wordType, String wordAttr, int row, int col) {
        this(word, wordType, wordAttr, row, col, false);
    }

}