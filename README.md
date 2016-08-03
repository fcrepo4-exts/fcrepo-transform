# fcrepo Transform [![Build Status](https://travis-ci.org/fcrepo4-exts/fcrepo-transform.png?branch=master)](https://travis-ci.org/fcrepo4-exts/fcrepo-transform)

fcrepo-transform allows you to set up and publish transformations of the RDF that composes a Fedora object's representation. You can use SPARQL for RDF-to-RDF transformations, or LDPath for RDF-to-named-fields transformations. 

Example uses might include:
* Presenting different views of an object's metadata to different audiences
* Live-translating from one kind of metadata to another
* Filling in the fields of an HTML form for editing metadata

## LDPath transformations

[LDPath](http://marmotta.apache.org/ldpath/) is a language for querying linked data graphs, implemented by the [Apache Marmotta](http://marmotta.apache.org) project.
It can be used for translating Fedora resources into named fields (i.e. JSON documents), but its power lies in being able to follow and dereference URIs.

For example, a Fedora resource may contain the following triples:

```
<> dcterms:title "Birds of Amherst, MA" ;
   dcterms:subject </fcrepo/rest/subjects/aaaaaa>, <http://id.loc.gov/authorities/subjects/sh85014310> ;
   dcterms:spatial <http://sws.geonames.org/4929022> .
```

which contains both in-domain and external URIs. While these URIs are unambiguous references to the subject (Birds) and location (Amherst, MA), they
often need to be dereferenced in order to be most useful.

Using Marmotta's `linkeddata` backend to the LDPath-based transform module, it is possible to query these resources and follow links to arbitrary depths.

For example:

```
@prefix gn : <http://www.geonames.org/ontology#>

title = dcterms:title :: xsd:string ;
subject = dcterms:subject / (skos:prefLabel | rdfs:label) :: xsd:string ;
location = dcterms:spatial / gn:name :: xsd:string ;
```

Could produce a response such as:

```
[{
    "title" : ["Birds of Amherst, MA"] ,
    "subject" : ["Birds", "Birds of New England"] ,
    "location" : ["Amherst"]
}]
```

By default, the `linkeddata` backend will cache responses from remote repositories for 1 day. It is possible to specify particular behavior on an endpoint-by-endpoint basis by configuring the Spring-based
transform context. For instance, adding support for a Getty endpoint could include (as part of the `./spring/transform.xml` configuration):

```
<util:list id="endpoints" value-type="org.apache.marmotta.ldclient.api.endpoint.Endpoint">

  <!-- add support for Getty vocabs -->
  <bean class="org.apache.marmotta.ldclient.endpoint.rdf.SPARQLEndpoint">
    <constructor-arg index="0" type="java.lang.String" value="Getty Vocabs"/>
    <constructor-arg index="1" type="java.lang.String" value="http://vocab.getty.edu/sparql"/>
    <constructor-arg index="2" type="java.lang.String" value="^http://vocab\\.getty\\.edu/.*"/>
  </bean>


  <bean class="org.fcrepo.transform.ldcache.FedoraEndpoint">
    <!-- set the base URL of the Fedora endpoint (hostname and port will suffice) -->
    <constructor-arg type="java.lang.String" value="http://localhost:8080"/>
    <!-- cache values for five minutes (300) seconds -->
    <!-- set this to 0 to ensure that data is always fresh -->
    <constructor-arg type="long" value="300"/>
  </bean>
</util:list>
```

If your Fedora repository has authorization enabled, you will need to provide credentails to the linked data client.

```
<!-- define the username and password for the user accessing the repo with the ldclient -->
<bean id="credentials" class="org.apache.http.auth.UsernamePasswordCredentials">
  <constructor-arg type="java.lang.String" value="USERNAME"/>
  <constructor-arg type="java.lang.String" value="PASSWORD"/>
</bean>

<!-- define the host and port for the repository -->
<bean id="authScope" class="org.apache.http.auth.AuthScope">
  <constructor-arg type="java.lang.String" value="localhost"/>
  <constructor-arg type="int" value="8080"/>
</bean>
```

If you customize the spring configuration, it is generally recommended to put your customization in a separate, system directory such as `/etc/fcrepo/spring/transform.xml`.
Then, you can add the following to your `JAVA_OPTS`: `-Dfcrepo.spring.transform.configuration=file:/etc/fcrepo/spring/transform.xml`.

## Maintainers

* [Jared Whiklo](https://github.com/whikloj)
* [Aaron Coburn](https://github.com/acoburn)
