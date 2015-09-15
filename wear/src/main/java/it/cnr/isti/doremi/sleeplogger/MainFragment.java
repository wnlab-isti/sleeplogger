package it.cnr.isti.doremi.sleeplogger;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainFragment.EventsListener} interface
 * to handle interaction events.
 */
public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getName();
    private static final String INFO_FORMAT = "Samples: %d @ %.2fHz";

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
        boolean isSampling();
        boolean toggleSampling();
        int getUiRefreshInterval();
        String getInfo(String format);
    }

    private class InfoRefresh extends TimerTask {

        private Runnable refreshTask = new Runnable() {

            @Override
            public void run() {
                sensorInformation.setText(listener.getInfo(INFO_FORMAT));
            }
        };

        @Override
        public void run() {
            Log.d(TAG, "Updating UI..");
            MainFragment.this.getActivity().runOnUiThread(refreshTask);
        }
    }

    private EventsListener listener = null;
    private InfoRefresh UIRefresher = null;

    private TextView rate = null;
    private TextView sensorInformation = null;
    private Button btnStart = null;

    private void setbtnStartStyle(String content) {
        btnStart.setText(content);
        if (content.equals("STOP")) {
            btnStart.setTextColor(Color.RED);
            rate.setText("Sampling..");
        }
        else if (content.equals("START")) {
            btnStart.setTextColor(Color.GREEN);
            rate.setText("Ready");
        }
    }

    public MainFragment() {} // Required empty public constructor

    @Override
    public void onAttach(Activity activity) {
        Log.d(TAG, "onAttach()");
        super.onAttach(activity);

        if (activity instanceof EventsListener)
            listener = (EventsListener) activity;
        else
            throw new ClassCastException(activity.toString()
                    + " must implement MainFragment.EventsListener");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);

        WatchViewStub layoutRoot = (WatchViewStub)
                getActivity().findViewById(R.id.mainFragmentLayoutRoot);

        layoutRoot.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub layoutRoot) {

                rate = (TextView) layoutRoot.findViewById(R.id.rate);
                sensorInformation = (TextView) layoutRoot.findViewById(R.id.sensor);
                btnStart = (Button) layoutRoot.findViewById(R.id.btnStart);

                btnStart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean isSampling = listener.toggleSampling();
                        if (isSampling) {
                            setbtnStartStyle("STOP");
                            UIRefresher = new InfoRefresh();
                            Scheduler.timer.scheduleAtFixedRate(UIRefresher, 0,
                                    listener.getUiRefreshInterval());
                        } else {
                            setbtnStartStyle("START");
                            UIRefresher.cancel();
                            UIRefresher = null;
                        }
                    }
                });

                if (!listener.isSampling()) {
                    setbtnStartStyle("START");
                } else {
                    setbtnStartStyle("STOP");
                    UIRefresher = new InfoRefresh();
                    Scheduler.timer.scheduleAtFixedRate(UIRefresher, 0,
                            listener.getUiRefreshInterval());
                }
            }
        });
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();

        if (UIRefresher != null) {
            UIRefresher.cancel();
            UIRefresher = null;
        }
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach()");
        super.onDetach();

        listener = null;
    }
}
