package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.osgi.InstanceInfo;
import dev.vml.es.acm.core.osgi.InstanceType;
import dev.vml.es.acm.core.script.ScriptRepository;
import dev.vml.es.acm.core.script.ScriptType;
import java.time.*;
import java.util.Date;
import java.util.Iterator;
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

    public boolean notSucceeded() {
        Execution passedExecution = passedExecution();
        return passedExecution == null || passedExecution.getStatus() != ExecutionStatus.SUCCEEDED;
    }

    public boolean isChangedExecutableContent(Execution execution) {
        return !StringUtils.equals(
                execution.getExecutable().getContent(),
                executionContext.getExecutable().getContent());
    }

    public boolean executedInRange(LocalDate date, LocalTime startTime, LocalTime endTime) {
        return executedInRange(date, startTime, date, endTime);
    }

    public boolean executedInRange(LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime) {
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

    public boolean executedInRange(String startDateTime, String endDateTime) {
        return executedInRange(LocalDateTime.parse(startDateTime), LocalDateTime.parse(endDateTime));
    }

    public boolean executedInRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return passedExecutionsInRange(startDateTime, endDateTime).findAny().isPresent();
    }

    public Stream<Execution> passedExecutionsInRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        checkStartAndEndDateTime(startDateTime, endDateTime);
        ExecutionQuery query = new ExecutionQuery();
        query.setExecutableId(executionContext.getExecutable().getId());
        query.setStartDate(
                Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        query.setEndDate(Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant()));
        return executionHistory.findAll(query);
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

    // Lock-based

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

    // Executable-based

    public boolean isConsole() {
        return Executable.ID_CONSOLE.equals(executableId());
    }

    public boolean isAutomaticScript() {
        return StringUtils.startsWith(executableId(), ScriptType.AUTOMATIC.root() + "/");
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
