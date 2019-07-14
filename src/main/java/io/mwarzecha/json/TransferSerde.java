package io.mwarzecha.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import io.mwarzecha.model.Transfer;
import io.mwarzecha.model.Money;
import java.lang.reflect.Type;

public class TransferSerde implements JsonSerializer<Transfer>, JsonDeserializer<Transfer> {

  @Override
  public JsonElement serialize(Transfer transfer, Type type, JsonSerializationContext ctx) {
    var jsonObject = new JsonObject();
    jsonObject.addProperty("id", transfer.getId());
    jsonObject.addProperty("from_account", transfer.getFromAccountId());
    jsonObject.addProperty("to_account", transfer.getToAccountId());
    jsonObject.addProperty("amount", transfer.getAmount().toPlainString());
    jsonObject.addProperty("currency", transfer.getCurrency());
    jsonObject.addProperty("timestamp", transfer.getTimestamp().toString());
    return jsonObject;
  }

  @Override
  public Transfer deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx)
      throws JsonParseException {
    var jsonObject = jsonElement.getAsJsonObject();
    return Transfer.newBuilder()
        .fromAccountId(jsonObject.get("from_account").getAsLong())
        .toAccountId(jsonObject.get("to_account").getAsLong())
        .amount(Money.of(jsonObject.get("currency").getAsString(),
            jsonObject.get("amount").getAsBigDecimal()))
        .build();
  }
}
