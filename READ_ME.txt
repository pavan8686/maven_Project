====
    QI Analyze is a Java EE framework for identifying and analyzing Quality Indicators
    Copyright (C) 2010  QIAnalyze LLC http://www.qianalyze.org

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
    Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see http://www.gnu.org/licenses/
====

Congratulations on starting your own QiAnalyze Module!

This file will help guide you through the process of getting your getting your own Quality Indicator working. 

Here are the suggested operations to follow. 

Make sure your infrastructure is in-place:
	1) Install QiAnalyze to your JBoss or Glassfish server.
		Insturctions can be found in the QiAnalyze project's readme.txt 
		http://java.net/projects/qianalyze
		
	2) Compile this example as it is to insure that all is working correctly. 
	    a) Set the <activeByDefault>true</activeByDefault> value for either the JBoss or Glassfish profile in pom.xml 
	    b) Run "mvn clean package" to generate the pavan.war
	    c) Copy the war to the deploy directory of your JEE server and launch. 
	    d) Using the SOAPUI tool ( http://www.soapui.org/ ) import the src/test/soapUI/sample-soapui-project.xml file and run the test. 
	    
Create your own data model. 
	1) Replace the current_observation.xsd with your own schema. 
	2) Using either the Sailing.java or the drools files as examples define the criteria for your Quality Indicator. 
	3) Run "mvn clean package" again to create your entity beans using HyperJaxb and build your war file. 
	
Please keep in touch with the QiAnalyze community to learn about improvements and share your work. 

Matthew Demerath 
QiAnalyze Founder. 
