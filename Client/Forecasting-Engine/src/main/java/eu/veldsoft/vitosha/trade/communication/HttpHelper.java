package eu.veldsoft.vitosha.trade.communication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;

import eu.veldsoft.vitosha.trade.dummy.InputData;

/**
 * It is used for HTTP communication with the remote server.
 *
 * @author Todor Balabanov
 */
public class HttpHelper {
    /**
     * Logger instance.
     */
    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    /**
     * Number of ANN found as response of the request.
     */
    private static final String JSON_SIZE_KEY = "size";

    /**
     * Time series symbol ticker.
     */
    private static final String JSON_SYMBOL_KEY = "symbol";

    /**
     * Time series period as integer number of minutes.
     */
    private static final String JSON_PERIOD_KEY = "period";

    /**
     * Fitness value of the ANN.
     */
    private static final String JSON_FITNESS_KEY = "fitness";

    /**
     * Array with neurons flags.
     */
    private static final String JSON_FLAGS_KEY = "flags";

    /**
     * Array with ANN weights.
     */
    private static final String JSON_WEIGHTS_KEY = "weights";

    /**
     * Array with ANN connections activities.
     */
    private static final String JSON_ACTIVITIES_KEY = "activities";

    /**
     * Number of training examples.
     */
    private static final String JSON_NUMBER_OF_EXAMPLES_KEY = "numberOfExamples";

    /**
     * Time array.
     */
    private static final String JSON_TIME_KEY = "time";

    /**
     * Open array.
     */
    private static final String JSON_OPEN_KEY = "open";

    /**
     * Low array.
     */
    private static final String JSON_LOW_KEY = "low";

    /**
     * High array.
     */
    private static final String JSON_HIGH_KEY = "high";

    /**
     * Close array.
     */
    private static final String JSON_CLOSE_KEY = "close";

    /**
     * Volume array.
     */
    private static final String JSON_VOLUME_KEY = "volume";

    /**
     * Load random ANN remote server script name.
     */
    private final String LOAD_RANDOM_ANN_SCRIPT = "logic/json_load_random_ann.php";

    /**
     * Loading training set for particular ticker and time period.
     */
    private final String LOAD_TRAINING_SET_SCRIPT = "logic/json_load_training_set.php";

    /**
     * Report retrained ANN remote server script name.
     */
    private final String SAVE_RETRAINED_ANN_SCRIPT = "logic/save_retrained_ann.php";

    /**
     * Number of neurons for the ANN.
     */
    private final String JSON_NUMBER_OF_NEURONS_KEY = "numberOfNeurons";

    /**
     * Remote server URl address.
     */
    private final String url;

