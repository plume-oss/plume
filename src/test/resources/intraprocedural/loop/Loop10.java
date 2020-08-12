package intraprocedural.loop;

public class Loop10 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        while (a < b) {
            a = a - b;
            do {
                a++;
                while (b < a) {
                    b++;
                }
                a = a + b;
            } while (a < b);
            b = a / b;
        }
        a = 3;
    }

}
