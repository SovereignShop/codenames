FROM adoptopenjdk/openjdk11:x86_64-ubuntu-jdk-11.0.3_7-slim
ADD target/lib/lib app/lib
ADD target/codenames-0.0.1-SNAPSHOT.jar app/
RUN mkdir /var/lib/codenames
WORKDIR /app
CMD java $JAVA_OPTS -cp "codenames-0.0.1-SNAPSHOT.jar:lib/*" codenames.core
EXPOSE 3000
