package intraprocedural.conditional;

public class Conditional8 {

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        if (a > b) {
            a -= b;
            if (a < 3)
                b -= b;
        } else if (a == b) {
            a *= b;
        } else {
            a /= b;
        }
        b += a;
    }

}
