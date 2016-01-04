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

import org.slf4j.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.trpr.platform.core.impl.event.PlatformApplicationEvent;
import org.trpr.platform.core.spi.event.PlatformEventConsumer;
import org.trpr.platform.model.event.PlatformEvent;
import org.trpr.platform.runtime.impl.event.BootstrapProgressMonitor;

import static com.flipkart.poseidon.PoseidonContext.getBean;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class BootstrapListener implements PlatformEventConsumer {

    private static final Logger logger = getLogger(BootstrapListener.class);

    public void setBootstrapProgressMonitor(BootstrapProgressMonitor bootstrapProgressMonitor) {
        bootstrapProgressMonitor.addBootstrapEventListener(this);
    }

    @Override
    public void onApplicationEvent(PlatformApplicationEvent event) {
        if (event.getSource() instanceof PlatformEvent) {
            if (((PlatformEvent) event.getSource()).getEventType().equals("BootstrapMonitoredEvent")) {
                if (((PlatformEvent) event.getSource()).getEventStatus().equals("started")) {
                    logger.info("\n************************************" +
                            "\n______ " +
                            "\n| ___ \\" +
                            "\n| |_/ /" +
                            "\n|  __/ " +
                            "\n| |    Handlers Initialized." +
                            "\n\\_|    Now starting Poseidon..." +
                            "\n************************************");
                    startPoseidon();
                }
            }
        }
    }

    private void startPoseidon() {
        new ClassPathXmlApplicationContext("web.xml");
        getBean(Poseidon.class).run();
    }
}
