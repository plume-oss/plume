package intraprocedural.loop;

public class Loop11 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        for (int i = 0; i < 2; i++) {
            a = a + b;
        }
    }

}
