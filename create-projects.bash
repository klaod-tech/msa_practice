#!/usr/bin/env bash
mkdir -p microservices
cd microservices

create() {
  spring init \
  --boot-version=4.0.9 \
  --type=gradle-project \
  --java-version=21 \
  --packaging=jar \
  --name=$1 \
  --package-name=$2 \
  --groupId=$2 \
  --dependencies=actuator,webflux \
  --version=1.0.0-SNAPSHOT \
  $1
}

create product-service           se.magnus.microservices.core.product
create review-service            se.magnus.microservices.core.review
create recommendation-service    se.magnus.microservices.core.recommendation
create product-composite-service se.magnus.microservices.composite.product
