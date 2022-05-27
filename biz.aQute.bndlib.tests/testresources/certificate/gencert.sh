keytool -genkeypair -alias test -keyalg EC -sigalg SHA384withECDSA -validity 30000 -keystore ../keystore -keypass testtest -storepass testtest -dname "CN=John Smith,O=ACME Inc,OU=ACME Cert Authority,L=Austin,ST=Texas,C=US"
# keytool -exportcert -alias test -file cert.crt -storepass testtest -keystore ../keystore
