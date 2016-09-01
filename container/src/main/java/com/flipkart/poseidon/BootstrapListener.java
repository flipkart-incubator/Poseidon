/*
 * Copyright 2015 Flipkart Internet, pvt ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.poseidon;

import com.flipkart.poseidon.metrics.Metrics;
import com.netflix.hystrix.contrib.codahalemetricspublisher.HystrixCodaHaleMetricsPublisher;
import com.netflix.hystrix.strategy.HystrixPlugins;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.trpr.platform.core.impl.event.PlatformApplicationEvent;
import org.trpr.platform.core.spi.event.PlatformEventConsumer;
import org.trpr.platform.model.event.PlatformEvent;
import org.trpr.platform.runtime.impl.event.BootstrapProgressMonitor;

import static com.flipkart.poseidon.Poseidon.STARTUP_LOGGER;
import static com.flipkart.poseidon.PoseidonContext.getBean;
import static org.trpr.platform.runtime.common.RuntimeConstants.BOOTSTRAP_START_STATE;
import static org.trpr.platform.runtime.common.RuntimeConstants.BOOTSTRAP_STOP_STATE;

@Component
public class BootstrapListener implements PlatformEventConsumer {

    public void setBootstrapProgressMonitor(BootstrapProgressMonitor bootstrapProgressMonitor) {
        bootstrapProgressMonitor.addBootstrapEventListener(this);
    }

    @Override
    public void onApplicationEvent(PlatformApplicationEvent event) {
        if (event.getSource() instanceof PlatformEvent) {
            PlatformEvent platformEvent = (PlatformEvent) event.getSource();
            if ("BootstrapMonitoredEvent".equals(platformEvent.getEventType())) {
                if (BOOTSTRAP_START_STATE.equals(platformEvent.getEventStatus())) {
                    registerHystrixPlugins();
                    STARTUP_LOGGER.info("\n************************************" +
                            "\n______ " +
                            "\n| ___ \\" +
                            "\n| |_/ /" +
                            "\n|  __/ " +
                            "\n| |    Handlers Initialized." +
                            "\n\\_|    Now starting Poseidon..." +
                            "\n************************************");
                    startPoseidon();
                } else if (BOOTSTRAP_STOP_STATE.equals(platformEvent.getEventStatus())) {
                    stopPoseidon();
                }
            }
        }
    }

    private void registerHystrixPlugins() {
        // Register hystrix codahale metrics publisher plugin
        STARTUP_LOGGER.info("Registering hystrix jmx metrics plugin");
        HystrixCodaHaleMetricsPublisher publisher = new HystrixCodaHaleMetricsPublisher(Metrics.getRegistry());
        HystrixPlugins.getInstance().registerMetricsPublisher(publisher);
        STARTUP_LOGGER.info("Registered hystrix jmx metrics plugin");
    }

    private void startPoseidon() {
        new ClassPathXmlApplicationContext("web.xml");
        getBean(Poseidon.class).start();
    }

    private void stopPoseidon() {
        getBean(Poseidon.class).stop();
    }
}
