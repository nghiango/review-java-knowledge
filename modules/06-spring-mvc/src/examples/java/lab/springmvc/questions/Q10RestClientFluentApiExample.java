package lab.springmvc.questions;

import org.springframework.web.client.RestClient;

public class Q10RestClientFluentApiExample {

    record WeatherForecast(String city, int temperatureCelsius) {}

    public static void main(String[] args) {
        // RestClient is Spring 6.1+ synchronous fluent HTTP client replacing RestTemplate
        RestClient restClient =
                RestClient.builder().baseUrl("https://api.weather.example.com").build();

        boolean isClientCreated = restClient != null; // true
        WeatherForecast sample = new WeatherForecast("Stockholm", 18);

        String city = sample.city(); // "Stockholm"
        int temp = sample.temperatureCelsius(); // 18

        System.out.println(
                "RestClient created: " + isClientCreated + ", city: " + city + ", temp: " + temp);
    }
}
