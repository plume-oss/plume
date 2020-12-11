package intraprocedural.conditional;

public class Conditional2 {

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        if (a > b) {
            a -= b;
            b -= b;
        }
        b += a;
    }

}
