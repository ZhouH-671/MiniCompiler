package inter;

public class Stmt extends Node {
    public Stmt() {}

    public static Stmt Null = new Stmt();
    public static Stmt Enclosing = Stmt.Null;

    public void gen(int b, int a) {}
    int after = 0;
}