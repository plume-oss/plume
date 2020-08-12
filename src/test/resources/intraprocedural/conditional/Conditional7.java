package intraprocedural.conditional;

public class Conditional7 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        if (a == 3) { // IF_ICMPNE L1 L397071633
            a -= b; // L?
            if (b < 5) { // IF_ICMPGE L2 L1668910247
                a *= 4;
            }
        } else { // GOTO L3 L1668910247
            // L1 L397071633
            if (b > 2) { // IF_ICMPLE L3 L768185844
                b -= b; // L?
            } else { // GOTO L4 L1561502550
                b /= b; // L3 L116289363
            }
            a *= b; // L4 L1561502550
        }
        // L2 L392904516
    }

}
