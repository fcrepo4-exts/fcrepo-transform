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
package org.fcrepo.transform.transformations;

import com.google.common.collect.ImmutableList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import org.apache.marmotta.ldpath.LDPath;
import org.apache.marmotta.ldpath.backend.jena.GenericJenaBackend;
import org.apache.marmotta.ldpath.exception.LDPathParseException;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.transform.TransformNotFoundException;
import org.fcrepo.transform.Transformation;

import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.slf4j.LoggerFactory.getLogger;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;

/**
 * Utilities for working with LDPath
 *
 * @author cbeer
 */
public class LDPathTransform implements Transformation<List<Map<String, Collection<Object>>>>  {

    public static final String CONFIGURATION_FOLDER = "/fedora:system/fedora:transform/fedora:ldpath/";

    public static final String DEFAULT_TRANSFORM_RESOURCE = CONFIGURATION_FOLDER + 
            "default/fedora:Resource";

    private static final Comparator<NodeType> BY_NAME =
            (final NodeType o1, final NodeType o2) -> o1.getName().compareTo(o2.getName());

    // TODO: this mime type was made up
    public static final String APPLICATION_RDF_LDPATH = "application/rdf+ldpath";
    private final InputStream query;

    private static final Logger LOGGER = getLogger(LDPathTransform.class);

    /**
     * Construct a new Transform from the InputStream
     * @param query the query
     */
    public LDPathTransform(final InputStream query) {
        this.query = query;
    }

    public static LDPathTransform getResourceTransform(final FedoraResource resource, final NodeService nodeService,
            final String key) throws RepositoryException {

        final FedoraResource programResource = nodeService.find(resource.getNode().getSession(), CONFIGURATION_FOLDER + key);
    
        LOGGER.debug("Found program resource: {}", programResource.getPath());
    
        final List<URI> rdfTypes = resource.getTypes();
    
        LOGGER.debug("Discovered rdf types: {}", rdfTypes);
        
        final List<String> rdfStringTypes = rdfTypes.stream().map(type -> programResource.getPath() + "/" + type.toString().replace( REPOSITORY_NAMESPACE, "fedora:") + "/jcr:content").collect(Collectors.toList());
        
        final FedoraBinary transform = (FedoraBinary) programResource.getChildren()
            .peek(child -> LOGGER.debug("programResource child path is {}", child.getPath()))
            .filter(child -> rdfStringTypes.contains(child.getPath()))
            .findFirst()
            .orElseThrow(() -> new TransformNotFoundException(String.format("Couldn't find transformation for {} and transformation key {}", resource.getPath(), key)));

        return new LDPathTransform(transform.getContent());
    }

    @Override
    public List<Map<String, Collection<Object>>> apply(final RdfStream stream) {
        final LDPath<RDFNode> ldpathForResource =
                getLdpathResource(stream);

        final Resource context = createResource(stream.topic().getURI());

        try {
            return ImmutableList.of(unsafeCast(
                    ldpathForResource.programQuery(context, new InputStreamReader(query))));
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

    /**
     * Get the LDPath resource for an object
     * @param rdfStream
     * @return the LDPath resource for the given object
     */
    private static LDPath<RDFNode> getLdpathResource(final RdfStream rdfStream) {

        return new LDPath<>(new GenericJenaBackend(rdfStream.collect(toModel())));

    }
}
