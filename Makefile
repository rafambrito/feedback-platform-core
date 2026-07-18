build-CollectorFunction:
	mvn -f pom.xml -pl feedback-collector -am clean package -DskipTests
	rm -rf $(ARTIFACTS_DIR)/*
	mkdir -p $(ARTIFACTS_DIR)/lib
	cp feedback-collector/target/quarkus-app/app/*.jar $(ARTIFACTS_DIR)/lib/
	cp feedback-collector/target/quarkus-app/lib/boot/*.jar $(ARTIFACTS_DIR)/lib/
	cp feedback-collector/target/quarkus-app/lib/main/*.jar $(ARTIFACTS_DIR)/lib/

build-NotifierFunction:
	mvn -f pom.xml -pl feedback-notifier -am clean package -DskipTests
	rm -rf $(ARTIFACTS_DIR)/*
	mkdir -p $(ARTIFACTS_DIR)/lib
	cp feedback-notifier/target/quarkus-app/app/*.jar $(ARTIFACTS_DIR)/lib/
	cp feedback-notifier/target/quarkus-app/lib/boot/*.jar $(ARTIFACTS_DIR)/lib/
	cp feedback-notifier/target/quarkus-app/lib/main/*.jar $(ARTIFACTS_DIR)/lib/

build-ReporterFunction:
	mvn -f pom.xml -pl feedback-reporter -am clean package -DskipTests
	rm -rf $(ARTIFACTS_DIR)/*
	mkdir -p $(ARTIFACTS_DIR)/lib
	cp feedback-reporter/target/quarkus-app/app/*.jar $(ARTIFACTS_DIR)/lib/
	cp feedback-reporter/target/quarkus-app/lib/boot/*.jar $(ARTIFACTS_DIR)/lib/
	cp feedback-reporter/target/quarkus-app/lib/main/*.jar $(ARTIFACTS_DIR)/lib/
