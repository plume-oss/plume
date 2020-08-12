package intraprocedural.loop;

public class Loop9 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        while (a < b) {
            do {
                a++;
                while (b < a) {
                    b++;
                }
            } while (a < b);
        }
        a = 3;
    }

}
