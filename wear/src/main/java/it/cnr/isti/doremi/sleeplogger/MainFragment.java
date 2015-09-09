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
        void startSampling();
        void stopSampling();
    }

    private class InfoRefresh extends TimerTask {

        @Override
        public void run() {
            Log.d(TAG, "Updating UI..");
            MainFragment.this.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    sensorInformation.setText(String.format("Samples: %d @ %.2fHz",
                            SensorSamplingService.getSamples(),
                            SensorSamplingService.getRealSamplingFrequency()));
                }
            });
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

        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
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
                        if (!SensorSamplingService.isStarted()) {
                            setbtnStartStyle("STOP");
                            UIRefresher = new InfoRefresh();
                            Scheduler.timer.scheduleAtFixedRate(UIRefresher,
                                    MainActivity.UI_REFRESH_INTERVAL, MainActivity.UI_REFRESH_INTERVAL);
                            listener.startSampling();
                        } else {
                            setbtnStartStyle("START");
                            listener.stopSampling();
                            UIRefresher.cancel();
                            UIRefresher = null;
                        }
                    }
                });

                if (!SensorSamplingService.isStarted()) {
                    setbtnStartStyle("START");
                } else {
                    setbtnStartStyle("STOP");
                    UIRefresher = new InfoRefresh();
                    Scheduler.timer.scheduleAtFixedRate(UIRefresher,
                            MainActivity.UI_REFRESH_INTERVAL, MainActivity.UI_REFRESH_INTERVAL);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        UIRefresher.cancel();
        UIRefresher = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        listener = null;
    }
}
