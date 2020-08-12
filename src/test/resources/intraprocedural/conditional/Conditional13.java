package intraprocedural.conditional;

public class Conditional13 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        if (a > b) { // IF_ICMPLE L1 L1104422581
            a -= b; // L?
            if (a < 3)
                b -= b; // L?
        } else if (a == b) { // GOTO L2 L170052458
            a *= b; // L1 L1104422581
        } else {
            a /= b;
        }
        b += a; // L2 L170052458
    }

}
