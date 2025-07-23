package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.InstanceType;
import dev.vml.es.acm.core.script.ScriptScheduler;
import dev.vml.es.acm.core.util.DateUtils;
import java.time.*;
import java.util.Iterator;
import java.util.Locale;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public class Conditions {

    private final ExecutionContext executionContext;

    private final ExecutionHistory executionHistory;

    public Conditions(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.executionHistory =
                new ExecutionHistory(executionContext.getCodeContext().getResourceResolver());
    }

    public boolean always() {
        return true;
    }

    public boolean never() {
        return false;
    }

    // History or queue-based

    public boolean once() {
        return passedExecution() == null;
    }

    public boolean changed() {
        return contentChanged() || retryIfInstanceChanged();
    }

    public boolean contentChanged() {
        Execution passedExecution = passedExecution();
        return passedExecution == null || isChangedExecutableContent(passedExecution);
    }

    public Execution passedExecution() {
        return passedExecutions().findFirst().orElse(null);
    }

    public Stream<Execution> passedExecutions() {
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        return executionHistory.findAll(query);
    }

    public boolean idle() {
        return queuedExecutions().noneMatch(e -> e.getStatus() == ExecutionStatus.RUNNING);
    }

    public boolean idleSelf() {
        return queuedSelfExecutions().noneMatch(e -> e.getStatus() == ExecutionStatus.RUNNING);
    }

    public Stream<Execution> queuedExecutions() {
        return getExecutionQueue().findAll().filter(e -> !isSelfExecution(e));
    }

    public Stream<Execution> queuedSelfExecutions() {
        return getExecutionQueue().findAll().filter(e -> !isSelfExecution(e) && isSameExecutable(e));
    }

    public boolean isSelfExecution(Execution e) {
        return StringUtils.equals(e.getId(), executionContext.getId());
    }

    public boolean isSameExecutable(Execution e) {
        return StringUtils.equals(
                e.getExecutable().getId(), executionContext.getExecutable().getId());
    }

    public boolean isChangedExecutableContent(Execution execution) {
        return !StringUtils.equals(
                execution.getExecutable().getContent(),
                executionContext.getExecutable().getContent());
    }

    private ExecutionQueue getExecutionQueue() {
        return executionContext.getCodeContext().getOsgiContext().getExecutionQueue();
    }

    // Date-time-based

    public boolean isDate(String dateString) {
        ZonedDateTime localDateTime = DateUtils.zonedDateTimeFromString(dateString);
        return isDate(localDateTime);
    }

    public boolean isDate(ZonedDateTime zonedDateTime) {
        LocalDateTime localDateTime =
                zonedDateTime.withZoneSameLocal(DateUtils.ZONE_ID).toLocalDateTime();
        return isDate(localDateTime);
    }

    public boolean isDate(LocalDateTime localDateTime) {
        long intervalMillis = getScriptScheduler().getIntervalMillis();
        return DateUtils.isInRange(localDateTime, LocalDateTime.now(), intervalMillis);
    }

    private ScriptScheduler getScriptScheduler() {
        return executionContext.getCodeContext().getOsgiContext().getScriptScheduler();
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

    public boolean everyMinute() {
        return everyMinute(0);
    }

    public boolean everyMinute(int second) {
        LocalTime now = LocalTime.now();
        LocalDateTime scheduledDateTime =
                LocalDate.now().atTime(now.withSecond(second).withNano(0));
        return isDate(scheduledDateTime);
    }

    public boolean everyHour() {
        return everyHour(0);
    }

    public boolean everyHour(int minute) {
        LocalTime now = LocalTime.now();
        LocalDateTime scheduledDateTime =
                LocalDate.now().atTime(now.withMinute(minute).withSecond(0).withNano(0));
        return isDate(scheduledDateTime);
    }

    public boolean everyDay() {
        return everyDay(LocalTime.MIDNIGHT);
    }

    public boolean everyDay(LocalTime time) {
        LocalDateTime scheduledDateTime = LocalDate.now().atTime(time);
        return isDate(scheduledDateTime);
    }

    public boolean everyDay(String time) {
        return everyDay(parseTime(time));
    }

    public boolean everyWeek() {
        return everyWeek(DayOfWeek.MONDAY, LocalTime.MIDNIGHT);
    }

    public boolean everyWeek(DayOfWeek dayOfWeek, LocalTime time) {
        LocalDate startOfWeek = LocalDate.now().with(dayOfWeek);
        LocalDateTime scheduledDateTime = startOfWeek.atTime(time);
        return isDate(scheduledDateTime);
    }

    public boolean everyWeek(String dayOfWeek, String time) {
        return everyWeek(parseDayOfWeek(dayOfWeek), parseTime(time));
    }

    public boolean everyMonth() {
        return everyMonth(1, LocalTime.MIDNIGHT);
    }

    public boolean everyMonth(int dayOfMonth) {
        return everyMonth(dayOfMonth, LocalTime.MIDNIGHT);
    }

    public boolean everyMonth(int dayOfMonth, LocalTime time) {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(dayOfMonth);
        LocalDateTime scheduledDateTime = startOfMonth.atTime(time);
        return isDate(scheduledDateTime);
    }

    public boolean everyMonth(int dayOfMonth, String time) {
        return everyMonth(dayOfMonth, parseTime(time));
    }

    public boolean everyYear() {
        return everyYear(Month.JANUARY, 1, LocalTime.MIDNIGHT);
    }

    public boolean everyYear(Month month, int dayOfMonth, LocalTime time) {
        LocalDate targetDay = LocalDate.now().withMonth(month.getValue()).withDayOfMonth(dayOfMonth);
        LocalDateTime scheduledDateTime = targetDay.atTime(time);
        return isDate(scheduledDateTime);
    }

    public boolean everyYear(String month, int dayOfMonth, String time) {
        return everyYear(parseMonth(month), dayOfMonth, parseTime(time));
    }

    private LocalTime parseTime(String time) {
        return LocalTime.parse(time);
    }

    private Month parseMonth(String month) {
        return Month.valueOf(month.trim().toUpperCase(Locale.ENGLISH));
    }

    private DayOfWeek parseDayOfWeek(String dayOfWeek) {
        return DayOfWeek.valueOf(dayOfWeek.trim().toUpperCase(Locale.ENGLISH));
    }

    // Duration-based since the last execution

    public boolean passed(Duration duration) {
        checkDuration(duration);
        Duration passedDuration = passedDuration();
        return passedDuration == null || passedDuration.compareTo(duration) >= 0;
    }

    public boolean passed(long seconds) {
        return passed(Duration.ofSeconds(seconds));
    }

    public Duration passedDuration() {
        Execution execution = passedExecution();
        if (execution == null) {
            return null;
        }
        return passedDuration(execution);
    }

    public Duration passedDuration(Execution execution) {
        return Duration.between(execution.getEndDate().toInstant(), Instant.now());
    }

    private void checkDuration(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException(String.format("Duration '%s' must be a positive value!", duration));
        }
    }

    // Run-count-based

    public boolean everyNthRun(long frequency) {
        if (frequency < 1) {
            throw new IllegalArgumentException("Run frequency must be greater than zero!");
        }
        return runCount() % frequency == 0;
    }

    public long runCount() {
        return getScriptScheduler().getRunCount();
    }

    // Lock-based

    public boolean unlocked() {
        return !locked();
    }

    public boolean locked() {
        return locked(executionContext.getExecutable().getId());
    }

    public boolean unlocked(String name) {
        return !locked(name);
    }

    public boolean locked(String name) {
        return executionContext.getCodeContext().getLocker().isLocked(name);
    }

    // Instance-based

    public boolean instanceChanged() {
        Execution passedExecution = passedExecution();
        if (passedExecution == null) {
            return true;
        }
        return instanceChanged(passedExecution);
    }

    public boolean retryIfInstanceChanged() {
        Execution passedExecution = passedExecution();
        if (passedExecution == null) {
            return true;
        }
        boolean passedFailed = passedExecution.getStatus() != ExecutionStatus.SUCCEEDED;
        return passedFailed && instanceChanged(passedExecution);
    }

    public boolean instanceChanged(Execution execution) {
        String stateCurrent = executionContext.getCodeContext().getOsgiContext().readInstanceState();
        String statePassed = execution.getInstance();
        return !StringUtils.equals(stateCurrent, statePassed);
    }

    public boolean isInstanceRunMode(String runMode) {
        return getInstanceInfo().isRunMode(runMode);
    }

    private InstanceInfo getInstanceInfo() {
        return executionContext.getCodeContext().getOsgiContext().getInstanceInfo();
    }

    public boolean isInstanceAuthor() {
        return getInstanceInfo().isAuthor();
    }

    public boolean isInstancePublish() {
        return getInstanceInfo().isPublish();
    }

    public boolean isInstanceOnPrem() {
        return getInstanceInfo().getType() == InstanceType.ON_PREM;
    }

    public boolean isInstanceCloud() {
        return getInstanceInfo().getType().isCloud();
    }

    public boolean isInstanceCloudContainer() {
        return getInstanceInfo().getType() == InstanceType.CLOUD_CONTAINER;
    }

    public boolean isInstanceCloudSdk() {
        return getInstanceInfo().getType() == InstanceType.CLOUD_SDK;
    }

    // Retry-based

    public boolean retry(long count) {
        if (count < 1) {
            throw new IllegalArgumentException("Retry count must be greater than zero!");
        }
        if (!idleSelf()) {
            return false;
        }
        Execution passedExecution = passedExecution();
        if (passedExecution == null) {
            return true;
        }
        if (passedExecution.getStatus() == ExecutionStatus.SUCCEEDED) {
            return false;
        }
        long consecutiveFailures = 0;
        Iterator<Execution> it = passedExecutions().iterator();
        while (it.hasNext()) {
            Execution e = it.next();
            if (e.getStatus() == ExecutionStatus.SUCCEEDED) {
                break;
            }
            consecutiveFailures++;
        }
        return consecutiveFailures < count;
    }
}
