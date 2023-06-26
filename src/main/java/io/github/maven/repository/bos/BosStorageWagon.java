
package io.github.maven.repository.bos;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.maven.wagon.PathUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.resource.Resource;

@Setter
@Getter
@Slf4j
public class BosStorageWagon extends AbstractStorageWagon {

    private BosStorageRepo bosStorageRepo;
    private final KeyResolver keyResolver = new KeyResolver();

    @Override
    public void get(String resourceName, File file) throws TransferFailedException, ResourceDoesNotExistException {
        Resource resource = new Resource(resourceName);
        listenerContainer.fireTransferInitiated(resource, TransferEvent.REQUEST_GET);
        listenerContainer.fireTransferStarted(resource, TransferEvent.REQUEST_GET, file);

        final Progress progress = new ProgressImpl(resource, TransferEvent.REQUEST_GET, listenerContainer);

        try {
            bosStorageRepo.copy(resourceName, file, progress);
            listenerContainer.fireTransferCompleted(resource, TransferEvent.REQUEST_GET);
        } catch (Exception e) {
            listenerContainer.fireTransferError(resource, TransferEvent.REQUEST_GET, e);
            throw e;
        }
    }


    @Override
    public List<String> getFileList(String s) throws TransferFailedException {
        try {
            List<String> list = bosStorageRepo.list(s);
            list = convertBosListToMavenFileList(list, s);
            if (list.isEmpty()) {
                throw new ResourceDoesNotExistException(s);
            }
            return list;
        } catch (Exception e) {
            throw new TransferFailedException("Could not fetch objects with prefix: " + s);
        }
    }

    @Override
    public void put(File file, String resourceName) throws TransferFailedException {
        Resource resource = new Resource(resourceName);
        log.info("Uploading file {} to {}", file.getAbsolutePath(), resourceName);
        listenerContainer.fireTransferInitiated(resource, TransferEvent.REQUEST_PUT);
        listenerContainer.fireTransferStarted(resource, TransferEvent.REQUEST_PUT, file);
        final Progress progress = new ProgressImpl(resource, TransferEvent.REQUEST_PUT, listenerContainer);
        try {
            bosStorageRepo.put(file, resourceName, progress);
            listenerContainer.fireTransferCompleted(resource, TransferEvent.REQUEST_PUT);
        } catch (TransferFailedException e) {
            listenerContainer.fireTransferError(resource, TransferEvent.REQUEST_PUT, e);
            throw e;
        }
    }

    @Override
    public boolean getIfNewer(String resourceName, File file, long timeStamp) throws TransferFailedException, ResourceDoesNotExistException {

        if (bosStorageRepo.newResourceAvailable(resourceName, timeStamp)) {
            get(resourceName, file);
            return true;
        }

        return false;
    }

    @Override
    public void putDirectory(File source, String destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Collection<File> allFiles = FileUtils.listFiles(source, null, true);
        String relativeDestination = destination;
        // the initial deleting.
        if (destination != null && destination.startsWith(".")) {
            relativeDestination = destination.length() == 1 ? "" : destination.substring(1);
        }
        for (File file : allFiles) {
            String relativePath = PathUtils.toRelative(source, file.getAbsolutePath());
            put(file, relativeDestination + "/" + relativePath);
        }
    }

    @Override
    public boolean resourceExists(String resourceName) {
        return bosStorageRepo.exists(resourceName);
    }

    private List<String> convertBosListToMavenFileList(List<String> list, String path) {
        String prefix = keyResolver.resolve(bosStorageRepo.getBaseDirectory(), path);
        Set<String> folders = new HashSet<>();
        List<String> result = list.stream().map(key -> {
            String filePath = key;
            // deleting the prefix from the object path
            if (prefix != null && prefix.length() > 0)
                filePath = key.substring(prefix.length() + 1);

            extractFolders(folders, filePath);

            return filePath;
        }).collect(Collectors.toList());
        result.addAll(folders);
        return result;
    }

    private void extractFolders(Set<String> folders, String filePath) {
        if (filePath.contains("/")) {
            String folder = filePath.substring(0, filePath.lastIndexOf('/'));
            folders.add(folder + '/');
            if (folder.contains("/")) {
                extractFolders(folders, folder);
            }
        } else {
            folders.add(filePath);
        }
    }

    @Override
    public void connect(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider) throws AuthenticationException {
        this.repository = repository;
        this.sessionListenerContainer.fireSessionOpening();

        String[] strings = repository.getBasedir().split("/");
        String bucket = strings[1];
        String directory = "/" + strings[2];
        log.info("Opening connection for bucket {} and directory {}", bucket, directory);
        bosStorageRepo = new BosStorageRepo(bucket, directory);
        bosStorageRepo.connect(authenticationInfo, repository.getHost());

        sessionListenerContainer.fireSessionLoggedIn();
        sessionListenerContainer.fireSessionOpened();
    }


    @Override
    public void disconnect() {
        sessionListenerContainer.fireSessionDisconnecting();
        bosStorageRepo.disconnect();
        sessionListenerContainer.fireSessionLoggedOff();
        sessionListenerContainer.fireSessionDisconnected();
    }

}
