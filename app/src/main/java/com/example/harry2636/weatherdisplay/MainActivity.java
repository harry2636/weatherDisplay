package com.example.harry2636.weatherdisplay;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final String APIKEY = "5663177a1dbaea25";
    private final String BASEURL = "api.wunderground.com";
    private final int DAYNUM = 4;
    private final int HOURNUM = 5;
    public final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            TextView latText = (TextView)findViewById(R.id.latitude);
            TextView lonText = (TextView)findViewById(R.id.longitude);

            latText.setText("Current latitude: " + String.valueOf(latitude));
            lonText.setText("Current longitude: " + String.valueOf(longitude));

        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        /* Referred location finding code in http://stackoverflow.com/questions/2227292/how-to-get-latitude-and-longitude-of-the-mobile-device-in-android */
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, locationListener);
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        showMainPageWithLocation(location);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        System.exit(0);
                        return;
                    }
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, locationListener);

                    Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    showMainPageWithLocation(location);
                } else {
                    System.exit(0);
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                Toast.makeText(this, "Refresh", Toast.LENGTH_LONG).show();
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    System.exit(0);
                    return false;
                }
                Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                showMainPageWithLocation(location);
        }

        return super.onOptionsItemSelected(item);
    }

    private void showMainWeatherInfoPage(double latitude, double longitude) {
        CityRequestAsyncTask cityRequestAsyncTask = new CityRequestAsyncTask();
        cityRequestAsyncTask.execute(latitude, longitude);
        //cityRequestAsyncTask.execute(36.368982, 127.363029);

    }

    private void showMainPageWithLocation(Location location) {
        double latitude, longitude;
        if (location == null) {
            latitude = 36.368982;
            longitude = 127.363029;
            Toast.makeText(this, "Showing default result, try refreshing later.", Toast.LENGTH_LONG).show();
        } else {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }

        showMainWeatherInfoPage(latitude, longitude);
    }



    //Example form: http://api.wunderground.com/api/cf2f35b0c17a9ca3/geolookup/q/36.368982,127.363029.json
    private String getCityRequestUrl(double latitude, double longitude) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
            .authority(BASEURL)
            .appendPath("api")
            .appendPath(APIKEY)
            .appendPath("geolookup")
            .appendPath("q");
        String requestUrl = builder.build().toString();
        requestUrl += "/" + String.valueOf(latitude) + "," + String.valueOf(longitude + ".json");

        return requestUrl;
    }

    //Example form: http://api.wunderground.com/api/cf2f35b0c17a9ca3/conditions/q/zmw:00000.1408.47133.json
    private String getCurrentWeatherRequestUrl(String zmw) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
            .authority(BASEURL)
            .appendPath("api")
            .appendPath(APIKEY)
            .appendPath("conditions");
        String requestUrl = builder.build().toString();
        requestUrl += zmw + ".json";

        return requestUrl;
    }

    //Example form: http://api.wunderground.com/api/cf2f35b0c17a9ca3/forecast/q/zmw:00000.1408.47133.json
    private String getFutureWeatherByDayRequestUrl(String zmw) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
            .authority(BASEURL)
            .appendPath("api")
            .appendPath(APIKEY)
            .appendPath("forecast");
        String requestUrl = builder.build().toString();
        requestUrl += zmw + ".json";

        return requestUrl;
    }

    //Example form: http://api.wunderground.com/api/cf2f35b0c17a9ca3/hourly/q/zmw:00000.1408.47133.json
    private String getFutureWeatherByHourRequestUrl(String zmw) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
            .authority(BASEURL)
            .appendPath("api")
            .appendPath(APIKEY)
            .appendPath("hourly");
        String requestUrl = builder.build().toString();
        requestUrl += zmw + ".json";

        return requestUrl;
    }

    private Map<String, String> parseCityJson(JSONObject cityJson) {
        Log.d("json", cityJson.toString());
        //String state = "";
        String city = "";
        String zmw = "";
        try {
            JSONObject location = cityJson.getJSONObject("location");
            zmw = location.getString("l");

            //state = location.getString("state");
            JSONArray candCities = location.getJSONObject("nearby_weather_stations")
                .getJSONObject("airport")
                .getJSONArray("station");

            if (candCities.length() > 0) {
                city = candCities.getJSONObject(0).getString("city");
            }

            if (city.equals("")) {
                city = location.getString("city");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Map<String, String> dict = new HashMap<String, String>();
        //dict.put("state", state);
        dict.put("city", city);
        dict.put("zmw", zmw);
        return dict;

    }

    private Map<String, String> parseCurrentWeatherJson(JSONObject currentWeatherJson) {
        String temperature = "";
        String weather = "";
        String iconUrl = "";
        try {
            JSONObject infoSet = currentWeatherJson.getJSONObject("current_observation");
            temperature = infoSet.getString("temp_c");
            weather = infoSet.getString("weather");
            iconUrl = infoSet.getString("icon_url");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Map<String, String> dict = new HashMap<String, String>();
        dict.put("temperature", temperature);
        dict.put("weather", weather);
        dict.put("iconUrl", iconUrl);

        return dict;
    }

    private ArrayList<Map<String, String> > parseFutureWeatherByDayJson(JSONObject futureWeatherJson) {
        ArrayList<Map<String, String> > dictArray = new ArrayList<Map<String, String> >();

        try {
            JSONArray dayInfo = futureWeatherJson.getJSONObject("forecast")
                .getJSONObject("simpleforecast")
                .getJSONArray("forecastday");

            for (int i = 0; i < dayInfo.length(); i++) {
                if (i >= DAYNUM) {
                    break;
                }
                JSONObject aDayInfo = dayInfo.getJSONObject(i);

                String weekday = "";
                weekday = aDayInfo.getJSONObject("date").getString("weekday");

                String highTemp = "";
                highTemp = aDayInfo.getJSONObject("high").getString("celsius");

                String lowTemp = "";
                lowTemp = aDayInfo.getJSONObject("low").getString("celsius");

                String pop = "";
                pop = aDayInfo.getString("pop");

                String iconUrl = "";
                iconUrl = aDayInfo.getString("icon_url");

                Map<String, String> dict = new HashMap<String, String>();
                dict.put("weekday", weekday);
                dict.put("highTemp", highTemp);
                dict.put("lowTemp", lowTemp);
                dict.put("pop", pop);
                dict.put("iconUrl", iconUrl);
                dictArray.add(dict);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return dictArray;
    }

    private ArrayList<Map<String, String> > parseFutureWeatherByHourJson(JSONObject futureWeatherJson) {
        ArrayList<Map<String, String> > dictArray = new ArrayList<Map<String, String> >();

        try {
            JSONArray hourInfo = futureWeatherJson.getJSONArray("hourly_forecast");

            for (int i = 0; i < hourInfo.length(); i++) {
                if (i >= HOURNUM) {
                    break;
                }
                JSONObject aHourInfo = hourInfo.getJSONObject(i);

                String hour = "";
                hour = aHourInfo.getJSONObject("FCTTIME").getString("hour");

                String temp = "";
                temp = aHourInfo.getJSONObject("temp").getString("metric");

                String pop = "";
                pop = aHourInfo.getString("pop");

                String iconUrl = "";
                iconUrl = aHourInfo.getString("icon_url");

                Map<String, String> dict = new HashMap<String, String>();
                dict.put("hour", hour);
                dict.put("temp", temp);
                dict.put("pop", pop);
                dict.put("iconUrl", iconUrl);
                dictArray.add(dict);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return dictArray;
    }

    private JSONObject getJsonFromStream(InputStream inputStream) {
        JSONObject resultObject = null;
        try {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            resultObject = new JSONObject(responseStrBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return resultObject;
    }

    private JSONObject getJsonFromUrl (String requestString) {
        JSONObject result = null;
        try {
            URL requestUrl = new URL(requestString);
            HttpURLConnection urlConnection = (HttpURLConnection) requestUrl.openConnection();
            InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
            result = getJsonFromStream(inputStream);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public class CityRequestAsyncTask extends AsyncTask<Double, Void, List<Object> > {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<Object> doInBackground(Double... params) {
            double latitude = params[0];
            double longitude = params[1];
            String cityRequestString = getCityRequestUrl(latitude, longitude);
            Log.d("city", cityRequestString);
            JSONObject cityJson = getJsonFromUrl(cityRequestString);
            Map<String, String> cityInfo = parseCityJson(cityJson);
            String zmw = cityInfo.get("zmw");


            String currentWeatherRequestString = getCurrentWeatherRequestUrl(zmw);
            Log.d("weather", currentWeatherRequestString);
            JSONObject currentWeatherJson = getJsonFromUrl(currentWeatherRequestString);
            Map<String, String> currentWeatherInfo = parseCurrentWeatherJson(currentWeatherJson);
            currentWeatherInfo.put("city", cityInfo.get("city"));
            Log.d("currentInfo", currentWeatherInfo.toString());

            String futureWeatherByDayRequestString = getFutureWeatherByDayRequestUrl(zmw);
            Log.d("weatherByDay", futureWeatherByDayRequestString);
            JSONObject futureWeatherByDayJson = getJsonFromUrl(futureWeatherByDayRequestString);
            ArrayList<Map<String, String> > futureWeatherByDayInfo = parseFutureWeatherByDayJson(futureWeatherByDayJson);
            Log.d("futureDayInfo", futureWeatherByDayInfo.toString());

            String futureWeatherByHourRequestString = getFutureWeatherByHourRequestUrl(zmw);
            Log.d("weatherByHour", futureWeatherByHourRequestString);
            JSONObject futureWeatherByHourJson = getJsonFromUrl(futureWeatherByHourRequestString);
            ArrayList<Map<String, String> > futureWeatherByHourInfo = parseFutureWeatherByHourJson(futureWeatherByHourJson);
            Log.d("futureHourInfo", futureWeatherByHourInfo.toString());

            List<Object> result = new ArrayList<Object>();
            
            result.add(currentWeatherInfo);
            result.add(futureWeatherByDayInfo);
            result.add(futureWeatherByHourInfo);
            return result;
        }

        @Override
        protected void onPostExecute(List<Object> result) {
            super.onPostExecute(result);

            Map<String, String> currentWeatherInfo = (Map<String, String>)result.get(0);
            ArrayList<Map<String, String> > futureWeatherByDayInfo = (ArrayList<Map<String, String> >)result.get(1);
            ArrayList<Map<String, String> > futureWeatherByHourInfo = (ArrayList<Map<String, String> >)result.get(2);

            TextView cityText = (TextView)findViewById(R.id.cityName);
            cityText.setText(currentWeatherInfo.get("city"));

            TextView tempCondText = (TextView)findViewById(R.id.tempCondition);
            String temp = currentWeatherInfo.get("temperature");
            String cond = currentWeatherInfo.get("weather");
            tempCondText.setText(temp +"'C" + " " + cond);

            Map<String, String> currentDetailInfo = futureWeatherByDayInfo.get(0);
            TextView hiLoTempText = (TextView)findViewById(R.id.hiLoTemp);
            String hiTemp = currentDetailInfo.get("highTemp");
            Log.d("temp", "hiTemp: " + hiTemp + "/" + hiTemp.length());
            if (hiTemp.equals("")) {
                hiTemp = String.valueOf(Integer.parseInt(temp) + 5);
            }
            String loTemp = currentDetailInfo.get("lowTemp");
            if (loTemp.equals("")) {
                loTemp = String.valueOf(Integer.parseInt(temp) - 5);
            }
            hiLoTempText.setText(hiTemp+"'C" + " / " + loTemp+"'C");

            TextView popText = (TextView)findViewById(R.id.pop);
            popText.setText(currentDetailInfo.get("pop") + "% chance of rain");

            ImageView currentWeatherImage = (ImageView)findViewById(R.id.currentWeatherImg);
            new DownloadImageTask(currentWeatherImage)
                .execute(currentWeatherInfo.get("iconUrl"));


            LinearLayout centerLinear = (LinearLayout) findViewById(R.id.centerHorLinear);
            centerLinear.removeAllViews();
            for (int i =1; i < futureWeatherByDayInfo.size(); i++) {
                Map<String, String> aFutureDayInfo = futureWeatherByDayInfo.get(i);
                LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View dayView = vi.inflate(R.layout.day_weather_template, null);

                TextView weekDayText = (TextView)dayView.findViewById(R.id.weekday);
                weekDayText.setText(aFutureDayInfo.get("weekday"));


                String hiDayTemp = aFutureDayInfo.get("highTemp");
                if (hiTemp.equals("")) {
                    hiTemp = "N/A";
                }
                String loDayTemp = aFutureDayInfo.get("lowTemp");
                if (loTemp.equals("")) {
                    loTemp = "N/A";
                }
                TextView dayHiLoText = (TextView)dayView.findViewById(R.id.dayHiLoTemp);
                dayHiLoText.setText(hiDayTemp+"'C" + " / " + loDayTemp+"'C");

                TextView dayPop = (TextView)dayView.findViewById(R.id.dayPop);
                dayPop.setText(aFutureDayInfo.get("pop") + "%");

                ImageView dayImage = (ImageView)dayView.findViewById(R.id.dayWeatherImg);
                new DownloadImageTask(dayImage)
                    .execute(aFutureDayInfo.get("iconUrl"));

                centerLinear.addView(dayView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));

            }

            LinearLayout bottomLinear = (LinearLayout) findViewById(R.id.bottomHorLinear);
            bottomLinear.removeAllViews();
            for (int i = 0; i < futureWeatherByHourInfo.size(); i++) {
                Map<String, String> aFutureHourInfo = futureWeatherByHourInfo.get(i);
                LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View hourView = vi.inflate(R.layout.hour_weather_template, null);

                TextView hourText = (TextView)hourView.findViewById(R.id.hour);
                hourText.setText(aFutureHourInfo.get("hour"));

                TextView hourTempText = (TextView)hourView.findViewById(R.id.hourTemp);
                hourTempText.setText(aFutureHourInfo.get("temp") + "'C");

                TextView hourPopText = (TextView)hourView.findViewById(R.id.hourPop);
                hourPopText.setText(aFutureHourInfo.get("pop") + "%");

                ImageView hourImage = (ImageView)hourView.findViewById(R.id.hourWeatherImg);
                new DownloadImageTask(hourImage)
                    .execute(aFutureHourInfo.get("iconUrl"));

                bottomLinear.addView(hourView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    /* Referred image download code in http://stackoverflow.com/questions/2471935/how-to-load-an-imageview-by-url-in-android */

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

}

