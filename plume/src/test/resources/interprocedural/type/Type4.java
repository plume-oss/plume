package intraprocedural.type;

import java.util.List;
import java.util.LinkedList;

public class Type4 {

    public static void main(String[] args) {
        List<String> x = new LinkedList<String>();
        if (x instanceof LinkedList) {
            x.add("Test");
        }
    }

}
