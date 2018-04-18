# Loystar #

## Android application for Loystar merchants ###


### Project Setup ###

* Clone the repository unto your machine
* The link to the API docs is [here](https://loystar-api.herokuapp.com/docs)
* Dependencies
	1. [RxJava](https://github.com/ReactiveX/RxJava) for reactive programming in java
	1. [Retrofit](http://square.github.io/retrofit/) library as a REST Client 
	1. [Jackson Databind](https://github.com/FasterXML/jackson-databind) library  for streaming json content from the API
	1. [Requery](https://github.com/requery/requery) library as Android ORM library 
	
* Database configuration
	1. Database in use on the API is `postgresql`
	1. Connection details for the test database is as follows:
		* host: ec2-54-243-148-160.compute-1.amazonaws.com
		* Database Name: d2t25n924amf8j
		* Username: dkmidvnqwqsplj
		* Password: 12fb19b279c4c6b56676e08dd240f09a2e225e3bcfcbb02f08c8f6547fc93f20
		
* Signing your builds
	1. To successfully sign your builds
		1. download app signing key [here](https://drive.google.com/open?id=15-8w5U_7WBfSa-98ACqwQdJGHBrmLpaR)
		1. inside your `build.gradle` file, under `signingConfigs` change the `storeFile` to reference the location of downloaded app signing key