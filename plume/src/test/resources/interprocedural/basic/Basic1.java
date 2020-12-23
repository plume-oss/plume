package interprocedural.basic;

public class Basic1 {

    public static void main(String args[]) {
        f();
        g();
    }

    private static void f() {
        System.out.println("Called f()");
    }

    protected static void g() {
        System.out.println("Called g()");
    }

}
