package interprocedural.inheritance;

public class Main {
    public static void main(String[] args) {
        Base b = new Base();
        Base d = new Derived();
        Derived bd = new Derived();
        b.show();
        d.show();
        bd.show();
    }
}