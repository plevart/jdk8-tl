package test;


import java.util.HashMap;
import java.util.IdentityHashMap;

/**
 * 536
 * 1016
 * 1064
 * 1272
 * 1472
 * 3232
 */
public class SizeOfTest {
    public static void main(String[] args) {

        SizeOf sizeOf = new SizeOf(SizeOf.Visitor.NULL);

        HashMap hm = new HashMap();
        IdentityHashMap ihm = new IdentityHashMap(5);

        hm.values();
        ihm.values();

        for (int i = 0; i < 101; i++) {
            long hms, ihms;
            System.out.println(
                i + " " +
                    (hms = sizeOf.deepSizeOf(hm)) +
                    " " +
                    (ihms = sizeOf.deepSizeOf(ihm)) +
                    " " + (ihms - hms)
            );
            hm.put(i, i);
            ihm.put(i, i);
        }
    }
}
