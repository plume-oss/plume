package intraprocedural.conditional;

public class Conditional9 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        if (a == 3) { // IF_ICMPNE L1 L1053967012
            a -= b; // L?
            if (b < 5) { // IF_ICMPGE L2 L764826684
                a *= 4; // L?
            } else { // GOTO L3 L2103763750
                // L2 L764826684
                if (b >= 6) { // IF_ICMPLT L3 L2103763750
                    a /= 2;
                }
            }
        } else {
            // L1 L1053967012
            if (b != 3) { // IF_ICMPEQ L4 L1238616099
                a *= 4; // L?
            } else { // GOTO L3 L2103763750
                // L4 L1238616099
                if (b <= 6) { // IF_ICMPGT L3 L2103763750
                    a /= 2;
                }
            }
        }
        // L3 L2103763750
    }

}
