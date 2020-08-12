package intraprocedural.loop;

public class Loop7 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        do {
            while (a < b) {
                a++;
            }
        } while (a < b);
        a = 3;
    }

}
