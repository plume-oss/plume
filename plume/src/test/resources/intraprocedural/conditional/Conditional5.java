package intraprocedural.conditional;

public class Conditional5 {

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        if (a == 3) {
            a -= b;
            if (b < 5) {
                a *= 4;
            } else {
                if (b >= 6) {
                    a /= 2;
                }
            }
        }
    }

}
