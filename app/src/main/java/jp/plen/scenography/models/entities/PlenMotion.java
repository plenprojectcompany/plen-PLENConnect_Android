package jp.plen.scenography.models.entities;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;

import jp.plen.scenography.R;
import jp.plen.scenography.Scenography;

/**
 * PLENの個々のモーションのデータ.
 * モーション番号(ID)で自然順序付けされる.
 */
public class PlenMotion implements Serializable, Cloneable, Comparable<PlenMotion> {
    private static final String TAG = PlenMotion.class.getSimpleName();
    private static final long serialVersionUID = -6268808265901811065L;

    private final int id;
    @NonNull private final String name;
    @NonNull private final String iconPath;

    public PlenMotion(int id, @NonNull String name, @NonNull String iconPath) {
        this.id = id;
        this.name = name;
        this.iconPath = iconPath;
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getIconPath() {
        return iconPath;
    }

    @NonNull
    @Override
    public PlenMotion clone() {
        try {
            return (PlenMotion) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlenMotion)) return false;
        PlenMotion that = (PlenMotion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @NonNull
    @Override
    public String toString() {
        return "PlenMotion{" +
                "id=" + String.format("%02X", id) +
                ", name='" + name + '\'' +
                ", iconPath='" + iconPath + '\'' +
                '}';
    }

    @Override
    public int compareTo(@NonNull PlenMotion another) {
        return this.getId() - another.getId();
    }
}
