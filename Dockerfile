FROM eclipse-temurin:17-jdk

# Install sbt
RUN apt-get update && \
    apt-get install -y curl && \
    curl -L -o sbt.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-1.9.8.deb && \
    dpkg -i sbt.deb && \
    rm sbt.deb && \
    apt-get clean

WORKDIR /app

# Copy project files
COPY movies-provider-service/ .

# Expose both service ports
EXPOSE 9090 8081

# Run the application with Main class in interactive mode
CMD ["sbt", "-Djline.terminal=none", "runMain Main"]
