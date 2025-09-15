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

@Component(service = NotificationManager.class, immediate = true)
public class NotificationManager {

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

    public boolean isConfigured() {
        return isConfigured(NotifierFactory.ID_DEFAULT);
    }

    public boolean isConfigured(String notifierId) {
        return isSlackConfigured(notifierId) || isTeamsConfigured(notifierId);
    }

    public void sendMessage(String text) {
        sendMessageTo(NotifierFactory.ID_DEFAULT, text);
    }

    public void sendMessage(String title, String text) {
        sendMessageTo(NotifierFactory.ID_DEFAULT, title, text);
    }

    public void sendMessage(String title, String text, Map<String, Object> fields) {
        sendMessageTo(NotifierFactory.ID_DEFAULT, title, text, fields);
    }

    public void sendMessageTo(String notifierId, String text) {
        sendMessageTo(notifierId, null, text, Collections.emptyMap());
    }

    public void sendMessageTo(String notifierId, String title, String text) {
        sendMessageTo(notifierId, title, text, Collections.emptyMap());
    }

    public void sendMessageTo(String notifierId, String title, String text, Map<String, Object> fields) {
        Optional<Slack> slackOpt = findSlackById(notifierId);
        Optional<Teams> teamsOpt = findTeamsById(notifierId);
        if (!slackOpt.isPresent() && !teamsOpt.isPresent()) {
            throw new NotificationException(
                    String.format("Notifier '%s' not configured for Slack or Teams!", notifierId));
        }
        slackOpt.ifPresent(slack -> slack.sendPayload(
                buildSlackPayload().message(title, text, fields).build()));
        teamsOpt.ifPresent(teams -> teams.sendPayload(
                buildTeamsPayload().message(title, text, fields).build()));
    }

    // === Teams ===

    public boolean isTeamsConfigured() {
        return isTeamsConfigured(NotifierFactory.ID_DEFAULT);
    }

    public boolean isTeamsConfigured(String notifierId) {
        return findTeamsById(notifierId).isPresent();
    }

    public Stream<Teams> findTeams() {
        return teamsFactories.stream().map(t -> t.getNotifier());
    }

    public Optional<Teams> findTeamsById(String id) {
        return findTeams().filter(t -> StringUtils.equals(id, t.getId())).findFirst();
    }

    public Teams getTeamsById(String id) {
        return findTeamsById(id)
                .orElseThrow(() -> new NotificationException(String.format("Teams notifier '%s' not configured!", id)));
    }

    public Optional<Teams> findTeamsDefault() {
        return findTeams()
                .filter(t -> t.getId().equals(NotifierFactory.ID_DEFAULT))
                .findFirst();
    }

    public Teams getTeamsDefault() {
        return findTeamsDefault()
                .orElseThrow(() -> new NotificationException(
                        String.format("Teams notifier '%s' not configured!", NotifierFactory.ID_DEFAULT)));
    }

    public TeamsPayload.Builder buildTeamsPayload() {
        return new TeamsPayload.Builder();
    }

    // ===[ Slack ]===

    public boolean isSlackConfigured() {
        return isSlackConfigured(NotifierFactory.ID_DEFAULT);
    }

    public boolean isSlackConfigured(String notifierId) {
        return findSlackById(notifierId).isPresent();
    }

    public Stream<Slack> findSlack() {
        return slackFactories.stream().map(s -> s.getNotifier());
    }

    public Optional<Slack> findSlackById(String id) {
        return findSlack().filter(s -> StringUtils.equals(id, s.getId())).findFirst();
    }

    public Slack getSlackById(String id) {
        return findSlackById(id)
                .orElseThrow(() -> new NotificationException(String.format("Slack notifier '%s' not configured!", id)));
    }

    public Optional<Slack> findSlackDefault() {
        return findSlack()
                .filter(s -> s.getId().equals(NotifierFactory.ID_DEFAULT))
                .findFirst();
    }

    public Slack getSlackDefault() {
        return findSlackDefault()
                .orElseThrow(() -> new NotificationException(
                        String.format("Slack notifier '%s' not configured!", NotifierFactory.ID_DEFAULT)));
    }

    public SlackPayload.Builder buildSlackPayload() {
        return new SlackPayload.Builder();
    }
}
