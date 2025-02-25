package com.wttech.aem.contentor.core.code;

import java.time.*;
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
        return !isExecutionWithId();
    }

    public boolean changed() {
        return !isExecutionWithIdAndContent();
    }

    public boolean isExecutionWithId() {
        return isExecutionWithId(executionContext.getExecutable().getId());
    }

    public boolean isExecutionWithId(String id) {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(id);
        return executionHistory.findAll(query).findAny().isPresent();
    }

    public boolean isExecutionWithIdAndContent() {
        return isExecutionWithIdAndContent(
                executionContext.getExecutable().getId(),
                executionContext.getExecutable().getContent());
    }

    public boolean isExecutionWithIdAndContent(String id, String content) {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(id);
        return executionHistory.findAll(query).anyMatch(e -> {
            return StringUtils.equals(e.getExecutable().getContent(), content);
        });
    }

    public boolean hourly() {
        LocalTime now = LocalTime.now();
        LocalTime startTime = now.withMinute(0).withSecond(0).withNano(0);
        LocalTime endTime = now.withMinute(59).withSecond(59).withNano(999999999);
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !isExecutionInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean hourlyInMinuteRange(int startMinute, int endMinute) {
        LocalTime now = LocalTime.now();
        LocalTime startTime = now.withMinute(startMinute).withSecond(0).withNano(0);
        LocalTime endTime = now.withMinute(endMinute).withSecond(59).withNano(999999999);
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !isExecutionInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean daily() {
        return dailyInTimeRange(dayStartTime(), dayEndTime());
    }

    public boolean dailyInTimeRange(LocalTime startTime, LocalTime endTime) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !isExecutionInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean dailyInTimeRange(String startTime, String endTime) {
        return dailyInTimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean weekly() {
        return weeklyInTimeRange(dayStartTime(), dayEndTime());
    }

    public boolean weeklyInTimeRange(LocalTime startTime, LocalTime endTime) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);
        return !isExecutionInTimeRange(startOfWeek, startTime, endOfWeek, endTime);
    }

    public boolean weeklyInTimeRange(String startTime, String endTime) {
        return weeklyInTimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean monthly() {
        return monthlyInTimeRange(dayStartTime(), dayEndTime());
    }

    public boolean monthlyInTimeRange(LocalTime startTime, LocalTime endTime) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        return !isExecutionInTimeRange(startOfMonth, startTime, endOfMonth, endTime);
    }

    public boolean monthlyInTimeRange(String startTime, String endTime) {
        return monthlyInTimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean yearly() {
        return yearlyInTimeRange(dayStartTime(), dayEndTime());
    }

    public boolean yearlyInTimeRange(LocalTime startTime, LocalTime endTime) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate startOfYear = today.withDayOfYear(1);
        LocalDate endOfYear = today.withDayOfYear(today.lengthOfYear());
        return !isExecutionInTimeRange(startOfYear, startTime, endOfYear, endTime);
    }

    public boolean yearlyInTimeRange(String startTime, String endTime) {
        return yearlyInTimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean isExecutionInTimeRange(LocalDate date, LocalTime startTime, LocalTime endTime) {
        return isExecutionInTimeRange(date, startTime, date, endTime);
    }

    public boolean isExecutionInTimeRange(
            LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime) {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(Date.from(
                startDate.atTime(startTime).atZone(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(
                Date.from(endDate.atTime(endTime).atZone(ZoneId.systemDefault()).toInstant()));
        Optional<Execution> executionInTimeRange =
                executionHistory.findAll(query).findAny();
        return executionInTimeRange.isPresent();
    }

    public boolean isExecutionInTimeRange(String startDateTime, String endDateTime) {
        return isExecutionInTimeRange(LocalDateTime.parse(startDateTime), LocalDateTime.parse(endDateTime));
    }

    public boolean isExecutionInTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(
                Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        Optional<Execution> executionInTimeRange =
                executionHistory.findAll(query).findAny();
        return executionInTimeRange.isPresent();
    }

    public boolean isWeekend() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    public boolean isWeekday() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    public boolean isDay(String day) {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return dayOfWeek.name().equalsIgnoreCase(day);
    }

    public boolean isDay(DayOfWeek day) {
        return LocalDate.now().getDayOfWeek() == day;
    }

    public boolean isMonday() {
        return isDay(DayOfWeek.MONDAY);
    }

    public boolean isTuesday() {
        return isDay(DayOfWeek.TUESDAY);
    }

    public boolean isWednesday() {
        return isDay(DayOfWeek.WEDNESDAY);
    }

    public boolean isThursday() {
        return isDay(DayOfWeek.THURSDAY);
    }

    public boolean isFriday() {
        return isDay(DayOfWeek.FRIDAY);
    }

    public boolean isSaturday() {
        return isDay(DayOfWeek.SATURDAY);
    }

    public boolean isSunday() {
        return isDay(DayOfWeek.SUNDAY);
    }

    public LocalTime dayStartTime() {
        return LocalTime.MIDNIGHT;
    }

    public LocalTime dayEndTime() {
        return LocalTime.of(23, 59, 59, 999999999);
    }
}
