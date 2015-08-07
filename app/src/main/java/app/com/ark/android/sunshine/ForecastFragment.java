package app.com.ark.android.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import app.com.ark.android.sunshine.data.WeatherContract;

/**
 * Created by ark on 7/25/2015.
 */

    /**
     * A placeholder fragment containing a simple view.
     */
    public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

        private ForecastAdapter mForecastAdapter;
        private static final int FORECAST_LOADER =0;

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
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
            inflater.inflate(R.menu.forecastfragment, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item){
            int id = item.getItemId();
            if(id==R.id.action_refresh){
                updateWeather();

                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onStart(){
            super.onStart();
            updateWeather();
        }

        private void updateWeather() {
//            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
//            String location = sharedPref.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
//            String unit = sharedPref.getString(getString(R.string.pref_units_key), getString(R.string.pref_units_default));
            //Log.v("Option:","Preference location: "+location);
            FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
            String location = Utility.getPreferredLocation(getActivity());
            weatherTask.execute(location);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            ListView forecast_entry = (ListView) rootView.findViewById(R.id.listview_forecast);

            mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);

            forecast_entry.setAdapter(mForecastAdapter);

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

            forecast_entry.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView adapterView, View view, int position, long l) {
                    // CursorAdapter returns a cursor at the correct position for getItem(), or null
                    // if it cannot seek to that position.
                    Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                    if (cursor != null) {
                        String locationSetting = Utility.getPreferredLocation(getActivity());
                        Intent intent = new Intent(getActivity(), DetailActivity.class)
                                .setData(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                        locationSetting, cursor.getLong(COL_WEATHER_DATE)
                                ));
                        startActivity(intent);
                    }
                }
            });
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
            mForecastAdapter.swapCursor(cursor_data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mForecastAdapter.swapCursor(null);
        }

