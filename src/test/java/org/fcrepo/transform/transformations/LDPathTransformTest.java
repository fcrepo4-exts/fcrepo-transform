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

import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static com.hp.hpl.jena.graph.Triple.create;
import static java.util.stream.Stream.of;
import static org.fcrepo.transform.transformations.LDPathTransform.CONFIGURATION_FOLDER;
import static org.fcrepo.transform.transformations.LDPathTransform.getResourceTransform;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.fcrepo.kernel.api.RdfLexicon.REPOSITORY_NAMESPACE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.stream.Stream;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriBuilder;

import org.fcrepo.kernel.api.RdfStream;
import org.fcrepo.kernel.api.models.FedoraBinary;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.api.rdf.DefaultRdfStream;
import org.fcrepo.kernel.api.services.NodeService;
import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.transform.TransformNotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * <p>LDPathTransformTest class.</p>
 *
 * @author cbeer
 */
public class LDPathTransformTest {

    @Mock
    private Node mockNode;

    @Mock
    private FedoraResourceImpl mockResource;

    @Mock
    private Session mockSession;

    @Mock
    private InputStream mockInputStream;

    @Mock
    private NodeType mockNodeType;

    @Mock
    private NodeService mockNodeService;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NamespaceRegistry mockRegistry;

    private LDPathTransform testObj;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);

        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockNode.getSession()).thenReturn(mockSession);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockRegistry);

    }

    @Test(expected = TransformNotFoundException.class)
    public void testGetNodeTypeSpecificLdpathProgramForMissingProgram() throws RepositoryException {
        when(mockRegistry.getPrefixes()).thenReturn(new String[] { "fedora" });
        when(mockRegistry.getURIs()).thenReturn(new String[] { REPOSITORY_NAMESPACE });
        when(mockRegistry.getPrefix(REPOSITORY_NAMESPACE)).thenReturn("fedora");


        final FedoraResource mockConfigNode = mock(FedoraResource.class);
        when(mockNodeService.find(mockSession, CONFIGURATION_FOLDER + "some-program"))
        .thenReturn(mockConfigNode);
        when(mockConfigNode.getPath()).thenReturn(CONFIGURATION_FOLDER + "some-program");
        when(mockConfigNode.getChildren()).thenReturn(Stream.of());

        final URI mockRdfType = UriBuilder.fromUri(REPOSITORY_NAMESPACE + "Resource").build();
        final List<URI> rdfTypes = new ArrayList<URI>();
        rdfTypes.add(mockRdfType);
        when(mockResource.getTypes()).thenReturn(rdfTypes);

        getResourceTransform(mockResource, mockNodeService, "some-program");
    }

    @Test
    public void testGetNodeTypeSpecificLdpathProgramForNodeTypeProgram() throws RepositoryException {
        final String customNsUri = "http://example-custom/type#";
        final String customNsPrefix = "custom";

        when(mockRegistry.getPrefixes()).thenReturn(new String[] { customNsPrefix });
        when(mockRegistry.getURIs()).thenReturn(new String[] { customNsUri });
        when(mockRegistry.getPrefix(customNsUri)).thenReturn(customNsPrefix);

        final FedoraResource mockConfigNode = mock(FedoraResource.class);
        when(mockNodeService.find(mockSession, CONFIGURATION_FOLDER + "some-program"))
        .thenReturn(mockConfigNode);
        when(mockConfigNode.getPath()).thenReturn(CONFIGURATION_FOLDER + "some-program");
        final FedoraBinary mockChildConfig = mock(FedoraBinary.class);
        when(mockChildConfig.getPath())
                .thenReturn(CONFIGURATION_FOLDER + "some-program/" + customNsPrefix + ":type/jcr:content");
        when(mockConfigNode.getChildren()).thenReturn(Stream.of(mockChildConfig));

        final URI mockRdfType = UriBuilder.fromUri(customNsUri + "type").build();
        when(mockResource.getTypes()).thenReturn(Arrays.asList(mockRdfType));

        when(mockChildConfig.getContent()).thenReturn(mockInputStream);

        final LDPathTransform nodeTypeSpecificLdpathProgramStream =
                getResourceTransform(mockResource, mockNodeService, "some-program");

        assertEquals(new LDPathTransform(mockInputStream), nodeTypeSpecificLdpathProgramStream);
    }

    @Test
    public void testProgramQuery() {

        final RdfStream rdfStream = new DefaultRdfStream(createURI("abc"), of(
                create(createURI("abc"),
                        createURI("http://purl.org/dc/elements/1.1/title"),
                        createLiteral("some-title"))));
        final InputStream testReader = new ByteArrayInputStream("title = dc:title :: xsd:string ;".getBytes());

        testObj = new LDPathTransform(testReader);
        final List<Map<String,Collection<Object>>> stringCollectionMapList = testObj.apply(rdfStream);
        final Map<String,Collection<Object>> stringCollectionMap = stringCollectionMapList.get(0);

        assert(stringCollectionMap != null);
        assertEquals(1, stringCollectionMap.size());
        assertEquals(1, stringCollectionMap.get("title").size());
        assertTrue(stringCollectionMap.get("title").contains("some-title"));
    }
}
