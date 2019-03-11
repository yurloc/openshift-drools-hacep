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
package org.kie.u212.producer;

import java.util.List;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.kie.u212.model.StockTickEvent;
import org.kie.u212.PubSubConfig;
import org.kie.u212.infra.producer.EventProducer;
import org.kie.u212.infra.utils.RecordMetadataUtil;

public class DroolsProducer {

    public RecordMetadata create(List<StockTickEvent> events) {
        EventProducer<StockTickEvent> eventProducer = new EventProducer<>();
        eventProducer.start(PubSubConfig.getDefaultConfig());
        RecordMetadata lastRecord = null;
        for(StockTickEvent event: events) {
            lastRecord = eventProducer.produceSync(new ProducerRecord<>(PubSubConfig.MASTER_TOPIC,
                                                                        event.getId(),
                                                                        event));
            RecordMetadataUtil.logRecord(lastRecord);
        }
        eventProducer.stop();
        return lastRecord;
    }

    public RecordMetadata create(StockTickEvent event) {
        EventProducer<StockTickEvent> eventProducer = new EventProducer<>();
        eventProducer.start(PubSubConfig.getDefaultConfig());
        RecordMetadata lastRecord =  eventProducer.produceSync(new ProducerRecord<>(PubSubConfig.MASTER_TOPIC,
                                                                        event.getId(),
                                                                        event));
        RecordMetadataUtil.logRecord(lastRecord);
        eventProducer.stop();
        return lastRecord;
    }
}