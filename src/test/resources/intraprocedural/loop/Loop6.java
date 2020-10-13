package intraprocedural.loop;

public class Loop6 {

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        while (a < b) {
            do {
                a++;
            } while (a < b);
        }
        a = 3;
    }

}
