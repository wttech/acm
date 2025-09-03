package dev.vml.es.acm.core.notification;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import dev.vml.es.acm.core.notification.slack.Slack;
import dev.vml.es.acm.core.notification.slack.SlackFactory;
import dev.vml.es.acm.core.notification.slack.SlackPayload;
import dev.vml.es.acm.core.notification.teams.Teams;
import dev.vml.es.acm.core.notification.teams.TeamsFactory;
import dev.vml.es.acm.core.notification.teams.TeamsPayload;

@Component(service = NotifierManager.class, immediate = true)
public class NotifierManager {

        @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            service = TeamsFactory.class)
    private final Collection<TeamsFactory> teamsFactories = new CopyOnWriteArrayList<>();

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            service = SlackFactory.class)
    private final Collection<SlackFactory> slackFactories = new CopyOnWriteArrayList<>();

    // === Teams ===

    public TeamsPayload.Builder newTeamsPayload() {
        return new TeamsPayload.Builder();
    }

    public Stream<Teams> findTeams() {
        return teamsFactories.stream().map(t -> t.getNotifier());
    }
    
    public Optional<Teams> findTeamsById(String id) {
        return findTeams().filter(t -> StringUtils.equals(id, t.getId())).findFirst();
    }

    public Teams getTeamsById(String id) {
        return findTeamsById(id)
            .orElseThrow(() -> new NotifierException(String.format("Teams notifier with ID '%s' not found!", id)));
    }

    public Optional<Teams> findTeamsDefault() {
        Optional<Teams> defaultTeams = findTeams().filter(t -> t.getId().equals(NotifierFactory.ID_DEFAULT)).findFirst();
        if (defaultTeams.isPresent()) {
            return defaultTeams;
        }
        Optional<Teams> enabledTeams = findTeams().filter(t -> t.isEnabled()).findFirst();
        if (enabledTeams.isPresent()) {
            return enabledTeams;
        }
        return Optional.empty();
    }

    public Teams getTeamsDefault() {
        return findTeamsDefault()
            .orElseThrow(() -> new NotifierException("Teams default notifier or any enabled notifier not available!"));
    }

    // ===[ Slack ]===

    public SlackPayload.Builder newSlackPayload() {
        return new SlackPayload.Builder();
    }

    public Stream<Slack> findSlack() {
        return slackFactories.stream().map(s -> s.getNotifier());
    }

    public Optional<Slack> findSlackById(String id) {
        return findSlack().filter(s -> StringUtils.equals(id, s.getId())).findFirst();
    }

    public Slack getSlackById(String id) {
        return findSlackById(id)
            .orElseThrow(() -> new NotifierException(String.format("Slack notifier with ID '%s' not found!", id)));
    }

    public Optional<Slack> findSlackDefault() {
        Optional<Slack> defaultSlack = findSlack().filter(s -> s.getId().equals(NotifierFactory.ID_DEFAULT)).findFirst();
        if (defaultSlack.isPresent()) {
            return defaultSlack;
        }
        Optional<Slack> enabledSlack = findSlack().filter(s -> s.isEnabled()).findFirst();
        if (enabledSlack.isPresent()) {
            return enabledSlack;
        }
        return Optional.empty();
    }

    public Slack getSlackDefault() {
        return findSlackDefault().orElseThrow(() -> new NotifierException("Slack default notifier or any enabled notifier not available!"));
    }
}
