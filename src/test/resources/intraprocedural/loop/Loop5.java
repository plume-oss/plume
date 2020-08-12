package intraprocedural.loop;

public class Loop5 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        do {
            do {
                a++;
            } while (a < b);
        } while (a < b);
        a = 3;
    }

}
