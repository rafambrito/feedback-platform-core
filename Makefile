build-CollectorFunction:
	mvn clean package -f feedback-collector/pom.xml
	cp feedback-collector/target/*.jar $(ARTIFACTS_DIR)

build-NotifierFunction:
	mvn clean package -f feedback-notifier/pom.xml
	cp feedback-notifier/target/*.jar $(ARTIFACTS_DIR)

build-ReporterFunction:
	mvn clean package -f feedback-reporter/pom.xml
	cp feedback-reporter/target/*.jar $(ARTIFACTS_DIR)
