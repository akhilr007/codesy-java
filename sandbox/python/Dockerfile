FROM python:3.12-slim

RUN useradd --create-home --uid 10001 sandbox

WORKDIR /workspace

COPY sandbox/common/runner.py /opt/codesy/runner.py
COPY sandbox/python/entrypoint.sh /opt/codesy/entrypoint.sh

RUN chmod +x /opt/codesy/runner.py /opt/codesy/entrypoint.sh

USER sandbox

ENTRYPOINT ["/opt/codesy/entrypoint.sh"]