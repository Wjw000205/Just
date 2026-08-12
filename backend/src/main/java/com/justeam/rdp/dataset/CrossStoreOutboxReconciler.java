package com.justeam.rdp.dataset;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CrossStoreOutboxReconciler {
    private final CrossStoreOutboxService outbox;
    private final MongoTemplate mongo;

    public CrossStoreOutboxReconciler(CrossStoreOutboxService outbox, MongoTemplate mongo) {
        this.outbox = outbox;
        this.mongo = mongo;
    }

    @Scheduled(fixedDelayString = "${rdp.outbox-reconcile-delay-ms:30000}")
    public void reconcile() {
        for (CrossStoreOutboxService.Event event : outbox.recoverableEvents()) {
            try {
                String collection = "dataset_data_" + event.aggregateId();
                Document record = switch (event.operation()) {
                    case "CREATE" -> mongo.findOne(Query.query(Criteria.where("outboxEventKey")
                            .is(event.eventKey().toString())), Document.class, collection);
                    case "UPDATE", "DELETE" -> mongo.findOne(Query.query(Criteria.where("_id")
                            .is(new org.bson.types.ObjectId(event.aggregateRecordId()))
                            .and("lastOutboxEventKey").is(event.eventKey().toString())), Document.class, collection);
                    default -> null;
                };
                if (record == null) {
                    outbox.retryMiss(event, "MongoDB中尚未发现对应变更，等待重试或人工处置");
                    continue;
                }
                switch (event.operation()) {
                    case "CREATE" -> outbox.completeCreate(event, event.aggregateId(), record.getObjectId("_id").toHexString(),outbox.details(event));
                    case "UPDATE" -> outbox.completeUpdate(event, outbox.details(event));
                    case "DELETE" -> {
                        if (record.getBoolean("deleted", false))
                            outbox.completeDelete(event, event.aggregateId(), outbox.details(event));
                    }
                    default -> { }
                }
            } catch (Exception ex) {
                outbox.failLeased(event, ex);
            }
        }
    }
}
