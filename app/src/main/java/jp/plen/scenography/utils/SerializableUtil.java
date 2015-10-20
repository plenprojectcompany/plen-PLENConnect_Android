package jp.plen.scenography.utils;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public final class SerializableUtil {
    private static final String TAG = SerializableUtil.class.getSimpleName();

    private SerializableUtil() {
    }

    public static Object fromString(String s) throws IOException, ClassNotFoundException {
        byte[] data = Base64.decode(s.getBytes(), Base64.DEFAULT);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object o = ois.readObject();
        ois.close();
        return o;
    }

    public static String toString(Serializable o) throws IOException {
        ByteArrayOutputStream out_b = new ByteArrayOutputStream();
        ObjectOutputStream out_o = new ObjectOutputStream(out_b);
        out_o.writeObject(o);
        out_o.close();
        return Base64.encodeToString(out_b.toByteArray(), Base64.DEFAULT);
    }
}
