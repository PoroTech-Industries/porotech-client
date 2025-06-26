MAIN_CLASS="com.porotech_industries.porocar.App"
JAR_NAME="porotech-client-1.0-SNAPSHOT.jar"
DEPENDENCY_FOLDER="target/dependency"

java -cp target/"$JAR_NAME":"$DEPENDENCY_FOLDER/*" "$MAIN_CLASS"
