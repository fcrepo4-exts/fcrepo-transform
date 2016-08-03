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
package org.fcrepo.transform.transformations;

import com.google.common.collect.ImmutableList;

import org.apache.marmotta.ldpath.LDPath;
import org.apache.marmotta.ldpath.backend.linkeddata.LDCacheBackend;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.transform.TransformNotFoundException;
import org.fcrepo.transform.Transformation;

import org.slf4j.Logger;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utilities for working with LDPath
 *
 * @author cbeer
 */
public class LDPathTransform implements Transformation<List<Map<String, Collection<Object>>>>  {

    public static final String CONFIGURATION_FOLDER = "/fedora:system/fedora:transform/fedora:ldpath/";

    public static final String DEFAULT_TRANSFORM_RESOURCE = "fedora:Resource";

    // TODO: this mime type was made up
    public static final String APPLICATION_RDF_LDPATH = "application/rdf+ldpath";

    private final InputStream query;

    private final LDCacheBackend backend;

    private static final Logger LOGGER = getLogger(LDPathTransform.class);

    /**
     * Construct a new Transform from the InputStream
     * @param backend the LDCache backend
     * @param query the query
     */
    public LDPathTransform(final LDCacheBackend backend, final InputStream query) {
        this.query = query;
        this.backend = backend;
    }

    /**
     * Pull a resource-type specific transform for the specified key
     * @param resource the resource
     * @param session the session
     * @param nodeService a nodeService
     * @param backend the LDCache backend
     * @param key the key
     * @return resource-type specific transform
     * @throws RepositoryException if repository exception occurred
     */
    public static LDPathTransform getResourceTransform(final FedoraResource resource, final Session session,
            final NodeService nodeService, final LDCacheBackend backend, final String key) throws RepositoryException {

        final FedoraResource transformResource = nodeService.find(session, CONFIGURATION_FOLDER + key);

        LOGGER.debug("Found transform resource: {}", transformResource.getPath());

        final List<URI> rdfTypes = resource.getTypes();

        LOGGER.debug("Discovered rdf types: {}", rdfTypes);

        final NamespaceRegistry nsRegistry = session.getWorkspace().getNamespaceRegistry();

        // convert rdf:type with URI namespace to prefixed namespace
        final Function<URI, String> namespaceUriToPrefix = x -> {
            final String uriString = x.toString();
            try {
                for (final String namespace : nsRegistry.getURIs()) {
                    // Ignoring zero-length namespaces return the appropriate prefix
                    if (namespace.length() > 0 && uriString.startsWith(namespace)) {
                        return uriString.replace(namespace, nsRegistry.getPrefix(namespace) + ":");
                    }
                }
                return uriString;
            } catch (final RepositoryException e) {
                return uriString;
            }
        };

        final List<String> rdfStringTypes = rdfTypes.stream().map(namespaceUriToPrefix)
                .map(stringType -> transformResource.getPath() + "/" + stringType)
                .collect(Collectors.toList());

        final FedoraBinary transform = (FedoraBinary) transformResource.getChildren()
                .filter(child -> rdfStringTypes.contains(child.getPath()))
                .findFirst()
                .orElseThrow(() -> new TransformNotFoundException(
                    String.format("Couldn't find transformation for {} and transformation key {}",
                    resource.getPath(), key)));
        return new LDPathTransform(backend, transform.getContent());
    }

    @Override
    public List<Map<String, Collection<Object>>> apply(final RdfStream stream) {
        try {
            final LDPath<Value> ldpathForResource = new LDPath<Value>(backend);
            return ImmutableList.of(unsafeCast(
                ldpathForResource.programQuery(new URIImpl(stream.topic().getURI()), new InputStreamReader(query))));
        } catch (final LDPathParseException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <F, T> T unsafeCast(final F from) {
        return (T) from;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof LDPathTransform && ((LDPathTransform) other).query.equals(query);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(query);
    }
}
