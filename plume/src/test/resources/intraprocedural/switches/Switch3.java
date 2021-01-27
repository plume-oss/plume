package intraprocedural.switches;

public class Switch3 {

    public static String main(String[] args) {
        int i = 2;
        String result;
        switch (i) {
            case 0:
                result = "zero";
                break;
            case 2:
            case 3:
                result = "two or three";
                break;
            default:
                result = "unknown number";
                break;
        }
        return result;
    }

}
