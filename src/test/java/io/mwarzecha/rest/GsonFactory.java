package io.mwarzecha.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import io.mwarzecha.json.AccountSerde;
import io.mwarzecha.json.TransferSerde;
import io.mwarzecha.model.Account;
import io.mwarzecha.model.Transfer;
import java.lang.reflect.Type;
import java.time.Instant;

class GsonFactory {

  static Gson create() {
    return new GsonBuilder()
        .registerTypeAdapter(Account.class, new AccountWithIdSerde())
        .registerTypeAdapter(Transfer.class, new TransferWithIdAndTimestampSerde())
        .create();
  }

  private static class AccountWithIdSerde extends AccountSerde {

    @Override
    public JsonElement serialize(Account account, Type type, JsonSerializationContext ctx) {
      var jsonObject = new JsonObject();
      jsonObject.addProperty("owner", account.getOwner());
      jsonObject.addProperty("balance", account.getBalance().toPlainString());
      jsonObject.addProperty("currency", account.getCurrency());
      return jsonObject;
    }

    @Override
    public Account deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx)
        throws JsonParseException {
      var jsonObject = jsonElement.getAsJsonObject();
      return super.deserialize(jsonElement, type, ctx)
          .withId(jsonObject.get("id").getAsLong());
    }
  }

  private static class TransferWithIdAndTimestampSerde extends TransferSerde {

    @Override
    public JsonElement serialize(Transfer transfer, Type type, JsonSerializationContext ctx) {
      var jsonObject = new JsonObject();
      jsonObject.addProperty("from_account", transfer.getFromAccountId());
      jsonObject.addProperty("to_account", transfer.getToAccountId());
      jsonObject.addProperty("amount", transfer.getAmount().toPlainString());
      jsonObject.addProperty("currency", transfer.getCurrency());
      return jsonObject;
    }

    @Override
    public Transfer deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx)
        throws JsonParseException {
      var jsonObject = jsonElement.getAsJsonObject();
      return super.deserialize(jsonElement, type, ctx)
          .withIdAndTimestamp(
              jsonObject.get("id").getAsLong(),
              Instant.parse(jsonObject.get("timestamp").getAsString()));
    }
  }
}
