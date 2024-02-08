# <img height="25" src="./images/AILLogoSmall.png" width="40"/> ACME-UI

<a href="https://www.legosoft.com.mx"><img height="150px" src="./images/Icon.png" alt="AI Legorreta" align="left"/></a>
Microservice that acts as the user interface for all ACME dummy company applications. ACME operations are defined as 
a POC operations for  a dummy Company. These oeprations are developed in the ailegorreta mas part of its MarketPl.

The `acme-ui` is the micro front that acts as the front-end for ACME compnany. Some of them are: `bup-service`, 
`expedienre-service`, and `order-service` (as part of the ingestor POC) to mention some of them.

`Vaadin-hilla` framework (latest version 2.3.2) for the `lit-element` .

note: this microservice is the first to use `Vaadin-hilla` `lit-element`  concepts and also integrates some 
`Vaadin-flow` views with Spring security.

# UI for the ACME UI 

## Introduction

This is a Vaadin Hilla 2.3.2 application that shows as POCs all UI for ACME operations.

The operations that this micro front has are:

- Catalog for all Company industrial sectors.
- CRUD for Persons (Personas físiacas).
- CRUD for Companies (Persona moral)
- Relationships between Company & Person:
  - Directs.
  - Employees.
  - other
- Relaionships between Person & Person
  - Recommendations
  - Friends
  - Parentship.
  - other.
- Relationship between Compani & Company:
  - Suppliers.
  - Subsidiary
  - other.

note: The CRUD for Personas & Companies handles internally all the catalogs for:
  - States
  - Countries
  - Zip codes
  - Neighboard (i.e., colonias).
  - Municipios
  - eMails
  - eMail servers
  - CLABE account number
  - RFC

The back-end microservice that utilze this fron are:
- `bup-service`: most important
- `cache-service`: system parameters.
- `iam-service`: security

## Client document flow

Another important operation is to administrate the Oficial Customer Documents (i.e., Expediente) using the `Camunda`
BPM and the `expediente-service` that stores the documents inside `Alfresco` repository.

The operations are:

- Insert new documents instanciatiing a new BPM process.
- Validate the document by the back-office area.
- Validate some documents by the legal department office.
- Query the existing documents per Customer.

## Ingestor result quer.

To query the output for the `Phyton-ingestor` example POC  with `orders`.

The ingestor POC is as follows:

- The datasource is defined in the `sys-ui` front end.
- The `Phyton ingestor` is run al listen to the inpit directory.
- When a `cvs` file is added to the directory, the ingestor validates the fila and for the correct records it send a
`kafka` event that is listened by the `order-service`.
- The `order-service` inserts a new order in the budNeo4j database.
- This microservice `acme-ui` queries these orders.
- 
## Tech concepts

This user interface use many UI concepts as POC in order to demonstrate different forms
to develop a vaadin view:

- CRUD example for the BUP version 3.0 using Neo4j
- Integration with `Vaadin-flow` with the same `Spring security`.
- Use of the `mxGraph` library, for example defining new relationshps. This frameworks is like the `d3.js` but with
edit functionally (consider an easier framework to show graphs).
- Keeps in `sync` the bupNeo4j database with the iamDB using `kafka` when a new Company or Person is inserted.
- `BUP` query the operations using the `Graphql` library.

## Npm installation

Optional npm install mxGraph
```
npm install mxGraph
```

### Important notes for JS upgrade version for Vaadin flow views.

#### Vaadin flow .vs. Hilla

The way that Vaadin flow and Hilla support JS files are different.

##### Hilla JS files

In Hilla views (e.g., MailView) the JS files are loaded in the `/frontend/generated/jar-resources`
directory. The JS scripts came from the addons, or any library; in this case from
`third-party-org-vaadin-addon-visjs-network` library. These JS files "in the library" are stored in the directory
`resources/META-INF/frontend directory`.

##### Vaadin flow

For vaadin flow views there must be read using the`page.addJavaScript` statement

note: do not use `@JSMode`directive because it is loaded immediately (like , like CKEditor) and this does
not work properly for the `Blockly` library.

So the JS files are stored in `resources/META-INF/frontend/built` directory.

For more information see:
https://vaadin.com/docs/latest/advanced/loading-resources


## Kubernetes container

### Vaadin kubernetes kit

To use Vaadin in `kubernetes` it is best to use Vaadin add-on for `kubernetes`. For more
information see: https://vaadin.com/docs/latest/tools/kubernetes

note: the Kubernetes kit is commercial add on. If we do not want to use it, but still want
to deploy in Kubernetes see next section. The main objective of the Kubernetes kit is for
scaling up or down the UI and have high availability.

