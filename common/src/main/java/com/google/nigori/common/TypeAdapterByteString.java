/*
 * Copyright (C) 2011 Alastair R. Beresford.
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
package com.google.nigori.common;

import java.lang.reflect.Type;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.protobuf.ByteString;

public class TypeAdapterByteString  implements JsonSerializer<ByteString>,
JsonDeserializer<ByteString> {

  @Override
  public JsonElement serialize(ByteString src, Type typeOfSrc,
      JsonSerializationContext context) {
    return context.serialize(new String(Base64.encodeBase64(src.toByteArray())));
  }

  @SuppressWarnings("deprecation")// see comment below
  @Override
  public ByteString deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    byte[] jsonBytes = json.getAsString().getBytes();
    // (drt24) Since android ships with an ancient version of org.apache.commons.codec which
    // overrides any version we ship we have to use old deprecated methods.
    if (Base64.isArrayByteBase64(jsonBytes)) {
      return ByteString.copyFrom(Base64.decodeBase64(jsonBytes));
    } else {
      throw new JsonParseException("JSON element is not correctly base64 encoded.");
    }
  }
}
