package intraprocedural.loop;

public class Loop3 {

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        do {
            do {
                a++;
            } while (a < b);
        } while (a < b);
        a = 3;
    }

}
