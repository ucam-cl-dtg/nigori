/*
 * Copyright (C) 2010 Google Inc.
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

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gson type adapter for protocol buffers
 *
 * @author Inderjeet Singh
 */
public class TypeAdapterProtobuf implements JsonSerializer<GeneratedMessage>,
JsonDeserializer<GeneratedMessage> {

  private JsonElement serializeWithEnumRewrite(JsonSerializationContext context, Object o) {
    if (o instanceof EnumValueDescriptor) {
      return context.serialize(((EnumValueDescriptor)o).getName());
    } else {
      return context.serialize(o);
    }
  }

  @Override
  public JsonElement serialize(GeneratedMessage src, Type typeOfSrc,
      JsonSerializationContext context) {
    JsonObject ret = new JsonObject();
    final Map<FieldDescriptor, Object> fields = src.getAllFields();
    for (Map.Entry<FieldDescriptor, Object> fieldPair : fields.entrySet()) {
      final FieldDescriptor desc = fieldPair.getKey();
      if (desc.isRepeated()) {
        List<?> fieldList = (List<?>) fieldPair.getValue();
        if (fieldList.size() != 0) {
          JsonArray array = new JsonArray();
          for (Object o : fieldList) {
            array.add(serializeWithEnumRewrite(context, o));
          }
          ret.add(desc.getName(), array);
          }
        } else {
          ret.add(desc.getName(), serializeWithEnumRewrite(context, fieldPair.getValue()));
        }
      }
      return ret;
    }

    private String camelCaseField(String underscoreName) {
      if (underscoreName == null)
        return null;
      char[] input = underscoreName.toCharArray();
      StringBuffer output = new StringBuffer();
      for(int i = 0; i < input.length; i++) {
        if (input[i] == '_' && (i + 1) < input.length) {
          output.append(Character.toUpperCase(input[++i]));
        } else {
          output.append(input[i]);
        }
      }
      return output.toString();
    }

    @Override
    public GeneratedMessage deserialize(JsonElement json, Type typeOfT,
        JsonDeserializationContext context) throws JsonParseException {

      JsonObject jsonObject = json.getAsJsonObject();
      @SuppressWarnings("unchecked")
      Class<? extends GeneratedMessage> protoClass =
        (Class<? extends GeneratedMessage>) typeOfT; 

      try {
        // Invoke the ProtoClass.newBuilder() method
        Object protoBuilder = getCachedMethod(protoClass, "newBuilder")
        .invoke(null);
        Class<?> builderClass = protoBuilder.getClass();

        Descriptor protoDescriptor = (Descriptor) getCachedMethod(
            protoClass, "getDescriptor").invoke(null);
        // Call setters on all of the available fields
        for (FieldDescriptor fieldDescriptor : protoDescriptor.getFields()) {
          String name = fieldDescriptor.getName();
          if (jsonObject.has(name)) {
            JsonElement jsonElement = jsonObject.get(name);
            String fieldName = camelCaseField(name + "_");
            Field field = protoClass.getDeclaredField(fieldName);
            Type fieldType = field.getGenericType();
            if (fieldType.equals(Object.class)){
            // TODO(drt24): this is very evil.
            // In NigoriMessages protobuf strings are stored in a field of type Object so that they
            // can use either String of ByteString as the implementation, however this causes a type
            // error when calling the set method. So we make a (potentially false) assumption that
            // all fields of type Object in NigoriMessages have that type because they actually
            // should have Strings set.
              fieldType = String.class;
            }
            Object fieldValue = context.deserialize(jsonElement, fieldType);
            if(fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.ENUM) {
              Method methodVD = getCachedMethod(fieldValue.getClass(), "getValueDescriptor");
              fieldValue = methodVD.invoke(fieldValue);
            }         
            Method method = getCachedMethod(
                builderClass, "setField", FieldDescriptor.class, Object.class);
            method.invoke(protoBuilder, fieldDescriptor, fieldValue);
          }
        }
        // Invoke the build method to return the final proto
        return (GeneratedMessage) getCachedMethod(builderClass, "build")
        .invoke(protoBuilder);
      } catch (SecurityException e) {
        throw new JsonParseException(e);
      } catch (NoSuchMethodException e) {
        throw new JsonParseException(e);
      } catch (IllegalArgumentException e) {
        throw new JsonParseException(e);
      } catch (IllegalAccessException e) {
        throw new JsonParseException(e);
      } catch (InvocationTargetException e) {
        throw new JsonParseException(e);
      } catch (NoSuchFieldException e) {
        throw new JsonParseException(e);
      }
    }

    private static Method getCachedMethod(Class<?> clazz, String methodName,
        Class<?>... methodParamTypes) throws NoSuchMethodException {
      Map<Class<?>, Method> mapOfMethods = mapOfMapOfMethods.get(methodName);
      if (mapOfMethods == null) {
        mapOfMethods = new HashMap<Class<?>, Method>();
        mapOfMapOfMethods.put(methodName, mapOfMethods);
      }
      Method method = mapOfMethods.get(clazz);
      if (method == null) {
        method = clazz.getMethod(methodName, methodParamTypes);
        mapOfMethods.put(clazz, method);
      }
      return method;
    }

    private static Map<String, Map<Class<?>, Method>> mapOfMapOfMethods =
      new HashMap<String, Map<Class<?>, Method>>();
  }