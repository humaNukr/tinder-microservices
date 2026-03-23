package com.tinder.profile.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
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
    private LocalDate birthDate;
    private Gender gender;
    private Gender targetGender;
    private String bio;

    private List<String> interests;
    private List<String> photos;
}