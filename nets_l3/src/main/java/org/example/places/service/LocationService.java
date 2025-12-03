package org.example.places.service;

import org.example.places.api.ApiClient;
import org.example.places.api.models.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LocationService {
    private final ApiClient apiClient;
    private final ObjectMapper mapper;

    private static final String GRAPHHOPPER_API_KEY = "deleted";
    private static final String OPENWEATHER_API_KEY = "deleted";

    public LocationService() {
        this.apiClient = new ApiClient();
        this.mapper = new ObjectMapper();
    }

    public CompletableFuture<List<Location>> searchLocations(String query) {
        String url = String.format("https://graphhopper.com/api/1/geocode?q=%s&key=%s",
                query.replace(" ", "%20"), GRAPHHOPPER_API_KEY);

        return apiClient.getAsync(url)
                .thenApply(response -> {
                    try {
                        JsonNode root = mapper.readTree(response);
                        JsonNode hits = root.path("hits");

                        List<Location> locations = new ArrayList<>();

                        if (hits.isArray()) {
                            for (JsonNode hit : hits) {
                                String name = hit.path("name").asText();

                                JsonNode point = hit.path("point");
                                double lat = point.path("lat").asDouble();
                                double lng = point.path("lng").asDouble();

                                locations.add(new Location(name, lat, lng));
                            }
                        }

                        return locations;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse locations", e);
                    }
                });
    }

    public CompletableFuture<Weather> getWeather(double lat, double lon) {
        String latStr = String.valueOf(lat).replace(',', '.');
        String lonStr = String.valueOf(lon).replace(',', '.');

        String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=metric&lang=ru",
                latStr, lonStr, OPENWEATHER_API_KEY);

        return apiClient.getAsync(url)
                .thenApply(response -> {
                    try {
                        JsonNode root = mapper.readTree(response);
                        JsonNode weather = root.path("weather").get(0);
                        JsonNode main = root.path("main");

                        return new Weather(
                                weather.path("description").asText(),
                                main.path("temp").asDouble(),
                                main.path("humidity").asDouble()
                        );
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse weather", e);
                    }
                });
    }

    public CompletableFuture<List<Place>> getInterestingPlaces(double lat, double lon) {
        String strLon = String.valueOf(lon).replace(",", ".");
        String strLat = String.valueOf(lat).replace(",", ".");
        String geoDataUrl = String.format(
                "https://en.wikipedia.org/w/api.php?action=query&format=json&list=geosearch&gsradius=10000&gscoord=%s|%s&gslimit=10&gsprop=type|name|dim",
                strLat, strLon
        );

        return apiClient.getAsync(geoDataUrl)
                .thenCompose(geoDataResponse -> {
                    try {
                        JsonNode geoRoot = mapper.readTree(geoDataResponse);
                        JsonNode geoSearchResults = geoRoot.path("query").path("geosearch");

                        if (!geoSearchResults.isArray() || geoSearchResults.size() == 0) {
                            return CompletableFuture.completedFuture(new ArrayList<>());
                        }

                        List<Integer> pageIds = new ArrayList<>();
                        for (JsonNode geoResult : geoSearchResults) {
                            int pageId = geoResult.path("pageid").asInt();
                            pageIds.add(pageId);
                        }

                        return getPlacesWithDescriptions(pageIds, geoSearchResults);

                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse GeoData", e);
                    }
                });
    }

    private CompletableFuture<List<Place>> getPlacesWithDescriptions(List<Integer> pageIds, JsonNode geoSearchResults) {
        String pageIdsStr = String.join("|",
                pageIds.stream().map(String::valueOf).toArray(String[]::new));

        String textExtractsUrl = String.format(
                "https://en.wikipedia.org/w/api.php?action=query&format=json&prop=extracts|info&inprop=url&exintro=true&explaintext=true&pageids=%s",
                pageIdsStr
        );

        return apiClient.getAsync(textExtractsUrl)
                .thenApply(extractsResponse -> {
                    try {
                        JsonNode extractsRoot = mapper.readTree(extractsResponse);
                        JsonNode pages = extractsRoot.path("query").path("pages");

                        List<Place> places = new ArrayList<>();

                        for (JsonNode geoResult : geoSearchResults) {
                            int pageId = geoResult.path("pageid").asInt();
                            JsonNode page = pages.path(String.valueOf(pageId));

                            if (!page.isMissingNode()) {
                                String title = page.path("title").asText();
                                String description = page.path("extract").asText();

                                if (description.length() > 300) {
                                    description = description.substring(0, 300).trim() + "...";
                                }

                                double distance = geoResult.path("dist").asDouble();
                                String distanceInfo = String.format("(%.1f км от вас)", distance / 1000);

                                String fullDescription = description + " " + distanceInfo;

                                places.add(new Place(title, fullDescription, String.valueOf(pageId)));
                            }
                        }

                        return places;

                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse TextExtracts", e);
                    }
                });
    }
}
