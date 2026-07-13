package com.nexuspms.notifications.domain;

public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED,
    DEAD_LETTERED
}
