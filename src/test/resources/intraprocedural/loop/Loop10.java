package intraprocedural.loop;

public class Loop10 {

    public static void main(String[] args) {
        int a = 1; // L?
        int b = 2; // L?
        for (int i = 0; i < 2; i++) {
            a = a + b;
            if (a == 5) break;
            else continue;
        }
    }

}
