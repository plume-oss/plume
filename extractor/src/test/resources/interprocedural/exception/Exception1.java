package intraprocedural.exception;

public class Exception1 {

    public static void main(String[] args) {
        int a;
        try {
            a = Integer.parseInt("2");
        } catch (Exception e) {
            a = 0;
        }
        int b = a + 1;
    }

}
