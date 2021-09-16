FROM docker-remote.artifacts.developer.gov.bc.ca/maven:3-jdk-17 as build
WORKDIR /workspace/app

COPY api/pom.xml .
COPY api/src src
RUN mvn package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM docker-remote.artifacts.developer.gov.bc.ca/openjdk:17-jdk
RUN useradd -ms /bin/bash spring
RUN mkdir -p /logs && mkdir -p /app
RUN chown -R spring:spring /logs && chown -R spring:spring /app
RUN chmod 755 /logs && chmod 755 /app
USER spring
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-Duser.name=PEN_NOMINAL_ROLL_API","-Xms500m","-Xmx500m","-noverify","-XX:TieredStopAtLevel=1","-XX:+UseParallelGC","-XX:MinHeapFreeRatio=20","-XX:MaxHeapFreeRatio=40","-XX:GCTimeRatio=4","-XX:AdaptiveSizePolicyWeight=90","-XX:MaxMetaspaceSize=300m","-XX:ParallelGCThreads=2","-Djava.util.concurrent.ForkJoinPool.common.parallelism=8","-XX:CICompilerCount=2","-XX:+ExitOnOutOfMemoryError","-Dspring.profiles.active=openshift","-Djava.security.egd=file:/dev/./urandom","-cp","app:app/lib/*","ca.bc.gov.educ.pen.nominalroll.api.NominalRollApiApplication"]
