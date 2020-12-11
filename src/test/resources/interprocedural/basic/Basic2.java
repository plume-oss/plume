package interprocedural.basic;

public class Basic2 {

    static int globalInt = 2;

    public static void main(String args[]) {
        Basic2.globalInt = f(5);
    }

    private static int f(int i) {
        System.out.println("Called f()");
        return i + 1;
    }

}
