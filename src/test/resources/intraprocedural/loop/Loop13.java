package intraprocedural.loop;

public class Loop13 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        for (int i = 0; i < 2; i++) {
            a = a + b;
            for (int j = 4; j >= 2; j--) {
                b = a + b;
            }
        }
    }

}
