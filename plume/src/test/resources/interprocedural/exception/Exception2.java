package intraprocedural.exception;

public class Exception2 {

    public static void main(String[] args) {
        int a;
        int b = 2;
        try {
            a = Integer.parseInt("2");
        } catch (NumberFormatException e1) {
            a = 3;
        } catch (Exception e2) {
            a = 0;
            throw e2;
        }
        int c = a + b;
    }

}
