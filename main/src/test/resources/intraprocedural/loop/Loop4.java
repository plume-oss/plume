package intraprocedural.loop;

public class Loop4 {

    public static void main(String[] args) {
        int a = 1;
        int b = 2;
        while (a < b) {
            while (a < b) {
                a++;
            }
        }
        a = 3;
    }

}
