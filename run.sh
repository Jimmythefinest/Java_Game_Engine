javac -d bin -cp "../../Java_libs/*" $(find src -name "*.java")
java -cp "../../Java_libs/*:bin" com.rebuild.RotatingCube
