package org.spring.canapa.backend.movie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "canapa.movie.config")
public class MovieApiProperties {

    private final Tmdb tmdb = new Tmdb();
    private final Omdb omdb = new Omdb();
    private long cacheTtlMinutes = 30;

    @Data
    public static class Tmdb {
        private String baseUrl = "https://api.themoviedb.org/3";
        private String apiKey = "";
        private String accessToken = "";

    }

    @Data
    public static class Omdb {
        private String baseUrl = "https://www.omdbapi.com/";
        private String apiKey = "";

    }
}
