package jp.plen.rx.binding;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import rx.Observable;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ReadOnlyPropertyTest {
    private static final String TAG = ReadOnlyPropertyTest.class.getSimpleName();

    @Test
    public void testGet() {
        ReadOnlyProperty<Integer> property;

        property = ReadOnlyProperty.create(Observable.empty());
        assertFalse(property.get().isPresent());

        property = ReadOnlyProperty.create(Observable.never());
        assertFalse(property.get().isPresent());

        Integer a = 1;
        property = ReadOnlyProperty.create(Observable.just(a));
        assertTrue(property.get().isPresent());
        assertEquals(property.get().get(), a);

        property = ReadOnlyProperty.create(Observable.just(a).concatWith(Observable.never()));
        assertTrue(property.get().isPresent());
        assertEquals(property.get().get(), a);

        Integer b = 2;
        property = ReadOnlyProperty.create(Observable.just(a, b).concatWith(Observable.never()));
        assertTrue(property.get().isPresent());
        assertEquals(property.get().get(), b);
    }
}