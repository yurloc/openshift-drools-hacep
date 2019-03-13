/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.u212.endpoint.bootstrap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.kie.u212.core.Core;
import org.kie.u212.core.WatcherFactory;
import org.kie.u212.election.KubernetesLockConfiguration;
import org.kie.u212.election.LeadershipElection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class BootstrapListener implements ServletContextListener {

  private Logger logger = LoggerFactory.getLogger(BootstrapListener.class);

  public void contextInitialized(ServletContextEvent event) {
    initServices();
  }

  private void initServices() {
    KubernetesLockConfiguration configuration = Core.getKubernetesLockConfiguration();
    logger.info("ServletContextInitialized on pod:{}", configuration.getPodName());
    KubernetesClient client = Core.getKubeClient();
    //@TODO configure from env the namespace
    client.events().inNamespace("my-kafka-project").watch(WatcherFactory.createModifiedLogWatcher(configuration.getPodName()));
    LeadershipElection leadership = Core.getLeadershipElection();
    try {
      leadership.start();
    } catch (Exception e) {
      logger.error(e.getMessage(),
                   e);
    }
    logger.info("Core system started");
  }

  public void contextDestroyed(ServletContextEvent event) {

    LeadershipElection leadership = Core.getLeadershipElection();
    try {
      leadership.stop();
    } catch (Exception e) {
      logger.error(e.getMessage(),
                   e);
    }
    logger.info("Core system closed");
  }
}