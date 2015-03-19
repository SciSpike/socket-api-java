mkdir -p src/main/resources
rm src/main/resources/*.pem
openssl genrsa 2048 > src/main/resources/privatekey.pem
openssl req -new -key src/main/resources/privatekey.pem -out src/main/resources/csr.pem
openssl x509 -req -days 3650 -in src/main/resources/csr.pem -signkey src/main/resources/privatekey.pem -out src/main/resources/server-crt.pem