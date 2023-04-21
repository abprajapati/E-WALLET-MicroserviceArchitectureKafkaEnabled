# E-WALLET-MicroserviceArchitectureKafkaEnabled
# Project
It is a microservice architecture-based RESTful Web Service with Kafka enabled to communicate between micro-services. e-wallet application facilitates online payment. Users can register to the application and can transfer the money to the existing user. Each user receives a sing up bonus upon registration. Depending on the transaction status, an email will be sent to the participant of the transaction. If the transaction is successful, an email will be sent to the sender and receiver. However, if the transaction gets failed then an email will be sent to the sender only.
# Used Skills, Technologies and Tools in Developing this Application
1) Java 
2) IntelliJ
3) Maven
4) Spring Boot
5) RESTful API
6) MVC
7) Dependency Injection
8) Inversion of Control   
9) Postman(API Testing)
10) JDBC
11) JPA
12) Hibernate(ORM)
13) MySQL(Persistant Storage)
14) Spring Security
15) Redis Caching
16) Microservices Architecture
17) Kafka(Massaging Queue)
# Functioning of Application
This application has four microservices such as user-service, wallet-service, transaction-service, and notification-service. As all the microservices run independently meaning on a different server and have different persistent storage, the shut-down of one microservice due to a technical issue does not stop the whole application. Only the functionality related to that microservice will be affected and the rest of the application would be running perfectly fine. 
 
Communication between microservices happens on the Kafka server. One microservice produces messages on a particular topic on the Kafka server and other microservices will consume that massage. The picture below shows how our microservices communicate which each other over the kafka server on different topics. ![Kafka](https://user-images.githubusercontent.com/106176892/233527330-a3f63f3f-097b-4aa8-b370-20458ca075d8.png)

In this application, every microservice plays different roles for every functionality and each of them have spring security enabled.

# 1)user-service: 

It has four RESTful API
1) @PostMapping("/user"): responsible for the signing up process. While the registration process is going on, the user-service produces the massage over the Kafka server and that will be consumed by the wallet-service. Once the wallet-service consumes a massage, a wallet, linked with a phone number, will be created, and sing up bonus will be credited to a wallet.
2) @GetMapping("/user"): responsible for getting the information of the authenticated and authorized user.
3) @GetMapping("/admin/all/users"): authenticated and authorized admin can view all the users of the system
4) @GetMapping("/admin/user/{userId}"): authenticated and authorized admin can view details of perticular user based on userId

# 2)wallet-service:

This microservice does not expose any REST APIs. It just produces and consumes the Kafka messages and performs the operation based on them.

# 3) transaction-service:

It has only one API:
1) @PostMapping("/txn"): It triggers the transaction between two candidates. When the User sends money to another user, firstly, the transaction will be in pending status. Then, it will produce the massage over Kafka and wallet-service will consume it and update the wallet. After that, wallet-service will produce a massage over Kafka saying the wallet is updated successfully and then transaction-service will consume that massage and update the transaction status. After all, transaction-service will produce a massage over a Kafka and that will be consumed by notification-service.

# 4) notification-service:

This service sends an email to the sender if the transaction gets failed. And if the transaction is successful then an email will be sent to the sender as well as the reciever. 


