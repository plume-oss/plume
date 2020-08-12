package intraprocedural.conditional;

public class Conditional5 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        if (a == 3) { // IF_ICMPNE L1 L1104422581
            a -= b; // L?
            if (b > 2) { // IF_ICMPLE L2 L170052458
                b -= b; // L?
            } else { // GOTO L3 L1504937617
                b /= b; // L2 L170052458
            }
        } else { // GOTO L3 L1504937617
            a *= b; // L1 L1104422581
        }
        b += a; // L3 L1504937617
    }

}
