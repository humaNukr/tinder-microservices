package com.tinder.swipe.repository;


import com.tinder.swipe.dto.swipe.SwipeStatusProjection;
import com.tinder.swipe.entity.Swipe;
import com.tinder.swipe.entity.SwipeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SwipeRepository extends JpaRepository<Swipe, SwipeId> {

    @Query(value = """
            INSERT INTO swipes (user1_id, user2_id, is_liked_by_user1)
            VALUES (:user1Id, :user2Id, :isLiked)
            ON CONFLICT (user1_id, user2_id)
            DO UPDATE SET
                is_liked_by_user1 = :isLiked,
                updated_at = NOW()
            WHERE swipes.is_liked_by_user1 IS NULL
            RETURNING is_liked_by_user1 AS "isLikedByUser1", 
                      is_liked_by_user2 AS "isLikedByUser2"
            """, nativeQuery = true)
    SwipeStatusProjection upsertSwipeByUser1(
            @Param("user1Id") UUID user1Id,
            @Param("user2Id") UUID user2Id,
            @Param("isLiked") Boolean isLiked);

    @Query(value = """
            INSERT INTO swipes (user1_id, user2_id, is_liked_by_user2)
            VALUES (:user1Id, :user2Id, :isLiked)
            ON CONFLICT (user1_id, user2_id)
            DO UPDATE SET
                is_liked_by_user2 = :isLiked,
                updated_at = NOW()
            WHERE swipes.is_liked_by_user2 IS NULL
            RETURNING is_liked_by_user1 AS "isLikedByUser1", 
                      is_liked_by_user2 AS "isLikedByUser2"
            """, nativeQuery = true)
    SwipeStatusProjection upsertSwipeByUser2(
            @Param("user1Id") UUID user1Id,
            @Param("user2Id") UUID user2Id,
            @Param("isLiked") Boolean isLiked);
}
