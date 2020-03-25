FROM adoptopenjdk/openjdk11:x86_64-ubuntu-jdk-11.0.3_7-slim
ADD target/lib/lib app/lib
ADD target/codenames-0.0.1-SNAPSHOT.jar app/
WORKDIR /app
ENTRYPOINT ["asEnvUser"]
CMD /bin/bash -c 'source /opt/shining_software/use_repo.sh && java $JAVA_OPTS -cp "assist-analysis-0.0.1-SNAPSHOT.jar:lib/*" assist_analysis.core'
EXPOSE 3000
