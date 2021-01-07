package intraprocedural.switches;

enum Color
{
    RED, GREEN, BLUE
}

public class Switch5 {

    public static String main(String[] args) {
        Color i = Color.BLUE;
        String result = "U";
        switch (i) {
            case RED:
                result = "R";
                break;
            case BLUE:
                result = "B";
                break;
            case GREEN:
                result = "G";
                break;
        }
        return result;
    }

}
