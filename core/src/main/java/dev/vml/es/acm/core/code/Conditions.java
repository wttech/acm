package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.InstanceType;
import dev.vml.es.acm.core.script.ScriptRepository;
import dev.vml.es.acm.core.script.ScriptScheduler;
import dev.vml.es.acm.core.script.ScriptType;
import dev.vml.es.acm.core.util.DateUtils;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
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
        return noneRunning();
    }

    public boolean idleSelf() {
        return notRunning();
    }

    public boolean notRunning() {
        if (executionContext.getExecutor().isLocking()) {
            return unlockedSelf();
        }
        return noneRunning(queuedSelfExecutions());
    }

    public boolean noneRunning() {
        if (executionContext.getExecutor().isLocking()) {
            return unlockedAll();
        }
        return noneRunning(queuedExecutions());
    }

    private boolean noneRunning(Stream<Execution> queuedExecutions) {
        return queuedExecutions.noneMatch(e -> e.getStatus() == ExecutionStatus.RUNNING);
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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = localDateTime;
        LocalDateTime to = from.plus(getScriptScheduler().getIntervalMillis(), ChronoUnit.MILLIS);
        return !now.isBefore(from) && !now.isAfter(to);
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

    public boolean everyMinuteStart() {
        return everyMinuteAt(0);
    }

    public boolean everyMinuteAt(int second) {
        LocalTime now = LocalTime.now();
        LocalDateTime scheduledDateTime =
                LocalDate.now().atTime(now.withSecond(second).withNano(0));
        return isDate(scheduledDateTime);
    }

    public boolean everyHourStart() {
        return everyHourAt(0);
    }

    public boolean everyHourAt(int minute) {
        LocalTime now = LocalTime.now();
        LocalDateTime scheduledDateTime =
                LocalDate.now().atTime(now.withMinute(minute).withSecond(0).withNano(0));
        return isDate(scheduledDateTime);
    }

    public boolean everyDayStart() {
        return everyDayAt(LocalTime.MIDNIGHT);
    }

    public boolean everyDayAt(LocalTime time) {
        LocalDateTime scheduledDateTime = LocalDate.now().atTime(time);
        return isDate(scheduledDateTime);
    }

    public boolean everyDayAt(String time) {
        return everyDayAt(parseTime(time));
    }

    public boolean everyWeekStart() {
        return everyWeekAt(DayOfWeek.MONDAY, LocalTime.MIDNIGHT);
    }

    public boolean everyWeekAt(DayOfWeek dayOfWeek, LocalTime time) {
        LocalDate startOfWeek = LocalDate.now().with(dayOfWeek);
        LocalDateTime scheduledDateTime = startOfWeek.atTime(time);
        return isDate(scheduledDateTime);
    }

    public boolean everyWeekAt(String dayOfWeek, String time) {
        return everyWeekAt(parseDayOfWeek(dayOfWeek), parseTime(time));
    }

    public boolean everyMonthStart() {
        return everyMonthAt(1, LocalTime.MIDNIGHT);
    }

    public boolean everyMonthAt(int dayOfMonth) {
        return everyMonthAt(dayOfMonth, LocalTime.MIDNIGHT);
    }

    public boolean everyMonthAt(int dayOfMonth, LocalTime time) {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(dayOfMonth);
        LocalDateTime scheduledDateTime = startOfMonth.atTime(time);
        return isDate(scheduledDateTime);
    }

    public boolean everyMonthAt(int dayOfMonth, String time) {
        return everyMonthAt(dayOfMonth, parseTime(time));
    }

    public boolean everyYearStart() {
        return everyYearAt(Month.JANUARY, 1, LocalTime.MIDNIGHT);
    }

    public boolean everyYearAt(Month month, int dayOfMonth, LocalTime time) {
        LocalDate targetDay = LocalDate.now().withMonth(month.getValue()).withDayOfMonth(dayOfMonth);
        LocalDateTime scheduledDateTime = targetDay.atTime(time);
        return isDate(scheduledDateTime);
    }

    public boolean everyYearAt(String month, int dayOfMonth, String time) {
        return everyYearAt(parseMonth(month), dayOfMonth, parseTime(time));
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

    public boolean everyMinuteInRange() {
        LocalTime now = LocalTime.now();
        LocalTime startTime = now.withSecond(0).withNano(0);
        LocalTime endTime = now.withSecond(59).withNano(999999999);
        checkStartAndEndTime(startTime, endTime);
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !executedInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean everyMinuteInRange(int startSecond, int endSecond) {
        LocalTime now = LocalTime.now();
        LocalTime startTime = now.withSecond(startSecond).withNano(0);
        LocalTime endTime = now.withSecond(endSecond).withNano(999999999);
        checkStartAndEndTime(startTime, endTime);
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !executedInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean everyHourInRange() {
        LocalTime now = LocalTime.now();
        LocalTime startTime = now.withMinute(0).withSecond(0).withNano(0);
        LocalTime endTime = now.withMinute(59).withSecond(59).withNano(999999999);
        checkStartAndEndTime(startTime, endTime);
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !executedInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean everyHourInRange(int startMinute, int endMinute) {
        LocalTime now = LocalTime.now();
        LocalTime startTime = now.withMinute(startMinute).withSecond(0).withNano(0);
        LocalTime endTime = now.withMinute(endMinute).withSecond(59).withNano(999999999);
        checkStartAndEndTime(startTime, endTime);
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !executedInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean everyDayInRange() {
        return everyDayInRange(dayStartTime(), dayEndTime());
    }

    public boolean everyDayInRange(LocalTime startTime, LocalTime endTime) {
        checkStartAndEndTime(startTime, endTime);
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        return !executedInTimeRange(LocalDate.now(), startTime, endTime);
    }

    public boolean everyDayInRange(String startTime, String endTime) {
        return everyDayInRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean everyWeekInRange() {
        return everyWeekInRange(dayStartTime(), dayEndTime());
    }

    public boolean everyWeekInRange(LocalTime startTime, LocalTime endTime) {
        checkStartAndEndTime(startTime, endTime);
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);
        return !executedInTimeRange(startOfWeek, startTime, endOfWeek, endTime);
    }

    public boolean everyWeekInRange(String startTime, String endTime) {
        return everyWeekInRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean everyMonthInRange() {
        return everyMonthInRange(dayStartTime(), dayEndTime());
    }

    public boolean everyMonthInRange(LocalTime startTime, LocalTime endTime) {
        checkStartAndEndTime(startTime, endTime);
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        return !executedInTimeRange(startOfMonth, startTime, endOfMonth, endTime);
    }

    public boolean everyMonthInRange(String startTime, String endTime) {
        return everyMonthInRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean everyYearInRange() {
        return everyYearInRange(dayStartTime(), dayEndTime());
    }

    public boolean everyYearInRange(LocalTime startTime, LocalTime endTime) {
        checkStartAndEndTime(startTime, endTime);
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate startOfYear = today.withDayOfYear(1);
        LocalDate endOfYear = today.withDayOfYear(today.lengthOfYear());
        return !executedInTimeRange(startOfYear, startTime, endOfYear, endTime);
    }

    public boolean everyYearInRange(String startTime, String endTime) {
        return everyYearInRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
    }

    public boolean executedInTimeRange(LocalDate date, LocalTime startTime, LocalTime endTime) {
        return executedInTimeRange(date, startTime, date, endTime);
    }

    public boolean executedInTimeRange(LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime) {
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

    public boolean executedInTimeRange(String startDateTime, String endDateTime) {
        return executedInTimeRange(LocalDateTime.parse(startDateTime), LocalDateTime.parse(endDateTime));
    }

    public boolean executedInTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return passedExecutionsInTimeRange(startDateTime, endDateTime).findAny().isPresent();
    }

    public Stream<Execution> passedExecutionsInTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        checkStartAndEndDateTime(startDateTime, endDateTime);
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(
                Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        return executionHistory.findAll(query);
    }

    private void checkStartAndEndTime(LocalTime start, LocalTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    String.format("Start time '%s' must be before end time '%s'!", start, end));
        }
    }

    private void checkStartAndEndDateTime(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    String.format("Start date-time '%s' must be before end date-time '%s'!", start, end));
        }
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

    public boolean unlockedSelf() {
        return !lockedSelf();
    }

    public boolean lockedSelf() {
        return locked(executionContext.getExecutable().getId());
    }

    public boolean unlocked(String name) {
        return !locked(name);
    }

    public boolean locked(String name) {
        requireLocking();
        return executionContext.getCodeContext().getLocker().isLocked(name);
    }

    public boolean unlockedAll() {
        requireLocking();
        return !executionContext.getCodeContext().getLocker().anyLocked();
    }

    private void requireLocking() {
        if (!executionContext.getExecutor().isLocking()) {
            throw new IllegalStateException("Executor locking is disabled, so cannot check if lock exists!");
        }
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

    // Executable-based

    public boolean isConsole() {
        return Executable.ID_CONSOLE.equals(executableId());
    }

    public boolean isAutomaticScript() {
        return StringUtils.startsWith(executableId(), ScriptType.ENABLED.root() + "/");
    }

    public boolean isManualScript() {
        return StringUtils.startsWith(executableId(), ScriptType.MANUAL.root() + "/");
    }

    public boolean isScript() {
        return StringUtils.startsWith(executableId(), ScriptRepository.ROOT + "/");
    }

    public boolean isExecutable(String idPattern) {
        return FilenameUtils.wildcardMatch(executableId(), idPattern);
    }

    public String executableId() {
        return executionContext.getExecutable().getId();
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
