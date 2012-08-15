/*
* Copyright 2012 Midokura Europe SARL
*/
package com.midokura.midonet.cluster.services;

import javax.inject.Inject;

import com.google.common.util.concurrent.AbstractService;
import com.midokura.midolman.config.ZookeeperConfig;
import org.apache.zookeeper.CreateMode;

import com.midokura.midolman.Setup;
import com.midokura.midolman.state.Directory;

/**
 * // TODO: mtoader ! Please explain yourself.
 */
public class MidostoreSetupService extends AbstractService {

    @Inject
    Directory directory;

    @Inject
    ZookeeperConfig config;

    @Override
    protected void doStart() {
        try {
            String rootKey = config.getMidolmanRootKey();

            String currentPath = "";
            for (String part : rootKey.split("/+")) {
                if (part.trim().isEmpty())
                    continue;

                currentPath += "/" + part;
                directory.add(currentPath, null, CreateMode.PERSISTENT);
            }
            Setup.createZkDirectoryStructure(directory, rootKey);
            notifyStarted();
        } catch (Exception e) {
            this.notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        notifyStopped();
    }
}