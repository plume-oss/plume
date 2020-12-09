package intraprocedural.basic;

public class Basic4 {

    public static void main(String[] args) {
        Sally();
        John();
        Dick();
        Nigel();
    }

    public static int Sally() {
        return 4;
    }

    public static int[] John() {
        return new int[] {1, 2};
    }

    public static boolean Dick() {
        return true;
    }

    public static double Nigel() {
        return 3.14;
    }

}
