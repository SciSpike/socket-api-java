mkdir -p src/main/resources
rm src/main/resources/*.pem
openssl req \
    -new \
    -newkey rsa:4096 \
    -days 3650 \
    -nodes \
    -x509 \
    -subj "/C=US/ST=Distributed/L=Cloud/O=Genesis/CN=*.amcstealth.com" \
    -keyout privatekey.pem \
    -out server-crt.pem