The Kubernetes Kit uses a combination of sticky sessions and session replication to 
enable scaling up or down, and high availability. Under normal circumstances, sticky 
sessions are used to route the same user always to the same node or pod. 

Kubernetes Kit is used to deploy Vaadin Flow applications on-premise or in the cloud
using Kubernetes. It helps in creating applications that are scalable, highly available,
and user-friendly. To elaborate, it enables the following:

- Horizontal scalability, saving on cloud costs by allowing applications to scale down
without impacting active user sessions and scale up when needed to meet your user and 
server needs.
- High availability, enabling users to continue their active sessions and keep using
your application even if a server fails.
- Non-disruptive rolling updates that don’t interfere with user sessions, thus reducing
the cost and inconvenience of after-hour deployments.

Serialization helpers that make it faster and easier to leverage fully horizontal scaling and fail-over.

Because Kubernetes in Vaadin is just for commercial license only, for the AI Legorreta
marketplace that it is 100% open-source it is not implemented for `Vaadin kubernetes kit` 
what is implemented is to be added inside the `kubernetes docker minicube` container. See
https://vaadin.com/blog/deploying-a-vaadin-app-to-kubernetes for more information.

### Vaadin kubernetes inside a minikube

It is assumed that you have the Kubernetes cluster from Docker Desktop running.

First build the Docker image for your application. You then need to make the Docker 
image available to you cluster. With Docker Desktop Kubernetes, this happens automatically. 
With Minikube, you can run `eval $(minikube docker-env)` and then build the image to 
make it available. 

The file `kubernetes.yaml` sets up a deployment with 2 pods (server instances) and a 
load balancer service. You can deploy the application on a Kubernetes cluster using:

```
kubectl apply -f kubernetes.yaml
```

If everything works, you can access your application by opening http://localhost:8190/.

note: Since this application is a User Interface application NO `gate-way` is used.

The load balancer port is defined in `kubernetes.yaml`.

Tip: If you want to understand which pod your requests go to, you can add the value
of `VaadinServletRequest.getCurrent().getLocalAddr()` somewhere in your UI.

#### Troubleshooting

If something is not working, you can try one of the following commands to see what is
deployed and their status.

```
kubectl get pods
kubectl get services
kubectl get deployments
```

If the pods say `Container image "iam-ui:latest" is not present with pull policy of Never` 
then you have not built your application using Docker or there is a mismatch in the
name. Use `docker images ls` to see which images are available.

If you need even more information, you can run

```
kubectl cluster-info dump
```

that will probably give you too much information but might reveal the cause of a problem.

If you want to remove your whole deployment and start over, run

```
kubectl delete -f kubernetes.yaml
```

## Docker container

### Compilation for local environment

This projects utilize `gradle` and no `maven`. For more information how to create a Vaadin application
with `gradle` see: https://vaadin.com/docs/latest/guide/start/gradle

### Running locally in development mode

If we want to run the SYS UI outside docker the process is as always. Inside the IntelliJ IDEA just run
the project and from the terminal. 

Running in development mode:

```bash
./gradlew clean
./gradlew clean bootRun
```
Run the following command in this repo, to create necessary Vaadin config files:
```
./gradlew clean vaadinPrepareFrontend
```
The build/vaadin-generated/ folder will now contain proper configuration files.

For more information see: https://hilla.dev/docs/lit/start/gradle#gradle-based-hilla-project-structure
where other gradle operation are described, like:

```
gradlew hillaConfigure
gradlew hillaGenerate
gradlew hillaInitApp
```

### Running locally in development mode

To run in develope mode is as simple as execute the gradle `bootRun`, but there is an issue
for vaadin tools: if we see the Chrome console it sends and error because CORS policy that 
cannot load Vaadin tools, which is ok for CORS policy. If we want to eliminate the CORS
security but JUST for development, we can initiate Chrome without CORS with the following 
command at terminal:

```bash
open -n -a /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --args --user-data-dir="/tmp/chrome_dev_test" --disable-web-security
```

note: Do not use this functionality in production mode. It is not necessary.


### Running locally in production mode

Vaadin needs to run in "Production mode" inside the docker and not in "Develop mode". For
more information of Vaadin modes see:

For more information see: https://hilla.dev/docs/lit/start/gradle#gradle-based-hilla-project-structure

To create a production build, call :

```bash
./gradlew -Philla.productionMode=true build
```

Or use the build.gradle.kts file as follows:

```
hilla {
   productionMode = false
}
```
This will build a JAR file with all the dependencies and front-end resources, ready to be deployed.
The file can be found in the `build/libs/` folder after the build completes.

Once the JAR file is built, you can run it using:

```bash
java -jar build/libs/acme-ui-2.0.0.jar
```

note : to store the micro-front in `Docker` you need to build `Vaadin Hilla` in production mode.


