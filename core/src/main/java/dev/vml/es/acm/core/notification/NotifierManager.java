package dev.vml.es.acm.core.notification;

import dev.vml.es.acm.core.notification.slack.Slack;
import dev.vml.es.acm.core.notification.slack.SlackFactory;
import dev.vml.es.acm.core.notification.slack.SlackPayload;
import dev.vml.es.acm.core.notification.teams.Teams;
import dev.vml.es.acm.core.notification.teams.TeamsFactory;
import dev.vml.es.acm.core.notification.teams.TeamsPayload;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

// TODO add configurable auto-notifications tied with executor
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

    // === Multi-notifier ===

    public boolean isConfigured() {
        return hasAnyDefaultNotifier();
    }

    public void sendMessage(String text) {
        sendMessage(null, text);
    }

    public void sendMessage(String title, String text) {
        sendMessage(title, text, Collections.emptyMap());
    }

    public void sendMessage(String title, String text, Map<String, Object> fields) {
        if (!hasAnyDefaultNotifier()) {
            throw new NotifierException(
                    String.format("Notifier '%s' (Slack or Teams) is not configured!", NotifierFactory.ID_DEFAULT));
        }
        findSlackDefault().ifPresent(slack ->  slack.sendPayload(buildSlackMessage(title, text, fields).build()));
        findTeamsDefault().ifPresent(teams ->teams.sendPayload(buildTeamsMessage(title, text, fields).build()));
    }

    private boolean hasAnyDefaultNotifier() {
        return findSlackDefault().isPresent() || findTeamsDefault().isPresent();
    }

    // === Teams ===

    public boolean isTeamsConfigured() {
        return findTeamsDefault().isPresent();
    }

    public void sendTeamsMessage(String text) {
        sendMessage(null, text);
    }

    public void sendTeamsMessage(String title, String text) {
        sendMessage(title, text, Collections.emptyMap());
    }

    public void sendTeamsMessage(String title, String text, Map<String, Object> fields) {
        Teams teamsDefault = findTeamsDefault()
                .orElseThrow(() -> new NotifierException(
                        String.format("Teams notifier '%s' is not configured!", NotifierFactory.ID_DEFAULT)));
        teamsDefault.sendPayload(buildTeamsMessage(title, text, fields).build());
    }

    public Stream<Teams> findTeams() {
        return teamsFactories.stream().map(t -> t.getNotifier());
    }

    public Optional<Teams> findTeamsById(String id) {
        return findTeams().filter(t -> StringUtils.equals(id, t.getId())).findFirst();
    }

    public Teams getTeamsById(String id) {
        return findTeamsById(id)
                .orElseThrow(() -> new NotifierException(String.format("Teams notifier '%s' not found!", id)));
    }

    public Optional<Teams> findTeamsDefault() {
        return findTeams()
                .filter(t -> t.getId().equals(NotifierFactory.ID_DEFAULT))
                .findFirst();
    }

    public Teams getTeamsDefault() {
        return findTeamsDefault()
                .orElseThrow(() -> new NotifierException(
                        String.format("Teams notifier '%s' is not configured!", NotifierFactory.ID_DEFAULT)));
    }

    public TeamsPayload.Builder buildTeamsMessage(String title, String text, Map<String, Object> fields) {
        TeamsPayload.Builder payload = buildTeamsPayload();
        if (StringUtils.isNotBlank(title)) {
            payload.title(title);
        }
        if (StringUtils.isNotBlank(text)) {
            payload.text(text);
        }
        if (fields != null && !fields.isEmpty()) {
            payload.facts(fields);
        }
        return payload;
    }

    public TeamsPayload.Builder buildTeamsPayload() {
        return new TeamsPayload.Builder();
    }

    // ===[ Slack ]===

    public boolean isSlackConfigured() {
        return findSlackDefault().isPresent();
    }

    public void sendSlackMessage(String text) {
        sendMessage(null, text);
    }

    public void sendSlackMessage(String title, String text) {
        sendMessage(title, text, Collections.emptyMap());
    }

    public void sendSlackMessage(String title, String text, Map<String, Object> fields) {
        Slack slackDefault = findSlackDefault()
                .orElseThrow(() -> new NotifierException(
                        String.format("Slack notifier '%s' is not configured!", NotifierFactory.ID_DEFAULT)));
        slackDefault.sendPayload(buildSlackMessage(title, text, fields).build());
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
        return findSlack()
                .filter(s -> s.getId().equals(NotifierFactory.ID_DEFAULT))
                .findFirst();
    }

    public Slack getSlackDefault() {
        return findSlackDefault().orElseThrow(() -> new NotifierException("Slack default notifier is not configured!"));
    }

    public SlackPayload.Builder buildSlackMessage(String title, String text, Map<String, Object> fields) {
        SlackPayload.Builder payload = buildSlackPayload();
        if (StringUtils.isNotBlank(title)) {
            payload.header(title);
        }
        if (StringUtils.isNotBlank(title) && StringUtils.isNotBlank(text)) {
            payload.divider();
        }
        if (StringUtils.isNotBlank(text)) {
            payload.sectionMarkdown(text);
        }
        if (fields != null && !fields.isEmpty()) {
            payload.fieldsMarkdown(fields);
        }
        return payload;
    }

    public SlackPayload.Builder buildSlackPayload() {
        return new SlackPayload.Builder();
    }
}
