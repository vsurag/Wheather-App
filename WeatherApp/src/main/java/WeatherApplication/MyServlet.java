package WeatherApplication;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/MyServlet")
public class MyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();
       
    
    public MyServlet() {
        super();
    }

     
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("index.html");
    }

    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String apiKey = "318b0e53453526b505df2509b1a928a1";
        String city = request.getParameter("city");

        if (city == null || city.trim().isEmpty()) {
            request.setAttribute("error", "City name cannot be empty");
            request.getRequestDispatcher("index.jsp").forward(request, response);
            return;
        }

        HttpURLConnection connection = null;
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + encodedCity + "&appid=" + apiKey;
            
            URI uri = URI.create(apiUrl);
            URL url = uri.toURL();
            
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream();
                     InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                     Scanner scanner = new Scanner(reader)) {
                    
                    StringBuilder responseContent = new StringBuilder();
                    while (scanner.hasNext()) {
                        responseContent.append(scanner.nextLine());
                    }

                    // Parse JSON response
                    JsonObject jsonObject = gson.fromJson(responseContent.toString(), JsonObject.class);
                    
                    // Extract data with null checks
                    // Date & Time
                    long dateTimestamp = jsonObject.get("dt").getAsLong() * 1000;
                    Date date = new Date(dateTimestamp);
                    
                    // Temperature
                    double temperatureKelvin = jsonObject.getAsJsonObject("main").get("temp").getAsDouble();
                    int temperatureCelsius = (int) (temperatureKelvin - 273.15);
                    
                    // Humidity
                    int humidity = jsonObject.getAsJsonObject("main").get("humidity").getAsInt();
                    
                    // Wind Speed
                    double windSpeed = jsonObject.getAsJsonObject("wind").get("speed").getAsDouble();
                    
                    // Weather Condition
                    String weatherCondition = jsonObject.getAsJsonArray("weather")
                                                     .get(0)
                                                     .getAsJsonObject()
                                                     .get("main")
                                                     .getAsString();
                    
                    request.setAttribute("date", date);
                    request.setAttribute("city", city);
                    request.setAttribute("temperature", temperatureCelsius);
                    request.setAttribute("weatherCondition", weatherCondition);
                    request.setAttribute("humidity", humidity);
                    request.setAttribute("windSpeed", windSpeed);
                    request.setAttribute("weatherData", responseContent.toString());
                }
            } else {
                try (InputStream errorStream = connection.getErrorStream();
                     InputStreamReader errorReader = new InputStreamReader(errorStream, StandardCharsets.UTF_8);
                     Scanner errorScanner = new Scanner(errorReader)) {
                    
                    StringBuilder errorContent = new StringBuilder();
                    while (errorScanner.hasNext()) {
                        errorContent.append(errorScanner.nextLine());
                    }
                    
                    JsonObject errorJson = gson.fromJson(errorContent.toString(), JsonObject.class);
                    String errorMessage = errorJson.has("message") ? 
                                       errorJson.get("message").getAsString() : 
                                       "Error fetching weather data";
                    
                    request.setAttribute("error", errorMessage);
                }
            }
        } catch (Exception e) {
            request.setAttribute("error", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        request.getRequestDispatcher("index.jsp").forward(request, response);
    }
}
