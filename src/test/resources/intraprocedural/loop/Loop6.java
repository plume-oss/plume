package intraprocedural.loop;

public class Loop6 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        while (a < b) {
            while (a < b) {
                a++;
            }
        }
        a = 3;
    }

}
