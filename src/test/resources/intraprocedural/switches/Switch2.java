package intraprocedural.switches;

public class Switch2 {

    public static String main(String[] args) {
        String animal = "DOG";
        String result;
        switch (animal) {
            case "DOG":
                result = "domestic animal";
                break;
            case "CAT":
                result = "domestic animal";
                break;
            case "TIGER":
                result = "wild animal";
                break;
            default:
                result = "unknown animal";
                break;
        }
        return result;
    }

}
