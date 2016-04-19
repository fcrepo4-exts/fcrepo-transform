/*
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.transform.http.responses;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This {@link Provider} adds configuration for the serialization of JSON resources.
 *
 * @author awoods
 * @since Feb 9, 2016
 */
@Provider
public class JsonObjectProvider implements ContextResolver<ObjectMapper> {

    private static final Logger LOGGER = getLogger(JsonObjectProvider.class);

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final ObjectMapper defaultObjectMapper;

    /**
     * Default constructor
     */
    public JsonObjectProvider() {
        defaultObjectMapper = createDefaultMapper();
    }

    @Override
    public ObjectMapper getContext(final Class<?> aClass) {
        LOGGER.debug("Object mapping for: {}", aClass.getCanonicalName());
        return defaultObjectMapper;
    }

    private static ObjectMapper createDefaultMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(DATE_FORMAT);

        return mapper;
    }
}
