build-CollectorFunction:
	mvn -f pom.xml -pl feedback-collector -am clean package -DskipTests
	cp feedback-collector/target/*.jar $(ARTIFACTS_DIR)

build-NotifierFunction:
	mvn -f pom.xml -pl feedback-notifier -am clean package -DskipTests
	cp feedback-notifier/target/*.jar $(ARTIFACTS_DIR)

build-ReporterFunction:
	mvn -f pom.xml -pl feedback-reporter -am clean package -DskipTests
	cp feedback-reporter/target/*.jar $(ARTIFACTS_DIR)
