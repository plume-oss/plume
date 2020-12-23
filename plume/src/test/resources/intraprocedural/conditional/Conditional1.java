package intraprocedural.conditional;

public class Conditional1 {

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        if (a > b) {
            a -= b;
            b -= b;
        } else {
            b += a;
        }
    }

}
