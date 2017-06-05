### **Quick Start:** Steps for setting up SYNC application in local

1.	Download JDK 7 64 bit from the from below link and install it with default configuration.
http://www.oracle.com/technetwork/java/javase/downloads/java-archive-downloads-javase7-521261.html

2.	Download apache tomcat 64 bit from the below link and install it with default configuration
https://tomcat.apache.org/download-70.cgi

3.	Download MongoDB 3.2 from the below link and install it with default configuration.
https://www.mongodb.com/mongodb-3.2

4.	Now we need to setup SYNC_HOME for our application, for that we need to follow below steps:-

       a) create a folder names “sync” and in that folder create another folder named “db-props”

      b) create a file “local.properties” in “db-props” and it should contain the below details:
            
            dbname=syncdev
            username=syncApp
            password=<Use your password>
            port=27017
            host=<your computer name*>
            *you will find this in Control Panel\All Control Panel Items\System

      c) Now in “sync” folder we need to add a file “sync.conf” which should contain below    details:
      
          LIFE=local
          APP_ID=local
          LOGGING_LEVEL=INFO
          SYNC_USER = <your preferred user name>

      d) Now we need to add one more file in “sync” naming “applicationContext.xml” which should contain below details:
      
      
      <?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:context="http://www.springframework.org/schema/context"
	xmlns:aop="http://www.springframework.org/schema/aop" xmlns:mvc="http://www.springframework.org/schema/mvc"
	xmlns:jee="http://www.springframework.org/schema/jee"
	xsi:schemaLocation="
        http://www.springframework.org/schema/mvc http://www.springframework.org/schema/mvc/spring-mvc.xsd
        http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd
        http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd
        http://www.springframework.org/schema/jee http://www.springframework.org/schema/jee/spring-jee-4.0.xsd">

	<context:annotation-config />
	<mvc:default-servlet-handler />
	<bean id="ds"
		class="oracle.jdbc.pool.OracleDataSource">		
		<property name="URL" value="#TNS-STRING#" /><!-- Replace with correct TNS entry for your db -->
		<property name="user" value="#USERNAME#" /><!-- Username for your db -->
		<property name="connectionCachingEnabled" value="true" />
		<property name="connectionCacheProperties">
            <props> 
                <prop key="maxActive">30</prop> 
                <prop key="initialSize">2</prop> 
                <prop key="maxIdle">2</prop> 
                <prop key="minIdle">2</prop> 
                <prop key="maxWait">20000</prop>
                <prop key="jmxEnabled">true</prop> 
                <prop key="factory">oracle.jdbc.pool.OracleDataSourceFactory</prop> 
                <prop key="validationQuery">select 1 from dual</prop> 
                <prop key="testOnBorrow">true</prop>
				<prop key="logAbandoned">true</prop> 
                <prop key="testOnConnect">true</prop> 
                <prop key="suspectTimeout">300</prop> 
                <prop key="accessToUnderlyingConnectionAllowed">true</prop>
				<prop key="removeAbandoned">true</prop> 
                <prop key="removeAbandonedTimeout">1800</prop> 
                <prop key="timeBetweenEvictionRunsMillis">180000</prop> 
                <prop key="minEvictableIdleTimeMillis">180000</prop>
				<prop key="validationInterval">200</prop> 
                <prop key="initSQL">select 1 from dual</prop>
            </props> 
        </property>
	</bean>  
	
	<bean id="#DBNAME#" class="org.springframework.jdbc.core.JdbcTemplate" lazy-init="true"> <!-- Replace with your DBname -->
		<property name="dataSource">
			<ref bean="ds" />
		</property>
	</bean>

	<bean id="applicationContextProvder"
		class="com.cisco.app.dbmigrator.migratorapp.config.ApplicationContextProvider"/>
</beans>


   e) Now create/modify mailer.properties file & add the following entries.
   
			mail.smtp.host=<Your outbound mail server>
			protocol=smtp
			mail.smtp.port=25
			fromAlias=donotreply@sync.com
			teamAlias=<Notification email alias or comma separated email ids> 
			
			
   f) Now we need to add environment variable “SYNC_HOME” with value <path to “sync” folder>
            
	    
	    
5.	Now we need to configure mongoDB on local system, below are the steps:-

    a) we need to add mongo path in env variables, in environment variable add path up     to bin folder in mongo installation.

    b) Open command prompt and type “mongod” and press enter. This will start mongoDB.

    c) Now in another command prompt instance type “mongo” and press enter, this will start mongo shell. Now in mongo shell type “use syncdev” and press enter. This will create a DB named “syncdev”

    d) Now in shell execute below command:

        db.createUser(
           {
              user: "syncApp",
              pwd: <Use your password>,
              roles: [ { role: "readWrite", db: "syncdev" }]
           }
            )

    e) Now in shell execute the below commands:
  
        db.createCollection("SyncUserDetail");

        db.SyncUserDetail.insert({"_id" : "<your User ID>", "sourceDbMap" : {"<RDBMS DB Name>" : [  "<RDBMS DB Username>" ]}, "targetDbMap" : {"<Mongo DB Name>" : [ "<Mongo DB Username>" ]}, "roles" : ["USER","APPOVER"], "team" : "ADMIN"})

        db.createCollection("SyncConnectionInfo");

        db.SyncConnectionInfo.insert({"dbName" : "<Mongo DB Name>","userName" : "<Mongo DB Username>","password" : "<password>", "hostToPortMap" : [{"host" : "<mongo host>","port" : <mongo port>}],  "dbType" : "Mongo"})

        db.SyncConnectionInfo.insert(  {"dbName" : "<RDBMS DB name>","userName" : "<RDBMS Username>","password" : "<password>"})

        db.createCollection("SyncNodeMapping")

        db.SyncNodeMapping.insert({"host" : "<Computer name>","node" : "local","state" : "AVAILABLE", "concurrencyLevel" : 10, "usedHeapSize" : NumberLong(253755392), "activeEvents" : [], "eventTypes" : ["OrclToMongo",   "MongoToOrcl","MongoToOrclSync", "OrclToMongoSync", "OrclToMongoGridFs"], "systemEvents" : [],"appId" : "local","lastPingTime" : NumberLong(1495199702910) });

        db.createCollection("SyncEvents")

        db.createCollection("SyncMappings")

6.	Now paste the CiscoSync.war file in C:\Program Files\Apache Software Foundation\Tomcat 7.0\webapps folder.

7.	Now go to C:\Program Files\Apache Software Foundation\Tomcat 7.0\bin and double click on “Tomcat7”, this will start the server.
8.	Now open your browser and type localhost:8080/CiscoSync/ , This will open the Sync homepage.

 
