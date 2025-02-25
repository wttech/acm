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
        return !isAnyExecutionForExecutable();
    }

    public boolean changed() {
        return !isAnyExecutionForExecutableAndContent();
    }

    public boolean isAnyExecutionForExecutable() {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        return executionHistory.findAll(query).findAny().isPresent();
    }

    public boolean isAnyExecutionForExecutableAndContent() {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        return executionHistory
                .findAll(query)
                .anyMatch(e -> StringUtils.equals(
                        e.getExecutable().getContent(),
                        executionContext.getExecutable().getContent()));
    }

    public boolean hourly() {
        LocalTime startTime = LocalTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalTime endTime = LocalTime.now().withMinute(59).withSecond(59).withNano(999999999);
        return isExecutionInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean hourlyBetweenMinutes(int startMinute, int endMinute) {
        LocalTime startTime = LocalTime.now().withMinute(startMinute).withSecond(0).withNano(0);
        LocalTime endTime = LocalTime.now().withMinute(endMinute).withSecond(59).withNano(999999999);
        return isExecutionInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean dailyBetweenTime(LocalTime startTime, LocalTime endTime) {
        return isExecutionInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean dailyBetweenTime(String startTime, String endTime) {
        return dailyBetweenTime(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean weeklyBetweenTime(LocalTime startTime, LocalTime endTime) {
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = now.with(DayOfWeek.SUNDAY);
        return isExecutionInTimeRange(startOfWeek, endOfWeek, startTime, endTime);
    }

    public boolean weeklyBetweenTime(String startTime, String endTime) {
        return weeklyBetweenTime(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean monthlyBetweenTime(LocalTime startTime, LocalTime endTime) {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        return isExecutionInTimeRange(startOfMonth, endOfMonth, startTime, endTime);
    }

    public boolean monthlyBetweenTime(String startTime, String endTime) {
        return monthlyBetweenTime(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean yearlyBetweenTime(LocalTime startTime, LocalTime endTime) {
        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withDayOfYear(1);
        LocalDate endOfYear = now.withDayOfYear(now.lengthOfYear());
        return isExecutionInTimeRange(startOfYear, endOfYear, startTime, endTime);
    }

    public boolean yearlyBetweenTime(String startTime, String endTime) {
        return yearlyBetweenTime(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    private boolean isExecutionInTimeRange(LocalDate date, LocalTime startTime, LocalTime endTime) {
        return isExecutionInTimeRange(date, date, startTime, endTime);
    }

    private boolean isExecutionInTimeRange(LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime) {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(Date.from(startDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(endDate.atTime(endTime).atZone(ZoneId.systemDefault()).toInstant()));
        Optional<Execution> executionInTimeRange = executionHistory.findAll(query).findAny();
        return !executionInTimeRange.isPresent();
    }

    public boolean isWeekend() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    public boolean isWeekday() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
}