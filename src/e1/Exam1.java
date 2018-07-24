package e1;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Vove
 * Date: 2018/7/2
 * Time: 13:05
 */
public class Exam1 {

    public static void main(String[] args) {
        LexicalAnalyzer a = new LexicalAnalyzer();
        a.analysis(new File("src/t1-1.txt"));
        a.printWords();
        a.analysis(new File("src/t1-2.txt"));
        a.printWords();


    }
}
