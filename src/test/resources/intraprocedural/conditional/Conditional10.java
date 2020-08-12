package intraprocedural.conditional;

public class Conditional10 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        if (a == 3) { // IF_ICMPNE L1 L1593224710

        } else { // GOTO L2 L1849015357
            // L1 L1593224710
            a *= 4; // L?
        }
        // L2 L1849015357
    }

}
