package intraprocedural.switches;

public class Switch4 {

    public static String main(String[] args) {
        char i = 'a';
        String result;
        switch (i) {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
                result = "Vowel";
                break;
            default:
                result = "Consanant";
                break;
        }
        return result;
    }

}