    /**
     * Read stream from the input stream.
     *
     * @param in Input stream.
     * @return String with the read stream.
     * @throws IOException If an I/O error occurs.
     */
    private String readStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        reader.close();
        return out.toString();
    }

    /**
     * Constructor with all parameters needed.
     *
     * @param url Remote server URL address.
     */
    public HttpHelper(String url) {
        this.url = url;
    }

    /*
     * Load remote data into input data structure.
     *
     * @return True if the loading was successful, false otherwise.
     */
    public boolean load() {
        String symbol = InputData.SYMBOL;
        int period = InputData.PERIOD;
        int[] flags = InputData.NEURONS;
        double[][] weights = InputData.WEIGHTS;
        double[][] activities = InputData.ACTIVITIES;
        long[] time = InputData.TIME;
        double[] open = InputData.OPEN;
        double[] low = InputData.LOW;
        double[] high = InputData.HIGH;
        double[] close = InputData.CLOSE;
        double[] volume = InputData.VOLUME;

        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL("http://" + url.trim() + "/" + LOAD_RANDOM_ANN_SCRIPT)).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept-Charset", "UTF-8");

            /*
             * Load randomly selected ANN.
             */
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }

            JSONObject result = new JSONObject(readStream(connection.getInputStream()));
            connection.disconnect();

            int size = result.getInt(JSON_SIZE_KEY);

            /*
             * If there is no ANN on the server side nothing can be loaded.
             */
            if (size <= 0) {
                return false;
            }

            /*
             * Extract JSON from HTTP response.
             */
            symbol = result.getString(JSON_SYMBOL_KEY);

            period = result.getInt(JSON_PERIOD_KEY);

            double fitness = result.getDouble(JSON_FITNESS_KEY);

            int numberOfNeurons = result.getInt(JSON_NUMBER_OF_NEURONS_KEY);

            flags = new int[numberOfNeurons];
            JSONArray array1 = result.getJSONArray(JSON_FLAGS_KEY);
            for (int i = 0; i < array1.length(); i++) {
                flags[i] = array1.getInt(i);
            }

            //TODO Matrix transpose is possible.
            weights = new double[numberOfNeurons][numberOfNeurons];
            JSONArray array2 = result.getJSONArray(JSON_WEIGHTS_KEY);
            for (int j = 0; j < array2.length(); j++) {
                JSONArray array3 = array2.getJSONArray(j);
                for (int i = 0; i < array3.length(); i++) {
                    weights[i][j] = array3.getDouble(i);
                }
            }

            //TODO Matrix transpose is possible.
            activities = new double[numberOfNeurons][numberOfNeurons];
            JSONArray array4 = result.getJSONArray(JSON_ACTIVITIES_KEY);
            for (int j = 0; j < array4.length(); j++) {
                JSONArray array5 = array4.getJSONArray(j);
                for (int i = 0; i < array5.length(); i++) {
                    activities[i][j] = array5.getDouble(i);
                }
            }
        } catch (MalformedURLException exception) {
            return false;
        } catch (IOException exception) {
            return false;
        } catch (JSONException exception) {
            return false;
        }

        /*
         * Load training set for the selected ANN.
         */
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL("http://" + url.trim() + "/" + LOAD_TRAINING_SET_SCRIPT)).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            String parameters = "symbol=" + URLEncoder.encode(symbol, "UTF-8") + "&period=" + period;
            try (OutputStream out = connection.getOutputStream()) {
                out.write(parameters.getBytes("UTF-8"));
                out.flush();
            }

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }

            JSONObject result = new JSONObject(readStream(connection.getInputStream()));

            int size = result.getInt(JSON_NUMBER_OF_EXAMPLES_KEY);

            /*
             * If there is no ANN training set on the server side nothing can be loaded.
             */
            if (size <= 0) {
                return false;
            }

            time = new long[size];
            JSONArray array1 = result.getJSONArray(JSON_TIME_KEY);
            for (int i = 0; i < array1.length(); i++) {
                time[i] = array1.getLong(i);
            }

            open = new double[size];
            JSONArray array2 = result.getJSONArray(JSON_OPEN_KEY);
            for (int i = 0; i < array2.length(); i++) {
                open[i] = array2.getDouble(i);
            }

            low = new double[size];
            JSONArray array3 = result.getJSONArray(JSON_LOW_KEY);
            for (int i = 0; i < array3.length(); i++) {
                low[i] = array3.getDouble(i);
            }

            high = new double[size];
            JSONArray array4 = result.getJSONArray(JSON_HIGH_KEY);
            for (int i = 0; i < array4.length(); i++) {
                high[i] = array4.getDouble(i);
            }

            close = new double[size];
            JSONArray array5 = result.getJSONArray(JSON_CLOSE_KEY);
            for (int i = 0; i < array5.length(); i++) {
                close[i] = array5.getDouble(i);
            }

            volume = new double[size];
            JSONArray array6 = result.getJSONArray(JSON_VOLUME_KEY);
            for (int i = 0; i < array6.length(); i++) {
                volume[i] = array6.getDouble(i);
            }
        } catch (IOException exception) {
            return false;
        } catch (JSONException exception) {
            return false;
        }

        /*
         * Load data in the global data structure.
         */
        InputData.SYMBOL = symbol;
        InputData.PERIOD = period;
        InputData.NEURONS = flags;
        InputData.WEIGHTS = weights;
        InputData.ACTIVITIES = activities;
        InputData.TIME = time;
        InputData.OPEN = open;
        InputData.LOW = low;
        InputData.HIGH = high;
        InputData.CLOSE = close;
        InputData.VOLUME = volume;
        InputData.RATES = new double[][]{InputData.OPEN, InputData.LOW, InputData.HIGH, InputData.CLOSE};

        return true;
    }

    /**
     * Store calculated ANN weights on the remote web server.
     *
     * @return True if the saving was successful, false otherwise.
     */
    public boolean store() {
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL("http://" + url.trim() + "/" + SAVE_RETRAINED_ANN_SCRIPT)).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            /*
             * Store retrained ANN.
             */
            StringBuilder parameters = new StringBuilder();
            parameters.append("symbol=").append(URLEncoder.encode(InputData.SYMBOL, "UTF-8"));
            parameters.append("&period=").append(InputData.PERIOD);
            parameters.append("&fitness=").append(InputData.FITNESS);
            parameters.append("&number_of_neurons=").append(InputData.NEURONS.length);

            StringBuilder flags = new StringBuilder();
            for (int i = 0; i < InputData.NEURONS.length; i++) {
                flags.append(InputData.NEURONS[i]).append(" ");
            }
            parameters.append("&flags=").append(URLEncoder.encode(flags.toString().trim(), "UTF-8"));

            //TODO Matrix transpose is possible.
            StringBuilder weights = new StringBuilder();
            for (int j = 0; j < InputData.NEURONS.length; j++) {
                for (int i = 0; i < InputData.NEURONS.length; i++) {
                    weights.append(InputData.WEIGHTS[i][j]).append(" ");
                }
                weights.append("\r\n");
            }
            parameters.append("&weights=").append(URLEncoder.encode(weights.toString().trim(), "UTF-8"));

            //TODO Matrix transpose is possible.
            StringBuilder activities = new StringBuilder();
            for (int j = 0; j < InputData.NEURONS.length; j++) {
                for (int i = 0; i < InputData.NEURONS.length; i++) {
                    activities.append(InputData.ACTIVITIES[i][j]).append(" ");
                }
                activities.append("\r\n");
            }
            parameters.append("&activities=").append(URLEncoder.encode(activities.toString().trim(), "UTF-8"));

            try (OutputStream os = connection.getOutputStream()) {
                os.write(parameters.toString().getBytes("UTF-8"));
                os.flush();
            }

            int code = connection.getResponseCode();
            connection.disconnect();
            return code == HttpURLConnection.HTTP_OK;
        } catch (MalformedURLException exception) {
            return false;
        } catch (ProtocolException exception) {
            return false;
        } catch (UnsupportedEncodingException exception) {
            return false;
        } catch (IOException exception) {
            return false;
        }
    }
}
