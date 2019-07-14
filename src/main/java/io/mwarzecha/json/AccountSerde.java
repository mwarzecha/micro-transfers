package io.mwarzecha.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import io.mwarzecha.model.Account;
import io.mwarzecha.model.Money;
import java.lang.reflect.Type;

public class AccountSerde implements JsonSerializer<Account>, JsonDeserializer<Account> {

  @Override
  public JsonElement serialize(Account account, Type type, JsonSerializationContext ctx) {
    var jsonObject = new JsonObject();
    jsonObject.addProperty("id", account.getId());
    jsonObject.addProperty("owner", account.getOwner());
    jsonObject.addProperty("balance", account.getBalance().toPlainString());
    jsonObject.addProperty("currency", account.getCurrency());
    return jsonObject;
  }

  @Override
  public Account deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx)
      throws JsonParseException {
    var jsonObject = jsonElement.getAsJsonObject();
    return Account.newBuilder()
        .owner(jsonObject.get("owner").getAsString())
        .balance(Money.of(jsonObject.get("currency").getAsString(),
            jsonObject.get("balance").getAsBigDecimal()))
        .build();
  }
}