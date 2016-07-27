/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.transform.ldcache;

import org.apache.marmotta.commons.http.ContentType;
import org.apache.marmotta.ldclient.api.endpoint.Endpoint;

/**
 * @author acoburn
 */
public class FedoraEndpoint extends Endpoint {

    /**
     * Create a Fedora endpoint
     * @param baseUrl the Base URL of the Fedora endpoint
     */
    public FedoraEndpoint(final String baseUrl) {
        this(baseUrl, 0L);
    }

    /**
     * Create a Fedora endpoint
     * @param baseUrl the Base URL of the Fedora endpoint
     * @param timeout the length of time (in seconds) to cache the data
     */
    public FedoraEndpoint(final String baseUrl, final long timeout) {
        super("Fedora Repository", "Linked Data", baseUrl + ".*", null, timeout);
        setPriority(PRIORITY_HIGH);
        addContentType(new ContentType("application", "rdf+xml", 0.8));
        addContentType(new ContentType("text", "turtle", 1.0));
        addContentType(new ContentType("text", "n3", 0.8));
        addContentType(new ContentType("application", "ld+json", 0.5));
    }
}
