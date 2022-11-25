package ru.home.weather.aggregator.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import ru.home.weather.aggregator.domain.City;
import ru.home.weather.aggregator.domain.Indication;
import ru.home.weather.aggregator.domain.WebSite;
import ru.home.weather.aggregator.repository.WebSiteRepository;
import ru.home.weather.aggregator.service.OpenWeatherMapParser;
import ru.home.weather.aggregator.service.for_test.TestHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

/**
 * @author Elena Demeneva
 */
@Service
public class OpenWeatherMapApiController implements ApiController {
    protected final HttpClient client = HttpClient.newBuilder().build();
    private final String token = "518ca609f4c02785d22c816ef52b6c9c";
    @Autowired
    private OpenWeatherMapParser parser;
    @Autowired
    WebSiteRepository webSiteRepository;

    @Override
    public List<Indication> getForecasts(City city) {
        int httpStatus;
        try {
            HttpResponse<String> httpResponse = getForecastsHttpResponse(city);
            httpStatus = httpResponse.statusCode();
            if (httpResponse.statusCode() == 200) {
                List<Indication> indications = parser.parseForecastIndications(httpResponse.body());
                for (Indication indication : indications) {
                    indication.setCity(city);
                }
                return indications;
            }
        } catch (JsonProcessingException exception) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка парсинга ответа от api.openweathermap.org");
        } catch (Exception e) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Ошибка при выполнении запроса к серверу api.openweathermap.org");
        }
        throw new HttpClientErrorException(HttpStatus.valueOf(httpStatus), "Ошибка в ответе от сервера api.openweathermap.org");
    }

    public List<City> getCities(String cityName, String area, String countryAlpha2Code, int limit) {
        int httpStatus;
        try {
            HttpResponse<String> httpResponse = getCityHttpResponse(cityName, area, countryAlpha2Code, limit);
            httpStatus = httpResponse.statusCode();
            if (httpStatus == 200) {
                return parser.parseCities(httpResponse.body());
            }
        } catch (JsonProcessingException exception) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка парсинга ответа от api.openweathermap.org");
        } catch (NoSuchElementException exception) {
            throw exception;
        } catch (Exception e) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Ошибка при выполнении запроса к серверу api.openweathermap.org");
        }
        throw new HttpClientErrorException(HttpStatus.valueOf(httpStatus), "Ошибка в ответе от сервера api.openweathermap.org");
    }

    @Override
    public Indication getObservation(City city) {
        int httpStatus;
        try {
            HttpResponse<String> httpResponse = getObservationHttpResponse(city);
            httpStatus = httpResponse.statusCode();
            if (httpResponse.statusCode() == 200) {
                Indication indication = parser.parseObservationIndication(httpResponse.body());
                indication.setCity(city);
                return indication;
            }
        } catch (JsonProcessingException exception) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка парсинга ответа от api.openweathermap.org");
        } catch (Exception e) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Ошибка при выполнении запроса к серверу api.openweathermap.org");
        }
        throw new HttpClientErrorException(HttpStatus.valueOf(httpStatus), "Ошибка в ответе от сервера api.openweathermap.org");
    }

    private HttpResponse<String> getCityHttpResponse(String cityName, String state, String countryAlpha2Code, int limit)
            throws InterruptedException, IOException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://api.openweathermap.org/geo/1.0/direct?q=" +
                        cityName +
                        "," + state +
                        "," + (countryAlpha2Code.isBlank() ? Locale.getDefault().getCountry() : countryAlpha2Code)
                        +"&limit=" + limit +
                        "&appid=" + token))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getForecastsHttpResponse(City city) throws InterruptedException, IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://api.openweathermap.org/data/2.5/forecast?lat=" +
                        city.getLatitude() +
                        "&lon=" +
                        city.getLongitude() +
                        "&appid=" + token))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getObservationHttpResponse(City city) throws InterruptedException, IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://api.openweathermap.org/data/2.5/weather?lat=" +
                        city.getLatitude() + "&lon=" +
                        city.getLongitude() +
                        "&appid=" +
                        token))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // Метод-заглушка
    private HttpResponse<String> getCityHttpResponse2(String cityName, String state, String countryAlpha2Code, int limit) throws InterruptedException, IOException {
        String response = "[{\"name\":\"Николаевка\",\"local_names\":{\"ba\":\"Николаевка\",\"pl\":\"Mikołajówka\",\"ru\":\"Николаевка\"},\"lat\":54.0243538,\"lon\":56.0996099,\"country\":\"RU\",\"state\":\"Bashkortostan\"},{\"name\":\"Николаевка\",\"local_names\":{\"ru\":\"Николаевка\",\"pl\":\"Mikołajówka\"},\"lat\":54.46471,\"lon\":56.137558,\"country\":\"RU\",\"state\":\"Bashkortostan\"},{\"name\":\"Николаевка\",\"local_names\":{\"ba\":\"Николаевка\",\"pl\":\"Mikołajówka\",\"ru\":\"Николаевка\"},\"lat\":53.749599,\"lon\":56.132137,\"country\":\"RU\",\"state\":\"Bashkortostan\"},{\"name\":\"Николаевка\",\"local_names\":{\"ru\":\"Николаевка\",\"ba\":\"Николаевка\",\"pl\":\"Mikołajówka\"},\"lat\":53.530891,\"lon\":55.570736,\"country\":\"RU\",\"state\":\"Bashkortostan\"},{\"name\":\"Николаевка\",\"local_names\":{\"ru\":\"Николаевка\",\"ba\":\"Николаевка\",\"pl\":\"Mikołajówka\"},\"lat\":53.981815,\"lon\":55.27166,\"country\":\"RU\",\"state\":\"Bashkortostan\"}]";
        return new TestHttpResponse(200, response);
    }

    // Метод-заглушка
    private HttpResponse<String> getForecastsHttpResponse2(City city) throws InterruptedException, IOException {
        String response = "{\"cod\":\"200\",\"message\":0,\"cnt\":40,\"list\":[{\"dt\":1666267200,\"main\":{\"temp\":277.01,\"feels_like\":273.39,\"temp_min\":277.01,\"temp_max\":277.06,\"pressure\":1008,\"sea_level\":1008,\"grnd_level\":978,\"humidity\":65,\"temp_kf\":-0.05},\"weather\":[{\"id\":803,\"main\":\"Clouds\",\"description\":\"broken clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":77},\"wind\":{\"speed\":4.37,\"deg\":273,\"gust\":7.88},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-20 12:00:00\"},{\"dt\":1666278000,\"main\":{\"temp\":276.43,\"feels_like\":273.34,\"temp_min\":276.15,\"temp_max\":276.43,\"pressure\":1009,\"sea_level\":1009,\"grnd_level\":979,\"humidity\":66,\"temp_kf\":0.28},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":86},\"wind\":{\"speed\":3.33,\"deg\":261,\"gust\":8.06},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-20 15:00:00\"},{\"dt\":1666288800,\"main\":{\"temp\":274.17,\"feels_like\":270.65,\"temp_min\":274.17,\"temp_max\":274.17,\"pressure\":1010,\"sea_level\":1010,\"grnd_level\":979,\"humidity\":78,\"temp_kf\":0},\"weather\":[{\"id\":802,\"main\":\"Clouds\",\"description\":\"scattered clouds\",\"icon\":\"03n\"}],\"clouds\":{\"all\":50},\"wind\":{\"speed\":3.27,\"deg\":242,\"gust\":9.83},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-20 18:00:00\"},{\"dt\":1666299600,\"main\":{\"temp\":273.81,\"feels_like\":269.8,\"temp_min\":273.81,\"temp_max\":273.81,\"pressure\":1010,\"sea_level\":1010,\"grnd_level\":979,\"humidity\":80,\"temp_kf\":0},\"weather\":[{\"id\":800,\"main\":\"Clear\",\"description\":\"clear sky\",\"icon\":\"01n\"}],\"clouds\":{\"all\":9},\"wind\":{\"speed\":3.82,\"deg\":254,\"gust\":11.92},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-20 21:00:00\"},{\"dt\":1666310400,\"main\":{\"temp\":273.28,\"feels_like\":269.02,\"temp_min\":273.28,\"temp_max\":273.28,\"pressure\":1010,\"sea_level\":1010,\"grnd_level\":979,\"humidity\":83,\"temp_kf\":0},\"weather\":[{\"id\":800,\"main\":\"Clear\",\"description\":\"clear sky\",\"icon\":\"01n\"}],\"clouds\":{\"all\":10},\"wind\":{\"speed\":4.01,\"deg\":250,\"gust\":12.4},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-21 00:00:00\"},{\"dt\":1666321200,\"main\":{\"temp\":273.44,\"feels_like\":269.14,\"temp_min\":273.44,\"temp_max\":273.44,\"pressure\":1009,\"sea_level\":1009,\"grnd_level\":979,\"humidity\":83,\"temp_kf\":0},\"weather\":[{\"id\":803,\"main\":\"Clouds\",\"description\":\"broken clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":64},\"wind\":{\"speed\":4.12,\"deg\":249,\"gust\":12.1},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-21 03:00:00\"},{\"dt\":1666332000,\"main\":{\"temp\":276.43,\"feels_like\":272.32,\"temp_min\":276.43,\"temp_max\":276.43,\"pressure\":1009,\"sea_level\":1009,\"grnd_level\":978,\"humidity\":69,\"temp_kf\":0},\"weather\":[{\"id\":802,\"main\":\"Clouds\",\"description\":\"scattered clouds\",\"icon\":\"03d\"}],\"clouds\":{\"all\":43},\"wind\":{\"speed\":5.03,\"deg\":256,\"gust\":10.18},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-21 06:00:00\"},{\"dt\":1666342800,\"main\":{\"temp\":278.08,\"feels_like\":274.43,\"temp_min\":278.08,\"temp_max\":278.08,\"pressure\":1008,\"sea_level\":1008,\"grnd_level\":978,\"humidity\":58,\"temp_kf\":0},\"weather\":[{\"id\":803,\"main\":\"Clouds\",\"description\":\"broken clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":75},\"wind\":{\"speed\":4.94,\"deg\":256,\"gust\":9.4},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-21 09:00:00\"},{\"dt\":1666353600,\"main\":{\"temp\":276.48,\"feels_like\":272.67,\"temp_min\":276.48,\"temp_max\":276.48,\"pressure\":1008,\"sea_level\":1008,\"grnd_level\":977,\"humidity\":67,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":88},\"wind\":{\"speed\":4.49,\"deg\":262,\"gust\":10.01},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-21 12:00:00\"},{\"dt\":1666364400,\"main\":{\"temp\":274.99,\"feels_like\":270.86,\"temp_min\":274.99,\"temp_max\":274.99,\"pressure\":1008,\"sea_level\":1008,\"grnd_level\":978,\"humidity\":79,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":100},\"wind\":{\"speed\":4.43,\"deg\":255,\"gust\":9.95},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-21 15:00:00\"},{\"dt\":1666375200,\"main\":{\"temp\":274.2,\"feels_like\":269.67,\"temp_min\":274.2,\"temp_max\":274.2,\"pressure\":1009,\"sea_level\":1009,\"grnd_level\":978,\"humidity\":87,\"temp_kf\":0},\"weather\":[{\"id\":600,\"main\":\"Snow\",\"description\":\"light snow\",\"icon\":\"13n\"}],\"clouds\":{\"all\":100},\"wind\":{\"speed\":4.79,\"deg\":281,\"gust\":10.37},\"visibility\":10000,\"pop\":0.31,\"snow\":{\"3h\":0.12},\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-21 18:00:00\"},{\"dt\":1666386000,\"main\":{\"temp\":271.37,\"feels_like\":266.84,\"temp_min\":271.37,\"temp_max\":271.37,\"pressure\":1012,\"sea_level\":1012,\"grnd_level\":980,\"humidity\":75,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":96},\"wind\":{\"speed\":3.79,\"deg\":304,\"gust\":10.25},\"visibility\":10000,\"pop\":0.01,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-21 21:00:00\"},{\"dt\":1666396800,\"main\":{\"temp\":270.83,\"feels_like\":266.63,\"temp_min\":270.83,\"temp_max\":270.83,\"pressure\":1013,\"sea_level\":1013,\"grnd_level\":982,\"humidity\":76,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":98},\"wind\":{\"speed\":3.25,\"deg\":294,\"gust\":8.36},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-22 00:00:00\"},{\"dt\":1666407600,\"main\":{\"temp\":270.15,\"feels_like\":265.44,\"temp_min\":270.15,\"temp_max\":270.15,\"pressure\":1014,\"sea_level\":1014,\"grnd_level\":983,\"humidity\":79,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":100},\"wind\":{\"speed\":3.67,\"deg\":297,\"gust\":9.93},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-22 03:00:00\"},{\"dt\":1666418400,\"main\":{\"temp\":273.15,\"feels_like\":268.73,\"temp_min\":273.15,\"temp_max\":273.15,\"pressure\":1014,\"sea_level\":1014,\"grnd_level\":983,\"humidity\":58,\"temp_kf\":0},\"weather\":[{\"id\":803,\"main\":\"Clouds\",\"description\":\"broken clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":80},\"wind\":{\"speed\":4.2,\"deg\":304,\"gust\":7.6},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-22 06:00:00\"},{\"dt\":1666429200,\"main\":{\"temp\":274.9,\"feels_like\":270.36,\"temp_min\":274.9,\"temp_max\":274.9,\"pressure\":1013,\"sea_level\":1013,\"grnd_level\":982,\"humidity\":57,\"temp_kf\":0},\"weather\":[{\"id\":802,\"main\":\"Clouds\",\"description\":\"scattered clouds\",\"icon\":\"03d\"}],\"clouds\":{\"all\":25},\"wind\":{\"speed\":5.12,\"deg\":313,\"gust\":7.35},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-22 09:00:00\"},{\"dt\":1666440000,\"main\":{\"temp\":273.13,\"feels_like\":268.28,\"temp_min\":273.13,\"temp_max\":273.13,\"pressure\":1013,\"sea_level\":1013,\"grnd_level\":982,\"humidity\":66,\"temp_kf\":0},\"weather\":[{\"id\":802,\"main\":\"Clouds\",\"description\":\"scattered clouds\",\"icon\":\"03d\"}],\"clouds\":{\"all\":50},\"wind\":{\"speed\":4.87,\"deg\":308,\"gust\":8.54},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-22 12:00:00\"},{\"dt\":1666450800,\"main\":{\"temp\":272.41,\"feels_like\":267.31,\"temp_min\":272.41,\"temp_max\":272.41,\"pressure\":1015,\"sea_level\":1015,\"grnd_level\":984,\"humidity\":71,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":93},\"wind\":{\"speed\":4.99,\"deg\":300,\"gust\":9.89},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-22 15:00:00\"},{\"dt\":1666461600,\"main\":{\"temp\":271.15,\"feels_like\":265.92,\"temp_min\":271.15,\"temp_max\":271.15,\"pressure\":1016,\"sea_level\":1016,\"grnd_level\":985,\"humidity\":82,\"temp_kf\":0},\"weather\":[{\"id\":803,\"main\":\"Clouds\",\"description\":\"broken clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":66},\"wind\":{\"speed\":4.7,\"deg\":296,\"gust\":10.52},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-22 18:00:00\"},{\"dt\":1666472400,\"main\":{\"temp\":272.01,\"feels_like\":266.68,\"temp_min\":272.01,\"temp_max\":272.01,\"pressure\":1017,\"sea_level\":1017,\"grnd_level\":985,\"humidity\":70,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":97},\"wind\":{\"speed\":5.22,\"deg\":302,\"gust\":9.13},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-22 21:00:00\"},{\"dt\":1666483200,\"main\":{\"temp\":271.82,\"feels_like\":266.81,\"temp_min\":271.82,\"temp_max\":271.82,\"pressure\":1018,\"sea_level\":1018,\"grnd_level\":987,\"humidity\":73,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":99},\"wind\":{\"speed\":4.61,\"deg\":292,\"gust\":8.94},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-23 00:00:00\"},{\"dt\":1666494000,\"main\":{\"temp\":270.67,\"feels_like\":265.25,\"temp_min\":270.67,\"temp_max\":270.67,\"pressure\":1020,\"sea_level\":1020,\"grnd_level\":989,\"humidity\":78,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":99},\"wind\":{\"speed\":4.81,\"deg\":293,\"gust\":9.62},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-23 03:00:00\"},{\"dt\":1666504800,\"main\":{\"temp\":272.37,\"feels_like\":267.14,\"temp_min\":272.37,\"temp_max\":272.37,\"pressure\":1022,\"sea_level\":1022,\"grnd_level\":990,\"humidity\":63,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":95},\"wind\":{\"speed\":5.2,\"deg\":287,\"gust\":8.03},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-23 06:00:00\"},{\"dt\":1666515600,\"main\":{\"temp\":274.25,\"feels_like\":269.59,\"temp_min\":274.25,\"temp_max\":274.25,\"pressure\":1022,\"sea_level\":1022,\"grnd_level\":991,\"humidity\":54,\"temp_kf\":0},\"weather\":[{\"id\":803,\"main\":\"Clouds\",\"description\":\"broken clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":83},\"wind\":{\"speed\":5.03,\"deg\":283,\"gust\":7.36},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-23 09:00:00\"},{\"dt\":1666526400,\"main\":{\"temp\":273.21,\"feels_like\":268.96,\"temp_min\":273.21,\"temp_max\":273.21,\"pressure\":1022,\"sea_level\":1022,\"grnd_level\":991,\"humidity\":59,\"temp_kf\":0},\"weather\":[{\"id\":803,\"main\":\"Clouds\",\"description\":\"broken clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":75},\"wind\":{\"speed\":3.97,\"deg\":264,\"gust\":8.77},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-23 12:00:00\"},{\"dt\":1666537200,\"main\":{\"temp\":272.12,\"feels_like\":267.54,\"temp_min\":272.12,\"temp_max\":272.12,\"pressure\":1023,\"sea_level\":1023,\"grnd_level\":992,\"humidity\":68,\"temp_kf\":0},\"weather\":[{\"id\":803,\"main\":\"Clouds\",\"description\":\"broken clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":76},\"wind\":{\"speed\":4.09,\"deg\":250,\"gust\":11.89},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-23 15:00:00\"},{\"dt\":1666548000,\"main\":{\"temp\":271.95,\"feels_like\":267.32,\"temp_min\":271.95,\"temp_max\":271.95,\"pressure\":1023,\"sea_level\":1023,\"grnd_level\":991,\"humidity\":69,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":88},\"wind\":{\"speed\":4.1,\"deg\":239,\"gust\":12.14},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-23 18:00:00\"},{\"dt\":1666558800,\"main\":{\"temp\":272.3,\"feels_like\":267.35,\"temp_min\":272.3,\"temp_max\":272.3,\"pressure\":1022,\"sea_level\":1022,\"grnd_level\":990,\"humidity\":74,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":91},\"wind\":{\"speed\":4.7,\"deg\":240,\"gust\":13.22},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-23 21:00:00\"},{\"dt\":1666569600,\"main\":{\"temp\":273.02,\"feels_like\":268.07,\"temp_min\":273.02,\"temp_max\":273.02,\"pressure\":1020,\"sea_level\":1020,\"grnd_level\":989,\"humidity\":75,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":94},\"wind\":{\"speed\":5,\"deg\":230,\"gust\":12.84},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-24 00:00:00\"},{\"dt\":1666580400,\"main\":{\"temp\":273.07,\"feels_like\":268.19,\"temp_min\":273.07,\"temp_max\":273.07,\"pressure\":1019,\"sea_level\":1019,\"grnd_level\":988,\"humidity\":78,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":99},\"wind\":{\"speed\":4.91,\"deg\":226,\"gust\":13.75},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-24 03:00:00\"},{\"dt\":1666591200,\"main\":{\"temp\":275.93,\"feels_like\":271.3,\"temp_min\":275.93,\"temp_max\":275.93,\"pressure\":1018,\"sea_level\":1018,\"grnd_level\":987,\"humidity\":60,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":100},\"wind\":{\"speed\":5.85,\"deg\":227,\"gust\":13.61},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-24 06:00:00\"},{\"dt\":1666602000,\"main\":{\"temp\":277.18,\"feels_like\":272.81,\"temp_min\":277.18,\"temp_max\":277.18,\"pressure\":1016,\"sea_level\":1016,\"grnd_level\":985,\"humidity\":53,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":100},\"wind\":{\"speed\":6.01,\"deg\":224,\"gust\":14.19},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-24 09:00:00\"},{\"dt\":1666612800,\"main\":{\"temp\":276.38,\"feels_like\":271.8,\"temp_min\":276.38,\"temp_max\":276.38,\"pressure\":1014,\"sea_level\":1014,\"grnd_level\":983,\"humidity\":56,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":99},\"wind\":{\"speed\":5.99,\"deg\":219,\"gust\":14.17},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-24 12:00:00\"},{\"dt\":1666623600,\"main\":{\"temp\":275.99,\"feels_like\":271.26,\"temp_min\":275.99,\"temp_max\":275.99,\"pressure\":1012,\"sea_level\":1012,\"grnd_level\":981,\"humidity\":62,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":100},\"wind\":{\"speed\":6.09,\"deg\":224,\"gust\":15.1},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-24 15:00:00\"},{\"dt\":1666634400,\"main\":{\"temp\":275.71,\"feels_like\":270.99,\"temp_min\":275.71,\"temp_max\":275.71,\"pressure\":1010,\"sea_level\":1010,\"grnd_level\":980,\"humidity\":72,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":100},\"wind\":{\"speed\":5.9,\"deg\":222,\"gust\":15.04},\"visibility\":10000,\"pop\":0,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-24 18:00:00\"},{\"dt\":1666645200,\"main\":{\"temp\":275.18,\"feels_like\":270.48,\"temp_min\":275.18,\"temp_max\":275.18,\"pressure\":1008,\"sea_level\":1008,\"grnd_level\":977,\"humidity\":80,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":100},\"wind\":{\"speed\":5.57,\"deg\":219,\"gust\":14.21},\"visibility\":10000,\"pop\":0.05,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-24 21:00:00\"},{\"dt\":1666656000,\"main\":{\"temp\":275.15,\"feels_like\":270.46,\"temp_min\":275.15,\"temp_max\":275.15,\"pressure\":1006,\"sea_level\":1006,\"grnd_level\":975,\"humidity\":93,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"clouds\":{\"all\":100},\"wind\":{\"speed\":5.54,\"deg\":224,\"gust\":13.77},\"visibility\":5759,\"pop\":0.12,\"sys\":{\"pod\":\"n\"},\"dt_txt\":\"2022-10-25 00:00:00\"},{\"dt\":1666666800,\"main\":{\"temp\":275.69,\"feels_like\":271.32,\"temp_min\":275.69,\"temp_max\":275.69,\"pressure\":1005,\"sea_level\":1005,\"grnd_level\":974,\"humidity\":96,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":100},\"wind\":{\"speed\":5.17,\"deg\":230,\"gust\":12.64},\"visibility\":39,\"pop\":0.18,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-25 03:00:00\"},{\"dt\":1666677600,\"main\":{\"temp\":276.68,\"feels_like\":272.78,\"temp_min\":276.68,\"temp_max\":276.68,\"pressure\":1004,\"sea_level\":1004,\"grnd_level\":974,\"humidity\":96,\"temp_kf\":0},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04d\"}],\"clouds\":{\"all\":100},\"wind\":{\"speed\":4.75,\"deg\":235,\"gust\":11.35},\"visibility\":136,\"pop\":0.33,\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-25 06:00:00\"},{\"dt\":1666688400,\"main\":{\"temp\":275.93,\"feels_like\":272.19,\"temp_min\":275.93,\"temp_max\":275.93,\"pressure\":1005,\"sea_level\":1005,\"grnd_level\":975,\"humidity\":96,\"temp_kf\":0},\"weather\":[{\"id\":500,\"main\":\"Rain\",\"description\":\"light rain\",\"icon\":\"10d\"}],\"clouds\":{\"all\":100},\"wind\":{\"speed\":4.14,\"deg\":268,\"gust\":9.63},\"visibility\":44,\"pop\":0.81,\"rain\":{\"3h\":0.41},\"sys\":{\"pod\":\"d\"},\"dt_txt\":\"2022-10-25 09:00:00\"}],\"city\":{\"id\":1494346,\"name\":\"Posëlok Rabochiy\",\"coord\":{\"lat\":56.8391,\"lon\":60.6083},\"country\":\"RU\",\"population\":2000,\"timezone\":18000,\"sunrise\":1666233609,\"sunset\":1666269860}}";
        return new TestHttpResponse(200, response);
    }

    // Метод-заглушка
    private HttpResponse<String> getObservationHttpResponse2(City city) throws InterruptedException, IOException {
        String response = "{\"coord\":{\"lon\":60.1659,\"lat\":56.4421},\"weather\":[{\"id\":804,\"main\":\"Clouds\",\"description\":\"overcast clouds\",\"icon\":\"04n\"}],\"base\":\"stations\",\"main\":{\"temp\":273.52,\"feels_like\":268.38,\"temp_min\":273.52,\"temp_max\":273.52,\"pressure\":1019,\"humidity\":91,\"sea_level\":1019,\"grnd_level\":972},\"visibility\":10000,\"wind\":{\"speed\":5.59,\"deg\":242,\"gust\":13.93},\"clouds\":{\"all\":100},\"dt\":1667996679,\"sys\":{\"country\":\"RU\",\"sunrise\":1667964265,\"sunset\":1667995315},\"timezone\":18000,\"id\":1494573,\"name\":\"Polevskoy\",\"cod\":200}";
        return new TestHttpResponse(200, response);
    }

}
