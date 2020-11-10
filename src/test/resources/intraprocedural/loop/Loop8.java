package intraprocedural.loop;

public class Loop8 {

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        for (int i = 0; i < 2; i++) {
            a = a + b;
            for (int j = 4; j >= 2; j--) {
                b = a + b;
            }
        }
    }

}
