package com.example.methodLogging;

@org.springframework.stereotype.Service
public class Service {

    // Recursion scenaria
    public void recursion(int i) {
        for (int j = 0; j < i; j++) {
            recursion(i+1);
        }
    }

    // Nested scenario
    public int nested(int i) {
        nested2(i);
        return i;
    }
    public int nested2(int i) {
        nested3(i);
        return i;
    }
    public int nested3(int i) {
        nested4(i);
        return i;
    }
    public int nested4(int i) {
        return i;
    }

    // Multiple scenario
        public int multiple(String foo, int i) {
        multiple1(i);
        multiple2(foo);
        multiple3(foo);
        return i;
    }

    public String multiple1(int i) {
        return "foo";
    }

    public int multiple2(String foo) {
        return 3;
    }

    public int multiple3(String foo) {
        return 4;
    }

}
