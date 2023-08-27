package xyz.srnyx.eventalerts.mongo.objects;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import org.jetbrains.annotations.Nullable;


public class Partner {
    @BsonProperty(value = "_id") public ObjectId id;
    public Long user;
    @BsonProperty(value = "last_renewal") @Nullable public Long lastRenewal;
}
