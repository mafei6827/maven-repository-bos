
package io.github.maven.repository.bos;

import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.wagon.authentication.AuthenticationInfo;

@Slf4j
public class BosConnect {


    public static BosClient connect(AuthenticationInfo authenticationInfo, String endpoint) {
        BosClientConfiguration config = new BosClientConfiguration();
        config.setCredentials(new DefaultBceCredentials(authenticationInfo.getUserName(), authenticationInfo.getPassword()));
        config.setEndpoint(endpoint);
        BosClient client = new BosClient(config);
        log.info("Connected to bos {}", endpoint);
        return client;
    }
}
