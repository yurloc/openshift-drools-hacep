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
package org.kie.hacep.consumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.kie.api.runtime.rule.FactHandle;
import org.kie.remote.RemoteFactHandle;
import org.kie.remote.command.DeleteCommand;
import org.kie.remote.command.FactCountCommand;
import org.kie.remote.command.InsertCommand;
import org.kie.remote.command.ListObjectsCommand;
import org.kie.remote.command.ListObjectsCommandClassType;
import org.kie.remote.command.ListObjectsCommandNamedQuery;
import org.kie.remote.command.UpdateCommand;
import org.kie.remote.command.VisitorCommand;
import org.kie.hacep.EnvConfig;
import org.kie.hacep.core.KieSessionHolder;
import org.kie.hacep.core.infra.producer.Producer;
import org.kie.hacep.model.FactCountMessage;
import org.kie.hacep.model.ListKieSessionObjectMessage;

public class CommandHandler implements VisitorCommand {

    private BidirectionalMap<RemoteFactHandle, FactHandle> fhMap;
    private KieSessionHolder kieSessionHolder;
    private EnvConfig config;
    private Producer producer;

    public CommandHandler(BidirectionalMap<RemoteFactHandle, FactHandle> fhMap,
                          KieSessionHolder kieSessionHolder,
                          EnvConfig config,
                          Producer producer) {
        this.fhMap = fhMap;
        this.kieSessionHolder = kieSessionHolder;
        this.config = config;
        this.producer = producer;
    }

    @Override
    public void visit(InsertCommand command, boolean execute) {
        if(execute) {
            RemoteFactHandle remoteFH = command.getFactHandle();
            FactHandle fh = kieSessionHolder.getKieSession().getEntryPoint(command.getEntryPoint()).insert(remoteFH.getObject());
            fhMap.put(remoteFH, fh);
            kieSessionHolder.getKieSession().fireAllRules();
        }
    }


    @Override
    public void visit(DeleteCommand command, boolean execute) {
        if(execute) {
            RemoteFactHandle remoteFH = command.getFactHandle();
            kieSessionHolder.getKieSession().getEntryPoint(command.getEntryPoint()).delete(fhMap.get(remoteFH));
            kieSessionHolder.getKieSession().fireAllRules();
        }
    }


    @Override
    public void visit(UpdateCommand command, boolean execute) {
        if(execute) {
            RemoteFactHandle remoteFH = command.getFactHandle();
            FactHandle factHandle = fhMap.get(remoteFH);
            kieSessionHolder.getKieSession().getEntryPoint(command.getEntryPoint()).update(factHandle, command.getObject());
            kieSessionHolder.getKieSession().fireAllRules();
        }
    }


    @Override
    public void visit(ListObjectsCommand command, boolean execute) {
        if(execute) {
            List serializableItems = getObjectList(command);
            ListKieSessionObjectMessage msg = new ListKieSessionObjectMessage(command.getFactHandle().getId(), serializableItems);
            producer.produceSync(config.getKieSessionInfosTopicName(), command.getFactHandle().getId(), msg);
        }
    }

    private List getObjectList(ListObjectsCommand command) {
        Collection<? extends Object> objects = kieSessionHolder.getKieSession().getEntryPoint(command.getEntryPoint()).getObjects();
        return getListFromSerializableCollection(objects);
    }


    @Override
    public void visit(ListObjectsCommandClassType command, boolean execute) {
        if(execute) {
            List serializableItems = getSerializableItemsByClassType(command);
            ListKieSessionObjectMessage msg = new ListKieSessionObjectMessage(command.getFactHandle().getId(), serializableItems);
            producer.produceSync(config.getKieSessionInfosTopicName(), command.getFactHandle().getId(), msg);
        }
    }

    private List getSerializableItemsByClassType(ListObjectsCommandClassType command) {
        Collection<? extends Object> objects = ObjectFilterHelper.getObjectsFilterByClassType(command.getClazzType(), kieSessionHolder.getKieSession());
        return getListFromSerializableCollection(objects);
    }

    private List getListFromSerializableCollection(Collection<?> objects) {
        List serializableItems = new ArrayList<>(objects.size());
        Iterator<? extends Object> iterator = objects.iterator();
        while (iterator.hasNext()) {
            Object o = iterator.next();
            serializableItems.add(o);
        }
        return serializableItems;
    }


    @Override
    public void visit(ListObjectsCommandNamedQuery command, boolean execute) {
        if(execute) {
            List serializableItems = getSerializableItemsByNamedQuery(command);
            ListKieSessionObjectMessage msg = new ListKieSessionObjectMessage(command.getFactHandle().getId(), serializableItems);
            producer.produceSync(config.getKieSessionInfosTopicName(), command.getFactHandle().getId(), msg);
        }
    }

    private List getSerializableItemsByNamedQuery(ListObjectsCommandNamedQuery command) {
        Collection<? extends Object> objects = ObjectFilterHelper.getObjectsFilterByNamedQuery(command.getNamedQuery(),
                                                                                               command.getObjectName(),
                                                                                               command.getParams(),
                                                                                               kieSessionHolder.getKieSession());
        return getListFromSerializableCollection(objects);
    }


    @Override
    public void visit(FactCountCommand command, boolean execute) {
        if(execute) {
            FactCountMessage msg = new FactCountMessage(command.getFactHandle().getId(), kieSessionHolder.getKieSession().getFactCount());
            producer.produceSync(config.getKieSessionInfosTopicName(), command.getFactHandle().getId(), msg);
        }
    }
}