package com.careeros.resumetailor.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class PersistentQuotaStore {

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("yyyyMMddHH").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM").withZone(ZoneOffset.UTC);
    private final JdbcTemplate jdbc;

    public PersistentQuotaStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public synchronized QuotaResult consumeTailor(String user, int hourlyLimit, int monthlyLimit) {
        String hourBucket = "hour:" + HOUR.format(Instant.now());
        String monthBucket = "month:" + MONTH.format(Instant.now());
        int hourly = count(user, hourBucket);
        int monthly = count(user, monthBucket);
        if (hourly >= hourlyLimit || monthly >= monthlyLimit) {
            return new QuotaResult(false, Math.max(0, hourlyLimit - hourly), Math.max(0, monthlyLimit - monthly));
        }
        increment(user, hourBucket);
        increment(user, monthBucket);
        return new QuotaResult(true, hourlyLimit - hourly - 1, monthlyLimit - monthly - 1);
    }

    public QuotaResult status(String user, int hourlyLimit, int monthlyLimit) {
        int hourly = count(user, "hour:" + HOUR.format(Instant.now()));
        int monthly = count(user, "month:" + MONTH.format(Instant.now()));
        return new QuotaResult(hourly < hourlyLimit && monthly < monthlyLimit,
                Math.max(0, hourlyLimit - hourly), Math.max(0, monthlyLimit - monthly));
    }

    private int count(String user, String bucket) {
        Integer value = jdbc.queryForObject(
                "select coalesce(max(request_count), 0) from beta_usage where user_id = ? and bucket = ?",
                Integer.class, user, bucket);
        return value == null ? 0 : value;
    }

    private void increment(String user, String bucket) {
        int updated = jdbc.update(
                "update beta_usage set request_count = request_count + 1, updated_at = current_timestamp where user_id = ? and bucket = ?",
                user, bucket);
        if (updated == 0) {
            jdbc.update("insert into beta_usage(user_id, bucket, request_count, updated_at) values (?, ?, 1, current_timestamp)",
                    user, bucket);
        }
    }

    public record QuotaResult(boolean allowed, int hourlyRemaining, int monthlyRemaining) {}
}
