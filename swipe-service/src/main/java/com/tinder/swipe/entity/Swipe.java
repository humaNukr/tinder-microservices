package com.tinder.swipe.entity;

import jakarta.persistence.Column;
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
    @Column(name = "user1_id")
    private UUID user1Id;
    @Id
    @Column(name = "user2_id")
    private UUID user2Id;

    /**
     * Null until that user has swiped; only then is the like/dislike value known.
     */
    @Column(name = "is_liked_by_user1")
    private Boolean isLikedByUser1;
    /**
     * Null until that user has swiped; only then is the like/dislike value known.
     */
    @Column(name = "is_liked_by_user2")
    private Boolean isLikedByUser2;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
