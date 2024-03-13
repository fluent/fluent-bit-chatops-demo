cls
call mvn clean package
echo -- ready to run --
call java -jar target/flb-chatops.jar
