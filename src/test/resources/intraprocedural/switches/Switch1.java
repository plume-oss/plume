package intraprocedural.lookup;

public class Switch1 {

    public static String main(String[] args) {
        int i = 1;
        String result;
        switch (i) {
            case 0:
                result = "zero";
                break;
            case 2:
                result = "two";
                break;
            case 3:
                result = "three";
                break;
            default:
                result = "unknown number";
                break;
        }
        return result;
    }

}
