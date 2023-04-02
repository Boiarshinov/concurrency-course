package course.concurrency.m3_shared.intro;

public class SimpleCode {

    public static void main(String[] args) {
        final int size = 50_000_000;
        Object[] objects = new Object[size];
        for (int i = 0; i < size; ++i) {
            objects[i] = new Object();
        }
    }
}
