package com.wttech.aem.contentor.core.assist;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Assistance implements Serializable {

    private final String word;

    private final List<Suggestion> suggestions;

    public Assistance(String code, List<Suggestion> suggestions) {
        this.word = code;
        this.suggestions = suggestions;
    }

    public String getWord() {
        return word;
    }

    public List<Suggestion> getSuggestions() {
        return suggestions;
    }

    public static Assistance mock(String code) {
        List<Suggestion> suggestions = new LinkedList<>();

        suggestions.add(new Suggestion("class", "com.day.cq.wcm.api.PageManager"));
        suggestions.add(new Suggestion("class", "com.day.cq.wcm.api.Page"));
        suggestions.add(new Suggestion("class", "org.apache.sling.api.resource.ResourceResolver"));
        suggestions.add(new Suggestion("class", "org.apache.sling.api.resource.Resource"));
        suggestions.add(new Suggestion("class", "com.day.cq.tagging.Tag"));
        suggestions.add(new Suggestion("class", "com.day.cq.dam.api.Asset"));
        suggestions.add(new Suggestion("class", "com.adobe.granite.workflow.WorkflowSession"));
        suggestions.add(new Suggestion("class", "com.adobe.granite.workflow.exec.WorkItem"));
        suggestions.add(new Suggestion("class", "com.adobe.granite.workflow.exec.WorkflowData"));
        suggestions.add(new Suggestion("class", "com.adobe.granite.workflow.model.WorkflowModel"));
        suggestions.add(new Suggestion("class", "com.adobe.granite.workflow.exec.WorkflowProcess"));
        suggestions.add(new Suggestion("class", "com.day.cq.replication.Replicator"));
        suggestions.add(new Suggestion("class", "com.day.cq.replication.ReplicationAction"));
        suggestions.add(new Suggestion("class", "com.day.cq.replication.ReplicationStatus"));
        suggestions.add(new Suggestion("class", "com.day.cq.search.QueryBuilder"));
        suggestions.add(new Suggestion("class", "com.day.cq.search.Query"));
        suggestions.add(new Suggestion("class", "com.day.cq.search.result.SearchResult"));
        suggestions.add(new Suggestion("class", "com.day.cq.search.result.Hit"));
        suggestions.add(new Suggestion("class", "com.day.cq.dam.api.DamConstants"));
        suggestions.add(new Suggestion("class", "com.day.cq.dam.api.AssetManager"));
        suggestions.add(new Suggestion("class", "com.day.cq.dam.api.Rendition"));
        suggestions.add(new Suggestion("class", "com.day.cq.dam.api.handler.AssetHandler"));

        suggestions.add(new Suggestion("text", "com.day.cq.wcm.cq-wcm-api", "Symbolic Name: Day Communique 5 WCM API"));
        suggestions.add(new Suggestion("text", "org.apache.sling.api.resource.ResourceResolverFactory", "Service PID: Apache Sling Resource Resolver Factory"));

        return new Assistance(code, suggestions);
    }
}
