package it.cnr.isti.doremi.sleeplogger;


import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;

/**
 * A simple {@link Fragment} subclass.
 */
public class UIRefreshFragment extends NumericSettingFragment {

    private static String TAG = UIRefreshFragment.class.getName();

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
    protected void setValue() {
        listener.setUiRefreshInterval(numberPicker.getValue());
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
                initNumberPicker(R.id.uiRefreshInterval, listener.getUiRefreshInterval(),
                        r.getInteger(R.integer.min_ui_refresh_interval),
                        r.getInteger(R.integer.max_ui_refresh_interval));
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
