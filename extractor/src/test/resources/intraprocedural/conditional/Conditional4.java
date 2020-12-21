package intraprocedural.conditional;

public class Conditional4 {

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        if (a == 3) {
            a -= b;
            if (b > 2) {
                b -= b;
            }
        } else {
            a *= b;
        }
        b += a;
    }

}
