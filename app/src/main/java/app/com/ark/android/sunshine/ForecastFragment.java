package app.com.ark.android.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import app.com.ark.android.sunshine.data.WeatherContract;
import app.com.ark.android.sunshine.sync.SunshineSyncAdapter;

/**
 * Created by ark on 7/25/2015.
 */

    /**
     * A placeholder fragment containing a simple view.
     */
    public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

        private ForecastAdapter mForecastAdapter;
        private static final int FORECAST_LOADER =0;
        private int mPosition= ListView.INVALID_POSITION;
        private boolean mUseTodayLayout;
        ListView mForecast_entry;
        private static final String SELECTED_KEY = "selected_position";

        private static final String[] FORECAST_COLUMNS = {
                // In this case the id needs to be fully qualified with a table name, since
                // the content provider joins the location & weather tables in the background
                // (both have an _id column)
                // On the one hand, that's annoying.  On the other, you can search the weather table
                // using the location set by the user, which is only in the Location table.
                // So the convenience is worth it.
                WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
                WeatherContract.WeatherEntry.COLUMN_DATE,
                WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
                WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                WeatherContract.LocationEntry.COLUMN_COORD_LAT,
                WeatherContract.LocationEntry.COLUMN_COORD_LONG
        };

        // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
        // must change.
        static final int COL_WEATHER_ID = 0;
        static final int COL_WEATHER_DATE = 1;
        static final int COL_WEATHER_DESC = 2;
        static final int COL_WEATHER_MAX_TEMP = 3;
        static final int COL_WEATHER_MIN_TEMP = 4;
        static final int COL_LOCATION_SETTING = 5;
        static final int COL_WEATHER_CONDITION_ID = 6;
        static final int COL_COORD_LAT = 7;
        static final int COL_COORD_LONG = 8;

        public ForecastFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.forecastfragment, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item){
            int id = item.getItemId();
//            if(id==R.id.action_refresh){
//                //updateWeather();
//                refreshWeather();
//                return true;
//            }

            if(id==R.id.action_map){
                openPreferredLocationInMap();
            }

            return super.onOptionsItemSelected(item);
        }

        private void openPreferredLocationInMap(){

            if(null!=mForecastAdapter) {
                Cursor c = mForecastAdapter.getCursor();
                if(null!=c) {
                    c.moveToPosition(0);
                    String posLat = c.getString(COL_COORD_LAT);
                    String posLong = c.getString(COL_COORD_LONG);
                    Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

//                    Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
//                            .appendQueryParameter("q", location)
//                            .build();

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(geoLocation);
                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        Log.d("location", "Couldn't call" + geoLocation.toString() + "no valid");
                    }
                }
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            if(mPosition!=ListView.INVALID_POSITION){
                outState.putInt(SELECTED_KEY,mPosition);
            }
            super.onSaveInstanceState(outState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            mForecast_entry = (ListView) rootView.findViewById(R.id.listview_forecast);

            mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);

            mForecast_entry.setAdapter(mForecastAdapter);


//            forecast_entry.setOnItemClickListener(new AdapterView.OnItemClickListener(){
//
//                @Override
//                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                    String forecast = mForecastAdapter.getItem(position);
//                    Intent intent = new Intent(getActivity(), DetailActivity.class)
//                            .putExtra(Intent.EXTRA_TEXT,forecast);
//                    startActivity(intent);
//
//                }
//            });

            mForecast_entry.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                    // CursorAdapter returns a cursor at the correct position for getItem(), or null
                    // if it cannot seek to that position.
                    mPosition=position;
                    Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                    if(cursor!=null) {
                        String locationSetting = Utility.getPreferredLocation(getActivity());
                        ((Callback) getActivity())
                                .onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                        locationSetting, cursor.getLong(COL_WEATHER_DATE)
                                ));
                    }
                }
            });

            if(savedInstanceState!=null&&savedInstanceState.containsKey(SELECTED_KEY)){
                mPosition = savedInstanceState.getInt(SELECTED_KEY);
            }


            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState){
            getLoaderManager().initLoader(FORECAST_LOADER, null, this);
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String locationSetting = Utility.getPreferredLocation(getActivity());

            // Sort order:  Ascending, by date.
            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                    locationSetting, System.currentTimeMillis());
            return new CursorLoader(getActivity(),
                    weatherForLocationUri,
                    FORECAST_COLUMNS,
                    null,
                    null,
                    sortOrder);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor_data) {
            if(cursor_data.moveToFirst()) {
                mForecastAdapter.swapCursor(cursor_data);
                if(mPosition!=ListView.INVALID_POSITION)
                {
                    mForecast_entry.smoothScrollToPosition(mPosition);
                }

            } else{
                //onLocationChanged();
            }

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mForecastAdapter.swapCursor(null);

        }

        public void setUseTodayLayout(boolean useTodayLayout) {
            mUseTodayLayout = useTodayLayout;
            if (mForecastAdapter != null) {
                mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
            }
        }

        public void onLocationChanged(){
            //updateWeather();
            refreshWeather();
            getLoaderManager().restartLoader(FORECAST_LOADER,null,this);
        }

        /**
         * A callback interface that all activities containing this fragment must
         * implement. This mechanism allows activities to be notified of item
         * selections.
         */
        public interface Callback {
            /**
             * DetailFragmentCallback for when an item has been selected.
             */
            public void onItemSelected(Uri dateUri);
        }

        public void refreshWeather(){
//            String mLocation = Utility.getPreferredLocation(getActivity());
//            Intent alarmIntent = new Intent(getActivity(),SunshineService.AlarmReceiver.class);
//            alarmIntent.putExtra(SunshineService.LOCATION_QUERY_EXTRA,mLocation);
//            PendingIntent pi = PendingIntent.getBroadcast(getActivity(),0,alarmIntent,PendingIntent.FLAG_ONE_SHOT);
//
//            AlarmManager am = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
//            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pi);

            SunshineSyncAdapter.syncImmediately(getActivity());


        }


    }
