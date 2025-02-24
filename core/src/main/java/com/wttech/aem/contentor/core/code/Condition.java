package com.wttech.aem.contentor.core.code;

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
}
