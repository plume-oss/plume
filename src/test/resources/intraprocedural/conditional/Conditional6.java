package intraprocedural.conditional;

public class Conditional6 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        if (a == 3) { // IF_ICMPNE L1 L413218476
            a -= b; // L?
        } else { // GOTO L2 L392904516
            // L1 L413218476
            if (b > 2) { // IF_ICMPLE L3 L116289363
                b -= b; // L?
            } else { // GOTO L4 L1561502550
                b /= b; // L3 L116289363
            }
            a *= b; // L4 L1561502550
        }
        b += a; // L2 L392904516
    }

}
