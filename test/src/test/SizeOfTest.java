package test;


import java.util.HashMap;
import java.util.Map;

/**
 */
public class SizeOfTest {
    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("aaaaa", "11111");
        map.put("bbbbb", "22222");
        map.put(new String("ccccc"), new String("33333"));

        SizeOf sizeOf = new SizeOf(SizeOf.Visitor.STDOUT);
        sizeOf.deepSizeOf(map);
    }
}
