package com.tinder.swipe.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class SwipeId implements java.io.Serializable {
    UUID user1Id;
    UUID user2Id;
}
