### Docker Build instructions

1. Compile with Gradle
2. `cd standalone/loader/build/libs`
3. `cp LuckPerms-*.jar luckperms-standalone.jar`
4. `docker build . -t luckperms -f ../../../docker/Dockerfile`
