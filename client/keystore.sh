#!/bin/bash

echo "Please insert your alias:"
read a
echo "Please insert your clientId:"
read b
echo "Please insert your password"
read -s c

keytool -genkeypair -alias $a -keyalg RSA -keysize 2048 \
  -dname "CN=BFT-BANKING" -validity 365 -storetype PKCS12 \
  -keystore client_$b.p12 -storepass $c