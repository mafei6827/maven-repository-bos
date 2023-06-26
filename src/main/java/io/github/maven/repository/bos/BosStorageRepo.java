
package io.github.maven.repository.bos;

import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.model.BosObject;
import com.baidubce.services.bos.model.ListObjectsRequest;
import com.baidubce.services.bos.model.ListObjectsResponse;
import com.baidubce.services.bos.model.ObjectMetadata;
import com.baidubce.services.bos.model.PutObjectRequest;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

@Slf4j
public class BosStorageRepo {

    @Getter
    private final String bucket;
    @Getter
    private final String baseDirectory;

    private final KeyResolver keyResolver = new KeyResolver();

    private BosClient bosClient;

    public BosStorageRepo(String bucket, String baseDirectory) {
        this.bucket = bucket;
        this.baseDirectory = baseDirectory;
    }


    public void connect(AuthenticationInfo authenticationInfo, String endpoint) throws AuthenticationException {
        this.bosClient = BosConnect.connect(authenticationInfo, endpoint);
    }

    public void copy(String resourceName, File destination, Progress progress) throws TransferFailedException, ResourceDoesNotExistException {

        final String key = resolveKey(resourceName);

        try {
            final BosObject bosObject = bosClient.getObject(bucket, key);
            //make sure the folder exists or the outputStream will fail.
            destination.getParentFile().mkdirs();
            try (OutputStream outputStream = new ProgressFileOutputStream(destination, progress);
                 InputStream inputStream = bosObject.getObjectContent()) {
                IOUtils.copy(inputStream, outputStream);
            }
        } catch (Exception e) {
            log.warn("Could not transfer file from [bucket={}, baseDirectory={}, key={}]", bucket, baseDirectory, key, e);
        }
    }


    public void put(File file, String destination, Progress progress) throws TransferFailedException {

        final String key = resolveKey(destination);

        try {
            try (InputStream inputStream = new ProgressFileInputStream(file, progress)) {
                PutObjectRequest
                    putObjectRequest = new PutObjectRequest(bucket, key, inputStream, createContentLengthMetadata(file));
                bosClient.putObject(putObjectRequest);
            }
        } catch (Exception e) {
            log.warn("Could not transfer file to [bucket={}, baseDirectory={}, key={}]", bucket, baseDirectory, key, e);
            throw new TransferFailedException("Could not transfer file " + file.getName());
        }
    }

    private ObjectMetadata createContentLengthMetadata(File file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.length());
        return metadata;
    }

    public boolean newResourceAvailable(String resourceName, long timeStamp) throws ResourceDoesNotExistException {
        final String key = resolveKey(resourceName);
        log.info("Checking if new key exists, [bucket={}, baseDirectory={}, key={}]", bucket, baseDirectory, key);
        try {
            ObjectMetadata objectMetadata = bosClient.getObjectMetadata(bucket, key);
            long updated = objectMetadata.getLastModified().getTime();
            return updated > timeStamp;
        } catch (Exception e) {
            log.warn("Could not find key [bucket={}, baseDirectory={}, key={}]", bucket, baseDirectory, key, e);
            throw new ResourceDoesNotExistException("Could not find key " + key);
        }
    }

    public List<String> list(String path) {
        String key = resolveKey(path);
        ListObjectsResponse listObjectsResponse = bosClient.listObjects(new ListObjectsRequest(bucket).withPrefix(key));
        List<String> objects = new ArrayList<>();
        retrieveAllObjects(listObjectsResponse, objects);
        return objects;
    }

    private void retrieveAllObjects(ListObjectsResponse listObjectsResponse, List<String> objects) {
        listObjectsResponse.getContents().forEach(os -> objects.add(os.getKey()));

        if (listObjectsResponse.isTruncated()) {
            ListObjectsResponse nextBatchOfObjects = bosClient.listNextBatchOfObjects(listObjectsResponse);
            retrieveAllObjects(nextBatchOfObjects, objects);
        }
    }

    public boolean exists(String resourceName) {
        final String key = resolveKey(resourceName);
        try {
            bosClient.getObjectMetadata(bucket, key);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void disconnect() {
        if (bosClient != null) {
            bosClient.shutdown();
        }
    }

    private String resolveKey(String path) {
        return keyResolver.resolve(baseDirectory, path);
    }


}
