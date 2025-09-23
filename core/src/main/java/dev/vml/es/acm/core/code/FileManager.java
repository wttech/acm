package dev.vml.es.acm.core.code;

import dev.vml.es.acm.core.AcmConstants;
import dev.vml.es.acm.core.AcmException;
import dev.vml.es.acm.core.repo.Repo;
import dev.vml.es.acm.core.repo.RepoResource;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;

public class FileManager {

    private Repo repo;

    private RepoResource root;

    public FileManager(ResourceResolver resolver) {
        this.repo = Repo.quiet(resolver);
        this.root = repo.get(AcmConstants.VAR_ROOT + "/file");
    }

    public Optional<String> find(String path) {
        RepoResource resource = findResource(path);
        return resource != null ? Optional.of(resource.getPath()) : Optional.empty();
    }

    public RepoResource findResource(String path) {
        RepoResource relative = root.child(path);
        if (relative.exists()) {
            return relative;
        }
        if (StringUtils.startsWith(path, root.getPath() + "/")) {
            RepoResource absolute = repo.get(path);
            if (absolute.exists()) {
                return absolute;
            }
        }
        return null;
    }

    public String save(String fileName, InputStream stream) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String formattedDate = now.format(formatter);
        RepoResource fileResource = root.child(
                String.format("%s/%s/%s", formattedDate, UUID.randomUUID().toString(), fileName));
        fileResource.parent().ensureRegularFolder();
        fileResource.saveFile("application/octet-stream", stream); // TODO tika?
        return fileResource.getPath();
    }

    public List<String> deleteAll(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return Collections.emptyList();
        }
        return paths.stream().map(this::delete).collect(Collectors.toList());
    }

    public String delete(String path) {
        RepoResource resource = findResource(path);
        if (resource == null) {
            throw new AcmException(String.format("File to be deleted does not exist '%s'!", path));
        }
        resource.delete();
        return resource.getPath();
    }
}
