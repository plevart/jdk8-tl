import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * @author Wouter Coekaerts
 */
public class SerialLambda implements Serializable {
    interface SerializableIntSupplier extends IntSupplier, Serializable {}

    public static void main(String[] args) throws Exception {
        new SerialLambda().go();
        go2();
    }

    SerializableIntSupplier supplier;

    void go() throws Exception {
        supplier = this::hashCode;
        System.err.println("go1 root=this: " + serialCopy(this).supplier.getAsInt());
        System.err.println("go1 root=lambda: " + serialCopy(this.supplier).getAsInt());
    }

    static void go2() throws Exception {
        List<SerializableIntSupplier> list = new ArrayList<>();
        list.add(() -> list.get(0).hashCode());
        System.err.println("go2 root=list: " + serialCopy(list).get(0).getAsInt());
        System.err.println("go2 root=lambda: " + serialCopy(list.get(0)).getAsInt());
    }

    @SuppressWarnings("unchecked")
    static <T> T serialCopy(T o) throws Exception {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        new ObjectOutputStream(ba).writeObject(o);
        return (T) new ObjectInputStream(new ByteArrayInputStream(ba.toByteArray())).readObject();
    }
}