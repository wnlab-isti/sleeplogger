package it.cnr.isti.doremi.sleeplogger;

import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;

public class MainActivity extends Activity
        implements MainFragment.EventsListener {

    private static final String TAG = MyActivity.class.getName();
    public static final int UI_REFRESH_INTERVAL = 1000;

    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     * Since this is a wear application, to avoid too high memory load
     * {@link android.support.v13.app.FragmentStatePagerAdapter} is used.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        // getItem is called to instantiate the fragment for the given page.
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new MainFragment();

                case 1:
                    return new SettingsFragment();

                default:
                    return null;
            }
        }

        // Returns total number of pages
        @Override
        public int getCount() { return 2; }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_fragment_main).toUpperCase(l);

                case 1:
                    return getString(R.string.title_fragment_settings).toUpperCase(l);

                default:
                    return null;
            }
        }
    }

    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    public void startSampling() {
        Intent myIntent = new Intent(MainActivity.this, SensorSamplingService.class);
        startService(myIntent);
    }

    public void stopSampling() {
        Intent myIntent = new Intent(MainActivity.this, SensorSamplingService.class);
        stopService(myIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each page of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
    }

    @Override
    protected void onStop()	{
        Log.d (TAG, "onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d (TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.d (TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d (TAG, "onResume()");
        super.onResume();
    }
}
