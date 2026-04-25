package com.tinder.profile.domain;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "profiles")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Profile {

    @Setter(AccessLevel.PRIVATE)
    @Id
    private String id;

    @EqualsAndHashCode.Include
    @Indexed(unique = true)
    @ToString.Include
    private UUID userId;

    @ToString.Include
    private String name;

    @Indexed
    private LocalDate birthDate;

    private Gender gender;

    private String bio;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private UserPreferences preferences;

    private List<String> interests = new ArrayList<>();
    private List<String> photos = new ArrayList<>();

    private LocalDateTime lastSeen;
}