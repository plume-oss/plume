package intraprocedural.conditional;

public class Conditional8 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        if (a == 3) { // IF_ICMPNE L1 L1651667865
            a -= b; // L?
            if (b < 5) { // IF_ICMPGE L2 L1937380187
                a *= 4; // L? L1937380187
            } else { // GOTO L1 L1651667865
                // L2 L1937380187
                if (b >= 6) { // IF_ICMPLT L1 L1651667865
                    a /= 2;
                }
            }
        }
        // L1 L1651667865
    }

}
