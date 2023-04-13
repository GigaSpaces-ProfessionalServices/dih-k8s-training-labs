# Smart-DIH-K8s-training - lab-solution

## 	Service Development – Build a new service from scratch

###### Lab Goals
1.  Understand how to build a simple Spring Boot application that connects to a space.
2.  Understand how to deploy the new service on a k8s environment.
###### Lab Description
This lab includes one solution in which we will perform the tasks required to implement a new simple service. 
Use the slides from the lesson as a reference.
## 1 Lab setup
##### Prerequisites

###### Kubernetes
1. Make sure you have an EKS cluster created and `kubectl` and `helm` are installed and configured to connect to EKS.

The jumper box will have the ability to connect to EKS.

###### For testing the REST application in a local environment
1. You will need a local installation of GigaSpaces installed.
2. Maven should be installed.
3. `curl` should be installed.
4. Intellij Community Edition is recommended, but not required.

###### For preparation of the Docker image
1. Install [Docker](https://docs.docker.com/engine/install/)

## Test the REST API application on a local environment
It's useful to test locally first to ensure everything is running smoothly before deployment onto k8s.

1. Start a GigaSpaces demo application in your local desktop - `$GS_HOME/bin/gs.sh demo`
2. In a separate terminal window run:
```
cd dih-k8s-training-labs/lab-service-dev-solution;
mvn clean spring-boot:run -Dspring-boot.run.profiles=localhost
```
3. Open another terminal window and run the following (or use http://localhost:8080/swagger-ui.html):
```
curl -X POST localhost:8080/newtable -H 'Content-type:application/txt' -d 'CREATE TABLE Persons (ID int NOT NULL,LastName varchar(255) NOT NULL,FirstName varchar(255),Age int,PRIMARY KEY (ID))'
```
4. Go to the Ops-UI (localhost:8090). See type Persons was created.

5. Run in a terminal window:
```
curl -X POST localhost:8080/insert?tableName=Persons -H  'Content-type:application/json' -d '{"ID": 1, "LastName": "Levin", "FirstName" :"Avi", "Age":34 }'
curl -X POST localhost:8080/insert?tableName=Persons -H  'Content-type:application/json' -d '{"ID": 2, "LastName": "Choen", "FirstName" :"Roni", "Age":34 }'
```
6. View the data in the space using the Ops-UI.
7. Confirm the REST service is able to return results. Run in a terminal window:
```
curl -v localhost:8080/queryrs?tableName=Persons
```

## Preparing the REST Application for deployment in the k8s environment
### EKS cluster setup
1. Connect to the jumper. Refer to [OOTB-DIH-k8s-provisioning](https://github.com/GigaSpaces-ProfessionalServices/OOTB-DIH-k8s-provisioning) for k8s setup instructions.
2. Run: `./install-dih-umbrella.sh`

### Space creation
In the following steps, we will create a space named `demo` using the Ops-UI.

1. Locate in the External-IP of the XAP Manager load balancer service.
`kubectl get services`; Find the manager External IP address.
2. Open the Ops-UI. In a browser, go to `<xap manager external ip>:8090`
3. Click on 'Monitor my services'; Click on '+' icon in top right, Deploy space service (space) name: ‘demo’. Single partition with no backup is ok.

### Examine k8s environment variables
This Spring Boot application uses Spring Profiles to manage configurations specific to an environment.
1. Go to `dih-k8s-training-labs/lab-service-dev-solution/src/main/resources`. Notice there is a file `application-k8s.properties`, confirm the `space.manager property` is correct.
2. Examine the file `dih-k8s-training-labs/yamls/mydeployment.yaml. There is a ConfigMap section and spec.containers.env section that sets the environment variable for the active spring profile.
3. When Spring Boot is run, it will read the `application-k8s.properties`, because of the spring profile that is set.

###  Build & push image
Refer to: [Docker instructions for managing repositories](https://docs.docker.com/docker-hub/repos/#:~:text=To%20push%20an%20image%20to,docs%2Fbase%3Atesting%20)
1. Create a user in dockerhub
2. Go to the parent directory of the DockerFile: `cd dih-k8s-training-labs/lab-service-dev-solution`.

3. `docker build -t <your-hub-user>/<repo-name>[:<tag>] . ` (e.g : docker build -t atzd1/myrest:1.0.1 .)
4. `docker login`
5. `docker push <your-hub-user>/<repo-name>[:<tag>]` (e.g : docker push atzd1/myrest:1.0.1)

### Prepare deployment yamls and deploy service
1. Edit `mydeployment.yaml`. Change image to the image you built (e.g., `image: atzd1/mytest1:1.01`)
2. Copy the yaml files to your jumper.
3. In the jumper, `kubectl apply -f mydeployment.yaml`
4. Validate a new pod was created.
5. In jumper, `kubectl apply -f myservice.yaml`
6. Add annotations to keep the loadbalancer of the service up

   In the jumper run:
```
kubectl patch svc my-rest-api -p '{"metadata":{"annotations":{"service.beta.kubernetes.io/aws-load-balancer-additional-resource-tags":"Owner=owner,Project=gstm385,Name=my-rest-api"}}}'
```
## Verify

Run curl against the newly created k8s service
1. In jumper run, `kubectl get services` 
2. See the External IP for your service (my-rest-api service)
3. Repeat verification steps in the first section of this lab. Replace local host with External IP of the my-rest-api service. For example,
```
curl -v <External IP of my rest api service>:8080/queryrs?tableName=Persons
```
   
## Teardown
Remove the deployed service and space
1. In jumper, `kubectl delete service my-rest-api` ( kubectl delete service &lt; service-name &gt; )
2. In jumper, `kubectl delete deploy my-rest-api` ( kubectl delete deploy &lt; deploy-name &gt; )  
3. Open Ops-UI, navigate to demo service,  Undeploy service.

Note: Instead of using a k8s LoadBalancer service you can deploy as a ClusterIP service and use an ingress for access as explained in the northbound lab.

## End of Lab
