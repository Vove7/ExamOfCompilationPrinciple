package e2;

import java.io.File;

/**
 * Created by Vove.
 * Date: 2018/7/2
 */
public class Exam2 {
    public static void main(String[] args) {
        //LL1 l = new LL1(new File("src/t2-g.txt"));
        //
        //l.analysis("i+i*i#");
        //l.analysis("(i+(i*i))#");
        //l.analysis("i(i*i)#");


        LL1 l2=new LL1(new File("src/t2-2.txt"));
        l2.analysis("i+(i+i*i)#");
    }
}
