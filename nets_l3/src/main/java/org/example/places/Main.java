package org.example.places;

import org.example.places.service.LocationService;
import org.example.places.api.models.*;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class Main {
    private static final LocationService service = new LocationService();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== Поиск мест и информации ===");

        boolean running = true;

        while (running) {
            try {
                running = performSearch();
            } catch (Exception e) {
                System.err.println("Произошла ошибка: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private static boolean performSearch() {
        System.out.print("Введите название места для поиска: ");
        String query = scanner.nextLine();

        if (query.equals("exit")) { return false; }

        if (query.isEmpty()) { return true; }

        service.searchLocations(query)
                .thenCompose(locations -> {
                    System.out.println("\nНайдены локации:");
                    for (int i = 0; i < locations.size(); i++) {
                        Location loc = locations.get(i);
                        System.out.printf("%d. %s (%.4f, %.4f)%n",
                                i + 1, loc.getName(), loc.getLat(), loc.getLon());
                    }

                    System.out.print("\nВыберите номер локации: ");
                    int choice = scanner.nextInt();

                    scanner.nextLine();

                    Location selectedLocation = locations.get(choice - 1);
                    System.out.println("Выбрана: " + selectedLocation.getName());

                    CompletableFuture<Weather> weatherFuture =
                            service.getWeather(selectedLocation.getLat(), selectedLocation.getLon());

                    CompletableFuture<List<Place>> placesFuture =
                            service.getInterestingPlaces(selectedLocation.getLat(), selectedLocation.getLon());

                    return weatherFuture.thenCombine(placesFuture, (weather, places) -> {
                        return new SearchResult(selectedLocation, weather, places);
                    });
                })
                .whenComplete((result, error) -> {
                    if (error != null) {
                        System.err.println("Ошибка: " + error.getMessage());
                        error.printStackTrace();
                    } else {
                        displayResults(result);
                    }
                })
                .join();

        return true;
    }

    private static void displayResults(SearchResult result) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("РЕЗУЛЬТАТЫ ПОИСКА");
        System.out.println("=".repeat(50));

        System.out.println("\n----Локация: " + result.getLocation().getName());
        System.out.printf("   Координаты: %.4f, %.4f%n",
                result.getLocation().getLat(), result.getLocation().getLon());

        System.out.println("\n----Погода:");
        System.out.printf("   Описание: %s%n", result.getWeather().getDescription());
        System.out.printf("   Температура: %.1f°C%n", result.getWeather().getTemperature());
        System.out.printf("   Влажность: %.0f%%%n", result.getWeather().getHumidity());

        System.out.println("\n----Интересные места рядом:");
        if (result.getPlaces().isEmpty()) {
            System.out.println("   Не найдено интересных мест");
        } else {
            for (Place place : result.getPlaces()) {
                System.out.println("   • " + place.getTitle());
                System.out.println("     " + place.getDescription());
                System.out.println();
            }
        }
    }

    static class SearchResult {
        private final Location location;
        private final Weather weather;
        private final List<Place> places;

        public SearchResult(Location location, Weather weather, List<Place> places) {
            this.location = location;
            this.weather = weather;
            this.places = places;
        }

        public Location getLocation() { return location; }
        public Weather getWeather() { return weather; }
        public List<Place> getPlaces() { return places; }
    }
}