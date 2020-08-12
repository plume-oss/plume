package intraprocedural.loop;

public class Loop4 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        do {
            a++;
        } while (a < b);
        b = 3;
    }

}
