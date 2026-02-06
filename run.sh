javac -d bin -cp "../../Java_libs/*" $(find src -name "*.java")
cp -r src/resources bin/
java -cp "../../Java_libs/*:bin" com.rebuild.RotatingCube