Wait for the application to start

Open http://localhost:8360/ to view the login panel for the  application.

### Load Balanced

Load balanced is just needed for `docker-compose` only because for `kubernetes` it native
support. To enabled load balance the property must be set to:

``acme-ui.kubernetes = false``

The back-end microservice, e.g., `bup-service` (or `expedienteservie` or any other) for `docker-compose` could have more
than one instance. For now just one instance is created For information about load balanced see the article:

https://www.linkedin.com/pulse/client-side-load-balance-spring-boot-microservice-docker-rodrigues/?published=t

The code is in:

https://github.com/rs-renato/service-disvovery


Following the guidance for this article we have:

- Eureka for the service discovery. Just for `docker-compose`.
- Gateway operates like a front UI, so is reads the service registry and does not net a health check.
- Any back-end microservice could be scaled to more than once.
- The `@Loadbalanced` is used in all services calls (i.e, web client instance)
- When a service is down (e.g., xxxxx-service1) we use Hystrics to catch the error.

### Run it inside the docker desktop

After the compilation in production mode was executed correctly build the docker image:

```bash
./gradlew bootBuildImage
```

Then change to the docker-compose directory

```bash
cd docker-platform-ui
docker-compose up -d
```

And to run the UI inside the docker:

```bash
docker-compose -f ./target/docker/docker-platform-acme-ui/docker-compose.yml up -d
```

Wait for the application to start

The application can be run inside the Docker desktop dashboard (recommended one).

Open http://localhost:8540/ to view the login panel for the  application.


## Chrome activate service worker for domains different from local host

user this link: chrome://flags/#unsafely-treat-insecure-origin-as-secure

## Useful links

- Read the documentation at [vaadin.com/docs](https://vaadin.com/docs).
- Follow the tutorial at [vaadin.com/docs/latest/tutorial/overview](https://vaadin.com/docs/latest/tutorial/overview).
- Create new projects at [start.vaadin.com](https://start.vaadin.com/).
- Search UI components and their usage examples at [vaadin.com/docs/latest/components](https://vaadin.com/docs/latest/components).
- View use case applications that demonstrate Vaadin capabilities at [vaadin.com/examples-and-demos](https://vaadin.com/examples-and-demos).
- Build any UI without custom CSS by discovering Vaadin's set of [CSS utility classes](https://vaadin.com/docs/styling/lumo/utility-classes).
- Find a collection of solutions to common use cases at [cookbook.vaadin.com](https://cookbook.vaadin.com/).
- Find add-ons at [vaadin.com/directory](https://vaadin.com/directory).
- Ask questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/vaadin) or join our [Discord channel](https://discord.gg/MYFq5RTbBn).
- Report issues, create pull requests in [GitHub](https://github.com/vaadin).


## Project structure

<table style="width:100%; text-align: left;">
  <tr><th>Directory</th><th>Description</th></tr>
  <tr><td><code>frontend/</code></td><td>Client-side source directory</td></tr>
  <tr><td>&nbsp;&nbsp;&nbsp;&nbsp;<code>index.html</code></td><td>HTML template</td></tr>
  <tr><td>&nbsp;&nbsp;&nbsp;&nbsp;<code>index.ts</code></td><td>Frontend 
entrypoint, bootstraps a React application</td></tr>
  <tr><td>&nbsp;&nbsp;&nbsp;&nbsp;<code>routes.tsx</code></td><td>React Router routes definition</td></tr>
  <tr><td>&nbsp;&nbsp;&nbsp;&nbsp;<code>MainLayout.tsx</code></td><td>Main 
layout component, contains the navigation menu, uses <a href="https://hilla.dev/docs/react/components/app-layout">
App Layout</a></td></tr>
  <tr><td>&nbsp;&nbsp;&nbsp;&nbsp;<code>views/</code></td><td>UI view 
components</td></tr>
  <tr><td>&nbsp;&nbsp;&nbsp;&nbsp;<code>themes/</code></td><td>Custom  
CSS styles</td></tr>
  <tr><td><code>src/main/java/&lt;groupId&gt;/</code></td><td>Server-side 
source directory, contains the server-side Java views</td></tr>ƒ
  <tr><td>&nbsp;&nbsp;&nbsp;&nbsp;<code>Application.java</code></td><td>Server entry-point</td></tr>
</table>

## Useful links

- Read the documentation at [hilla.dev/docs](https://hilla.dev/docs/).
- Report issues for `Vadin-hilla`, create pull requests in [GitHub](https://github.com/vaadin/hilla).

### Contact AI Legorreta

Feel free to reach out to AI Legorreta on [web page](https://legosoft.com.mx).


Version: 2.0.0
©LegoSoft Soluciones, S.C., 2023