//
//        public class FetchWeatherTask extends AsyncTask<String,Void,String[]> {
//            //private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
//
//            /**
//             * Prepare the weather high/lows for presentation.
//             */
//            private String formatHighLows(double high, double low) {
//                // For presentation, assume the user doesn't care about tenths of a degree.
//                long roundedHigh = Math.round(high);
//                long roundedLow = Math.round(low);
//
//                String highLowStr = roundedHigh + "/" + roundedLow;
//                return highLowStr;
//            }
//
//            /**
//             * Take the String representing the complete forecast in JSON Format and
//             * pull out the data we need to construct the Strings needed for the wireframes.
//             *
//             * Fortunately parsing is easy:  constructor takes the JSON string and converts it
//             * into an Object hierarchy for us.
//             */
//            private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
//                    throws JSONException {
//
//                // These are the names of the JSON objects that need to be extracted.
//                final String OWM_LIST = "list";
//                final String OWM_WEATHER = "weather";
//                final String OWM_TEMPERATURE = "temp";
//                final String OWM_MAX = "max";
//                final String OWM_MIN = "min";
//                final String OWM_DESCRIPTION = "main";
//
//                JSONObject forecastJson = new JSONObject(forecastJsonStr);
//                JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
//
//                String[] resultStrs = new String[numDays];
//                for(int i = 0; i < weatherArray.length(); i++) {
//                    // For now, using the format "Day, description, hi/low"
//                    String day;
//                    String description;
//                    String highAndLow;
//
//                    // Get the JSON object representing the day
//                    JSONObject dayForecast = weatherArray.getJSONObject(i);
//
//                    //create a Gregorian Calendar, which is in current date
//                    GregorianCalendar gc = new GregorianCalendar();
//                    //add i dates to current date of calendar
//                    gc.add(GregorianCalendar.DATE, i);
//                    //get that date, format it, and "save" it on variable day
//                    Date time = gc.getTime();
//                    SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE, MMM dd");
//                    day = shortenedDateFormat.format(time);
//
//                    // description is in a child array called "weather", which is 1 element long.
//                    JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
//                    description = weatherObject.getString(OWM_DESCRIPTION);
//
//                    // Temperatures are in a child object called "temp".  Try not to name variables
//                    // "temp" when working with temperature.  It confuses everybody.
//                    JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
//                    double high = temperatureObject.getDouble(OWM_MAX);
//                    double low = temperatureObject.getDouble(OWM_MIN);
//
//                    highAndLow = formatHighLows(high, low);
//                    resultStrs[i] = day + " - " + description + " - " + highAndLow;
//                }
//
//                for (String s : resultStrs) {
//                    //Log.v(LOG_TAG, "Forecast entry: " + s);
//                }
//                return resultStrs;
//
//            }
//
//            @Override
//            protected String[] doInBackground(String... params) {
//
//                if(params.length==0){
//                    return null;
//                }
//                // These two need to be declared outside the try/catch
//                // so that they can be closed in the finally block.
//                HttpURLConnection urlConnection = null;
//                BufferedReader reader = null;
//
//                // Will contain the raw JSON response as a string.
//                String forecastJsonStr = null;
//
//                String format ="json";
//                //String units = "metric";
//                int numDays = 7;
//
//                try {
//                    // Construct the URL for the OpenWeatherMap query
//                    // Possible parameters are available at OWM's forecast API page, at
//                    // http://openweathermap.org/API#forecast
//                    //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7");
//
//                    final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
//                    final String QUERY_PARAM ="q";
//                    final String FORMAT_PARAM="mode";
//                    final String UNITS_PARAM ="units";
//                    final String DAYS_PARAM = "cnt";
//
//                    Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
//                            .appendQueryParameter(QUERY_PARAM, params[0])
//                            .appendQueryParameter(FORMAT_PARAM, format)
//                            .appendQueryParameter(UNITS_PARAM,params[1])
//                            .appendQueryParameter(DAYS_PARAM,Integer.toString(numDays))
//                            .build();
//
//                    URL url = new URL(builtUri.toString());
//                    //Log.v(LOG_TAG,"Built URL "+builtUri.toString());
//                    // Create the request to OpenWeatherMap, and open the connection
//                    urlConnection = (HttpURLConnection) url.openConnection();
//                    urlConnection.setRequestMethod("GET");
//                    urlConnection.connect();
//
//                    // Read the input stream into a String
//                    InputStream inputStream = urlConnection.getInputStream();
//                    StringBuffer buffer = new StringBuffer();
//                    if (inputStream == null) {
//                        // Nothing to do.
//                        return null;
//                    }
//                    reader = new BufferedReader(new InputStreamReader(inputStream));
//
//                    String line;
//                    while ((line = reader.readLine()) != null) {
//                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
//                        // But it does make debugging a *lot* easier if you print out the completed
//                        // buffer for debugging.
//                        buffer.append(line + "\n");
//                    }
//
//                    if (buffer.length() == 0) {
//                        // Stream was empty.  No point in parsing.
//                        return null;
//                    }
//                    forecastJsonStr = buffer.toString();
//                    //Log.v(LOG_TAG,"Forecast JSON String: "+forecastJsonStr);
//                } catch (IOException e) {
//                    //Log.e(LOG_TAG,"Error",e);
//                    // If the code didn't successfully get the weather data, there's no point in attempting
//                    // to parse it.
//                    return null;
//                } finally{
//                    if (urlConnection != null) {
//                        urlConnection.disconnect();
//                    }
//                    if (reader != null) {
//                        try {
//                            reader.close();
//                        } catch (final IOException e) {
//                            //Log.e(LOG_TAG, "Error closing stream", e);
//                        }
//                    }
//                }
//
//                try{
//                    return getWeatherDataFromJson(forecastJsonStr,numDays);
//                } catch(JSONException e){
//                    //Log.e(LOG_TAG, e.getMessage(), e);
//                    e.printStackTrace();
//                }
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(String[] result) {
//                if(result!=null){
//                    mForecastAdapter.clear();
//                    mForecastAdapter.addAll(result);
//                }
//            }
//        }
//


    }
