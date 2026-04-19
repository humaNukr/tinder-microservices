package com.tinder.profile.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Document(collection = "profiles")
public class Profile {
    @Id
    private String id;

    @Indexed(unique = true)
    private UUID userId;

    private String name;
    @Indexed
    private LocalDate birthDate;
    private Gender gender;
    private String bio;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private UserPreferences preferences;

    private List<String> interests;
    private List<String> photos;
    private LocalDateTime lastSeen;
}