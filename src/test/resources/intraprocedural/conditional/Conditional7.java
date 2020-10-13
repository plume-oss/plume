package intraprocedural.conditional;

public class Conditional7 {

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        if (a > b) {
            a -= b;
            b -= b;
        } else if (a == b) {
            a *= b;
        } else {
            if (a < 3)
                a /= b;
        }
        b += a;
    }

}
