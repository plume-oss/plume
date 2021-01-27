package interprocedural.basic;

public class Basic4 {

    int globalInt = 2;

    public static void main(String args[]) {
        new Basic4();
    }

    public Basic4() {
        this.globalInt = f(5, 6);
    }

    private static int f(int i, int j) {
        return i + j;
    }

}
