package intraprocedural.conditional;

public class Conditional3 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        if (a > b) { // IF_ICMPLE L1 L1104422581
            a -= b; // L?
            b -= b; // L?
        } else { // GOTO L2 L170052458
            a *= b; // L1 L1104422581
        }
        b += a; // L2 L170052458
    }

}
