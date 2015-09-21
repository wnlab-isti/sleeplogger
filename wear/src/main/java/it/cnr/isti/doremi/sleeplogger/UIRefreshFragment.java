package it.cnr.isti.doremi.sleeplogger;


import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.NumberPicker;

/**
 * This class displays a {@link NumberPicker} to allow setting of
 * User Interface refresh interval. It extends {@link NumericSettingFragment},
 * adding an interface for Activity communication and using super-class
 * methods to implement its features, in method onActivityCreated().
 * Minimum, maximum, default and step values are taken from integers.xml
 */
public class UIRefreshFragment extends NumericSettingFragment {

    private static String TAG = UIRefreshFragment.class.getName();
    private static String[] values;

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface EventsListener {
        int getUiRefreshInterval();
        void setUiRefreshInterval(int uiRefreshInterval);
    }

    private EventsListener listener = null;

    public UIRefreshFragment() {
        super(TAG, R.layout.fragment_ui_refresh);
    }

    @Override
    protected void setValue(int val) {
        listener.setUiRefreshInterval(val);
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d(TAG, "onAttach()");
        super.onAttach(activity);

        if (activity instanceof EventsListener)
            listener = (EventsListener) activity;
        else
            throw new ClassCastException(activity.toString()
                    + " must implement UIRefreshFragment.EventsListener");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated()");
        super.onStart();

        layoutRoot = (WatchViewStub)
                getActivity().findViewById(R.id.uiRefreshRoot);

        layoutRoot.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub watchViewStub) {
                Resources r = getResources();

                if (values == null)
                    values = NumericSettingFragment.makeValues(
                            r.getInteger(R.integer.min_ui_refresh_interval),
                            r.getInteger(R.integer.max_ui_refresh_interval),
                            r.getInteger(R.integer.step_ui_refresh_interval));

                initNumberPicker(R.id.uiRefreshInterval, listener.getUiRefreshInterval(),
                        values);
            }
        });
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach()");
        super.onDetach();

        listener = null;
    }
}
