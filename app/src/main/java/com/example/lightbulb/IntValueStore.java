package com.example.lightbulb;


/**
 * A store of an int value. You can register a listener that will be notified
 * when the value changes.
 */
public class IntValueStore {

	public final static int SOURCE_GUI = 0;
	public final static int SOURCE_LOOP = 1;
    /**
     * The current value.
     */
    int[] mValue = new int[3];
    /**
     * The listener (you might want turn this into an array to support many
     * listeners)
     */
    private IntValueStoreListener mListener;

    /**
     * Construct a the int store.
     *
     * @param initialValue The initial value.
     */
    public IntValueStore(int[] initialValue) {
        mValue = initialValue;
    }

    /**
     * Sets a listener on the store. The listener will be modified when the
     * value changes.
     *
     * @param listener The {@link IntValueStoreListener}.
     */
    public void setListener(IntValueStoreListener listener) {
        mListener = listener;
    }

    /**
     * Set a new int value.
     *
     * @param newValue The new value.
     */
    public void setValue(int[] newValue, int source) {
        mValue = newValue;
        if (mListener != null) {
            mListener.onValueChanged(mValue,source);
        }
    }

    /**
     * Get the current value.
     *
     * @return The current int value.
     */
    public int[] getValue() {
        return mValue;
    }

    public double getMax() {
    	return Math.max(Math.max(mValue[0], mValue[1]),mValue[2]);
    }
    /**
     * Callbacks by {@link IntValueModel}.
     */
    public static interface IntValueStoreListener {
        /**
         * Called when the value of the int changes.
         *
         * @param newValue The new value.
         */
        void onValueChanged(int[] newValue, int source);
    }
}