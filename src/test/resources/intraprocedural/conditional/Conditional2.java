package intraprocedural.conditional;

public class Conditional2 {

    public static void main(String args[]) {
        int a = 1; // L?
        int b = 2; // L?
        if (a > b) { // IF_ICMPLE L1 L1104422581
            a -= b; // L?
            b -= b; // L?
        }
        b += a; // L1 L1104422581
    }

}
