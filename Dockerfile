FROM artifacts.developer.gov.bc.ca/docker-remote/maven:3-jdk-11 as build
WORKDIR /workspace/app

COPY api/pom.xml .
COPY api/src src
RUN mvn package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM artifacts.developer.gov.bc.ca/docker-remote/openjdk:11-jdk
RUN useradd -ms /bin/bash spring
RUN mkdir -p /logs && mkdir -p /temp
RUN chown -R spring:spring /logs && chown -R spring:spring /temp
RUN chmod 755 /logs && chmod 755 /temp
USER spring
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-Duser.name=PEN_NOMINAL_ROLL_API","-Xms600m","-Xmx600m","-noverify","-XX:TieredStopAtLevel=1","-XX:+UseParallelGC","-XX:MinHeapFreeRatio=20","-XX:MaxHeapFreeRatio=40","-XX:GCTimeRatio=4","-XX:AdaptiveSizePolicyWeight=90","-XX:MaxMetaspaceSize=300m","-XX:ParallelGCThreads=2","-Djava.util.concurrent.ForkJoinPool.common.parallelism=8","-XX:CICompilerCount=2","-XX:+ExitOnOutOfMemoryError","-Dspring.profiles.active=openshift","-Djava.security.egd=file:/dev/./urandom","-cp","app:app/lib/*","ca.bc.gov.educ.pen.nominalroll.api.NominalRollApiApplication"]
