FROM ubuntu:latest
LABEL authors="gerie"

ENTRYPOINT ["top", "-b"]