package com.tinder.swipe.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "swipes")
@Data
@IdClass(SwipeId.class)
public class Swipe {
    @Id
    private UUID user1Id;
    @Id
    private UUID user2Id;

    private Boolean isLikedByUser1;
    private Boolean isLikedByUser2;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private LocalDateTime updatedAt;
}
