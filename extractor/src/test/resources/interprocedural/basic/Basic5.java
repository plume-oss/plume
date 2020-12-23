package interprocedural.basic;

public class Basic5 {

    public static void main(String args[]) {
        new Basic5();
    }

    public Basic5() {
        f("Test", "Case");
    }

    private static String f(String prefix, String suffix) {
        return prefix + " " + suffix;
    }

}
