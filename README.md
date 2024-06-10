# SendMail bridge
El próposito de este código es leer la bandeja de correo de office 365 para enviarlo a un servidor de correo interno. Los correos se leen cada minuto. 

##Configuraciones
### Configuración intervalo de consulta de inbox
Este parametro se configura en la clase SenMailCommandLine con la variable de fixedRate, el valor esta expresado en milesegundos.

`@Scheduled(
fixedRate = 60000L
)`

### Perfiles
La aplicacion cuenta con la definicion de perfiles(profiles) de ejecución, esta definicion se da en los archivos 
`application-[perfil].properties
` ubicados en [src/resources] previo a compilar, en BOOT-INF/classes dentro del jar o si se quiere tener de manera externa es necesario especificar la carpeta dentro del classpath de java.

##Configuración de JAVA
El proyecto fue compilado para la version 1.8 de Java por lo cual se verificará que este activo en las variables de ambiente.

### Unix o Linux
`export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_301.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH:$CXF_HOME/bin`
### Windows
`set JAVA_HOME=
set PATH=%JAVA_HOME%\bin;%PATH%`

mvn clean install


# Ejecución con archivos de configuracion internos

`java -Dspring.profiles.active=qa -jar target/SendMail-2.0.1-SNAPSHOT.jar
`

###### Ejecución con archivos de configuracion externa
Tener en la misma carpeta el archivo .properties correspondiente al ambiente.

`
java -Dspring.profiles.active=prod -cp . -jar target/SendMail-2.0.1-SNAPSHOT.jar
`
`java -Dspring.profiles.active=qa -classpath .\config -jar SendMail-2.0.1-SNAPSHOT.jar
##Modificación de archivo properties para actualizar configuraciones
Para modificar los archivos properties internos se extrae el archivo dentro del jar, se edita y se actualiza.

1.Extraer el archivo properties si estamos en qa extraemos el archivo application-qa.properties

`jar -xvf SendMail-2.0.1-SNAPSHOT.jar   BOOT-INF/classes/application-prod.properties
`

2.Editar el archivo con los valores a modificar.
`vi BOOT-INF/classes/application-prod.properties
`

3.Actualizar el archivo properties que editamos dentro del jar.
`jar -uvf SendMail-2.0.1-SNAPSHOT.jar   BOOT-INF/classes/application-prod.properties
`
