package jp.plen.scenography.models;

import java.io.Serializable;

/**
 * PLENのモーション情報
 * Created by kzm4269 on 15/06/14.
 */
public class PlenMotion implements Serializable, Cloneable {
    private final CharSequence mName;
    private final int mNumber;
    private final String mIconName;
    private int mLoopCount = 1;

    public PlenMotion(int number, CharSequence name, String iconName) {
        mNumber = number;
        mName = name;
        mIconName = iconName;
    }

    public CharSequence getName() {
        return mName;
    }

    public int getNumber() {
        return mNumber;
    }

    public String getIconName() {
        return mIconName;
    }

    public int getLoopCount() {
        return mLoopCount;
    }

    public void setLoopCount(int loopCount) {
        mLoopCount = loopCount;
    }

    @Override
    public PlenMotion clone() {
        try {
            return (PlenMotion) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        return "{" +
                "number: " + mNumber + ", " +
                "name: " + mName + ", " +
                "iconName: " + mIconName + ", " +
                "loopCount: " + mLoopCount +
                "}";
    }
}
