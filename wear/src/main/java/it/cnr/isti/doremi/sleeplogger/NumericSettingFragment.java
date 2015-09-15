package it.cnr.isti.doremi.sleeplogger;


import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;


/**
 */
public abstract class NumericSettingFragment extends Fragment {

    private String TAG;
    private int layoutId;

    protected NumberPicker numberPicker = null;
    protected WatchViewStub layoutRoot = null;

    protected void initNumberPicker(int id, int value, int min, int max) {
        numberPicker = (NumberPicker) layoutRoot.findViewById(id);
        numberPicker.setMinValue(min);
        numberPicker.setMaxValue(max);
        numberPicker.setValue(value);
        numberPicker.setWrapSelectorWheel(true);
    }

    protected abstract void setValue();

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

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

        if (numberPicker != null)
            setValue();
    }
}
