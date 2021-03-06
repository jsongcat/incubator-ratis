/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ratis.conf;

import org.apache.ratis.shaded.com.google.common.base.Objects;
import org.apache.ratis.util.CheckedBiConsumer;
import org.apache.ratis.util.NetUtils;
import org.apache.ratis.util.SizeInBytes;
import org.apache.ratis.util.TimeDuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface ConfUtils {
  Logger LOG = LoggerFactory.getLogger(ConfUtils.class);

  static <T> void logGet(String key, T value, T defaultValue) {
    LOG.info("{} = {} ({})", key, value, Objects.equal(value, defaultValue)? "default": "custom");
  }

  static void logSet(String key, Object value) {
    LOG.debug("set {} = {}", key, value);
  }

  static BiConsumer<String, Integer> requireMin(int min) {
    return (key, value) -> {
      if (value < min) {
        throw new IllegalArgumentException(
            key + " = " + value + " < min = " + min);
      }
    };
  }

  static BiConsumer<String, Integer> requireMax(int max) {
    return (key, value) -> {
      if (value > max) {
        throw new IllegalArgumentException(
            key + " = " + value + " > max = " + max);
      }
    };
  }

  static BiConsumer<String, Long> requireMin(long min) {
    return (key, value) -> {
      if (value < min) {
        throw new IllegalArgumentException(
            key + " = " + value + " < min = " + min);
      }
    };
  }

  static BiConsumer<String, Long> requireMax(long max) {
    return (key, value) -> {
      if (value > max) {
        throw new IllegalArgumentException(
            key + " = " + value + " > max = " + max);
      }
    };
  }

  static BiConsumer<String, TimeDuration> requireNonNegativeTimeDuration() {
    return (key, value) -> {
      if (value.isNegative()) {
        throw new IllegalArgumentException(
            key + " = " + value + " is negative.");
      }
    };
  }

  static BiFunction<String, Long, Integer> requireInt() {
    return (key, value) -> {
      try {
        return Math.toIntExact(value);
      } catch (ArithmeticException ae) {
        throw new IllegalArgumentException(
            "Failed to cast " + key + " = " + value + " to int.", ae);
      }
    };
  }

  @SafeVarargs
  static boolean getBoolean(
      BiFunction<String, Boolean, Boolean> booleanGetter,
      String key, boolean defaultValue, BiConsumer<String, Boolean>... assertions) {
    return get(booleanGetter, key, defaultValue, assertions);
  }

  @SafeVarargs
  static int getInt(
      BiFunction<String, Integer, Integer> integerGetter,
      String key, int defaultValue, BiConsumer<String, Integer>... assertions) {
    return get(integerGetter, key, defaultValue, assertions);
  }

  @SafeVarargs
  static long getLong(
      BiFunction<String, Long, Long> longGetter,
      String key, long defaultValue, BiConsumer<String, Long>... assertions) {
    return get(longGetter, key, defaultValue, assertions);
  }

  @SafeVarargs
  static File getFile(
      BiFunction<String, File, File> fileGetter,
      String key, File defaultValue, BiConsumer<String, File>... assertions) {
    return get(fileGetter, key, defaultValue, assertions);
  }


  @SafeVarargs
  static SizeInBytes getSizeInBytes(
      BiFunction<String, SizeInBytes, SizeInBytes> getter,
      String key, SizeInBytes defaultValue, BiConsumer<String, SizeInBytes>... assertions) {
    final SizeInBytes value = get(getter, key, defaultValue, assertions);
    requireMin(0L).accept(key, value.getSize());
    return value;
  }

  @SafeVarargs
  static TimeDuration getTimeDuration(
      BiFunction<String, TimeDuration, TimeDuration> getter,
      String key, TimeDuration defaultValue, BiConsumer<String, TimeDuration>... assertions) {
    final TimeDuration value = get(getter, key, defaultValue, assertions);
    requireNonNegativeTimeDuration().accept(key, value);
    return value;
  }

  @SafeVarargs
  static <T> T get(BiFunction<String, T, T> getter,
      String key, T defaultValue, BiConsumer<String, T>... assertions) {
    final T value = getter.apply(key, defaultValue);
    logGet(key, value, defaultValue);
    Arrays.asList(assertions).forEach(a -> a.accept(key, value));
    return value;
  }

  static InetSocketAddress getInetSocketAddress(
      BiFunction<String, String, String> stringGetter,
      String key, String defaultValue) {
    return NetUtils.createSocketAddr(get(stringGetter, key, defaultValue));
  }

  @SafeVarargs
  static void setBoolean(
      BiConsumer<String, Boolean> booleanSetter, String key, boolean value,
      BiConsumer<String, Boolean>... assertions) {
    set(booleanSetter, key, value, assertions);
  }

  @SafeVarargs
  static void setInt(
      BiConsumer<String, Integer> integerSetter, String key, int value,
      BiConsumer<String, Integer>... assertions) {
    set(integerSetter, key, value, assertions);
  }

  @SafeVarargs
  static void setLong(
      BiConsumer<String, Long> longSetter, String key, long value,
      BiConsumer<String, Long>... assertions) {
    set(longSetter, key, value, assertions);
  }

  @SafeVarargs
  static void setFile(
      BiConsumer<String, File> fileSetter, String key, File value,
      BiConsumer<String, File>... assertions) {
    set(fileSetter, key, value, assertions);
  }

  @SafeVarargs
  static void setSizeInBytes(
      BiConsumer<String, String> stringSetter, String key, SizeInBytes value,
      BiConsumer<String, Long>... assertions) {
    final long v = value.getSize();
    Arrays.asList(assertions).forEach(a -> a.accept(key, v));
    set(stringSetter, key, value.getInput());
  }

  @SafeVarargs
  static <T> void set(
      BiConsumer<String, T> setter, String key, T value,
      BiConsumer<String, T>... assertions) {
    Arrays.asList(assertions).forEach(a -> a.accept(key, value));
    setter.accept(key, value);
    logSet(key, value);
  }

  static void printAll(Class<?> confClass) {
    ConfUtils.printAll(confClass, System.out::println);
  }

  static void printAll(Class<?> confClass, Consumer<Object> out) {
    out.accept("");
    out.accept("******* " + confClass + " *******");
    Arrays.asList(confClass.getDeclaredFields())
        .forEach(f -> printField(confClass, out, f));
    Arrays.asList(confClass.getClasses())
        .forEach(c -> printAll(c, s -> out.accept("  " + s)));
  }

  static void printField(Class<?> confClass, Consumer<Object> out, Field f) {
    if (!Modifier.isStatic(f.getModifiers())) {
      out.accept("WARNING: Found non-static field " + f);
      return;
    }
    if (printKey(confClass, out, f, "KEY", "DEFAULT",
        (b, defaultField) ->
            b.append(defaultField.getType().getSimpleName()).append(", ")
                .append("default=" + defaultField.get(null)))) {
      return;
    }
    if (printKey(confClass, out, f, "PARAMETER", "CLASS",
        (b, classField) -> b.append(classField.get(null)))) {
      return;
    }
    final String fieldName = f.getName();
    try {
      out.accept("constant: " + fieldName + " = " + f.get(null));
    } catch (IllegalAccessException e) {
      out.accept("WARNING: Failed to access " + f);
    }
  }

  static boolean printKey(
      Class<?> confClass, Consumer<Object> out, Field f, String KEY, String DEFAULT,
      CheckedBiConsumer<StringBuilder, Field, IllegalAccessException> processDefault) {
    final String fieldName = f.getName();
    if (fieldName.endsWith("_" + DEFAULT)) {
      return true;
    }
    if (!fieldName.endsWith("_" + KEY)) {
      return false;
    }
    final StringBuilder b = new StringBuilder();
    try {
      final Object keyName = f.get(null);
      b.append(KEY.toLowerCase()).append(": ").append(keyName);
    } catch (IllegalAccessException e) {
      out.accept("WARNING: Failed to access " + fieldName);
      b.append(fieldName + " is not public");
    }
    final int len = fieldName.length() - KEY.length();
    final String defaultFieldName = fieldName.substring(0, len) + DEFAULT;
    b.append(" (");
    try {
      final Field defaultField = confClass.getDeclaredField(defaultFieldName);
      processDefault.accept(b, defaultField);
    } catch (NoSuchFieldException e) {
      out.accept("WARNING: " + DEFAULT + " not found for field " + f);
      b.append(DEFAULT).append(" not found");
    } catch (IllegalAccessException e) {
      out.accept("WARNING: Failed to access " + defaultFieldName);
      b.append(defaultFieldName).append(" is not public");
    }
    b.append(")");
    out.accept(b);
    return true;
  }
}
