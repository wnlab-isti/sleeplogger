package it.cnr.isti.doremi.sleeplogger;


import android.app.Fragment;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import java.util.Arrays;


/**
 * This is a base class for SamplingIntervalFragment and UIRefreshFragment,
 * since they share a lot: they have a very similar layout, and both serve
 * to set a numerical value.
 * This class implements a couple of methods to easily manage NumberPicker
 * and onCreateView() to inflate the layout
 */
public abstract class NumericSettingFragment extends Fragment {

    private String TAG;
    private int layoutId;
    private String[] values;

    protected NumberPicker numberPicker = null;
    protected WatchViewStub layoutRoot = null;

    /**
     * This implements the basics of the class: displayed values those
     * of values, but actual NumberPicker values are its indexes.
     * To set a value, we must use its index in values array, while to get
     * it we have to use the element of values array having index equal to
     * NumberPicker value.
     * @param id the id of NumberPicker element
     * @param value the initial value
     * @param values array containing all values
     */
    protected void initNumberPicker(int id, int value, String[] values) {
        this.values = values;

        numberPicker = (NumberPicker) layoutRoot.findViewById(id);

        numberPicker.setWrapSelectorWheel(true);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                setValue(Integer.parseInt(NumericSettingFragment.this.values[newVal]));
            }
        });

        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(values.length - 1);
        numberPicker.setValue(Arrays.asList(values).indexOf(Integer.toString(value)));
        numberPicker.setDisplayedValues(values);
    }

    protected static String[] makeValues(int min, int max, int step) {
        if (min > max || min % step != 0 || max % step != 0)
            throw new IllegalArgumentException();

        String[] values = new String[(max - min) / step + 1];
        int value = min;

        for (int k = 0; k < values.length; ++k) {
            values[k] = Integer.toString(value);
            value += step;
        }

        return values;
    }

    // This is used by subclasses to actually set the new value in the application
    protected abstract void setValue(int val);

    public NumericSettingFragment(String TAG, int layoutId) {
        this.TAG = TAG;
        this.layoutId = layoutId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        return inflater.inflate(layoutId, container, false);
    }
}
