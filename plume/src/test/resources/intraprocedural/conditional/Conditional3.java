package intraprocedural.conditional;

public class Conditional3 {

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        if (a > b && b == 2) {
            a -= b;
        } else {
            a *= b;
        }
        b += a;
    }

}
