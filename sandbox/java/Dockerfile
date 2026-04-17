FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 sandbox

WORKDIR /workspace

COPY sandbox/common/runner.py /opt/codesy/runner.py
COPY sandbox/java/entrypoint.sh /opt/codesy/entrypoint.sh

RUN chmod +x /opt/codesy/runner.py /opt/codesy/entrypoint.sh

USER sandbox

ENTRYPOINT ["/opt/codesy/entrypoint.sh"]