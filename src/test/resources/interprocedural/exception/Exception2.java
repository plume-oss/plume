package intraprocedural.exception;

public class Exception2 {

    public static void main(String[] args) {
        int a;
        int b = 2;
        try {
            a = Integer.parseInt("2");
        } catch (Exception e) {
            a = 0;
        }
        int c = a + b;
    }

}
