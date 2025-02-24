package com.wttech.aem.contentor.core.code;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class Condition {

    private final ExecutionContext executionContext;

    private final ExecutionHistory executionHistory;

    public Condition(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.executionHistory = new ExecutionHistory(executionContext.getResourceResolver());
    }

    public boolean always() {
        return true;
    }

    public boolean never() {
        return false;
    }

    public boolean once() {
        return oncePerExecutableId();
    }

    public boolean changed() {
        return oncePerExecutableIdAndContent();
    }

    public boolean oncePerExecutableId() {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        return !executionHistory.findAll(query).findAny().isPresent();
    }

    public boolean oncePerExecutableIdAndContent() {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        return executionHistory
                .findAll(query)
                .noneMatch(e -> StringUtils.equals(
                        e.getExecutable().getContent(),
                        executionContext.getExecutable().getContent()));
    }

    public boolean hourly() {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(Date.from(LocalDate.now().atTime(LocalTime.now().withMinute(0).withSecond(0).withNano(0)).atZone(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(LocalDate.now().atTime(LocalTime.now().withMinute(59).withSecond(59).withNano(999999999)).atZone(ZoneId.systemDefault()).toInstant()));
        Optional<Execution> executionThisHour = executionHistory.findAll(query).findAny();
        return !executionThisHour.isPresent();
    }

    public boolean hourlyBetweenMinutes(int startMinute, int endMinute) {
        LocalTime startTime = LocalTime.now().withMinute(startMinute).withSecond(0).withNano(0);
        LocalTime endTime = LocalTime.now().withMinute(endMinute).withSecond(59).withNano(999999999);

        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(Date.from(LocalDate.now().atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(LocalDate.now().atTime(endTime).atZone(ZoneId.systemDefault()).toInstant()));
        Optional<Execution> executionInTimeRange = executionHistory.findAll(query).findAny();
        return !executionInTimeRange.isPresent();
    }

    public boolean daily() {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(
                Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(LocalDate.now()
                .atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toInstant()));
        Optional<Execution> executionFromToday = executionHistory.findAll(query).findAny();
        return !executionFromToday.isPresent();
    }

    public boolean dailyBetweenTime(String timeRange) {
        String[] times = timeRange.split("-");
        LocalTime startTime = LocalTime.parse(times[0]);
        LocalTime endTime = LocalTime.parse(times[1]);

        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(Date.from(LocalDate.now().atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(LocalDate.now().atTime(endTime).atZone(ZoneId.systemDefault()).toInstant()));
        Optional<Execution> executionInTimeRange = executionHistory.findAll(query).findAny();
        return !executionInTimeRange.isPresent();
    }

    public boolean weekly() {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = now.with(DayOfWeek.SUNDAY);

        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(Date.from(startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(endOfWeek.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()));
        Optional<Execution> executionThisWeek = executionHistory.findAll(query).findAny();
        return !executionThisWeek.isPresent();
    }

    public boolean weeklyBetweenTime(String timeRange) {
        String[] times = timeRange.split("-");
        LocalTime startTime = LocalTime.parse(times[0]);
        LocalTime endTime = LocalTime.parse(times[1]);

        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = now.with(DayOfWeek.SUNDAY);

        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(Date.from(startOfWeek.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(endOfWeek.atTime(endTime).atZone(ZoneId.systemDefault()).toInstant()));
        Optional<Execution> executionInTimeRange = executionHistory.findAll(query).findAny();
        return !executionInTimeRange.isPresent();
    }

    public boolean monthly() {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(Date.from(LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()));
        Optional<Execution> executionThisMonth = executionHistory.findAll(query).findAny();
        return !executionThisMonth.isPresent();
    }

    public boolean monthlyBetweenTime(String timeRange) {
        String[] times = timeRange.split("-");
        LocalTime startTime = LocalTime.parse(times[0]);
        LocalTime endTime = LocalTime.parse(times[1]);

        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(Date.from(LocalDate.now().withDayOfMonth(1).atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(endTime).atZone(ZoneId.systemDefault()).toInstant()));
        Optional<Execution> executionInTimeRange = executionHistory.findAll(query).findAny();
        return !executionInTimeRange.isPresent();
    }

    public boolean yearly() {
        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withDayOfYear(1);
        LocalDate endOfYear = now.withDayOfYear(now.lengthOfYear());

        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(Date.from(startOfYear.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(endOfYear.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()));
        Optional<Execution> executionThisYear = executionHistory.findAll(query).findAny();
        return !executionThisYear.isPresent();
    }

    public boolean yearlyBetweenTime(String timeRange) {
        String[] times = timeRange.split("-");
        LocalTime startTime = LocalTime.parse(times[0]);
        LocalTime endTime = LocalTime.parse(times[1]);

        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withDayOfYear(1);
        LocalDate endOfYear = now.withDayOfYear(now.lengthOfYear());

        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(Date.from(startOfYear.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(endOfYear.atTime(endTime).atZone(ZoneId.systemDefault()).toInstant()));
        Optional<Execution> executionInTimeRange = executionHistory.findAll(query).findAny();
        return !executionInTimeRange.isPresent();
    }
}
