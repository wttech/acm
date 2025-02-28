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

    // Time period-based

    public boolean everyMinute() {
        LocalTime now = LocalTime.now();
        LocalTime startTime = now.withSecond(0).withNano(0);
        LocalTime endTime = now.withSecond(59).withNano(999999999);
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !isExecutionInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean everyMinuteInSecondRange(int startSecond, int endSecond) {
        LocalTime now = LocalTime.now();
        LocalTime startTime = now.withSecond(startSecond).withNano(0);
        LocalTime endTime = now.withSecond(endSecond).withNano(999999999);
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !isExecutionInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean everyHour() {
        LocalTime now = LocalTime.now();
        LocalTime startTime = now.withMinute(0).withSecond(0).withNano(0);
        LocalTime endTime = now.withMinute(59).withSecond(59).withNano(999999999);
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !isExecutionInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean everyHourInMinuteRange(int startMinute, int endMinute) {
        LocalTime now = LocalTime.now();
        LocalTime startTime = now.withMinute(startMinute).withSecond(0).withNano(0);
        LocalTime endTime = now.withMinute(endMinute).withSecond(59).withNano(999999999);
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !isExecutionInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean everyDay() {
        return everyDayInTimeRange(dayStartTime(), dayEndTime());
    }

    public boolean everyDayInTimeRange(LocalTime startTime, LocalTime endTime) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !isExecutionInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean everyDayInTimeRange(String startTime, String endTime) {
        return everyDayInTimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean everyWeek() {
        return everyWeekInTimeRange(dayStartTime(), dayEndTime());
    }

    public boolean everyWeekInTimeRange(LocalTime startTime, LocalTime endTime) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);
        return !isExecutionInTimeRange(startOfWeek, startTime, endOfWeek, endTime);
    }

    public boolean everyWeekInTimeRange(String startTime, String endTime) {
        return everyWeekInTimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean everyMonth() {
        return everyMonthInTimeRange(dayStartTime(), dayEndTime());
    }

    public boolean everyMonthInTimeRange(LocalTime startTime, LocalTime endTime) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        return !isExecutionInTimeRange(startOfMonth, startTime, endOfMonth, endTime);
    }

    public boolean everyMonthInTimeRange(String startTime, String endTime) {
        return everyMonthInTimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean everyYear() {
        return everyYearInTimeRange(dayStartTime(), dayEndTime());
    }

    public boolean everyYearInTimeRange(LocalTime startTime, LocalTime endTime) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate startOfYear = today.withDayOfYear(1);
        LocalDate endOfYear = today.withDayOfYear(today.lengthOfYear());
        return !isExecutionInTimeRange(startOfYear, startTime, endOfYear, endTime);
    }

    public boolean everyYearInTimeRange(String startTime, String endTime) {
        return everyYearInTimeRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
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

    // Current time-based

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

    // Duration-based since last execution

    public Execution passedExecution() {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        return executionHistory.findAll(query).findFirst().orElse(null);
    }

    public Duration passed() {
        Execution lastExecution = passedExecution();
        if (lastExecution == null) {
            return null;
        }
        return Duration.between(lastExecution.getEndDate().toInstant(), Instant.now());
    }

    public boolean passedSeconds(long seconds) {
        Duration duration = passed();
        return duration == null || duration.getSeconds() >= seconds;
    }

    public boolean passedMinutes(long minutes) {
        Duration duration = passed();
        return duration == null || duration.toMinutes() >= minutes;
    }

    public boolean passedHours(long hours) {
        Duration duration = passed();
        return duration == null || duration.toHours() >= hours;
    }

    public boolean passedDays(long days) {
        Duration duration = passed();
        return duration == null || duration.toDays() >= days;
    }

    // Instance-based

    public boolean isInstanceAuthor() {
        return executionContext.getOsgiContext().getInstanceManager().isAuthor();
    }

    public boolean isInstancePublish() {
        return executionContext.getOsgiContext().getInstanceManager().isPublish();
    }
}
