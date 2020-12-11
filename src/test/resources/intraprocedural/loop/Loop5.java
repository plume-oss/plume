package intraprocedural.loop;

public class Loop5 {

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        do {
            while (a < b) {
                a++;
            }
        } while (a < b);
        a = 3;
    }

}
