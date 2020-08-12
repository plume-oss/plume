package intraprocedural.conditional;

public class Conditional4 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        if (a == 3) { // IF_ICMPNE L1 L1104422581
            a -= b; // L?
            if (b > 2) { // IF_ICMPLE L2 L170052458
                b -= b; // L?
            }
        } else { // GOTO L2 L170052458
            a *= b; // L1 L1104422581
        }
        b += a; // L2 L170052458
    }

}
