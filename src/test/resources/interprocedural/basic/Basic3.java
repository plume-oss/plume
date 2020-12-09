package interprocedural.basic;

public class Basic3 {

    int globalInt = 2;

    public static void main(String args[]) {
        new Basic3();
    }

    public Basic3() {
        this.globalInt = f(5);
    }

    private static int f(int i) {
        System.out.println("Called f()");
        return i + 1;
    }

}
